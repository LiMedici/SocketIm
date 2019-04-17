package com.mrmedici.clink.impl

import com.mrmedici.clink.core.*
import com.mrmedici.clink.impl.exceptions.EmptyIoArgsException
import com.mrmedici.clink.utils.CloseUtils
import java.io.Closeable
import java.io.IOException
import java.nio.channels.SelectionKey
import java.nio.channels.SocketChannel
import java.util.concurrent.atomic.AtomicBoolean

class SocketChannelAdapter(private val channel: SocketChannel,
                           private val ioProvider: IoProvider,
                           private val listener: OnChannelStatusChangedListener) : Sender, Receiver, Closeable {

    private val isClosed = AtomicBoolean(false)
    private val inputCallback = InputProviderCallback(ioProvider,channel,SelectionKey.OP_READ)
    private val outputCallback = OutputProviderCallback(ioProvider,channel,SelectionKey.OP_WRITE)

    override fun setReceiveListener(processor: IoArgsEventProcessor?) {
        this.inputCallback.eventProcessor = processor
    }

    override fun setSendListener(processor: IoArgsEventProcessor?) {
        this.outputCallback.eventProcessor = processor
    }

    @Throws(Exception::class)
    override fun postSendAsync() {
        if (isClosed.get() || !channel.isOpen) {
            throw IOException("Current channel is closed")
        }

        // 进行Callback状态监测，监测是否处于自循环状态
        outputCallback.checkAttachNull()
        // 当前发送的数据附加到回调中
        ioProvider.register(outputCallback)
    }

    override fun getLastWriteTime(): Long {
        return outputCallback.lastActiveTime
    }

    @Throws(Exception::class)
    override fun postReceiveAsync() {
        if (isClosed.get()) {
            throw IOException("Current channel is closed")
        }

        // 进行Callback状态监测，监测是否处于自循环状态
        inputCallback.checkAttachNull()
        return ioProvider.register(inputCallback)
    }

    override fun getLastReadTime():Long {
        return inputCallback.lastActiveTime
    }

    override fun close() {
        if (isClosed.compareAndSet(false, true)) {
            ioProvider.unRegister(channel)

            CloseUtils.close(channel)
            listener.onChannelClosed(channel)
        }
    }

    abstract inner class AbsProviderCallback(ioProvider: IoProvider,
                                             channel: SocketChannel,
                                             ops:Int) : IoProvider.HandleProviderCallback(ioProvider,channel,ops){

        var eventProcessor:IoArgsEventProcessor? = null
        @Volatile
        var lastActiveTime:Long = System.currentTimeMillis()

        override fun onProviderIo(args: IoArgs?): Boolean {
            if (isClosed.get()) {
                return false
            }

            val processor = this.eventProcessor?:return false

            lastActiveTime = System.currentTimeMillis()

            var ioArgs:IoArgs? = args
            if(ioArgs == null) {
                // 拿一份新的IoArgs
                ioArgs = processor.provideIoArgs()
            }

            try {
                // 具体的写入操作
                if (ioArgs == null) {
                    throw EmptyIoArgsException("ProvideIoArgs is null.")
                }

                val count = consumeIoArgs(ioArgs,channel)

                return if (ioArgs.remained() && (count ==0 || ioArgs.isNeedConsumeRemaining())) {
                    // 附加当前未消费完成的args
                    attach = ioArgs
                    // 再次注册数据发送
                    true
                } else {
                    // 输出完成回调
                    processor.onConsumerCompleted(ioArgs)
                }
            } catch (e: IOException) {
                if(processor.onConsumerFailed(e)){
                    CloseUtils.close(this@SocketChannelAdapter)
                }

                return false
            }

            return false
        }

        @Throws(IOException::class)
        abstract fun consumeIoArgs(args:IoArgs, channel: SocketChannel):Int

        override fun fireThrowable(e: Throwable) {
            val processor:IoArgsEventProcessor? = eventProcessor
            if(processor == null || processor.onConsumerFailed(e)){
                CloseUtils.close(this@SocketChannelAdapter)
            }
        }
    }

    inner class InputProviderCallback(ioProvider: IoProvider,
                                      channel: SocketChannel,
                                      ops:Int) : AbsProviderCallback(ioProvider,channel,ops){
        @Throws(IOException::class)
        override fun consumeIoArgs(args: IoArgs, channel: SocketChannel): Int {
            return args.readFrom(channel)
        }
    }

    inner class OutputProviderCallback(ioProvider: IoProvider,
                                      channel: SocketChannel,
                                      ops:Int) : AbsProviderCallback(ioProvider,channel,ops){
        @Throws(IOException::class)
        override fun consumeIoArgs(args: IoArgs, channel: SocketChannel): Int {
            return args.writeTo(channel)
        }
    }
}

interface OnChannelStatusChangedListener {
    fun onChannelClosed(channel: SocketChannel)
}