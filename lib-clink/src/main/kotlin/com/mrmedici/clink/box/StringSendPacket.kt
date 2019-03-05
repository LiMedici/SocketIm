package com.mrmedici.clink.box

import com.mrmedici.clink.core.SendPacket

class StringSendPacket(msg:String) : SendPacket(){

    private val bytes:ByteArray = msg.toByteArray()

    init {
        this.length = bytes.size
    }

    override fun bytes(): ByteArray {
        return bytes
    }

    override fun close() {

    }
}