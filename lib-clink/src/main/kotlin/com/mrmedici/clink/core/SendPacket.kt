package com.mrmedici.clink.core

import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStream

/**
 * 发送包定义
 */
abstract class SendPacket<T : InputStream> : Packet<T>(){

    private var isCanceled:Boolean = false

    fun isCanceled():Boolean{
        return isCanceled
    }
}