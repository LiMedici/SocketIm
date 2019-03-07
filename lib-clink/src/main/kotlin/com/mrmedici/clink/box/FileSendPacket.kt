package com.mrmedici.clink.box

import com.mrmedici.clink.core.SendPacket
import java.io.File
import java.io.FileInputStream

class FileSendPacket(file:File) : SendPacket<FileInputStream>(){

    init {
        this.length = file.length()
    }

    override fun createStream(): FileInputStream {
        return null!!
    }
}