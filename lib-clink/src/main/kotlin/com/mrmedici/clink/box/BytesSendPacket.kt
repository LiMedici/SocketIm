package com.mrmedici.clink.box

import com.mrmedici.clink.core.SendPacket
import com.mrmedici.clink.core.TYPE_MEMORY_BYTES
import java.io.ByteArrayInputStream

open class BytesSendPacket(private val bytes:ByteArray) : SendPacket<ByteArrayInputStream>(){

    init {
        length = bytes.size.toLong()
    }

    override fun type(): Byte {
        return TYPE_MEMORY_BYTES
    }

    override fun createStream(): ByteArrayInputStream {
        return ByteArrayInputStream(bytes)
    }
}