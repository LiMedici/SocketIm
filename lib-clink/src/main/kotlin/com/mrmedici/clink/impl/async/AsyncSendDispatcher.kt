package com.mrmedici.clink.impl.async

import com.mrmedici.clink.core.*
import com.mrmedici.clink.utils.CloseUtils
import java.io.IOException
import java.nio.channels.Channels
import java.nio.channels.ReadableByteChannel
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean

class AsyncSendDispatcher(private val sender:Sender) : SendDispatcher, IoArgsEventProcessor{

    private val queue = ConcurrentLinkedQueue<SendPacket<*>>()
    private val isSending = AtomicBoolean(false)
    private val isClosed = AtomicBoolean(false)

    private val ioArgs = IoArgs()
    private var currentPacket:SendPacket<*>? = null

    // 当前发送Packet的大小与进度
    private var packetChannel:ReadableByteChannel? = null
    private var total:Long = 0
    private var position:Long = 0

    init {
        sender.setSendListener(this)
    }

    override fun send(packet: SendPacket<*>) {
        queue.offer(packet)
        if(isSending.compareAndSet(false,true)){
            sendNextPacket()
        }
    }

    override fun cancel(packet: SendPacket<*>) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    private fun sendNextPacket(){
        val temp:SendPacket<*>? = currentPacket
        if(temp != null){
            CloseUtils.close(temp)
        }

        val packet = takePacket()
        this.currentPacket = packet

        if(packet == null){
            // 队列为空，取消发送状态
            isSending.set(false)
            return
        }

        total = packet.length()
        position = 0

        sendCurrentPacket()
    }

    private fun takePacket():SendPacket<*>?{
        val packet:SendPacket<*>? = queue.poll()
        if(packet != null && packet.isCanceled()){
            // 已取消不用发送
            return takePacket()
        }

        return packet
    }

    private fun sendCurrentPacket(){
        if(position >= total){
            completePacket(position == total)
            sendNextPacket()
            return
        }

        try {
            sender.postSendAsync()
        }catch (e:IOException){
            closeAndNotify()
        }
    }

    private fun completePacket(isSucceed:Boolean){
        val packet:SendPacket<*>? = this.currentPacket ?: return

        packet?.let{
            CloseUtils.close(it)
        }

        packetChannel?.let {
            CloseUtils.close(it)
        }

        currentPacket = null
        packetChannel = null

        total = 0
        position = 0

    }

    private fun closeAndNotify(){
        CloseUtils.close(this)
    }

    override fun close() {
        if(isClosed.compareAndSet(false,true)){
            isSending.set(false)
            // 异常关闭导致的完成
            completePacket(false)
        }
    }

    override fun provideIoArgs(): IoArgs {
        val args = ioArgs


        if(packetChannel == null){
            packetChannel = Channels.newChannel(currentPacket!!.open())
            args.limit(4)
            args.writeLength(currentPacket!!.length().toInt())
        }else{
            args.limit(Math.min(args.capacity().toLong(),total - position).toInt())

            try {
                val count = args.writeFrom(packetChannel!!)
                position += count
            }catch (e:IOException){
                e.printStackTrace()
                // 这里写入失败了
                completePacket(false)
                // return null
            }
        }

        return args
    }

    override fun onConsumerFailed(args: IoArgs, e: Exception) {
        e.printStackTrace()
    }

    override fun onConsumerCompleted(args: IoArgs) {
        // 继续发送当前包
        sendCurrentPacket()
    }
}