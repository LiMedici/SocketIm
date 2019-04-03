package com.mrmedici.clink.frames

import com.mrmedici.clink.core.*
import java.io.InputStream
import java.nio.channels.Channels
import java.nio.channels.ReadableByteChannel

class SendDirectEntityFrame(identifier: Short,
                            available:Int,
                            packet:SendPacket<*>,
                            private val channel: ReadableByteChannel)
    :AbsSendPacketFrame(Math.min(available, MAX_CAPACITY),
        TYPE_PACKET_ENTITY, FLAG_NONE,
        identifier,packet){

    override fun consumeBody(args: IoArgs): Int {
        if(isAbort.get()){
            // 已终止当前帧，则填充假数据
            return args.fillEmpty(bodyRemaining)
        }
        return args.writeFrom(channel)
    }

    override fun buildNextFrame(): Frame? {
        // 直流类型
        val available = packet.available()
        return if(available <= 0){
            CancelSendFrame(getBodyIdentifier())
        }else{
            // 下一个帧
            SendDirectEntityFrame(getBodyIdentifier(),available,packet,channel)
        }
    }

    companion object {
        fun buildEntityFrame(packet:SendPacket<*>,identifier:Short):Frame{
            val available = packet.available()
            return if(available <= 0){
                // 直流结束
                CancelSendFrame(identifier)
            }else{
                // 构建首帧
                val stream = packet.open()
                val channel = Channels.newChannel(stream)
                SendDirectEntityFrame(identifier,available,packet,channel)
            }
        }
    }
}