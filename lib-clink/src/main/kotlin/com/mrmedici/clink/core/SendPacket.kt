package com.mrmedici.clink.core

import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStream

/**
 * 发送包定义
 */
abstract class SendPacket<Stream : InputStream> : Packet<Stream>(){

    private var isCanceled:Boolean = false

    fun cancel(){
        isCanceled = true
    }

    fun isCanceled():Boolean{
        return isCanceled
    }

}