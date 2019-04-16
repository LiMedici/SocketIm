package com.mrmedici.clink.impl

import com.mrmedici.clink.core.*
import com.mrmedici.clink.utils.CloseUtils
import java.io.Closeable
import java.io.IOException
import java.nio.channels.SocketChannel
import java.util.concurrent.atomic.AtomicBoolean

class SocketChannelAdapter(private val channel: SocketChannel,
                           private val ioProvider: IoProvider,
                           private val listener: OnChannelStatusChangedListener) : Sender, Receiver, Closeable {

    private val isClosed = AtomicBoolean(false)

    private var receiveIoEventProcessor: IoArgsEventProcessor? = null
    private var sendIoEventLisProcessor: IoArgsEventProcessor? = null

    @Volatile
    private var lastReadTime = System.currentTimeMillis()
    @Volatile
    private var lastWriteTime = System.currentTimeMillis()

    init {
        channel.configureBlocking(false)
    }

    override fun setReceiveListener(processor: IoArgsEventProcessor?) {
        this.receiveIoEventProcessor = processor
    }

    override fun setSendListener(processor: IoArgsEventProcessor?) {
        this.sendIoEventLisProcessor = processor
    }

    @Throws(IOException::class)
    override fun postSendAsync(): Boolean {
        if (isClosed.get()) {
            throw IOException("Current channel is closed")
        }

        // 进行Callback状态监测，监测是否处于自循环状态
        outputCallback.checkAttachNull()
        // 当前发送的数据附加到回调中
        // return ioProvider.registerOutput(channel, outputCallback)
        outputCallback.run()
        return true
    }

    override fun getLastWriteTime(): Long {
        return lastWriteTime
    }

    @Throws(IOException::class)
    override fun postReceiveAsync(): Boolean {
        if (isClosed.get()) {
            throw IOException("Current channel is closed")
        }

        // 进行Callback状态监测，监测是否处于自循环状态
        inputCallback.checkAttachNull()
        return ioProvider.registerInput(channel, inputCallback)
    }

    override fun getLastReadTime():Long {
        return lastReadTime
    }

    override fun close() {
        if (isClosed.compareAndSet(false, true)) {
            ioProvider.unRegisterInput(channel)
            ioProvider.unRegisterOutput(channel)

            CloseUtils.close(channel)
            listener.onChannelClosed(channel)
        }
    }


    private val inputCallback: IoProvider.HandleProviderCallback = object : IoProvider.HandleProviderCallback() {

        @Throws(IOException::class)
        override fun onProviderIo(args: IoArgs?) {
            if (isClosed.get()) {
                return
            }

            lastReadTime = System.currentTimeMillis()

            val processor = this@SocketChannelAdapter.receiveIoEventProcessor ?: return

            var ioArgs:IoArgs? = args
            if(ioArgs == null){
                ioArgs = processor.provideIoArgs()
            }

            try {
                // 具体的读取操作
                if(ioArgs == null){
                    processor.onConsumerFailed(null, IOException("ProvideIoArgs is null."))
                }else{
                    val count = ioArgs.readFrom(channel)
                    if (count == 0) {
                        println("Current read zero data!")
                    }

                    if (ioArgs.remained() && ioArgs.isNeedConsumeRemaining()) {
                        // 附加当前未消费完成的args
                        attach = ioArgs
                        // 再次注册数据发送
                        ioProvider.registerInput(channel, this)
                    } else {
                        // 设置为null
                        attach = null
                        // 输出完成回调
                        processor.onConsumerCompleted(ioArgs)
                    }
                }
            } catch (ignored: IOException) {
                CloseUtils.close(this@SocketChannelAdapter)
            }
        }
    }

    private val outputCallback: IoProvider.HandleProviderCallback = object : IoProvider.HandleProviderCallback() {

        @Throws(IOException::class)
        override fun onProviderIo(args: IoArgs?) {
            if (isClosed.get()) {
                return
            }

            lastWriteTime = System.currentTimeMillis()

            val processor = this@SocketChannelAdapter.sendIoEventLisProcessor?:return

            var ioArgs:IoArgs? = args
            if(ioArgs == null) {
                // 拿一份新的IoArgs
                ioArgs = processor.provideIoArgs()
            }

            try {
                // 具体的写入操作
                if (ioArgs == null) {
                    processor.onConsumerFailed(null, IOException("ProvideIoArgs is null."))
                } else {
                    val count = ioArgs.writeTo(channel)
                    if (count == 0) {
                        println("Current write zero data!")
                    }

                    if (ioArgs.remained() && ioArgs.isNeedConsumeRemaining()) {
                        // 附加当前未消费完成的args
                        attach = ioArgs
                        // 再次注册数据发送
                        ioProvider.registerOutput(channel, this)
                    } else {
                        // 设置为null
                        attach = null
                        // 输出完成回调
                        processor.onConsumerCompleted(ioArgs)
                    }
                }
            } catch (ignored: IOException) {
                CloseUtils.close(this@SocketChannelAdapter)
            }
        }
    }
}

interface OnChannelStatusChangedListener {
    fun onChannelClosed(channel: SocketChannel)
}