package com.mrmedici.clink.core

import java.io.Closeable
import java.io.IOException

interface Sender : Closeable{
    @Throws(IOException::class)
    fun sendAsync(args: IoArgs,listener: IoArgsEventListener):Boolean
}