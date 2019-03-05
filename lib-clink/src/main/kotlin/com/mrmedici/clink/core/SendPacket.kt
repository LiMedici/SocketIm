package com.mrmedici.clink.core

/**
 * 发送包定义
 */
abstract class SendPacket : Packet(){
    private var isCanceled:Boolean = false

    abstract fun bytes():ByteArray

    fun isCanceled():Boolean{
        return isCanceled
    }
}