package com.mrmedici.clink.core

import java.io.Closeable
import java.io.IOException
import java.io.InputStream

/**
 * 公共的数据封装
 * 提供了类型以及基本的长度的定义
 */
abstract class Packet<T : Closeable> : Closeable{

    protected open var type:Byte = 0
    protected open var length:Long = 0
    private var stream:T? = null

    fun type():Byte = type
    fun length():Long = length

    fun open(): T {
        if(stream == null){
            stream = createStream()
        }

        return stream as T
    }

    protected abstract fun createStream():T

    @Throws(IOException::class)
    protected open fun closeStream(stream:T){
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