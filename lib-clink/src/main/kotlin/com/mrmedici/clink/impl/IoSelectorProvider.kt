package com.mrmedici.clink.impl

import com.mrmedici.clink.core.IoProvider
import com.mrmedici.clink.extensions.notifyK
import com.mrmedici.clink.extensions.waitK
import com.mrmedici.clink.utils.CloseUtils
import java.io.IOException
import java.nio.channels.ClosedChannelException
import java.nio.channels.SelectionKey
import java.nio.channels.Selector
import java.nio.channels.SocketChannel
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
            IoProviderThreadFactory("IoProvider-Input-Thread-"))
    private val outputHandlePool = Executors.newFixedThreadPool(4,
            IoProviderThreadFactory("IoProvider-Output-Thread-"))

    init {
        startRead()
        startWrite()
    }

    private fun startRead() {
        val thread = object : Thread("Clink IoSelectorProvider WriteSelector Thread") {
            override fun run() {
                while (!isClosed.get()) {
                    try {
                        if (readSelector.select() == 0) {
                            waitSelection(isRegInput)
                            continue
                        }

                        val selectedKeys = readSelector.selectedKeys()
                        selectedKeys
                                .filter { it.isValid }
                                .forEach { handleSelection(it, SelectionKey.OP_READ, inputCallbackMap, inputHandlePool) }
                        selectedKeys.clear()
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }
                }
            }
        }
        thread.priority = Thread.MAX_PRIORITY
        thread.start()
    }

    private fun startWrite() {
        val thread = object : Thread("Clink IoSelectorProvider ReadSelector Thread") {
            override fun run() {
                while (!isClosed.get()) {
                    try {
                        if (writeSelector.select() == 0) {
                            waitSelection(isRegOutput)
                            continue
                        }

                        val selectedKeys = writeSelector.selectedKeys()
                        selectedKeys
                                .filter { it.isValid }
                                .forEach { handleSelection(it, SelectionKey.OP_WRITE, outputCallbackMap, outputHandlePool) }
                        selectedKeys.clear()
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }
                }
            }
        }
        thread.priority = Thread.MAX_PRIORITY
        thread.start()
    }


    override fun registerInput(channel: SocketChannel, callback: IoProvider.HandleInputCallback): Boolean {
        return registerSelection(channel,readSelector,SelectionKey.OP_READ,isRegInput,
                inputCallbackMap,callback) != null
    }

    override fun registerOutput(channel: SocketChannel, callback: IoProvider.HandleOutputCallback): Boolean {
        return registerSelection(channel,writeSelector,SelectionKey.OP_WRITE,isRegOutput,
                outputCallbackMap,callback) != null
    }

    override fun unRegisterInput(channel: SocketChannel) {
        unRegisterSelection(channel,readSelector,inputCallbackMap)
    }

    override fun unRegisterOutput(channel: SocketChannel) {
        unRegisterSelection(channel,writeSelector,outputCallbackMap)
    }

    override fun close() {
        if(isClosed.compareAndSet(false,true)){
            inputHandlePool.shutdownNow()
            outputHandlePool.shutdownNow()

            inputCallbackMap.clear()
            outputCallbackMap.clear()

            readSelector.wakeup()
            writeSelector.wakeup()

            CloseUtils.close(readSelector,writeSelector)
        }
    }


    companion object {

        private fun waitSelection(locker: AtomicBoolean){
            synchronized(locker){
                if(locker.get()){
                    try {
                        locker.waitK()
                    }catch (e:InterruptedException){
                        e.printStackTrace()
                    }
                }
            }
        }

        private fun registerSelection(channel: SocketChannel, selector: Selector,
                             registerOps: Int, locker: AtomicBoolean,
                             map: HashMap<SelectionKey, Runnable>,
                             runnable: Runnable):SelectionKey? {
            synchronized(locker){
                // 设置锁定状态
                locker.set(true)

                try{
                    // 唤醒当前的selector,让selector不处于select()状态
                    selector.wakeup()

                    var key:SelectionKey? = null
                    if(channel.isRegistered){
                        key = channel.keyFor(selector)
                        if(key != null){
                            key.interestOps(key.interestOps().or(registerOps))
                        }
                    }

                    if(key == null){
                        // 注册selector得到Key
                        key = channel.register(selector,registerOps)
                        map.put(key,runnable)
                    }

                    return key
                }catch (e:ClosedChannelException){
                    return null
                }finally {
                    // 解除锁定状态
                    locker.set(false)
                    // 通知
                    locker.notifyK()
                }
            }
        }

        private fun unRegisterSelection(channel: SocketChannel,selector: Selector,
                                        map:HashMap<SelectionKey,Runnable>){
            if(channel.isRegistered){
                val key = channel.keyFor(selector)
                key?.let {
                    it.cancel()
                    map.remove(it)
                    selector.wakeup()
                }
            }
        }

        private fun handleSelection(key: SelectionKey, keyOps: Int,
                                    map: HashMap<SelectionKey, Runnable>,
                                    pool: ExecutorService) {
            // TODO 重点 疑问：都取消对KeyOps的监听了，为什么还会出现单个消息多分接收的问题。
            // 取消继续对KeyOps的监听
            key.interestOps(key.interestOps().and(keyOps.inv()))

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

internal class IoProviderThreadFactory(namePrefix: String) : ThreadFactory {
    private val group: ThreadGroup
    private val threadNumber = AtomicInteger(1)
    private val namePrefix: String

    init {
        val manager = System.getSecurityManager()
        group = if (manager != null) manager.threadGroup
        else Thread.currentThread().threadGroup
        this.namePrefix = namePrefix
    }

    override fun newThread(runnable: Runnable): Thread {
        val thread = Thread(group, runnable, namePrefix + threadNumber.getAndIncrement(), 0)
        if (thread.isDaemon) thread.isDaemon = false
        if (thread.priority != Thread.NORM_PRIORITY) thread.priority = Thread.NORM_PRIORITY
        return thread
    }
}