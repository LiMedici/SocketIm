package com.mrmedici.clink.core

import java.io.EOFException
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.channels.ReadableByteChannel
import java.nio.channels.SocketChannel
import java.nio.channels.WritableByteChannel

class IoArgs{
    private var limit:Int = 5
    private val byteBuffer = ByteArray(limit)
    private val buffer = ByteBuffer.wrap(byteBuffer)

    /**
     * 从channel中写入数据
     */
    @Throws(IOException::class)
    fun writeFrom(channel:ReadableByteChannel):Int{
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
        startWriting()
        buffer.putInt(total)
        finishWriting()
    }

    fun readLength():Int{
        return buffer.int
    }

    fun capacity(): Int {
        return buffer.capacity()
    }
}

/**
 * IoArgs 提供者，处理者；数据的生产和消费者
 */
interface IoArgsEventProcessor{
    // 提供一个可消费的IoArgs
    fun provideIoArgs():IoArgs
    // 消费失败的回调
    fun onConsumerFailed(args:IoArgs,e:Exception)
    // 消费成功
    fun onConsumerCompleted(args:IoArgs)

}