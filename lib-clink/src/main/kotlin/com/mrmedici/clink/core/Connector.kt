package com.mrmedici.clink.core

import com.mrmedici.clink.box.StringReceivePacket
import com.mrmedici.clink.box.StringSendPacket
import com.mrmedici.clink.impl.OnChannelStatusChangedListener
import com.mrmedici.clink.impl.SocketChannelAdapter
import com.mrmedici.clink.impl.async.AsyncReceiveDispatcher
import com.mrmedici.clink.impl.async.AsyncSendDispatcher
import java.io.Closeable
import java.io.IOException
import java.nio.channels.SocketChannel
import java.util.*

open class Connector : OnChannelStatusChangedListener,Closeable{
    private val key = UUID.randomUUID()
    private var channel:SocketChannel? = null
    private var sender:Sender? = null
    private var receiver:Receiver? = null
    private var sendDispatcher:SendDispatcher? = null
    private var receiveDispatcher:ReceiveDispatcher? = null

    @Throws(IOException::class)
    fun setup(socketChannel: SocketChannel){
        this.channel = socketChannel

        val context = IoContext.get()
        val adapter = SocketChannelAdapter(channel!!,context!!.ioProvider!!,this)

        this.sender = adapter
        this.receiver = adapter

        sendDispatcher = AsyncSendDispatcher(sender!!)
        receiveDispatcher = AsyncReceiveDispatcher(receiver!!,receivePacketCallback)

        // 启动接收
        receiveDispatcher!!.start()

    }

    fun send(msg:String){
        val packet = StringSendPacket(msg)
        sendDispatcher?.send(packet)
    }

    override fun onChannelClosed(channel: SocketChannel) {

    }

    override fun close() {
        receiveDispatcher?.close()
        sendDispatcher?.close()

        receiver?.close()
        sender?.close()

        channel?.close()
    }

    private val receivePacketCallback = object : ReceiveDispatcher.ReceivePacketCallback{
        override fun onReceivePacketCompleted(packet: ReceivePacket) {
            when(packet){
                is StringReceivePacket -> {
                    val msg = packet.string()
                    onReceiveNewMessage(msg)
                }
            }
        }
    }

    protected open fun onReceiveNewMessage(str:String){
        println("$key:$str")
    }
}