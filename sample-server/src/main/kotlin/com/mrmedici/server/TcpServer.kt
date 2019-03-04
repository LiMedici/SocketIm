package server

import com.mrmedici.clink.utils.CloseUtils
import server.handle.ClientHandler
import server.handle.ClientHandlerCallback
import java.io.IOException
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.nio.channels.SelectionKey
import java.nio.channels.Selector
import java.nio.channels.ServerSocketChannel
import java.util.concurrent.Executors

class TcpServer(private val port:Int) : ClientHandlerCallback{
    private var listener:ClientListener? = null
    private var selector:Selector? = null
    private var server:ServerSocketChannel? = null
    private val clientHandlerList = ArrayList<ClientHandler>()
    private val forwardingThreadPoolExecutor = Executors.newSingleThreadExecutor()

    fun start():Boolean {
        try {
            selector = Selector.open()
            val server = ServerSocketChannel.open()
            // 设置为非阻塞状态
            server.configureBlocking(false)
            // 绑定本地端口
            server.socket().bind(InetSocketAddress(port))

            server.register(selector,SelectionKey.OP_ACCEPT)

            this.server = server

            println("服务器信息：${server.localAddress}")

            // 启动客户端监听
            val listener = ClientListener()
            this.listener = listener
            listener.start()
        }catch (e:IOException){
            e.printStackTrace()
            return false
        }

        return true
    }

    fun stop() {
        listener?.exit()

        CloseUtils.close(server!!)
        CloseUtils.close(selector!!)


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
        forwardingThreadPoolExecutor.execute({
            synchronized(this@TcpServer){
                clientHandlerList
                        .filter { it !== handler }
                        .forEach { it.send(msg) }
            }
        })
    }

    private inner class ClientListener : Thread() {
        private var done = false

        override fun run() {
            val selector = this@TcpServer.selector
            println("服务器准备就绪~")
            do{
                try {
                    // 等待客户端连接
                    if(selector!!.select() == 0){
                        if(done){
                            break
                        }
                        continue
                    }

                    val iterator = selector.selectedKeys().iterator()
                    while (iterator.hasNext()){
                        if(done) break
                        val selectionKey = iterator.next()
                        iterator.remove()
                        // 检查当前Key的状态是否是我们关注的
                        // 客户端到达状态
                        if(selectionKey.isValid && selectionKey.isAcceptable){
                            val serverSocketChannel = selectionKey.channel() as ServerSocketChannel
                            // 非阻塞状态拿到客户端连接
                            val socketChannel = serverSocketChannel.accept()

                            try {
                                // 客户端构建异步线程
                                val clientHandler = ClientHandler(socketChannel, this@TcpServer)
                                // 添加同步处理
                                synchronized(this@TcpServer) {
                                    this@TcpServer.clientHandlerList.add(clientHandler)
                                }
                            }catch (e:IOException){
                                e.printStackTrace()
                                println("客户端连接异常:${e.message}")
                            }
                        }
                    }
                }catch (e:IOException){
                    e.printStackTrace()
                }
            }while (!done)

            println("服务器已关闭！")
        }

        fun exit(){
            done = true
            // 唤醒当前的阻塞
            selector!!.wakeup()
        }
    }
}