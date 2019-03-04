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
                    private val clientHandlerCallback: ClientHandlerCallback){

    private var connector:Connector? = null
    private var writeHandler:ClientWriteHandler
    private val clientInfo:String

    init {
        connector = object : Connector() {
            override fun onChannelClosed(channel: SocketChannel) {
                super.onChannelClosed(channel)
                exitBySelf()
            }

            override fun onReceiveNewMessage(str: String) {
                super.onReceiveNewMessage(str)
                clientHandlerCallback.onNewMessageArrived(this@ClientHandler,str)
            }
        }
        connector?.setup(socketChannel)

        val writeSelector = Selector.open()
        socketChannel.register(writeSelector,SelectionKey.OP_WRITE)
        this.writeHandler = ClientWriteHandler(writeSelector)

        this.clientInfo = socketChannel.remoteAddress.toString()
        println("新客户端连接：$clientInfo")
    }

    fun getClientInfo():String = clientInfo

    fun send(str: String) {
        writeHandler.send(str)
    }

    fun exit(){
        CloseUtils.close(connector!!)
        writeHandler.exit()
        CloseUtils.close(socketChannel)
        println("客户端已退出：$clientInfo")
    }

    fun exitBySelf(){
        exit()
        clientHandlerCallback.onSelfClosed(this)
    }

    inner class ClientWriteHandler(private val selector: Selector){

        private var done = false
        private val byteBuffer = ByteBuffer.allocate(256)
        private val executorService:ExecutorService = Executors.newSingleThreadExecutor()

        fun send(str: String) {
            if(done) return
            executorService.execute(WriteRunnable(str))
        }

        fun exit(){
            done = true
            CloseUtils.close(selector)
            executorService.shutdownNow()
        }

        inner class WriteRunnable(private var msg:String) : Runnable{

            init {
                // 添加换行符
                msg += "\n"
            }

            override fun run() {
                if(this@ClientWriteHandler.done){
                    return
                }

                byteBuffer.clear()
                byteBuffer.put(msg.toByteArray())
                // 反转操作,重点
                byteBuffer.flip()
                try {
                    while (!done && byteBuffer.hasRemaining()){
                        val len = socketChannel.write(byteBuffer)
                        if(len < 0){
                            println("客户端已无法接收数据！")
                            // 退出当前客户端
                            this@ClientHandler.exitBySelf()
                            break
                        }
                    }
                }catch (e:Exception){
                    e.printStackTrace()
                }
            }
        }
    }
}

interface ClientHandlerCallback {
    fun onSelfClosed(clientHandler: ClientHandler)
    fun onNewMessageArrived(clientHandler: ClientHandler,msg:String)
}