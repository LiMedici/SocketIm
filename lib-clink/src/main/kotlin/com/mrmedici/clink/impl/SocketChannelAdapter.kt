package com.mrmedici.clink.impl

import com.mrmedici.clink.core.*
import com.mrmedici.clink.utils.CloseUtils
import java.io.Closeable
import java.io.IOException
import java.nio.channels.SocketChannel
import java.util.concurrent.atomic.AtomicBoolean

class SocketChannelAdapter(private val channel: SocketChannel,
                           private val ioProvider: IoProvider,
                           private val listener: OnChannelStatusChangedListener) : Sender,Receiver,Closeable{

    private val isClosed = AtomicBoolean(false)

    private var receiveIoEventProcessor:IoArgsEventProcessor? = null
    private var sendIoEventLisProcessor:IoArgsEventProcessor? = null

    init {
        channel.configureBlocking(false)
    }

    override fun setReceiveListener(processor: IoArgsEventProcessor) {
        this.receiveIoEventProcessor = processor
    }

    override fun setSendListener(processor: IoArgsEventProcessor) {
        this.sendIoEventLisProcessor = processor
    }

    @Throws(IOException::class)
    override fun postSendAsync(): Boolean {
        if(isClosed.get()){
            throw IOException("Current channel is closed")
        }

        return ioProvider.registerOutput(channel, outputCallback)
    }

    @Throws(IOException::class)
    override fun postReceiveAsync(): Boolean {
        if(isClosed.get()){
            throw IOException("Current channel is closed")
        }

        return ioProvider.registerInput(channel, inputCallback)
    }

    override fun close() {
        if(isClosed.compareAndSet(false,true)){
            ioProvider.unRegisterInput(channel)
            ioProvider.unRegisterOutput(channel)

            CloseUtils.close(channel)
            listener.onChannelClosed(channel)
        }
    }



    private val inputCallback:IoProvider.HandleInputCallback = object :IoProvider.HandleInputCallback(){

        @Throws(IOException::class)
        override fun canProviderInput() {
            if(isClosed.get()){
                return
            }

            val processor = this@SocketChannelAdapter.receiveIoEventProcessor

            processor?.let {
                val args = it.provideIoArgs()

                try {
                    // 具体的读取操作
                    when {
                        args == null -> it.onConsumerFailed(null,IOException("ProvideIoArgs is null."))
                        args.readFrom(channel) > 0 -> // 读取完成回调
                            it.onConsumerCompleted(args)
                        else -> it.onConsumerFailed(args,IOException("Cannot read any data!"))
                    }
                } catch (ignored: IOException) {
                    CloseUtils.close(this@SocketChannelAdapter)
                }
            }
        }
    }

    private val outputCallback:IoProvider.HandleOutputCallback = object : IoProvider.HandleOutputCallback(){
        override fun canProviderOutput() {
            if(isClosed.get()){
                return
            }

            val processor = this@SocketChannelAdapter.sendIoEventLisProcessor

            processor?.let {
                val args = it.provideIoArgs()

                try{
                    // 具体的写入操作
                    when {
                        args == null -> it.onConsumerFailed(null,IOException("ProvideIoArgs is null."))
                        args.writeTo(channel) > 0 -> // 读取完成回调
                            it.onConsumerCompleted(args)
                        else -> it.onConsumerFailed(args,IOException("Cannot write any data!"))
                    }
                }catch (ignored:IOException){
                    CloseUtils.close(this@SocketChannelAdapter)
                }
            }

        }
    }
}

interface OnChannelStatusChangedListener{
    fun onChannelClosed(channel: SocketChannel)
}