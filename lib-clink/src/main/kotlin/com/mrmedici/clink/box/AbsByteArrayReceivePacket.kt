package com.mrmedici.clink.box

import com.mrmedici.clink.core.ReceivePacket
import java.io.ByteArrayOutputStream

abstract class AbsByteArrayReceivePacket<Entity>(len:Long) : ReceivePacket<ByteArrayOutputStream,Entity>(){
    init {
        length = len
    }

    override fun createStream(): ByteArrayOutputStream {
        return ByteArrayOutputStream(length.toInt())
    }
}