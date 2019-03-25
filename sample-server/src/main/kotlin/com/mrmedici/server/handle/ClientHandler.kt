package server.handle

import com.mrmedici.clink.core.Connector
import com.mrmedici.clink.core.ReceivePacket
import com.mrmedici.clink.core.TYPE_MEMORY_STRING
import com.mrmedici.clink.utils.CloseUtils
import com.mrmedici.foo.Foo
import java.io.*
import java.net.Socket
import java.nio.ByteBuffer
import java.nio.channels.SelectionKey
import java.nio.channels.Selector
import java.nio.channels.SocketChannel
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class ClientHandler(private val socketChannel: SocketChannel,
                    private val clientHandlerCallback: ClientHandlerCallback,
                    private val cachePath:File):Connector(){

    private val clientInfo = socketChannel.remoteAddress.toString()

    init {
        setup(socketChannel)
        println("新客户端连接：$clientInfo")
    }

    override fun onChannelClosed(channel: SocketChannel) {
        super.onChannelClosed(channel)
        exitBySelf()
    }

    override fun createNewReceiveFile(): File {
        return Foo.createRandomTemp(cachePath)
    }

    override fun onReceivedPacket(packet: ReceivePacket<*, *>) {
        super.onReceivedPacket(packet)
        if(packet.type() == TYPE_MEMORY_STRING){
            val str:String? = packet.entity() as String
            str?.let{
                // println("$key:$str")
                clientHandlerCallback.onNewMessageArrived(this,str)
            }
        }
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