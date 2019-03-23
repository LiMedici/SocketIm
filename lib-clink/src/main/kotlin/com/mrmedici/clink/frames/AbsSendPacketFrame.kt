package com.mrmedici.clink.frames

import com.mrmedici.clink.core.FRAME_HEADER_LENGTH
import com.mrmedici.clink.core.Frame
import com.mrmedici.clink.core.IoArgs
import com.mrmedici.clink.core.SendPacket
import java.io.IOException
import java.util.concurrent.atomic.AtomicBoolean

// TODO 对packet的 Get进行同步
abstract class AbsSendPacketFrame(length: Int, type: Byte, flag: Byte, identifier: Short,@Volatile var packet:SendPacket<*>) :
        AbsSendFrame(length, type, flag, identifier) {

    protected val isAbort:AtomicBoolean = AtomicBoolean(false)

    @Synchronized
    override fun handle(args: IoArgs): Boolean {
        // 已取消，并且未发送任何数据，直接返回结束，发送下一帧
        if(isAbort.get() && !isSending()){
            return true
        }
        return super.handle(args)
    }

    @Synchronized
    override fun nextFrame(): Frame? {
        return if(isAbort.get()){
            null
        }else{
            buildNextFrame()
        }
    }

    @Synchronized
    fun abort():Boolean{
        val isSending = isSending()
        if(isSending){
            fillDirtyDataOnAbort()
        }

        isAbort.compareAndSet(false,true)
        return !isSending
    }

    open fun fillDirtyDataOnAbort(){

    }

    abstract fun buildNextFrame():Frame?
}