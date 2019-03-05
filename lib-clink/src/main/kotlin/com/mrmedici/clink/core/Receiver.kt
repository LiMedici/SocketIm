package com.mrmedici.clink.core

import java.io.Closeable
import java.io.IOException

interface Receiver : Closeable{

    fun setReceiveListener(listener: IoArgsEventListener)

    @Throws(IOException::class)
    fun receiveAsync(args: IoArgs):Boolean
}