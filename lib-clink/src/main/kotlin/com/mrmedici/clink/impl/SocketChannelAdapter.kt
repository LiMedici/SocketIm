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

    private var receiveIoEventListener:IoArgsEventListener? = null
    private var sendIoEventListener:IoArgsEventListener? = null

    private var receiveArgs:IoArgs? = null

    init {
        channel.configureBlocking(false)
    }

    override fun sendAsync(args: IoArgs, listener: IoArgsEventListener): Boolean {
        if(isClosed.get()){
            throw IOException("Current channel is closed")
        }

        this.sendIoEventListener = listener
        // 当前发送的数据附加到回调中
        outputCallback.setAttach(args)
        return ioProvider.registerOutput(channel, outputCallback)
    }

    override fun setReceiveListener(listener: IoArgsEventListener) {
        this.receiveIoEventListener = listener
    }

    override fun receiveAsync(args: IoArgs): Boolean {
        if(isClosed.get()){
            throw IOException("Current channel is closed")
        }

        this.receiveArgs = args

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

            val args = receiveArgs
            val listener = this@SocketChannelAdapter.receiveIoEventListener

            if(listener != null && args != null) {
                listener.onStarted(args)

                try {
                    // 具体的读取操作
                    if (args.readFrom(channel) > 0) {
                        // 读取完成回调
                        listener?.onCompleted(args)
                    } else {
                        throw IOException("Cannot read any data!")
                    }
                } catch (ignored: IOException) {
                    CloseUtils.close(this@SocketChannelAdapter)
                }
            }
        }
    }

    private val outputCallback:IoProvider.HandleOutputCallback = object : IoProvider.HandleOutputCallback(){
        override fun canProviderOutput(attach: Any?) {
            if(isClosed.get()){
                return
            }

            val args = getAttach<IoArgs>()
            val listener = this@SocketChannelAdapter.sendIoEventListener


            if(args != null && listener != null){
                listener.onStarted(args)

                try{
                    // 具体的写入操作
                    if(args.writeTo(channel) > 0){
                        // 读取完成回调
                        listener.onCompleted(args)
                    }else {
                        throw IOException("Cannot write any data!")
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