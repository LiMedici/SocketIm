package com.mrmedici.clink.core

import java.io.Closeable
import java.io.IOException

interface Receiver : Closeable{
    @Throws(IOException::class)
    fun receiveAsync(listener: IoArgsEventListener):Boolean
}