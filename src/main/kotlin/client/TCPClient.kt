package client

import client.bean.ServerInfo
import utils.CloseUtils
import java.io.*
import java.net.Inet4Address
import java.net.InetSocketAddress
import java.net.Socket
import java.net.SocketTimeoutException

class TCPClient{
    companion object {
        @Throws(IOException::class)
        fun linkWith(info:ServerInfo){
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

                // 发送接收数据
                write(socket)

                readHandler.exit()
            } catch (e: IOException) {
                e.printStackTrace()
                println("异常关闭")
            }

            socket.close()
            println("客户端已退出~")
        }

        private fun write(client:Socket){
            // 获取键盘输入流
            val input = BufferedReader(InputStreamReader(System.`in`))

            // 构建Socket输出流，并转换为打印流
            val outputStream = client.getOutputStream()
            val socketPrintStream = PrintStream(outputStream)

            do{
                // 键盘读取一行
                val str = input.readLine()
                // 发送到服务器
                socketPrintStream.println(str)

                if("00bye00".equals(str,true)) {
                    break
                }
            }while (true)

            // 资源释放
            socketPrintStream.close()
        }
    }

    private class ReadHandler(private val inputStream: InputStream) : Thread(){

        private var done = false

        override fun run() {

            try{
                // 得到输入流，用于接收数据
                val socketInput = BufferedReader(InputStreamReader(inputStream))

                do{
                    var str:String

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