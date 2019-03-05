package server.handle

import com.mrmedici.clink.core.Connector
import com.mrmedici.clink.utils.CloseUtils
import java.io.*
import java.net.Socket
import java.nio.ByteBuffer
import java.nio.channels.SelectionKey
import java.nio.channels.Selector
import java.nio.channels.SocketChannel
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class ClientHandler(private val socketChannel: SocketChannel,
                    private val clientHandlerCallback: ClientHandlerCallback):Connector(){

    private val clientInfo = socketChannel.remoteAddress.toString()

    init {
        setup(socketChannel)
        println("新客户端连接：$clientInfo")
    }

    override fun onReceiveNewMessage(str: String) {
        super.onReceiveNewMessage(str)
        clientHandlerCallback.onNewMessageArrived(this,str)
    }

    override fun onChannelClosed(channel: SocketChannel) {
        super.onChannelClosed(channel)
        exitBySelf()
    }

    private fun exitBySelf(){
        exit()
        clientHandlerCallback.onSelfClosed(this)
    }

    fun exit(){
        CloseUtils.close(this)
        println("客户端已退出：$clientInfo")
    }
}

interface ClientHandlerCallback {
    fun onSelfClosed(clientHandler: ClientHandler)
    fun onNewMessageArrived(clientHandler: ClientHandler,msg:String)
}