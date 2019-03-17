package com.mrmedici.clink.core

import java.io.OutputStream

/**
 * 接收包的定义
 */
abstract class ReceivePacket<Stream : OutputStream,Entity> : Packet<Stream>(){
    private var entity:Entity? = null

    fun entity():Entity?{
        return entity
    }

    /**
     * 根据接收到的流，转换为对应的实体
     */
    protected abstract fun buildEntity(stream:Stream):Entity

    override fun closeStream(stream: Stream) {
        super.closeStream(stream)
        entity = buildEntity(stream)
    }
}