package com.mrmedici.server

import com.mrmedici.clink.box.StringReceivePacket
import com.mrmedici.server.handle.ConnectorStringPacketChain
import com.mrmedici.server.handle.DefaultNonConnectorStringPacketChain
import server.handle.ClientHandler

class ServerStatistics{
    var sendSize: Long = 0
    var receiveSize: Long = 0

    fun statisticsChain():StatisticsConnectorStringPacketChain{
        return StatisticsConnectorStringPacketChain()
    }

    inner class StatisticsConnectorStringPacketChain : ConnectorStringPacketChain(){
        override fun consume(handler: ClientHandler, model: StringReceivePacket): Boolean {
            // 接收数据自增
            receiveSize++
            return false
        }
    }
}