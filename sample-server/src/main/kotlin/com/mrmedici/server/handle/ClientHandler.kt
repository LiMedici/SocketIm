package server.handle

import com.mrmedici.clink.utils.CloseUtils
import java.io.*
import java.net.Socket
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class ClientHandler(private val socket: Socket,
                    private val clientHandlerCallback: ClientHandlerCallback){

    private var readHandler:ClientReadHandler
    private var writeHandler:ClientWriteHandler
    private val clientInfo:String

    init {
        this.readHandler = ClientReadHandler(socket.getInputStream())
        this.writeHandler = ClientWriteHandler(socket.getOutputStream())
        this.clientInfo = "A[${socket.inetAddress}] P[${socket.port}]"
        println("新客户端连接：$clientInfo")
    }

    fun getClientInfo():String = clientInfo

    fun send(str: String) {
        writeHandler.send(str)
    }

    fun readToPrint() {
        readHandler.start()
    }

    fun exit(){
        readHandler.exit()
        writeHandler.exit()
        CloseUtils.close(socket)
        println("客户端已退出：${socket.inetAddress} P：${socket.port}")
    }

    fun exitBySelf(){
        exit()
        clientHandlerCallback.onSelfClosed(this)
    }

    inner class ClientReadHandler(private val inputStream:InputStream) : Thread(){

        private var done = false

        override fun run() {

            try{
                // 得到输入流，用于接收数据
                val socketInput = BufferedReader(InputStreamReader(inputStream))

                do{
                    val str = socketInput.readLine()
                    if(str == null){
                        println("客户端已无法读取数据！")
                        // 退出当前客户端
                        this@ClientHandler.exitBySelf()
                        break
                    }

                    // 通知到TcpServer
                    clientHandlerCallback.onNewMessageArrived(this@ClientHandler,str)
                }while (!done)

            }catch (e:Exception){
                if(!done) {
                    println("连接异常断开")
                    this@ClientHandler.exitBySelf()
                }
            }finally {
                // 连接关闭
                CloseUtils.close(inputStream)
            }
        }

        fun exit(){
            done = true
            CloseUtils.close(inputStream)
        }
    }

    inner class ClientWriteHandler(outputStream:OutputStream){

        private var done = false
        private val printStream:PrintStream = PrintStream(outputStream)
        private val executorService:ExecutorService = Executors.newSingleThreadExecutor()

        fun send(str: String) {
            if(done) return
            executorService.execute(WriteRunnable(str))
        }

        fun exit(){
            done = true
            CloseUtils.close(printStream)
            executorService.shutdownNow()
        }

        inner class WriteRunnable(private val msg:String) : Runnable{

            override fun run() {
                if(this@ClientWriteHandler.done){
                    return
                }

                try {
                    this@ClientWriteHandler.printStream.println(msg)
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