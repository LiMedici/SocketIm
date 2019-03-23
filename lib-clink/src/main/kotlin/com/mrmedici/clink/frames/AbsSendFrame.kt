package com.mrmedici.clink.frames

import com.mrmedici.clink.core.FRAME_HEADER_LENGTH
import com.mrmedici.clink.core.Frame
import com.mrmedici.clink.core.IoArgs
import java.io.IOException

abstract class AbsSendFrame(length: Int, type: Byte, flag: Byte, identifier: Short) : Frame(length, type, flag, identifier) {
    @Volatile
    protected var headerRemaining:Int = FRAME_HEADER_LENGTH
    @Volatile
    protected var bodyRemaining:Int = length

    @Synchronized
    override fun handle(args: IoArgs):Boolean {
        try{
            args.limit(headerRemaining + bodyRemaining)
            args.startWriting()

            if(headerRemaining > 0 && args.remained()){
                headerRemaining -= consumeHeader(args)
            }

            if(headerRemaining == 0 && args.remained() && bodyRemaining > 0){
                bodyRemaining -= consumeBody(args)
            }

            return headerRemaining == 0 && bodyRemaining == 0
        }finally {
            args.finishWriting()
        }

    }

    @Throws(IOException::class)
    fun consumeHeader(args: IoArgs): Int{
        val count:Int = headerRemaining
        val offset = header.size - headerRemaining
        return args.writeFrom(header,offset,count)
    }

    @Throws(IOException::class)
    abstract fun consumeBody(args: IoArgs): Int

    override fun getConsumableLength(): Int {
        return headerRemaining + bodyRemaining
    }

    @Synchronized
    fun isSending():Boolean{
        return headerRemaining < FRAME_HEADER_LENGTH
    }
}