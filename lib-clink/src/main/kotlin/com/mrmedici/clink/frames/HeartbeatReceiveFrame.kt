package com.mrmedici.clink.frames

import com.mrmedici.clink.core.IoArgs

object HeartbeatReceiveFrame : AbsReceiveFrame(HeartbeatSendFrame.HEARTBEAT_DATA){

    override fun consumeBody(args: IoArgs): Int {
        return 0
    }
}