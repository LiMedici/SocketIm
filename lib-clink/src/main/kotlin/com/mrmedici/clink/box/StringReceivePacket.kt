package com.mrmedici.clink.box

import com.mrmedici.clink.core.ReceivePacket
import com.mrmedici.clink.core.TYPE_MEMORY_STRING
import java.io.ByteArrayOutputStream

class StringReceivePacket(len:Long) : AbsByteArrayReceivePacket<String>(len){

    override fun buildEntity(stream: ByteArrayOutputStream): String {
        return String(stream.toByteArray())
    }

    override fun type(): Byte {
        return TYPE_MEMORY_STRING
    }
}