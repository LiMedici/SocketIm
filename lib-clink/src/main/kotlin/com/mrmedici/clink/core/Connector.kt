package com.mrmedici.clink.core

import com.mrmedici.clink.impl.OnChannelStatusChangedListener
import com.mrmedici.clink.impl.SocketChannelAdapter
import java.io.Closeable
import java.io.IOException
import java.nio.channels.SocketChannel
import java.util.*

open class Connector : OnChannelStatusChangedListener,Closeable{
    private val key = UUID.randomUUID()
    private var channel:SocketChannel? = null
    private var sender:Sender? = null
    private var receiver:Receiver? = null

    @Throws(IOException::class)
    fun setup(socketChannel: SocketChannel){
        this.channel = socketChannel

        val context = IoContext.get()
        val adapter = SocketChannelAdapter(channel!!,context!!.ioProvider!!,this)

        this.sender = adapter
        this.receiver = adapter

        readNextMessage()

    }

    private fun readNextMessage(){
        try {
            receiver?.receiveAsync(echoReceiveListener)
        }catch (e:IOException){
            println("开始接收数据异常：${e.message}")
        }
    }

    override fun onChannelClosed(channel: SocketChannel) {

    }

    override fun close() {

    }

    private val echoReceiveListener = object : IoArgsEventListener{
        override fun onStarted(args: IoArgs) {

        }

        override fun onCompleted(args: IoArgs) {
            // 打印数据
            onReceiveNewMessage(args.bufferString())
            // 读取下一条数据
            readNextMessage()
        }
    }

    protected open fun onReceiveNewMessage(str:String){
        println("$key:$str")
    }
}