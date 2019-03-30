package com.mrmedici.clink.frames

import com.mrmedici.clink.core.*

class HeartbeatSendFrame(header:ByteArray):AbsSendFrame(header){

    companion object {
        val HEARTBEAT_DATA = byteArrayOf(0,0,TYPE_COMMAND_HEARTBEAT,0,0,0)
    }

    constructor():this(HEARTBEAT_DATA)

    override fun consumeBody(args: IoArgs): Int {
        return 0
    }

    override fun nextFrame(): Frame? {
        return null
    }
}