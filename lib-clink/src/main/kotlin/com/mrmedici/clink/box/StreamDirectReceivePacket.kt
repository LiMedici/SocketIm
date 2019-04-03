package com.mrmedici.clink.box

import com.mrmedici.clink.core.ReceivePacket
import com.mrmedici.clink.core.TYPE_STREAM_DIRECT
import sun.security.util.Length
import java.io.OutputStream

class StreamDirectReceivePacket(private val outputStream: OutputStream,length: Long) : ReceivePacket<OutputStream,OutputStream>(){

    init {
        this.length = length
    }

    override fun type(): Byte {
        return TYPE_STREAM_DIRECT
    }

    override fun createStream(): OutputStream {
        return outputStream
    }

    override fun buildEntity(stream: OutputStream): OutputStream {
        return outputStream
    }
}