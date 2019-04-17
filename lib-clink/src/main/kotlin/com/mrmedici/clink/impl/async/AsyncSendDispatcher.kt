package com.mrmedici.clink.impl.async

import com.mrmedici.clink.core.*
import com.mrmedici.clink.impl.exceptions.EmptyIoArgsException
import com.mrmedici.clink.utils.CloseUtils
import java.io.IOException
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean

class AsyncSendDispatcher(private val sender: Sender) : SendDispatcher,
        IoArgsEventProcessor, AsyncPacketReader.PacketProvider {

    private val queue = ArrayBlockingQueue<SendPacket<*>>(16)
    private val isSending = AtomicBoolean(false)
    private val isClosed = AtomicBoolean(false)

    private val reader = AsyncPacketReader(this)

    init {
        sender.setSendListener(this)
    }

    override fun send(packet: SendPacket<*>) {
        try {
            queue.put(packet)
            requestSend()
        }catch (e:InterruptedException){
            e.printStackTrace()
        }
    }

    override fun sendHeartbeat() {
        if(!queue.isEmpty()){
            return
        }

        if(reader.requestSendHeartbeatFrame()){
            requestSend()
        }
    }

    override fun cancel(packet: SendPacket<*>) {
        var ret = queue.remove(packet)
        if (ret) {
            packet.cancel()
            return
        }

        reader.cancel(packet)
    }

    override fun takePacket(): SendPacket<*>? {
        var packet: SendPacket<*>? = queue.poll() ?: return null

        if (packet!!.isCanceled()) {
            // 已取消不用发送
            return takePacket()
        }

        return packet
    }

    override fun completedPacket(packet: SendPacket<*>, isSucceed: Boolean) {
        CloseUtils.close(packet)
    }

    private fun requestSend(callFromIoConsume:Boolean = false) {
        synchronized(isSending){
            val isRegisterSending = this.isSending
            val oldState = isRegisterSending.get()
            if(isClosed.get() || (oldState && !callFromIoConsume)){
                return
            }

            if(callFromIoConsume && !oldState){
                throw IllegalStateException("Call from IoConsume, current state should in sending!")
            }

            if(reader.requestTakePacket()){
                try {
                    isRegisterSending.set(true)
                    sender.postSendAsync()
                }catch (e:Exception){
                    e.printStackTrace()
                    CloseUtils.close(this)
                }
            }else{
                isRegisterSending.set(false)
            }
        }
    }

    override fun close() {
        if (isClosed.compareAndSet(false, true)) {
            // 异常关闭导致的完成
            CloseUtils.close(reader)
            // 清理队列
            queue.clear()

            synchronized(isSending){
                isSending.set(false)
            }
        }
    }

    override fun provideIoArgs(): IoArgs? {
        return if(isClosed.get()) null
        else reader.fillData()
    }

    override fun onConsumerFailed(e: Throwable):Boolean {
        return if(e is EmptyIoArgsException){
            // 继续请求发送当前的数据
            requestSend(true)
            false
        }else{
            CloseUtils.close(this)
            true
        }
    }

    override fun onConsumerCompleted(args: IoArgs):Boolean {
        // 继续发送当前包
        synchronized(isSending){
            val isRegisterSending = this.isSending
            val isRunning = !isClosed.get()
            if(!isRegisterSending.get() && isRunning){
                throw IllegalStateException("Call from IoConsume, current state should in sending!")
            }

            // TODO 这里有疑问，可能是一个Packet或一个Frame只消费一个Args,未消费完成
            isRegisterSending.set(isRunning && reader.requestTakePacket())

            return isRegisterSending.get()
        }
    }
}