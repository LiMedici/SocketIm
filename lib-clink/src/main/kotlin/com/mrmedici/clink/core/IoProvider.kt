package com.mrmedici.clink.core

import java.io.Closeable
import java.io.IOException
import java.nio.channels.SocketChannel

interface IoProvider : Closeable{
    @Throws(Exception::class)
    fun register(callback: HandleProviderCallback)
    fun unRegister(channel: SocketChannel)

    abstract class HandleProviderCallback(private val ioProvider: IoProvider,
                                          channel: SocketChannel,ops:Int)
        : IoTask(channel,ops),Runnable{

        override fun onProcessIo(): Boolean {
            val attach = this.attach
            this.attach = null
            return onProviderIo(attach)
        }

        override fun fireThrowable(e: Throwable) {

        }

        @Volatile
        protected var attach:IoArgs? = null

        override fun run() {
            val attach = this.attach
            this.attach = null
            if(onProviderIo(attach)){
                try{
                    ioProvider.register(this)
                }catch (e:Exception){
                    fireThrowable(e)
                }
            }
        }

        @Throws(IOException::class)
        abstract fun onProviderIo(args:IoArgs?):Boolean

        @Throws(IllegalStateException::class)
        fun checkAttachNull() {
            if(attach != null){
                throw IllegalStateException("Current attach is not empty!")
            }
        }
    }
}