package com.mrmedici.server

import com.mrmedici.clink.box.StringReceivePacket
import com.mrmedici.server.handle.ConnectorStringPacketChain
import server.handle.ClientHandler

class Group(private val name:String,
            private val adapter:GroupMessageAdapter){

    private val members = ArrayList<ClientHandler>()

    fun getName():String = name

    fun addMember(handler: ClientHandler):Boolean{
        synchronized(members){
            if(!members.contains(handler)){
                members.add(handler)
                handler.stringPacketChain.appendLast(ForwardConnectorStringPacketChain())
                println("Group[$name] add new member:${handler.getClientInfo()}")
                return true
            }
        }
        return false
    }

    fun removeMember(handler: ClientHandler):Boolean{
        synchronized(members){
            if(members.remove(handler)){
                handler.stringPacketChain.remove(ForwardConnectorStringPacketChain::class.java)
                println("Group[$name] leave member:${handler.getClientInfo()}")
                return true
            }
        }

        return false
    }

    private inner class ForwardConnectorStringPacketChain : ConnectorStringPacketChain(){
        override fun consume(handler: ClientHandler, model: StringReceivePacket): Boolean {
            synchronized(members){
                for (member in members){
                    if(member === handler) continue
                    adapter.sendMessageToClient(member,model.entity()!!)
                }

                return true
            }
        }
    }
}

interface GroupMessageAdapter{
    fun sendMessageToClient(handler: ClientHandler,msg:String)
}