package com.mrmedici.clink.frames

import com.mrmedici.clink.core.*

class ReceiveFrameFactory{
    companion object {
        fun createInstance(args:IoArgs):AbsReceiveFrame{
            val buffer = ByteArray(FRAME_HEADER_LENGTH)
            args.readTo(buffer,0)
            val type = buffer[2]
            return when(type){
                TYPE_COMMAND_SEND_CANCEL -> CancelReceiveFrame(buffer)
                TYPE_PACKET_HEADER -> ReceiveHeaderFrame(buffer)
                TYPE_PACKET_ENTITY -> ReceiveEntityFrame(buffer)
                else -> throw UnsupportedOperationException("Unsupported frame type:$type")
            }
        }
    }
}