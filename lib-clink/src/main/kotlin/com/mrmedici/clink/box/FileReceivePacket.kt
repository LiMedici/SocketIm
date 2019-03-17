package com.mrmedici.clink.box

import com.mrmedici.clink.core.ReceivePacket
import com.mrmedici.clink.core.TYPE_STREAM_FILE
import java.io.File
import java.io.FileOutputStream

class FileReceivePacket(length:Long,
                        private val file:File) : ReceivePacket<FileOutputStream, File>() {

    init {
        this.length = length
    }

    override fun buildEntity(stream: FileOutputStream): File {
        return file
    }

    override fun type(): Byte {
        return TYPE_STREAM_FILE
    }

    override fun createStream(): FileOutputStream {
        return FileOutputStream(file)
    }
}