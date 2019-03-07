package com.mrmedici.clink.box

import com.mrmedici.clink.core.SendPacket
import java.io.ByteArrayInputStream

class StringSendPacket(msg:String) : SendPacket<ByteArrayInputStream>(){

    private val bytes:ByteArray = msg.toByteArray()

    init {
        this.length = bytes.size.toLong()
    }

    override fun createStream(): ByteArrayInputStream {
        return ByteArrayInputStream(bytes)
    }
}