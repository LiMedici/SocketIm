package com.mrmedici.clink.impl

import com.mrmedici.clink.core.IoProvider
import com.mrmedici.clink.impl.stealing.IoTask
import com.mrmedici.clink.impl.stealing.StealingSelectorThread
import java.nio.channels.SelectionKey
import java.nio.channels.Selector
import java.nio.channels.SocketChannel

class IoStealingSelectorProvider : IoProvider{

    private val thread:StealingSelectorThread

    constructor(poolSize:Int){
        val selector = Selector.open()
        thread = object : StealingSelectorThread(selector){
            override fun processTask(task: IoTask): Boolean {
                task.providerCallback?.run()
                return false
            }
        }

        thread.start()
    }

    override fun registerInput(channel: SocketChannel, callback: IoProvider.HandleProviderCallback): Boolean {
        return thread.register(channel,SelectionKey.OP_READ,callback)
    }

    override fun registerOutput(channel: SocketChannel, callback: IoProvider.HandleProviderCallback): Boolean {
        return thread.register(channel,SelectionKey.OP_WRITE,callback)
    }

    override fun unRegisterInput(channel: SocketChannel) {
        thread.unregister(channel)
    }

    override fun unRegisterOutput(channel: SocketChannel) {

    }

    override fun close() {
        thread.exit()
    }
}