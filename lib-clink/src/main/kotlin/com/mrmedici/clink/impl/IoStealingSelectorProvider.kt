package com.mrmedici.clink.impl

import com.mrmedici.clink.core.IoProvider
import com.mrmedici.clink.impl.stealing.IoTask
import com.mrmedici.clink.impl.stealing.StealingSelectorThread
import com.mrmedici.clink.impl.stealing.StealingService
import java.nio.channels.SelectionKey
import java.nio.channels.Selector
import java.nio.channels.SocketChannel
import kotlin.concurrent.thread

class IoStealingSelectorProvider : IoProvider{

    private val threads:Array<IoStealingThread>
    private val stealingService:StealingService

    constructor(poolSize:Int){
        val threadsTemp = arrayOfNulls<IoStealingThread>(poolSize)
        for (index in 0 until threadsTemp.size){
            val selector = Selector.open()
            threadsTemp[index] = IoStealingThread("IoProvider-Thread-${index+1}",selector)
        }

        val threads = threadsTemp.filterNotNull().toTypedArray()
        val stealingService = StealingService(threads,10)
        for (thread in threads){
            thread.setStealingService(stealingService)
            thread.start()
        }

        this.threads = threads
        this.stealingService = stealingService
    }

    override fun registerInput(channel: SocketChannel, callback: IoProvider.HandleProviderCallback): Boolean {
        val thread:StealingSelectorThread? = this.stealingService.getNotBusyThread()
        return thread?.register(channel,SelectionKey.OP_READ,callback)?:false
    }

    override fun registerOutput(channel: SocketChannel, callback: IoProvider.HandleProviderCallback): Boolean {
        val thread:StealingSelectorThread? = this.stealingService.getNotBusyThread()
        return thread?.register(channel,SelectionKey.OP_WRITE,callback)?:false
    }

    override fun unRegisterInput(channel: SocketChannel) {
        threads.forEach {
            it.unregister(channel)
        }
    }

    override fun unRegisterOutput(channel: SocketChannel) {

    }

    override fun close() {
        stealingService.shutdownNow()
    }

    class IoStealingThread(name:String,selector: Selector) : StealingSelectorThread(selector){

        init {
            setName(name)
        }

        override fun processTask(task: IoTask): Boolean {
            task.providerCallback?.run()
            return false
        }
    }
}