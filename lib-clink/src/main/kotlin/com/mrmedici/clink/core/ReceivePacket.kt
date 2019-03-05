package com.mrmedici.clink.core

/**
 * 接收包的定义
 */
abstract class ReceivePacket : Packet(){
    abstract fun save(bytes:ByteArray,count:Int)
}