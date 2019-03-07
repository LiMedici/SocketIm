package com.mrmedici.clink.impl.async

import com.mrmedici.clink.box.StringReceivePacket
import com.mrmedici.clink.core.*
import com.mrmedici.clink.utils.CloseUtils
import java.io.IOException
import java.nio.channels.Channels
import java.nio.channels.WritableByteChannel
import java.util.concurrent.atomic.AtomicBoolean

class AsyncReceiveDispatcher(private val receiver:Receiver,
                             private val callback:ReceiveDispatcher.ReceivePacketCallback) : ReceiveDispatcher,IoArgsEventProcessor{

    private val isClosed = AtomicBoolean(false)

    private val ioArgs = IoArgs()
    private var receivePacket:ReceivePacket<*>? = null
    private var packetChannel:WritableByteChannel? = null
    private var total:Long = 0
    private var position:Long = 0


    init {
        receiver.setReceiveListener(this)
    }


    override fun start() {
        registerReceive()
    }

    override fun stop() {

    }

    override fun close() {
        if(isClosed.compareAndSet(false,true)){
            completePacket(false)
        }
    }

    private fun closeAndNotify(){
        CloseUtils.close(this)
    }

    private fun registerReceive(){
        try {
            receiver.postReceiveAsync()
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
            receivePacket = StringReceivePacket(length.toLong())
            packetChannel = Channels.newChannel(receivePacket!!.open())
            total = length.toLong()
            position = 0
        }

        try{
            val count = args.readTo(packetChannel!!)
            position += count
            // 检查是否已完成一份Packet接收
            if(position == total){
                completePacket(true)
            }

        }catch (e:IOException){
            e.printStackTrace()
            completePacket(false)
        }
    }

    /**
     * 完成数据接收操作
     */
    private fun completePacket(isCucceed:Boolean) {
        val packet:ReceivePacket<*>? = this.receivePacket
        CloseUtils.close(packet)
        receivePacket = null


        val channel:WritableByteChannel? = this.packetChannel
        CloseUtils.close(channel)
        packetChannel = null

        packet?.let {
            callback.onReceivePacketCompleted(it)
        }
    }

    override fun provideIoArgs(): IoArgs {
        val args = ioArgs
        var receiveSize = if(receivePacket == null) 4
        else Math.min(total - position,args.capacity().toLong()).toInt()
        // 设置本次接收数据大小
        args.limit(receiveSize)
        return args
    }

    override fun onConsumerFailed(args: IoArgs, e: Exception) {
        e.printStackTrace()
    }

    override fun onConsumerCompleted(args: IoArgs) {
        assemblePacket(args)
        registerReceive()
    }
}