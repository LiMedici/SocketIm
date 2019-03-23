package com.mrmedici.clink.impl.async

import com.mrmedici.clink.core.*
import com.mrmedici.clink.utils.CloseUtils
import java.io.IOException
import java.nio.channels.Channels
import java.nio.channels.ReadableByteChannel
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean

class AsyncSendDispatcher(private val sender: Sender) : SendDispatcher,
        IoArgsEventProcessor, AsyncPacketReader.PacketProvider {

    private val queue = ConcurrentLinkedQueue<SendPacket<*>>()
    private val isSending = AtomicBoolean(false)
    private val isClosed = AtomicBoolean(false)

    private val reader = AsyncPacketReader(this)
    private val queueLock = Any()

    init {
        sender.setSendListener(this)
    }

    override fun send(packet: SendPacket<*>) {
        synchronized(queueLock){
            queue.offer(packet)
            if (isSending.compareAndSet(false, true)) {
                if(reader.requestTakePacket()){
                    requestSend()
                }
            }
        }
    }

    override fun cancel(packet: SendPacket<*>) {
        var ret = false
        synchronized(queueLock){
            ret = queue.remove(packet)
        }
        if(ret){
            packet.cancel()
            return
        }

        reader.cancel(packet)
    }

    override fun takePacket(): SendPacket<*>? {
        var packet: SendPacket<*>? = null
        synchronized(queueLock){
            packet = queue.poll()
            if(packet == null){
                // 队列为空，取消发送状态
                isSending.set(false)
                return null
            }
        }

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
        try {
            sender.postSendAsync()
        } catch (e: IOException) {
            closeAndNotify()
        }
    }


    private fun closeAndNotify() {
        CloseUtils.close(this)
    }

    override fun close() {
        if (isClosed.compareAndSet(false, true)) {
            isSending.set(false)
            // 异常关闭导致的完成
            CloseUtils.close(reader)
        }
    }

    override fun provideIoArgs(): IoArgs? {
        return reader.fillData()
    }

    override fun onConsumerFailed(args: IoArgs?, e: Exception) {
        if(args != null) {
            e.printStackTrace()
        }else{
            // TODO 后续补充
        }
    }

    override fun onConsumerCompleted(args: IoArgs) {
        // 继续发送当前包
        if(reader.requestTakePacket()){
            requestSend()
        }
    }
}