package com.mrmedici.foo.handle

import com.mrmedici.clink.box.StringReceivePacket
import server.handle.ConnectorHandler

/**
 * 默认String接收节点，不做任何事情
 */
class DefaultNonConnectorStringPacketChain : ConnectorStringPacketChain(){
    override fun consume(handler: ConnectorHandler, model: StringReceivePacket): Boolean {
        return false
    }
}