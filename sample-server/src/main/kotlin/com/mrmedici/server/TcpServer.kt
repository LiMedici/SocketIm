package server

import com.mrmedici.clink.box.StringReceivePacket
import com.mrmedici.clink.core.Connector
import com.mrmedici.clink.core.schedule.IdleTimeoutScheduleJob
import com.mrmedici.clink.utils.CloseUtils
import com.mrmedici.foo.*
import com.mrmedici.server.*
import com.mrmedici.foo.handle.ConnectorCloseChain
import com.mrmedici.foo.handle.ConnectorStringPacketChain
import com.mrmedici.server.audio.AudioRoom
import server.handle.ConnectorHandler
import java.io.File
import java.io.IOException
import java.net.InetSocketAddress
import java.nio.channels.SelectionKey
import java.nio.channels.ServerSocketChannel
import java.nio.channels.SocketChannel
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class TcpServer(private val port: Int,
                private val cachePath: File) : AcceptListener,GroupMessageAdapter {
    private lateinit var accepter: ServerAccepter
    private var server: ServerSocketChannel? = null
    private val clientHandlerList = ArrayList<ConnectorHandler>()
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

        var connectorHandlers:Array<ConnectorHandler?> = arrayOfNulls<ConnectorHandler>(0)
        synchronized(clientHandlerList) {
            connectorHandlers = clientHandlerList.toArray(connectorHandlers)
            clientHandlerList.clear()
        }

        connectorHandlers.forEach { it?.exit() }
        CloseUtils.close(server!!)
    }

    fun broadcast(str: String) {
        val notificationStr = "系统通知: $str"
        var connectorHandlers:Array<ConnectorHandler?> = arrayOfNulls<ConnectorHandler>(0)
        synchronized(clientHandlerList) {
            connectorHandlers = clientHandlerList.toArray(connectorHandlers)
        }

        connectorHandlers.forEach { sendMessageToClient(it!!,notificationStr) }
    }

    override fun sendMessageToClient(handler: ConnectorHandler, msg:String){
        handler.send(msg)
        statistics.sendSize++
    }

    fun getStatusString(): Array<Any> {
        return arrayOf("客户端数量：${clientHandlerList.size}","发送数量：${statistics.sendSize}","接收数量：${statistics.receiveSize}")
    }

    override fun onNewSocketArrived(channel: SocketChannel) {
        try {
            // 客户端构建异步线程
            val clientHandler = ConnectorHandler(channel,this@TcpServer.cachePath)
            println("${clientHandler.getClientInfo()}:Connected!")

            clientHandler.stringPacketChain.appendLast(statistics.statisticsChain())
                    .appendLast(ParseCommandConnectorStringPacketChain())
                    .appendLast(ParseAudioStreamCommandStringPacketChain())
            clientHandler.closeChain.appendLast(RemoveAudioQueueOnConnectorCloseChain())
                    .appendLast(RemoveQueueOnConnectorCloseChain())

            val scheduleJob = IdleTimeoutScheduleJob(20,TimeUnit.SECONDS,clientHandler)
            clientHandler.schedule(scheduleJob)


            // 添加同步处理
            synchronized(clientHandlerList) {
                this@TcpServer.clientHandlerList.add(clientHandler)
                println("当前客户端数量：${this@TcpServer.clientHandlerList.size}")
            }

            // 回送客户端在服务器端的唯一标志
            sendMessageToClient(clientHandler, COMMAND_INFO_NAME + clientHandler.getKey().toString())
        } catch (e: IOException) {
            e.printStackTrace()
            println("客户端连接异常:${e.message}")
        }
    }

    private fun findConnectorFromKey(key:String):ConnectorHandler?{
        synchronized(clientHandlerList){
            for (handler in clientHandlerList){
                if(handler.getKey().toString().equals(key,true)){
                    return handler
                }
            }
        }

        return null
    }

    // 通过音频命令控制链接寻找数据传输链接，未找到则发送错误
    private fun findAudioStreamConnector(handler: ConnectorHandler):ConnectorHandler?{
        val connectorHandler:ConnectorHandler? = this.audioCmdToStreamMap[handler]
        if(connectorHandler == null){
            sendMessageToClient(handler, COMMAND_INFO_AUDIO_ERROR)
            return null
        }else{
            return connectorHandler
        }
    }

    // 通过音频数据传输流寻找命令控制流
    private fun findAudioCmdConnector(handler: ConnectorHandler):ConnectorHandler?{
        return audioStreamToCmdMap[handler]
    }

    // 创建房间的操作
    private fun createNewRoom():AudioRoom{
        var room:AudioRoom
        do{
            room = AudioRoom()
        }while (audioRoomMap.containsKey(room.getRoomCode()))
        audioRoomMap[room.getRoomCode()] = room
        return room
    }

    // 加入房间
    private fun joinRoom(room: AudioRoom,streamConnector:ConnectorHandler):Boolean{
        if(room.enterRoom(streamConnector)){
            audioStreamRoomMap[streamConnector] = room
            return true
        }
        return false
    }

    fun dissolveRoom(streamConnector: ConnectorHandler){
        val room = audioStreamRoomMap[streamConnector]
        room?:return

        val connectors = room.getConnectors()
        for (connector in connectors){
            // 解除桥接
            connector.unBindToBridge()
            // 移除缓存
            audioStreamRoomMap.remove(connector)
            if(connector != streamConnector){
                // 退出房间，并 获取对方
                sendStreamConnectorMessage(connector, COMMAND_INFO_AUDIO_STOP)
            }
        }

        // 销毁房间
        audioRoomMap.remove(room.getRoomCode())
    }

    private fun sendStreamConnectorMessage(streamConnector: ConnectorHandler,msg: String){
        streamConnector?.let {
            val audioCmdConnector:ConnectorHandler? = this.findAudioStreamConnector(it)
            if(audioCmdConnector!=null){
                sendMessageToClient(audioCmdConnector,msg)
            }
        }
    }

    // 音频命令控制与数据流传输链接映射表
    private val audioCmdToStreamMap = HashMap<ConnectorHandler,ConnectorHandler>(100)
    private val audioStreamToCmdMap = HashMap<ConnectorHandler,ConnectorHandler>(100)

    // 房间映射表，房间号-房间的映射
    private val audioRoomMap = HashMap<String,AudioRoom>(50)
    // 链接与房间的映射，音视频链接-房间的映射
    private val audioStreamRoomMap = HashMap<ConnectorHandler,AudioRoom>(50)

    private inner class RemoveQueueOnConnectorCloseChain : ConnectorCloseChain(){
        override fun consume(handler: ConnectorHandler, model: Connector): Boolean {
            synchronized(this@TcpServer.clientHandlerList){
                this@TcpServer.clientHandlerList.remove(handler)
                // 移除群聊的客户端
                val group = groups[DEFAULT_GROUP_NAME]
                group?.removeMember(handler)
                return true
            }
        }
    }

    private inner class RemoveAudioQueueOnConnectorCloseChain : ConnectorCloseChain(){
        override fun consume(handler: ConnectorHandler, model: Connector): Boolean {
            if(audioCmdToStreamMap.containsKey(handler)){
                // 命令链接断开
                audioCmdToStreamMap.remove(handler)
            }else if(audioStreamToCmdMap.containsKey(handler)){
                // 流断开
                audioStreamToCmdMap.remove(handler)
                // 解散房间
                dissolveRoom(handler)
            }

            return false
        }
    }

    private inner class ParseCommandConnectorStringPacketChain : ConnectorStringPacketChain(){
        override fun consume(handler: ConnectorHandler, model: StringReceivePacket): Boolean {
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

        override fun consumeAgain(handler: ConnectorHandler, model: StringReceivePacket): Boolean {
            // 捡漏的模式，当我们第一次未消费，然后又没有加入群，自然没有后续的节点消费
            // 此时我们进行第二次消费，返回发送过来的消息
            sendMessageToClient(handler,model.entity()!!)
            return true
        }
    }

    private inner class ParseAudioStreamCommandStringPacketChain : ConnectorStringPacketChain(){
        override fun consume(handler: ConnectorHandler, model: StringReceivePacket): Boolean {
            val str = model.entity()?:return false

            when{
                str.startsWith(COMMAND_CONNECTOR_BIND) -> {
                    val key = str.substring(COMMAND_CONNECTOR_BIND.length)

                    val audioStreamConnector = this@TcpServer.findConnectorFromKey(key)
                    if(audioStreamConnector != null){
                        // 添加绑定关系
                        audioCmdToStreamMap[handler] = audioStreamConnector
                        audioStreamToCmdMap[audioStreamConnector] = handler

                        // 转换为桥接模式
                        audioStreamConnector.chargeToBridge()
                    }
                }

                str.startsWith(COMMAND_AUDIO_CREATE_ROOM) -> {
                    // 创建房间操作
                    val audioStreamConnector:ConnectorHandler? = this@TcpServer.findAudioStreamConnector(handler)
                    if(audioStreamConnector != null){
                        // 随机创建房间
                        val room = createNewRoom()
                        // 加入一个客户端
                        joinRoom(room,audioStreamConnector)
                        // 发送成功消息
                        sendMessageToClient(handler, COMMAND_INFO_AUDIO_ROOM + room.getRoomCode())
                    }
                }

                str.startsWith(COMMAND_AUDIO_LEAVE_ROOM) -> {
                    // 离开房间命令
                    val audioStreamConnector:ConnectorHandler? = this@TcpServer.findAudioStreamConnector(handler)
                    if(audioStreamConnector != null){
                        // 任何一人离开都销毁房间
                        dissolveRoom(audioStreamConnector)
                        // 发送成功消息
                        sendMessageToClient(handler, COMMAND_INFO_AUDIO_STOP)
                    }
                }

                str.startsWith(COMMAND_AUDIO_JOIN_ROOM) -> {
                    // 加入房间操作
                    val audioStreamConnector = findAudioStreamConnector(handler)
                    if(audioStreamConnector != null){
                        // 取得房间号
                        val roomCode = str.substring(COMMAND_AUDIO_JOIN_ROOM.length)
                        val room = this@TcpServer.audioRoomMap[roomCode]
                        // 如果找到了房间，就走后面流程
                        if(room != null && joinRoom(room,audioStreamConnector)){
                            // 对方
                            val theOtherConnector = room.getTheOtherHandler(audioStreamConnector)
                            theOtherConnector?.let {
                                // 相互搭建桥接
                                it.bindToBridge(audioStreamConnector.getSender())
                                audioStreamConnector.bindToBridge(it.getSender())

                                // 成功加入房间
                                sendMessageToClient(handler, COMMAND_INFO_AUDIO_START)
                                // 给对方发送可聊天的消息
                                sendStreamConnectorMessage(theOtherConnector, COMMAND_INFO_AUDIO_START)
                            }
                        }else{
                            // 放假未找到，或者满员
                            sendMessageToClient(handler, COMMAND_INFO_AUDIO_ERROR)
                        }

                    }
                }
            }

            return true
        }

    }
}