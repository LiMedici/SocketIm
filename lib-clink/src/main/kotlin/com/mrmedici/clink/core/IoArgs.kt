package com.mrmedici.clink.core

import java.io.IOException
import java.nio.ByteBuffer
import java.nio.channels.SocketChannel

class IoArgs{
    private val byteBuffer = ByteArray(256)
    private val buffer = ByteBuffer.wrap(byteBuffer)

    @Throws(IOException::class)
    fun read(channel: SocketChannel):Int{
        buffer.clear()
        return channel.read(buffer)
    }

    @Throws(IOException::class)
    fun write(channel: SocketChannel):Int{
        return channel.write(buffer)
    }

    fun bufferString():String{
        // 丢弃换行符
        return String(byteBuffer,0,buffer.position() - 1)
    }


}

interface IoArgsEventListener{
    fun onStarted(args:IoArgs)
    fun onCompleted(args: IoArgs)
}