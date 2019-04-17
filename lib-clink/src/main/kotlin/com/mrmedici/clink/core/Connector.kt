package com.mrmedici.clink.core

import com.mrmedici.clink.box.*
import com.mrmedici.clink.impl.OnChannelStatusChangedListener
import com.mrmedici.clink.impl.SocketChannelAdapter
import com.mrmedici.clink.impl.async.AsyncReceiveDispatcher
import com.mrmedici.clink.impl.async.AsyncSendDispatcher
import com.mrmedici.clink.impl.bridge.BridgeSocketDispatcher
import com.mrmedici.clink.utils.CloseUtils
import java.io.Closeable
import java.io.File
import java.io.IOException
import java.io.OutputStream
import java.net.StandardSocketOptions
import java.nio.channels.SocketChannel
import java.util.*

abstract class Connector : OnChannelStatusChangedListener, Closeable {
    private val key = UUID.randomUUID()
    private lateinit var channel: SocketChannel
    private lateinit var sender: Sender
    private lateinit var receiver: Receiver
    private lateinit var sendDispatcher: SendDispatcher
    private lateinit var receiveDispatcher: ReceiveDispatcher

    private val scheduleJobs = ArrayList<ScheduleJob>(4)

    @Throws(IOException::class)
    fun setup(socketChannel: SocketChannel) {
        this.channel = socketChannel

        socketChannel.configureBlocking(false)
        socketChannel.socket().soTimeout = 1000
        socketChannel.socket().setPerformancePreferences(1,3,3)
        socketChannel.setOption(StandardSocketOptions.SO_RCVBUF,16 * 1024)
        socketChannel.setOption(StandardSocketOptions.SO_SNDBUF,16 * 1024)
        socketChannel.setOption(StandardSocketOptions.SO_KEEPALIVE,true)
        socketChannel.setOption(StandardSocketOptions.SO_REUSEADDR,true)

        val context: IoContext? = IoContext.get()
        val adapter = SocketChannelAdapter(channel, context!!.ioProvider!!, this)

        this.sender = adapter
        this.receiver = adapter

        sendDispatcher = AsyncSendDispatcher(sender)
        receiveDispatcher = AsyncReceiveDispatcher(receiver, receivePacketCallback)

        // 启动接收
        receiveDispatcher.start()

    }

    fun chargeToBridge(){
        if(receiveDispatcher is BridgeSocketDispatcher) return

        receiveDispatcher.stop()

        val dispatcher = BridgeSocketDispatcher(receiver)
        receiveDispatcher = dispatcher
        // 启动
        dispatcher.start()
    }

    fun bindToBridge(sender: Sender){
        if(sender == this.sender){
            throw UnsupportedOperationException("Can not set current connector sender to bridge")
        }

        val dispatcher = receiveDispatcher as? BridgeSocketDispatcher ?: throw IllegalStateException("receiveDispatcher is not BridgeSocketDispatcher")
        dispatcher.bindSender(sender)
    }

    fun unBindToBridge(){
        val dispatcher = receiveDispatcher as? BridgeSocketDispatcher ?: throw IllegalStateException("receiveDispatcher is not BridgeSocketDispatcher")
        dispatcher.bindSender(null)
    }

    fun getSender():Sender = sender

    fun send(msg: String) {
        val packet = StringSendPacket(msg)
        sendDispatcher.send(packet)
    }

    fun send(packet: SendPacket<*>) {
        sendDispatcher.send(packet)
    }

    fun schedule(job: ScheduleJob) {
        synchronized(scheduleJobs) {
            if (scheduleJobs.contains(job)) {
                return
            }

            val context: IoContext? = IoContext.get()
            val scheduler: Scheduler? = context?.scheduler
            if (scheduler != null) {
                job.schedule(scheduler)
                scheduleJobs.add(job)
            }

        }
    }

    fun getLastActiveTime(): Long {
        return Math.max(sender.getLastWriteTime(), receiver.getLastReadTime())
    }


    fun fireIdleTimeoutEvent() {
        sendDispatcher.sendHeartbeat()
    }

    fun fireExceptionCaught(throwable: Throwable) {

    }

    override fun onChannelClosed(channel: SocketChannel) {
        CloseUtils.close(this)
        synchronized(scheduleJobs) {
            scheduleJobs.forEach(ScheduleJob::unSchedule)
        }
    }

    override fun close() {
        receiveDispatcher.close()
        sendDispatcher.close()

        receiver.close()
        sender.close()

        channel.close()
    }

    protected abstract fun createNewReceiveFile(length:Long,headerInfo:ByteArray?): File

    protected abstract fun createNewReceiveDirectOutputStream(length:Long,headerInfo:ByteArray?):OutputStream

    private val receivePacketCallback = object : ReceiveDispatcher.ReceivePacketCallback {

        override fun onArrivedNewPacket(type: Byte, length: Long,headerInfo: ByteArray?): ReceivePacket<*, *> {
            return when (type) {
                TYPE_MEMORY_BYTES -> BytesReceivePacket(length)
                TYPE_MEMORY_STRING -> StringReceivePacket(length)
                TYPE_STREAM_FILE -> FileReceivePacket(length, createNewReceiveFile(length,headerInfo))
                TYPE_STREAM_DIRECT -> StreamDirectReceivePacket(createNewReceiveDirectOutputStream(length,headerInfo),length)
                else -> throw UnsupportedOperationException("Unsupported packet type:$type")
            }
        }

        override fun onReceivePacketCompleted(packet: ReceivePacket<*, *>) {
            onReceivedPacket(packet)
        }

        override fun onReceiveHeartbeat() {
            println("$key:[Heartbeat]")
        }
    }

    protected open fun onReceivedPacket(packet: ReceivePacket<*, *>) {
        // println("$key:[New Packet]-Type:${packet.type()}, Length:${packet.length()}")
    }

    fun getKey(): UUID = key
}