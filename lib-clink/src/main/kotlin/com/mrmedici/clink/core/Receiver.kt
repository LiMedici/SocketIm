package com.mrmedici.clink.core

import java.io.Closeable
import java.io.IOException

interface Receiver : Closeable{

    fun setReceiveListener(processor: IoArgsEventProcessor?)

    @Throws(IOException::class)
    fun postReceiveAsync():Boolean

    fun getLastReadTime():Long
}