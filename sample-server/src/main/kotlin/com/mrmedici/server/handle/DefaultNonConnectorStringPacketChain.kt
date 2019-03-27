package com.mrmedici.server.handle

import com.mrmedici.clink.box.StringReceivePacket
import server.handle.ClientHandler

/**
 * 默认String接收节点，不做任何事情
 */
class DefaultNonConnectorStringPacketChain : ConnectorStringPacketChain(){
    override fun consume(handler: ClientHandler, model: StringReceivePacket): Boolean {
        return false
    }
}