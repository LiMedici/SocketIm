package com.mrmedici.clink.impl

import com.mrmedici.clink.core.IoProvider
import com.mrmedici.clink.core.IoTask
import com.mrmedici.clink.impl.stealing.StealingSelectorThread
import com.mrmedici.clink.impl.stealing.StealingService
import java.io.IOException
import java.nio.channels.SelectionKey
import java.nio.channels.Selector
import java.nio.channels.SocketChannel

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
            thread.isDaemon = false
            thread.priority = Thread.MAX_PRIORITY
            thread.start()
        }

        this.threads = threads
        this.stealingService = stealingService
    }

    override fun register(callback: IoProvider.HandleProviderCallback) {
        val thread:StealingSelectorThread? = this.stealingService.getNotBusyThread()
        thread?:throw IOException("IoStealingSelectorProvider is shutdown!")
        thread.register(callback)
    }

    override fun unRegister(channel: SocketChannel) {
        if(!channel.isOpen) return
        threads.forEach {
            it.unregister(channel)
        }
    }

    override fun close() {
        stealingService.shutdownNow()
    }

    class IoStealingThread(name:String,selector: Selector) : StealingSelectorThread(selector){

        init {
            setName(name)
        }

        override fun processTask(task: IoTask): Boolean {
            return task.onProcessIo()
        }
    }
}