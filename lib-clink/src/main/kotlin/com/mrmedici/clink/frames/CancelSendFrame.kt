package com.mrmedici.clink.frames

import com.mrmedici.clink.core.FLAG_NONE
import com.mrmedici.clink.core.Frame
import com.mrmedici.clink.core.IoArgs
import com.mrmedici.clink.core.TYPE_COMMAND_SEND_CANCEL

class CancelSendFrame(identifier:Short) :
        AbsSendFrame(0, TYPE_COMMAND_SEND_CANCEL, FLAG_NONE,identifier){

    override fun consumeBody(args: IoArgs): Int {
        return 0
    }

    override fun nextFrame(): Frame? {
        return null
    }
}