package com.mrmedici.clink.frames

import com.mrmedici.clink.core.IoArgs

class CancelReceiveFrame(header:ByteArray) : AbsReceiveFrame(header){

    override fun consumeBody(args: IoArgs): Int {
        return 0
    }
}