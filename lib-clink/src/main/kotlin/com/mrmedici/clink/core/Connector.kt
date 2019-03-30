package com.mrmedici.clink.core

import com.mrmedici.clink.box.BytesReceivePacket
import com.mrmedici.clink.box.FileReceivePacket
import com.mrmedici.clink.box.StringReceivePacket
import com.mrmedici.clink.box.StringSendPacket
import com.mrmedici.clink.impl.OnChannelStatusChangedListener
import com.mrmedici.clink.impl.SocketChannelAdapter
import com.mrmedici.clink.impl.async.AsyncReceiveDispatcher
import com.mrmedici.clink.impl.async.AsyncSendDispatcher
import com.mrmedici.clink.utils.CloseUtils
import java.io.Closeable
import java.io.File
import java.io.IOException
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

        val context: IoContext? = IoContext.get()
        val adapter = SocketChannelAdapter(channel, context!!.ioProvider!!, this)

        this.sender = adapter
        this.receiver = adapter

        sendDispatcher = AsyncSendDispatcher(sender)
        receiveDispatcher = AsyncReceiveDispatcher(receiver, receivePacketCallback)

        // 启动接收
        receiveDispatcher.start()

    }

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

    protected abstract fun createNewReceiveFile(): File

    private val receivePacketCallback = object : ReceiveDispatcher.ReceivePacketCallback {

        override fun onArrivedNewPacket(type: Byte, length: Long): ReceivePacket<*, *> {
            return when (type) {
                TYPE_MEMORY_BYTES -> BytesReceivePacket(length)
                TYPE_MEMORY_STRING -> StringReceivePacket(length)
                TYPE_STREAM_FILE -> FileReceivePacket(length, createNewReceiveFile())
                TYPE_STREAM_DIRECT -> BytesReceivePacket(length)
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