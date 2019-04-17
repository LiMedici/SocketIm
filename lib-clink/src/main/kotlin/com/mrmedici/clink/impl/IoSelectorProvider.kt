package com.mrmedici.clink.impl

import com.mrmedici.clink.core.IoProvider
import com.mrmedici.clink.extensions.notifyAllK
import com.mrmedici.clink.extensions.notifyK
import com.mrmedici.clink.extensions.waitK
import com.mrmedici.clink.impl.IoSelectorProvider.Companion.handleSelection
import com.mrmedici.clink.impl.IoSelectorProvider.Companion.waitSelection
import com.mrmedici.clink.utils.CloseUtils
import java.io.IOException
import java.nio.channels.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.ThreadFactory
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

class IoSelectorProvider : IoProvider {

    private val isClosed = AtomicBoolean(false)
    private val isRegInput = AtomicBoolean(false)
    private val isRegOutput = AtomicBoolean(false)


    private val readSelector = Selector.open()
    private val writeSelector = Selector.open()

    private val inputCallbackMap = HashMap<SelectionKey, Runnable>()
    private val outputCallbackMap = HashMap<SelectionKey, Runnable>()

    private val inputHandlePool = Executors.newFixedThreadPool(4,
            NameableThreadFactory("IoProvider-Input-Thread-"))
    private val outputHandlePool = Executors.newFixedThreadPool(4,
            NameableThreadFactory("IoProvider-Output-Thread-"))

    init {
        startRead()
        startWrite()
    }

    private fun startRead() {
        val thread = SelectThread("Clink IoSelectorProvider ReadSelector Thread",
                isClosed, isRegInput, readSelector,
                inputCallbackMap, inputHandlePool,
                SelectionKey.OP_READ)
        thread.start()
    }

    private fun startWrite() {
        val thread = SelectThread("Clink IoSelectorProvider WriteSelector Thread",
                isClosed, isRegOutput, writeSelector,
                outputCallbackMap, outputHandlePool,
                SelectionKey.OP_WRITE)
        thread.start()
    }

    override fun register(callback: IoProvider.HandleProviderCallback) {
        var key:SelectionKey? = null
        if(callback.ops == SelectionKey.OP_READ){
            key = registerSelection(callback.channel, readSelector, SelectionKey.OP_READ, isRegInput,
                    inputCallbackMap, callback)
        }else if(callback.ops == SelectionKey.OP_WRITE){
            key = registerSelection(callback.channel, writeSelector, SelectionKey.OP_WRITE, isRegOutput,
                    outputCallbackMap, callback)
        }

        key?: throw IOException("Register error: channel:${callback.channel} ops:${callback.ops}")
    }

    override fun unRegister(channel: SocketChannel) {
        unRegisterSelection(channel, readSelector, inputCallbackMap,isRegInput)
        unRegisterSelection(channel, writeSelector, outputCallbackMap,isRegOutput)
    }

    override fun close() {
        if (isClosed.compareAndSet(false, true)) {
            inputHandlePool.shutdownNow()
            outputHandlePool.shutdownNow()

            inputCallbackMap.clear()
            outputCallbackMap.clear()
            // close方法已经作唤醒操作
            CloseUtils.close(readSelector, writeSelector)
        }
    }


    companion object {

        fun waitSelection(locker: AtomicBoolean) {
            synchronized(locker) {
                if (locker.get()) {
                    try {
                        locker.waitK()
                    } catch (e: InterruptedException) {
                        e.printStackTrace()
                    }
                }
            }
        }

        fun registerSelection(channel: SocketChannel, selector: Selector,
                              registerOps: Int, locker: AtomicBoolean,
                              map: HashMap<SelectionKey, Runnable>,
                              runnable: Runnable): SelectionKey? {
            synchronized(locker) {
                // 设置锁定状态
                locker.set(true)

                try {
                    // 唤醒当前的selector,让selector不处于select()状态
                    selector.wakeup()

                    var key: SelectionKey? = null
                    if (channel.isRegistered) {
                        key = channel.keyFor(selector)
                        key?.interestOps(key.interestOps().or(registerOps))
                    }

                    if (key == null) {
                        // 注册selector得到Key
                        key = channel.register(selector, registerOps)
                        map[key] = runnable
                    }

                    return key
                } catch (e: ClosedChannelException){
                    return null
                } catch (e: CancelledKeyException){
                    return null
                } catch (e: ClosedSelectorException){
                    return null
                } finally {
                    // 解除锁定状态
                    locker.set(false)
                    // 通知
                    locker.notifyK()
                }
            }
        }

        fun unRegisterSelection(channel: SocketChannel, selector: Selector,
                                map: HashMap<SelectionKey, Runnable>,
                                locker: AtomicBoolean) {
            synchronized(locker){
                locker.set(true)
                selector.wakeup()
                try{
                    if (channel.isRegistered) {
                        val key = channel.keyFor(selector)
                        key?.let {
                            it.cancel()
                            map.remove(it)
                        }
                    }
                }finally {
                    locker.set(false)
                    try{
                        locker.notifyAllK()
                    } catch (ignored:Exception){

                    }
                }
            }
        }

        fun handleSelection(key: SelectionKey, keyOps: Int,
                            map: HashMap<SelectionKey, Runnable>,
                            pool: ExecutorService,
                            locker: AtomicBoolean) {
            // 取消继续对KeyOps的监听
            synchronized(locker) {
                try {
                    key.interestOps(key.interestOps().and(keyOps.inv()))
                } catch (e: CancelledKeyException) {
                    return
                }
            }

            var runnable: Runnable? = null
            try {
                runnable = map[key]
            } catch (ignored: Exception) {

            }

            if (runnable != null && !pool.isShutdown) {
                // 异步调度
                pool.execute(runnable)
            }
        }
    }

}

private class SelectThread(name: String,
                           private val isClosed: AtomicBoolean,
                           private val locker: AtomicBoolean,
                           private val selector: Selector,
                           private val callMap: HashMap<SelectionKey, Runnable>,
                           private val pool: ExecutorService,
                           private val keyOps: Int) : Thread(name) {
    init {
        this.priority = Thread.MAX_PRIORITY
    }

    override fun run() {
        val isClosed = this.isClosed
        val locker = this.locker
        val selector = this.selector
        val callMap = this.callMap
        val pool = this.pool
        val keyOps = this.keyOps

        while (!isClosed.get()) {
            try {
                if (selector.select() == 0) {
                    waitSelection(locker)
                    continue
                } else if (locker.get()) {
                    waitSelection(locker)
                }

                val selectedKeys = selector.selectedKeys()
                val iterator = selectedKeys.iterator()
                while (iterator.hasNext()) {
                    val next = iterator.next()
                    if (next.isValid) {
                        handleSelection(next, keyOps, callMap, pool,locker)
                    }
                    iterator.remove()
                }
            } catch (e: IOException) {
                e.printStackTrace()
            } catch (ignored:ClosedSelectorException){
                break
            }
        }
    }
}