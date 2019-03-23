package com.mrmedici.clink.frames

import com.mrmedici.clink.core.Frame
import com.mrmedici.clink.core.IoArgs
import java.io.IOException

abstract class AbsReceiveFrame(header:ByteArray) : Frame(header){
    // 帧体可读写区域大小
    @Volatile
    var bodyRemaining:Int = 0

    init {
        bodyRemaining = getBodyLength()
    }

    @Synchronized
    override fun handle(args: IoArgs): Boolean {
        if(bodyRemaining == 0){
            // 已读取所有数据
            return true
        }

        bodyRemaining -= consumeBody(args)

        return bodyRemaining == 0
    }

    override fun nextFrame(): Frame? {
        return null
    }

    override fun getConsumableLength(): Int {
        return bodyRemaining
    }

    @Throws(IOException::class)
    protected abstract fun consumeBody(args:IoArgs):Int
}