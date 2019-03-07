package com.mrmedici.clink.core

import java.io.Closeable

/**
 * 接收的数据调度封装
 * 把一份或者多分IoArgs组合成一分Packet
 */
interface ReceiveDispatcher : Closeable{

    fun start()
    fun stop()

    interface ReceivePacketCallback{
        fun onReceivePacketCompleted(packet:ReceivePacket<*>)
    }

}