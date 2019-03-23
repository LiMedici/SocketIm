package com.mrmedici.clink.frames

import com.mrmedici.clink.core.*
import java.nio.channels.Channels
import java.nio.channels.ReadableByteChannel

const val PACKET_HEADER_FRAME_MIN_LENGTH:Int = 6

class SendHeaderFrame(identifier:Short,packet:SendPacket<*>) :
        AbsSendPacketFrame(PACKET_HEADER_FRAME_MIN_LENGTH,
                TYPE_PACKET_HEADER,
                FLAG_NONE,
                identifier,
                packet){

    private val body:ByteArray

    init {
        val packetLength:Long = packet.length()
        val packetType:Byte = packet.type()
        var packetHeaderInfo:ByteArray? = packet.headerInfo()

        body = ByteArray(bodyRemaining)

        body[0] = packetLength.shr(32).toByte()
        body[1] = packetLength.shr(24).toByte()
        body[2] = packetLength.shr(16).toByte()
        body[3] = packetLength.shr(8).toByte()
        body[4] = packetLength.toByte()
        body[5] = packetType

        // TODO 疑问：这里传的BodyRemaining == 6，怎么有空间传HeaderInfo信息
        if(packetHeaderInfo != null){
            System.arraycopy(packetHeaderInfo,0,body,PACKET_HEADER_FRAME_MIN_LENGTH,packetHeaderInfo.size)
        }
    }

    override fun consumeBody(args: IoArgs): Int {
        val count:Int = bodyRemaining
        val offset = body.size - count
        return args.writeFrom(body,offset,count)
    }

    override fun buildNextFrame(): Frame? {
        val stream = packet.open()
        val channel = Channels.newChannel(stream)
        return SendEntityFrame(getBodyIdentifier(),packet.length(),channel,packet)
    }
}