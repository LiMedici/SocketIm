package com.mrmedici.clink.core

import java.io.Closeable
import java.nio.channels.SocketChannel

interface IoProvider : Closeable{
    fun registerInput(channel: SocketChannel,callback: HandleProviderCallback):Boolean
    fun registerOutput(channel: SocketChannel,callback: HandleProviderCallback):Boolean
    fun unRegisterInput(channel: SocketChannel)
    fun unRegisterOutput(channel: SocketChannel)

    abstract class HandleProviderCallback : Runnable{

        @Volatile
        protected var attach:IoArgs? = null

        override fun run() {
            onProviderIo(attach)
        }

        abstract fun onProviderIo(args:IoArgs?)

        @Throws(IllegalStateException::class)
        fun checkAttachNull() {
            if(attach != null){
                throw IllegalStateException("Current attach is not empty!")
            }
        }
    }
}