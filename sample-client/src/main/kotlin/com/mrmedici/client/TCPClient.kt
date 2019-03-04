package client

import client.bean.ServerInfo
import com.mrmedici.clink.utils.CloseUtils
import java.io.*
import java.net.Inet4Address
import java.net.InetSocketAddress
import java.net.Socket
import java.net.SocketTimeoutException

class TCPClient(private val socket:Socket,
                private val readHandler:ReadHandler){

    private val printStream = PrintStream(socket.getOutputStream())

    fun exit(){
        readHandler.exit()
        CloseUtils.close(printStream)
        CloseUtils.close(socket)
    }

    fun send(msg:String){
        printStream.println(msg)
    }

    companion object {
        @Throws(IOException::class)
        fun startWith(info:ServerInfo):TCPClient?{
            val socket = Socket()

            // 设置读取超时时间为3秒
            socket.soTimeout = 3000

            // 链接到本地20000端口，超时时间设置为3秒，超出则抛出异常
            socket.connect(InetSocketAddress(Inet4Address.getByName(info.address), info.port), 3000)

            println("已发起服务器连接，并进入后续流程~")
            println("客户端信息:${socket.localAddress} P:${socket.localPort}")
            println("服务端信息:${socket.remoteSocketAddress} P:${socket.port}")

            try {
                val readHandler = ReadHandler(socket.getInputStream())
                readHandler.start()
                return TCPClient(socket,readHandler)
            } catch (e: IOException) {
                println("连接异常")
                CloseUtils.close(socket)
            }

            return null
        }
    }

    class ReadHandler(private val inputStream: InputStream) : Thread(){

        private var done = false

        override fun run() {

            try{
                // 得到输入流，用于接收数据
                val socketInput = BufferedReader(InputStreamReader(inputStream))

                do{
                    var str:String?

                    try {
                        str = socketInput.readLine()
                    }catch (e:SocketTimeoutException){
                        continue
                    }

                    if(str == null){
                        println("连接已关闭，无法读取数据！")
                        break
                    }
                    // 打印到屏幕
                    println(str)
                }while (!done)

            }catch (e:Exception){
                if(!done) {
                    println("连接异常断开:${e.message}")
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


}