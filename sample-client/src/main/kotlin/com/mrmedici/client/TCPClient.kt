package client

import client.bean.ServerInfo
import com.mrmedici.clink.core.Connector
import com.mrmedici.clink.utils.CloseUtils
import java.io.*
import java.net.Inet4Address
import java.net.InetSocketAddress
import java.net.Socket
import java.net.SocketTimeoutException
import java.nio.channels.SocketChannel

class TCPClient(private val socketChannel: SocketChannel) : Connector(){

    init {
        setup(socketChannel)
    }

    override fun onChannelClosed(channel: SocketChannel) {
        super.onChannelClosed(channel)
        println("连接已关闭，无法读取数据！")
    }

    override fun onReceiveNewMessage(str: String) {
        super.onReceiveNewMessage(str)
    }

    fun exit(){
        CloseUtils.close(this)
    }

    companion object {
        @Throws(IOException::class)
        fun startWith(info:ServerInfo):TCPClient?{
            val socketChannel = SocketChannel.open()

            // 链接到本地20000端口，超时时间设置为3秒，超出则抛出异常
            socketChannel.connect(InetSocketAddress(Inet4Address.getByName(info.address), info.port))

            println("已发起服务器连接，并进入后续流程~")
            println("客户端信息:${socketChannel.localAddress}")
            println("服务端信息:${socketChannel.remoteAddress}")

            try {
                return TCPClient(socketChannel)
            } catch (e: IOException) {
                println("连接异常")
                CloseUtils.close(socketChannel)
            }

            return null
        }
    }


}