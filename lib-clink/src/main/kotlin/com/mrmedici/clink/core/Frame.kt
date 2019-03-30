package com.mrmedici.clink.core

import java.io.IOException
import java.lang.RuntimeException
import kotlin.experimental.and

const val FRAME_HEADER_LENGTH:Int = 6
const val MAX_CAPACITY:Int = 64 * 1024 - 1

const val TYPE_PACKET_HEADER:Byte = 11
const val TYPE_PACKET_ENTITY:Byte = 12

const val TYPE_COMMAND_SEND_CANCEL:Byte = 41
const val TYPE_COMMAND_RECEIVE_REJECT:Byte = 42

const val TYPE_COMMAND_HEARTBEAT:Byte = 81

const val FLAG_NONE:Byte = 0

abstract class Frame(){

    protected open val header:ByteArray = ByteArray(FRAME_HEADER_LENGTH)

    constructor(header:ByteArray):this(){
        System.arraycopy(header,0,this.header,0, FRAME_HEADER_LENGTH)
    }

    @Throws(RuntimeException::class)
    constructor(length:Int,type:Byte,flag:Byte,identifier:Short):this(){
        if(length < 0 || length > MAX_CAPACITY){
            throw RuntimeException()
        }

        if(identifier < 1 || identifier > 255){
            throw RuntimeException()
        }

        header[0] = length.shr(8).toByte()
        header[1] = length.toByte()

        header[2] = type
        header[3] = flag
        header[4] = identifier.toByte()
        header[5] = 0
    }

    fun getBodyLength():Int{
        return header[0].toInt().and(0xFF).shl(8).or(header[1].toInt().and(0xFF))
    }

    fun getBodyType():Byte{
        return header[2]
    }

    fun getBodyFlag():Byte{
        return header[3]
    }

    fun getBodyIdentifier():Short{
        return header[4].toShort().and(0xFF)
    }

    @Throws(IOException::class)
    abstract fun handle(args : IoArgs):Boolean

    abstract fun nextFrame():Frame?
    abstract fun getConsumableLength(): Int

}