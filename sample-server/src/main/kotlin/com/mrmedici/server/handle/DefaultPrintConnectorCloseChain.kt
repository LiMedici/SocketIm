package com.mrmedici.server.handle

import com.mrmedici.clink.core.Connector
import server.handle.ClientHandler

class DefaultPrintConnectorCloseChain : ConnectorCloseChain(){
    override fun consume(handler: ClientHandler, model: Connector): Boolean {
        println("${handler.getClientInfo()}:Exit!, Key:${handler.getKey()}")
        return false
    }
}