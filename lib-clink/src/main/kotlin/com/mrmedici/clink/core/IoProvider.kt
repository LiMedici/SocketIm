package com.mrmedici.clink.core

import java.io.Closeable
import java.nio.channels.SocketChannel

interface IoProvider : Closeable{
    fun registerInput(channel: SocketChannel,callback: HandleInputCallback):Boolean
    fun registerOutput(channel: SocketChannel,callback: HandleOutputCallback):Boolean
    fun unRegisterInput(channel: SocketChannel)
    fun unRegisterOutput(channel: SocketChannel)

    abstract class HandleInputCallback : Runnable{
        override fun run() {
            canProviderInput()
        }

        abstract fun canProviderInput()
    }

    abstract class HandleOutputCallback : Runnable{

        private var attach:Any? = null

        override fun run() {
            canProviderOutput(attach)
        }

        fun setAttach(attach: Any){
            this.attach = attach
        }

        abstract fun canProviderOutput(attach:Any?)
    }
}