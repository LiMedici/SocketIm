package com.mrmedici.clink.impl.bridge

import com.mrmedici.clink.core.*
import com.mrmedici.clink.utils.plugin.CircularByteBuffer
import java.io.IOException
import java.nio.channels.Channels
import java.util.concurrent.atomic.AtomicBoolean

class BridgeSocketDispatcher(private val receiver: Receiver) : SendDispatcher, ReceiveDispatcher {

    // 数据暂存的缓冲区
    private val mBuffer = CircularByteBuffer(512, true)
    // 根据缓冲区得到的读取、输入通道
    private val readableByteChannel = Channels.newChannel(mBuffer.inputStream)
    private val writableByteChannel = Channels.newChannel(mBuffer.outputStream)

    // 有数据则接收，无数据不强求填满，有多少返回多少。
    private val receiveIoArgs = IoArgs(256, false)

    // 当前是否处于发送中
    private val isSending = AtomicBoolean()
    // 用以发送的IoArgs，默认全部发送数据
    private val sendIoArgs = IoArgs()
    private var sender: Sender? = null

    fun bindSender(sender: Sender?) {
        val oldSender:Sender? = this.sender
        oldSender?.setSendListener(null)

        synchronized(isSending) {
            isSending.set(false)
        }

        mBuffer.clear()

        // 设置新的发送者
        this.sender = sender
        sender?.let {
            it.setSendListener(senderEventProcessor)
            requestSend()
        }
    }

    private fun requestSend() {
        synchronized(isSending) {
            val sender = this.sender
            if (isSending.get() || sender == null) return

            // 返回True代表有数据需要发送
            if (mBuffer.available > 0) {
                try {
                    val isSucceed = sender.postSendAsync()
                    if (isSucceed) {
                        isSending.set(true)
                    }
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }
    }


    override fun start() {
        receiver.setReceiveListener(receiveEventProcessor)
        registerReceive()
    }

    private fun registerReceive() {
        try {
            receiver.postReceiveAsync()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    override fun stop() {
    }

    override fun send(packet: SendPacket<*>) {
    }

    override fun sendHeartbeat() {
    }

    override fun cancel(packet: SendPacket<*>) {
    }

    override fun close() {
    }


    private val receiveEventProcessor = object : IoArgsEventProcessor {
        override fun provideIoArgs(): IoArgs? {
            receiveIoArgs.resetLimit()
            // 一份新的IoArgs需要调用一次写入数据的操作
            receiveIoArgs.startWriting()
            return receiveIoArgs
        }

        override fun onConsumerFailed(args: IoArgs?, e: Exception) {
            e.printStackTrace()
        }

        override fun onConsumerCompleted(args: IoArgs) {
            args.finishWriting()
            try {
                args.readTo(writableByteChannel)
            } catch (e: IOException) {
                e.printStackTrace()
            }

            registerReceive()
            // 接收数据后请求发送数据
            requestSend()
        }
    }

    private val senderEventProcessor = object : IoArgsEventProcessor {
        override fun provideIoArgs(): IoArgs? {
            try {
                val available = mBuffer.available
                val args = this@BridgeSocketDispatcher.sendIoArgs
                if (available > 0) {
                    args.limit(available)
                    args.startWriting()
                    args.writeFrom(readableByteChannel)
                    args.finishWriting()
                    return args
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }
            return null
        }

        override fun onConsumerFailed(args: IoArgs?, e: Exception) {
            e.printStackTrace()
            // 设置当前发送状态
            synchronized(isSending){
                isSending.set(false)
            }
            // 继续请求发送当前的数据
            requestSend()
        }

        override fun onConsumerCompleted(args: IoArgs) {
            // 设置当前发送状态
            synchronized(isSending){
                isSending.set(false)
            }
            // 继续请求发送当前的数据
            requestSend()
        }
    }
}