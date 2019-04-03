package com.mrmedici.server.audio

import com.sun.xml.internal.ws.handler.ClientLogicalHandlerTube
import server.handle.ConnectorHandler
import java.util.*
import kotlin.collections.ArrayList

class AudioRoom{
    private val roomCode:String
    @Volatile
    private var handler1:ConnectorHandler? = null
    @Volatile
    private var handler2:ConnectorHandler? = null

    constructor(){
        roomCode = getRandomString(5)
    }

    fun getRoomCode():String = roomCode

    fun getConnectors():Array<ConnectorHandler>{
        val handlers = ArrayList<ConnectorHandler>(2)
        if(handler1 != null){
            handlers.add(handler1!!)
        }
        if(handler2 != null){
            handlers.add(handler2!!)
        }

        return handlers.toArray(arrayOfNulls(0))
    }

    // 获取对方
    fun getTheOtherHandler(handler: ConnectorHandler):ConnectorHandler?{
        return if(handler1 == null || handler1 == handler) handler2 else handler1
    }

    // 房间是否可以聊天，是否两个客户端都具有
    @Synchronized
    fun isEnable():Boolean{
        return handler1 != null && handler2 != null
    }

    // 加入房间
    @Synchronized
    fun enterRoom(handler: ConnectorHandler):Boolean{
        return when {
            handler1 == null -> {
                handler1 = handler
                true
            }
            handler2 == null -> {
                handler2 = handler
                true
            }
            else -> false
        }
    }

    // 退出房间
    @Synchronized
    public fun exitRoom(handler: ConnectorHandler):ConnectorHandler?{
        if(handler1 == handler){
            handler1 = null
        }else if(handler2 == handler){
            handler2 = null
        }

        return if(handler1 == null) handler2 else handler1
    }

    // 生成一个简单的字符串
    private fun getRandomString(length: Int): String {
        val str = "123456789"
        val random = Random()
        val sb = StringBuilder()
        for (index in 0 until length){
            val number = random.nextInt(str.length)
            sb.append(str[number])
        }
        return sb.toString()
    }
}