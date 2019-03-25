package com.mrmedici.clink.core

import java.io.EOFException
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.channels.ReadableByteChannel
import java.nio.channels.SocketChannel
import java.nio.channels.WritableByteChannel

class IoArgs{
    private var limit:Int = 255
    private val byteBuffer = ByteArray(limit)
    private val buffer = ByteBuffer.wrap(byteBuffer)

    fun writeFrom(bytes:ByteArray,offset:Int,count:Int):Int{
        var size:Int = Math.min(count,buffer.remaining())
        if(size <= 0){
            return 0
        }

        buffer.put(bytes,offset,count)
        return size
    }

    fun readTo(bytes:ByteArray,offset:Int):Int{
        var size:Int = Math.min(bytes.size - offset,buffer.remaining())
        buffer.get(bytes,offset,size)
        return size
    }

    /**
     * 从channel中写入数据
     */
    @Throws(IOException::class)
    fun writeFrom(channel:ReadableByteChannel):Int{
        var bytesProduced = 0
        while (buffer.hasRemaining()){
            var len = channel.read(buffer)
            if(len < 0){
                throw EOFException()
            }
            bytesProduced += len
        }
        return bytesProduced
    }

    /**
     * 读取数据到channel中
     */
    @Throws(IOException::class)
    fun readTo(channel:WritableByteChannel):Int{
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
     * 从SocketChannel中读取数据
     */
    @Throws(IOException::class)
    fun readFrom(channel: SocketChannel):Int{

        val buffer = this.buffer
        var bytesProduced = 0
        var len: Int
        do{
            len = channel.read(buffer)
            if(len < 0){
                throw EOFException("Cannot read any data with：$channel")
            }
            bytesProduced += len
        }while (buffer.hasRemaining() && len != 0)

        return bytesProduced
    }

    /**
     * 写数据到SocketChannel中
     */
    @Throws(IOException::class)
    fun writeTo(channel: SocketChannel):Int{
        val buffer = this.buffer
        var bytesProduced = 0
        var len: Int
        do{
            len = channel.write(buffer)
            if(len < 0){
                throw EOFException("Current write data with:$channel")
            }
            bytesProduced += len
        }while (buffer.hasRemaining() && len != 0)
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
        this.limit = Math.min(limit,buffer.capacity())
    }

    fun readLength():Int{
        return buffer.int
    }

    fun capacity(): Int {
        return buffer.capacity()
    }

    fun remained(): Boolean {
        return buffer.hasRemaining()
    }

    fun fillEmpty(size: Int):Int {
        val fillSize = Math.min(size,buffer.remaining())
        buffer.position(buffer.position() + fillSize)
        return fillSize
    }

    fun setEmpty(size:Int):Int{
        val emptySize = Math.min(size,buffer.remaining())
        buffer.position(buffer.position() + emptySize)
        return emptySize
    }
}

/**
 * IoArgs 提供者，处理者；数据的生产和消费者
 */
interface IoArgsEventProcessor{
    // 提供一个可消费的IoArgs
    fun provideIoArgs():IoArgs?
    // 消费失败的回调
    fun onConsumerFailed(args:IoArgs?,e:Exception)
    // 消费成功
    fun onConsumerCompleted(args:IoArgs)

}