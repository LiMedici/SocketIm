package com.mrmedici.clink.core

import java.io.EOFException
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.channels.SocketChannel

class IoArgs{
    private var limit:Int = 256
    private val byteBuffer = ByteArray(limit)
    private val buffer = ByteBuffer.wrap(byteBuffer)

    /**
     * 从bytes中写入数据
     */
    fun writeFrom(bytes:ByteArray,offset:Int):Int{
        val size = Math.min(bytes.size - offset,buffer.remaining())
        buffer.put(bytes,offset,size)
        return size
    }

    /**
     * 读取数据到bytes中
     */
    fun readTo(bytes:ByteArray,offset:Int):Int{
        val size = Math.min(bytes.size - offset,buffer.remaining())
        buffer.get(bytes,offset,size)
        return size
    }

    /**
     * 从SocketChannel中读取数据
     */
    @Throws(IOException::class)
    fun readFrom(channel: SocketChannel):Int{
        startWriting()

        var bytesProduced = 0
        while (buffer.hasRemaining()){
            var len = channel.read(buffer)
            if(len < 0){
                throw EOFException()
            }
            bytesProduced += len
        }

        finishWriting()
        return bytesProduced
    }

    /**
     * 写数据到SocketChannel中
     */
    @Throws(IOException::class)
    fun writeTo(channel: SocketChannel):Int{
        var bytesProduced = 0
        while (buffer.hasRemaining()){
            var len = channel.write(buffer)
            if(len < 0){
                throw EOFException()
            }
            bytesProduced += len
        }
        return bytesProduced
    }

    /**
     * 开始写数据到IoArgs
     */
    fun startWriting(){
        buffer.clear()
        // 定义容纳区间
        buffer.limit(limit)
    }

    /**
     * 写完数据后调用
     */
    fun finishWriting(){
        buffer.flip()
    }

    /**
     * 设置单词写操作的容纳区间
     */
    fun limit(limit:Int){
        this.limit = limit
    }

    fun writeLength(total: Int) {
        buffer.putInt(total)
    }

    fun readLength():Int{
        return buffer.int
    }

    fun capacity(): Int {
        return buffer.capacity()
    }
}

interface IoArgsEventListener{
    fun onStarted(args:IoArgs)
    fun onCompleted(args: IoArgs)
}