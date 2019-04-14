package client

import client.bean.ServerInfo
import com.mrmedici.clink.box.StringReceivePacket
import com.mrmedici.clink.core.Connector
import com.mrmedici.clink.core.ReceivePacket
import com.mrmedici.clink.core.TYPE_MEMORY_STRING
import com.mrmedici.clink.utils.CloseUtils
import com.mrmedici.foo.Foo
import com.mrmedici.foo.handle.ConnectorStringPacketChain
import server.handle.ConnectorHandler
import java.io.*
import java.net.Inet4Address
import java.net.InetSocketAddress
import java.net.Socket
import java.net.SocketTimeoutException
import java.nio.channels.SocketChannel

class TCPClient(socketChannel: SocketChannel,
                cachePath:File,printReceiveString: Boolean) : ConnectorHandler(socketChannel,cachePath){

    init {
        if(printReceiveString)
            stringPacketChain.appendLast(PrintStringPacketChain())
    }

    private class PrintStringPacketChain : ConnectorStringPacketChain(){
        override fun consume(handler: ConnectorHandler, model: StringReceivePacket): Boolean {
            val str:String? = model.entity()?:return false
            println(str)
            return true
        }
    }

    companion object {
        @Throws(IOException::class)
        fun startWith(info:ServerInfo,cachePath: File,printReceiveString:Boolean = true):TCPClient?{
            val socketChannel = SocketChannel.open()

            // 链接到本地20000端口，超时时间设置为3秒，超出则抛出异常
            socketChannel.connect(InetSocketAddress(Inet4Address.getByName(info.address), info.port))

            println("已发起服务器连接，并进入后续流程~")
            println("客户端信息:${socketChannel.localAddress}")
            println("服务端信息:${socketChannel.remoteAddress}")

            try {
                return TCPClient(socketChannel,cachePath,printReceiveString)
            } catch (e: IOException) {
                println("连接异常")
                CloseUtils.close(socketChannel)
            }

            return null
        }
    }


}