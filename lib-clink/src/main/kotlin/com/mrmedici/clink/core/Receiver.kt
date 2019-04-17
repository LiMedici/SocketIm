package com.mrmedici.clink.core

import java.io.Closeable
import java.io.IOException

interface Receiver : Closeable{

    fun setReceiveListener(processor: IoArgsEventProcessor?)

    @Throws(Exception::class)
    fun postReceiveAsync()

    fun getLastReadTime():Long
}