package com.mrmedici.clink.impl.async

import com.mrmedici.clink.box.StringReceivePacket
import com.mrmedici.clink.core.*
import com.mrmedici.clink.utils.CloseUtils
import java.io.IOException
import java.util.concurrent.atomic.AtomicBoolean

class AsyncReceiveDispatcher(private val receiver:Receiver,
                             private val callback:ReceiveDispatcher.ReceivePacketCallback) : ReceiveDispatcher{

    private val ioArgsEventListener = object : IoArgsEventListener{
        override fun onStarted(args: IoArgs) {
            var receiveSize = if(receivePacket == null) 4
            else Math.min(total - position,args.capacity())
            // 设置本次接收数据大小
            args.limit(receiveSize)
        }

        override fun onCompleted(args: IoArgs) {
            assemblePacket(args)
            // 继续接收下一条数据
            registerReceive()
        }
    }

    private val isClosed = AtomicBoolean(false)

    private val ioArgs = IoArgs()
    private var receivePacket:ReceivePacket? = null
    private var buffer:ByteArray? = null
    private var total:Int = 0
    private var position:Int = 0


    init {
        receiver.setReceiveListener(ioArgsEventListener)
    }


    override fun start() {
        registerReceive()
    }

    override fun stop() {

    }

    override fun close() {
        if(isClosed.compareAndSet(false,true)){
            val packet = receivePacket
            if(packet != null){
                receivePacket = null
                CloseUtils.close(packet)
            }
        }
    }

    private fun closeAndNotify(){
        CloseUtils.close(this)
    }

    private fun registerReceive(){
        try {
            receiver.receiveAsync(ioArgs)
        }catch (e:IOException){
            closeAndNotify()
        }
    }

    /**
     * 解析数据到Packet
     */
    private fun assemblePacket(args: IoArgs){
        if(receivePacket == null){
            val length = args.readLength()
            receivePacket = StringReceivePacket(length)
            buffer = ByteArray(length)
            total = length
            position = 0
        }

        val count = args.readTo(buffer!!,0)
        if(count > 0 && receivePacket != null){
            receivePacket!!.save(buffer!!,count)
            position += count

            // 检查是否已完成一份Packet接收
            if(position == total){
                completePacket()
                receivePacket = null
            }
        }
    }

    /**
     * 完成数据接收操作
     */
    private fun completePacket() {
        val packet = receivePacket
        packet?.let {
            CloseUtils.close(it)
            callback.onReceivePacketCompleted(it)
        }
    }
}