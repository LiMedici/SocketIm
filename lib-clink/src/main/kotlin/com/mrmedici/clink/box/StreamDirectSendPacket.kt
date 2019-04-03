package com.mrmedici.clink.box

import com.mrmedici.clink.core.MAX_PACKET_SIZE
import com.mrmedici.clink.core.SendPacket
import com.mrmedici.clink.core.TYPE_STREAM_DIRECT
import java.io.InputStream

class StreamDirectSendPacket(private val inputStream: InputStream) : SendPacket<InputStream>(){

    init {
        length = MAX_PACKET_SIZE
    }

    override fun type(): Byte {
        return TYPE_STREAM_DIRECT
    }

    override fun createStream(): InputStream {
        return inputStream
    }
}