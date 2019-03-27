package server

import com.mrmedici.clink.box.StringReceivePacket
import com.mrmedici.clink.core.Connector
import com.mrmedici.clink.utils.CloseUtils
import com.mrmedici.foo.COMMAND_GROUP_JOIN
import com.mrmedici.foo.COMMAND_GROUP_LEAVE
import com.mrmedici.foo.DEFAULT_GROUP_NAME
import com.mrmedici.server.*
import com.mrmedici.server.handle.ConnectorCloseChain
import com.mrmedici.server.handle.ConnectorStringPacketChain
import server.handle.ClientHandler
import java.io.File
import java.io.IOException
import java.net.InetSocketAddress
import java.nio.channels.SelectionKey
import java.nio.channels.Selector
import java.nio.channels.ServerSocketChannel
import java.nio.channels.SocketChannel
import java.util.concurrent.Executors

class TcpServer(private val port: Int,
                private val cachePath: File) : AcceptListener,GroupMessageAdapter {
    private lateinit var accepter: ServerAccepter
    private var server: ServerSocketChannel? = null
    private val clientHandlerList = ArrayList<ClientHandler>()
    private val deliveryPool = Executors.newSingleThreadExecutor()
    private val statistics = ServerStatistics()
    private val groups = HashMap<String,Group>()

    init {
        groups[DEFAULT_GROUP_NAME] = Group(DEFAULT_GROUP_NAME,this)
    }

    fun start(): Boolean {
        try {
            // 启动Acceptor线程
            val accepter = ServerAccepter(this)

            val server = ServerSocketChannel.open()
            // 设置为非阻塞状态
            server.configureBlocking(false)
            // 绑定本地端口
            server.socket().bind(InetSocketAddress(port))

            server.register(accepter.selector, SelectionKey.OP_ACCEPT)

            this.server = server
            this.accepter = accepter

            // 线程需要启动
            accepter.start()
            return if(accepter.awaitRunning()){
                println("服务器准备就绪~")
                println("服务器信息：${server.localAddress}")
                true
            }else{
                println("启动异常!")
                false
            }
        } catch (e: IOException) {
            e.printStackTrace()
            return false
        }

        return true
    }

    fun stop() {
        accepter.exit()

        synchronized(clientHandlerList) {
            clientHandlerList.forEach { it.exit() }
            clientHandlerList.clear()
        }

        CloseUtils.close(server!!)

        deliveryPool.shutdownNow()
    }

    fun broadcast(str: String) {
        val notificationStr = "系统通知: $str"
        synchronized(clientHandlerList){
            clientHandlerList.forEach { sendMessageToClient(it,notificationStr) }
        }
    }

    override fun sendMessageToClient(handler: ClientHandler,msg:String){
        handler.send(msg)
        statistics.sendSize++
    }

    fun getStatusString(): Array<Any> {
        return arrayOf("客户端数量：${clientHandlerList.size}","发送数量：${statistics.sendSize}","接收数量：${statistics.receiveSize}")
    }

    override fun onNewSocketArrived(channel: SocketChannel) {
        try {
            // 客户端构建异步线程
            val clientHandler = ClientHandler(channel, deliveryPool,this@TcpServer.cachePath)
            println("${clientHandler.getClientInfo()}:Connected!")

            clientHandler.stringPacketChain.appendLast(statistics.statisticsChain())
                    .appendLast(ParseCommandConnectorStringPacketChain())
            clientHandler.closeChain.appendLast(RemoveQueueOnConnectorCloseChain())

            // 添加同步处理
            synchronized(this@TcpServer) {
                this@TcpServer.clientHandlerList.add(clientHandler)
                println("当前客户端数量：${this@TcpServer.clientHandlerList.size}")
            }
        } catch (e: IOException) {
            e.printStackTrace()
            println("客户端连接异常:${e.message}")
        }
    }

    private inner class RemoveQueueOnConnectorCloseChain : ConnectorCloseChain(){
        override fun consume(handler: ClientHandler, model: Connector): Boolean {
            synchronized(this@TcpServer.clientHandlerList){
                this@TcpServer.clientHandlerList.remove(handler)
                // 移除群聊的客户端
                val group = groups[DEFAULT_GROUP_NAME]
                group?.removeMember(handler)
                return true
            }
        }
    }

    private inner class ParseCommandConnectorStringPacketChain : ConnectorStringPacketChain(){
        override fun consume(handler: ClientHandler, model: StringReceivePacket): Boolean {
            val str:String? = model.entity()
            return when {
                str == null -> false
                str.startsWith(COMMAND_GROUP_JOIN) -> {
                    val group:Group = groups[DEFAULT_GROUP_NAME]!!
                    if(group.addMember(handler)){
                        sendMessageToClient(handler,"Join Group:${group.getName()}")
                    }
                    true
                }
                str.startsWith(COMMAND_GROUP_LEAVE) -> {
                    val group:Group = groups[DEFAULT_GROUP_NAME]!!
                    if(group.removeMember(handler)){
                        sendMessageToClient(handler,"Leave Group:${group.getName()}")
                    }
                    true
                }
                else -> false
            }
        }

        override fun consumeAgain(handler: ClientHandler, model: StringReceivePacket): Boolean {
            // 捡漏的模式，当我们第一次未消费，然后又没有加入群，自然没有后续的节点消费
            // 此时我们进行第二次消费，返回发送过来的消息
            sendMessageToClient(handler,model.entity()!!)
            return true
        }
    }
}