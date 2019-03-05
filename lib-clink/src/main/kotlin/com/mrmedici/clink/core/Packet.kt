package com.mrmedici.clink.core

import java.io.Closeable

/**
 * 公共的数据封装
 * 提供了类型以及基本的长度的定义
 */
abstract class Packet : Closeable{
    protected open var type:Byte = 0
    protected open var length:Int = 0

    fun type():Byte = type
    fun length():Int = length
}