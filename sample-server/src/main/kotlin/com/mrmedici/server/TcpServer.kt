package server

import server.handle.ClientHandler
import server.handle.ClientHandlerCallback
import java.io.IOException
import java.net.ServerSocket
import java.util.concurrent.Executors

class TcpServer(private val port:Int) : ClientHandlerCallback{
    private var mListener:ClientListener? = null
    private val clientHandlerList = ArrayList<ClientHandler>()
    private val forwardingThreadPoolExecutor = Executors.newSingleThreadExecutor()

    fun start():Boolean {
        try {
            val listener = ClientListener(port)
            mListener = listener
            listener.start()
        }catch (e:IOException){
            e.printStackTrace()
            return false
        }

        return true
    }

    fun stop() {
        mListener?.exit()

        synchronized(this@TcpServer){
            clientHandlerList.forEach { it.exit() }
            clientHandlerList.clear()
        }

        forwardingThreadPoolExecutor.shutdownNow()
    }

    @Synchronized
    fun broadcast(str: String) {
        clientHandlerList.forEach { it.send(str) }
    }

    @Synchronized
    override fun onSelfClosed(clientHandler: ClientHandler) {
        this@TcpServer.clientHandlerList.remove(clientHandler)
    }

    override fun onNewMessageArrived(handler: ClientHandler, msg: String) {
        println("Received-${handler.getClientInfo()}:$msg")
        forwardingThreadPoolExecutor.execute({
            synchronized(this@TcpServer){
                clientHandlerList
                        .filter { it !== handler }
                        .forEach { it.send(msg) }
            }
        })
    }

    private inner class ClientListener(port:Int) : Thread() {

        private var server: ServerSocket = ServerSocket(port)
        private var done = false

        init {
            println("服务器信息：${server.inetAddress} P：${server.localPort}")
        }

        override fun run() {

            println("服务器准备就绪~")

            // 等待客户端连接
            do{
                try {
                    val client = server.accept()

                    // 客户端构建异步线程
                    val clientHandler = ClientHandler(client, this@TcpServer)
                    // 启动线程
                    clientHandler.readToPrint()
                    // 添加同步处理
                    synchronized(this@TcpServer){
                        this@TcpServer.clientHandlerList.add(clientHandler)
                    }
                }catch (e:IOException){
                    e.printStackTrace()
                }
            }while (!done)

            println("服务器已关闭！")
        }

        fun exit(){
            done = true
            server.close()
        }
    }
}