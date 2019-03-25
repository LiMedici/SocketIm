package com.mrmedici.clink.impl.async

import com.mrmedici.clink.box.StringReceivePacket
import com.mrmedici.clink.core.*
import com.mrmedici.clink.utils.CloseUtils
import java.io.IOException
import java.nio.channels.Channels
import java.nio.channels.WritableByteChannel
import java.util.concurrent.atomic.AtomicBoolean

class AsyncReceiveDispatcher(private val receiver:Receiver,
                             private val callback:ReceiveDispatcher.ReceivePacketCallback) :
        ReceiveDispatcher,IoArgsEventProcessor,AsyncPacketWriter.PacketProvider{

    private val isClosed = AtomicBoolean(false)

    private val writer:AsyncPacketWriter = AsyncPacketWriter(this)


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
            CloseUtils.close(writer)
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

    override fun provideIoArgs(): IoArgs {
        val ioArgs = writer.taskIoArgs()
        // 一份新的IoArgs需要调用一次需要写入数据的操作
        ioArgs.startWriting()
        return ioArgs
    }

    override fun onConsumerFailed(args: IoArgs?, e: Exception) {
        e.printStackTrace()
    }

    override fun onConsumerCompleted(args: IoArgs) {
        if(isClosed.get()){
            return
        }

        args.finishWriting()

        do{
            writer.consumeIoArgs(args)
        }while (args.remained() && !isClosed.get())
        registerReceive()
    }

    override fun taskPacket(type: Byte, length: Long, headerInfo: ByteArray?): ReceivePacket<*, *> {
        return callback.onArrivedNewPacket(type,length)
    }

    override fun completedPacket(packet: ReceivePacket<*, *>, isSucceed: Boolean) {
        CloseUtils.close(packet)
        callback.onReceivePacketCompleted(packet)
    }
}