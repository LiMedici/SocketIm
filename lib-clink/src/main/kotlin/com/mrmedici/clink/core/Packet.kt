package com.mrmedici.clink.core

import java.io.Closeable
import java.io.IOException
import java.io.InputStream

/**
 * 公共的数据封装
 * 提供了类型以及基本的长度的定义
 */
// BYTES 类型
const val TYPE_MEMORY_BYTES:Byte = 1
// String 类型
const val TYPE_MEMORY_STRING:Byte = 2
// 文件 类型
const val TYPE_STREAM_FILE:Byte = 3
// 长链接流 类型
const val TYPE_STREAM_DIRECT:Byte = 4


abstract class Packet<Stream : Closeable> : Closeable{


    protected open var length:Long = 0
    private var stream:Stream? = null

    fun length():Long = length

    fun open(): Stream {
        if(stream == null){
            stream = createStream()
        }

        return stream as Stream
    }

    /**
     * 类型，直接通过方法得到
     * {@link #TYPE_MEMORY_BYTES}
     * {@link #TYPE_MEMORY_STRING}
     * {@link #TYPE_STREAM_FILE}
     * {@link #TYPE_STREAM_DIRECT}
     */
    abstract fun type():Byte

    protected abstract fun createStream():Stream

    @Throws(IOException::class)
    protected open fun closeStream(stream:Stream){
        stream?.close()
    }

    @Throws(IOException::class)
    override fun close() {
        stream?.let {
            closeStream(it)
            stream = null
        }
    }
}