package com.mrmedici.clink.impl.async

import com.mrmedici.clink.core.*
import com.mrmedici.clink.utils.CloseUtils
import java.io.IOException
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean

class AsyncSendDispatcher(private val sender:Sender) : SendDispatcher{

    private val queue = ConcurrentLinkedQueue<SendPacket>()
    private val isSending = AtomicBoolean(false)
    private val isClosed = AtomicBoolean(false)

    private val ioArgs = IoArgs()
    private var currentPacket:SendPacket? = null

    // 当前发送Packet的大小与进度
    private var total:Int = 0
    private var position:Int = 0

    override fun send(packet: SendPacket) {
        queue.offer(packet)
        if(isSending.compareAndSet(false,true)){
            sendNextPacket()
        }
    }

    override fun cancel(packet: SendPacket) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    private fun sendNextPacket(){
        val temp:SendPacket? = currentPacket
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

    private fun takePacket():SendPacket?{
        val packet:SendPacket? = queue.poll()
        if(packet != null && packet.isCanceled()){
            // 已取消不用发送
            return takePacket()
        }

        return packet
    }

    private fun sendCurrentPacket(){
        val args = ioArgs

        // 开始，清理
        args.startWriting()

        if(position >= total){
            sendNextPacket()
            return
        }else if(position == 0){
            // 首包，需要携带长度信息
            args.writeLength(total)
        }

        currentPacket?.let{
            val bytes = it.bytes()
            // 把bytes的数据写入到IoArgs
            val count = args.writeFrom(bytes,position)
            position += count
        }

        // 完成封装
        args.finishWriting()

        try {
            sender.sendAsync(args, ioArgsEventListener)
        }catch (e:IOException){
            closeAndNotify()
        }
    }

    private fun closeAndNotify(){
        CloseUtils.close(this)
    }

    override fun close() {
        if(isClosed.compareAndSet(false,true)){
            isSending.set(false)
            val packet = currentPacket
            if(packet != null){
                currentPacket = null
                CloseUtils.close(packet)
            }
        }
    }

    private val ioArgsEventListener = object : IoArgsEventListener{
        override fun onStarted(args: IoArgs) {

        }

        override fun onCompleted(args: IoArgs) {
            // 继续发送当前包
            sendCurrentPacket()
        }
    }
}