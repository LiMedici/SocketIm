package com.mrmedici.clink.box

import com.mrmedici.clink.core.TYPE_MEMORY_BYTES
import java.io.ByteArrayOutputStream

class BytesReceivePacket(len:Long) : AbsByteArrayReceivePacket<ByteArray>(len){

    override fun type(): Byte {
        return TYPE_MEMORY_BYTES
    }

    override fun buildEntity(stream: ByteArrayOutputStream): ByteArray {
        return stream.toByteArray()
    }

}