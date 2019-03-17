package com.mrmedici.clink.box

import com.mrmedici.clink.core.SendPacket
import com.mrmedici.clink.core.TYPE_STREAM_FILE
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException

class FileSendPacket(private val file:File) : SendPacket<FileInputStream>(){

    init {
        this.length = file.length()
    }

    override fun type(): Byte {
        return TYPE_STREAM_FILE
    }

    override fun createStream(): FileInputStream {
        return FileInputStream(file)
    }
}