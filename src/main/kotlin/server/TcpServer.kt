package server

import server.handle.ClientHandler
import server.handle.CloseNotify
import java.io.IOException
import java.net.ServerSocket

class TcpServer(private val port:Int){

    private var mListener:ClientListener? = null
    private val clientHandlerList = ArrayList<ClientHandler>()

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

        clientHandlerList.forEach { it.exit() }
        clientHandlerList.clear()
    }

    fun broadcast(str: String) {
        clientHandlerList.forEach { it.send(str) }
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
                    val clientHandler = ClientHandler(client, object : CloseNotify {
                        override fun onSelfClosed(clientHandler: ClientHandler) {
                            this@TcpServer.clientHandlerList.remove(clientHandler)
                        }
                    })
                    // 启动线程
                    clientHandler.readToPrint()
                    this@TcpServer.clientHandlerList.add(clientHandler)
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