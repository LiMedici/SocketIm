package com.mrmedici.clink.box

import com.mrmedici.clink.core.ReceivePacket

class StringReceivePacket(len:Int) : ReceivePacket(){
    private val buffer:ByteArray = ByteArray(len)
    private var position:Int = 0

    init {
        this.length = len
    }

    override fun save(bytes: ByteArray, count: Int) {
        System.arraycopy(bytes,0,buffer,position,count)
        position += count
    }

    fun string():String = String(buffer)

    override fun close() {

    }
}