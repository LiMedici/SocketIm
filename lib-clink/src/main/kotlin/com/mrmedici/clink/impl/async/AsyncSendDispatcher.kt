package com.mrmedici.clink.impl.async

import com.mrmedici.clink.core.*
import com.mrmedici.clink.utils.CloseUtils
import java.io.IOException
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean

class AsyncSendDispatcher(private val sender: Sender) : SendDispatcher,
        IoArgsEventProcessor, AsyncPacketReader.PacketProvider {

    private val queue = ConcurrentLinkedQueue<SendPacket<*>>()
    private val isSending = AtomicBoolean(false)
    private val isClosed = AtomicBoolean(false)

    private val reader = AsyncPacketReader(this)

    init {
        sender.setSendListener(this)
    }

    override fun send(packet: SendPacket<*>) {
        queue.offer(packet)
        requestSend()
    }

    override fun sendHeartbeat() {
        if(queue.size > 0){
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

    private fun requestSend() {
        synchronized(isSending){
            if(isSending.get().or(isClosed.get())){
                return
            }

            if(reader.requestTakePacket()){
                try {
                    isSending.set(true)
                    val isSucceed = sender.postSendAsync()
                    if(!isSucceed){
                        isSending.set(false)
                    }
                } catch (e: IOException) {
                    closeAndNotify()
                }
            }
        }
    }


    private fun closeAndNotify() {
        CloseUtils.close(this)
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

    override fun onConsumerFailed(args: IoArgs?, e: Exception) {
        e.printStackTrace()
        synchronized(isSending){
            isSending.set(false)
        }

        // 继续请求发送当前的数据
        requestSend()
    }

    override fun onConsumerCompleted(args: IoArgs) {
        // 继续发送当前包
        synchronized(isSending){
            isSending.set(false)
        }

        // 继续请求发送当前的数据
        requestSend()
    }
}