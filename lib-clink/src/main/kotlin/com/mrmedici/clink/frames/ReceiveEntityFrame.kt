package com.mrmedici.clink.frames

import com.mrmedici.clink.core.IoArgs
import java.nio.channels.WritableByteChannel

class ReceiveEntityFrame(header:ByteArray) : AbsReceiveFrame(header){

    private var channel:WritableByteChannel? = null

    fun bindPacketChannel(channel: WritableByteChannel?){
        this.channel = channel
    }

    override fun consumeBody(args: IoArgs): Int {
        return if(channel == null){
            args.setEmpty(bodyRemaining)
        }else{
            args.readTo(channel!!)
        }
    }
}