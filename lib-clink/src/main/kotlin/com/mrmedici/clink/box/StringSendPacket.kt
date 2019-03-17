package com.mrmedici.clink.box

import com.mrmedici.clink.core.TYPE_MEMORY_STRING

class StringSendPacket(msg:String) : BytesSendPacket(msg.toByteArray()){

    override fun type(): Byte {
        return TYPE_MEMORY_STRING
    }

}