package com.mrmedici.foo.handle

import com.mrmedici.clink.core.Connector
import server.handle.ConnectorHandler

class DefaultPrintConnectorCloseChain : ConnectorCloseChain(){
    override fun consume(handler: ConnectorHandler, model: Connector): Boolean {
        println("${handler.getClientInfo()}:Exit!, Key:${handler.getKey()}")
        return false
    }
}