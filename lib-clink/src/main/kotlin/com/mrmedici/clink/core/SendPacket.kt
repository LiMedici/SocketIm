package com.mrmedici.clink.core

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

    fun available():Int{
        val stream = this.open()
        try {
            val available = stream.available()
            if(available < 0){
                return 0
            }
            return available
        }catch (e:IOException){
            return 0
        }
    }

}