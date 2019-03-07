package com.mrmedici.clink.box

import com.mrmedici.clink.core.ReceivePacket
import java.io.ByteArrayOutputStream

class StringReceivePacket(length:Long) : ReceivePacket<ByteArrayOutputStream>(){

    private var  string:String? = null

    init {
        this.length = length
    }

    fun string():String? = string

    override fun closeStream(stream:ByteArrayOutputStream) {
        super.closeStream(stream)
        string = String(stream.toByteArray())
    }

    override fun createStream(): ByteArrayOutputStream {
        return ByteArrayOutputStream(length.toInt())
    }
}