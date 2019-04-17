package com.mrmedici.clink.core

import java.io.Closeable
import java.io.IOException

interface Sender : Closeable{
    fun setSendListener(processor:IoArgsEventProcessor?)

    @Throws(Exception::class)
    fun postSendAsync()

    fun getLastWriteTime():Long
}