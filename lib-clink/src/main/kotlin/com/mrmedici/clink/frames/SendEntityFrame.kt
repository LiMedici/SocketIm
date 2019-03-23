package com.mrmedici.clink.frames

import com.mrmedici.clink.core.*
import java.nio.channels.ReadableByteChannel

class SendEntityFrame(identifier:Short,
                      entityLength:Long,
                      val channel: ReadableByteChannel,
                      packet:SendPacket<*>) :
        AbsSendPacketFrame(Math.min(entityLength, MAX_CAPACITY.toLong()).toInt(),
                TYPE_PACKET_ENTITY,
                FLAG_NONE,
                identifier,
                packet){

    private val unConsumeEntityLength = entityLength - bodyRemaining

    override fun consumeBody(args: IoArgs): Int {
        if(isAbort.get()){
            // 已终止当前帧，则填充假数据
            args.fillEmpty(bodyRemaining)
        }

        return args.writeFrom(channel)
    }

    override fun buildNextFrame(): Frame? {
        if(unConsumeEntityLength == 0.toLong()){
            return null
        }
        return SendEntityFrame(getBodyIdentifier(),unConsumeEntityLength,channel,packet)
    }
}