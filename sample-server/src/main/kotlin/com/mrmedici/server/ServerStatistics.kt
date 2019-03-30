package com.mrmedici.server

import com.mrmedici.clink.box.StringReceivePacket
import com.mrmedici.foo.handle.ConnectorStringPacketChain
import server.handle.ConnectorHandler

class ServerStatistics{
    var sendSize: Long = 0
    var receiveSize: Long = 0

    fun statisticsChain():StatisticsConnectorStringPacketChain{
        return StatisticsConnectorStringPacketChain()
    }

    inner class StatisticsConnectorStringPacketChain : ConnectorStringPacketChain(){
        override fun consume(handler: ConnectorHandler, model: StringReceivePacket): Boolean {
            // 接收数据自增
            receiveSize++
            return false
        }
    }
}