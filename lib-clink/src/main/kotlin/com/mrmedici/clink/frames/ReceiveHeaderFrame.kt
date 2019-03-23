package com.mrmedici.clink.frames

import com.mrmedici.clink.core.IoArgs

class ReceiveHeaderFrame(header: ByteArray) : AbsReceiveFrame(header) {

    private val body: ByteArray = ByteArray(bodyRemaining)

    override fun consumeBody(args: IoArgs): Int {
        val offset = body.size - bodyRemaining
        return args.readTo(body, offset)
    }

    fun getPacketLength(): Long {
        return body[0].toLong().and(0xFFL).shl(32).or(
                body[1].toLong().and(0xFFL).shl(24).or(
                        body[2].toLong().and(0xFFL).shl(16).or(
                                body[3].toLong().and(0xFFL).shl(8).or(
                                        body[4].toLong().and(0xFFL)
                                )
                        )
                )
        )
    }

    fun getPacketType():Byte{
        return body[5]
    }

    fun getPacketHeaderInfo():ByteArray?{
        if(body.size > PACKET_HEADER_FRAME_MIN_LENGTH){
            val headerInfo = ByteArray(body.size - PACKET_HEADER_FRAME_MIN_LENGTH)
            System.arraycopy(body, PACKET_HEADER_FRAME_MIN_LENGTH,headerInfo,0,headerInfo.size)
            return headerInfo
        }
        return null
    }
}