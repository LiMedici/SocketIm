package com.mrmedici.clink.impl.async

import com.mrmedici.clink.core.FRAME_HEADER_LENGTH
import com.mrmedici.clink.core.Frame
import com.mrmedici.clink.core.IoArgs
import com.mrmedici.clink.core.ReceivePacket
import com.mrmedici.clink.frames.*
import java.io.Closeable
import java.io.IOException
import java.nio.channels.Channels
import java.nio.channels.WritableByteChannel

class AsyncPacketWriter(private val provider: PacketProvider) : Closeable {

    private val packetMap:HashMap<Short,PacketModel> = HashMap()
    private val ioArgs = IoArgs()
    @Volatile
    private var frameTemp: Frame? = null

    interface PacketProvider {
        fun taskPacket(type: Byte, length: Long, headerInfo: ByteArray?): ReceivePacket<*, *>
        fun completedPacket(packet: ReceivePacket<*, *>, isSucceed: Boolean)
        fun onReceiveHeartbeat()
    }

    @Synchronized
    fun taskIoArgs(): IoArgs {
        var limit = if(frameTemp == null) FRAME_HEADER_LENGTH
        else frameTemp!!.getConsumableLength()
        ioArgs.limit(limit)
        return ioArgs
    }

    @Synchronized
    fun consumeIoArgs(args: IoArgs) {
        if(frameTemp == null){
            var temp:Frame? = null
            do{
                temp = buildNewFrame(args)
            }while (temp == null && args.remained())

            if(temp == null){
                return
            }

            frameTemp = temp
            if(!args.remained()){
                return
            }
        }

        val currentFrame = frameTemp!!
        do{
            try{
                if(currentFrame.handle(args)){
                    if(currentFrame is ReceiveHeaderFrame){
                        val packet = provider.taskPacket(currentFrame.getPacketType(),
                                currentFrame.getPacketLength(),
                                currentFrame.getPacketHeaderInfo())
                        appendNewPacket(currentFrame.getBodyIdentifier(),packet)
                    }else if(currentFrame is ReceiveEntityFrame){
                        completeEntityFrame(currentFrame)
                    }

                    frameTemp = null
                    break
                }
            }catch (e:IOException){
                e.printStackTrace()
            }
        }while (args.remained())
    }


    private fun buildNewFrame(args: IoArgs): Frame? {
        val frame = ReceiveFrameFactory.createInstance(args)
        when (frame) {
            is CancelReceiveFrame -> {
                cancelReceivePacket(frame.getBodyIdentifier())
                return null
            }
            is HeartbeatReceiveFrame -> {
                provider.onReceiveHeartbeat()
                return null
            }
            is ReceiveEntityFrame -> {
                val channel = getPacketChannel(frame.getBodyIdentifier())
                frame.bindPacketChannel(channel)
            }
        }

        return frame
    }

    private fun completeEntityFrame(frame: ReceiveEntityFrame){
        synchronized(packetMap){
            val identifier = frame.getBodyIdentifier()
            val length = frame.getBodyLength()
            val model: PacketModel = this.packetMap[identifier] ?: return
            model.unReceivedLength -= length
            if(model.unReceivedLength <= 0){
                provider.completedPacket(model.packet,true)
                this.packetMap.remove(identifier)
            }
        }
    }

    private fun appendNewPacket(identifier:Short,packet:ReceivePacket<*,*>){
        synchronized(packetMap){
            val model = PacketModel(packet)
            packetMap[identifier] = model
        }
    }

    private fun getPacketChannel(identifier: Short): WritableByteChannel? {
        synchronized(packetMap){
            val model:PacketModel? = packetMap[identifier]
            return model?.channel
        }
    }

    private fun cancelReceivePacket(identifier: Short) {
        synchronized(packetMap){
            val model:PacketModel? = packetMap[identifier]
            if(model != null){
                val packet = model.packet
                provider.completedPacket(packet,false)
            }
        }
    }

    override fun close() {
        synchronized(packetMap){
            val values = packetMap.values
            for (value in values){
                provider.completedPacket(value.packet,false)
            }
            packetMap.clear()
        }
    }

    class PacketModel(var packet: ReceivePacket<*,*>) {
        @Volatile
        var unReceivedLength = packet.length()
        val channel = Channels.newChannel(packet.open())!!
    }
}