import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.io.PrintStream
import java.net.Inet4Address
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.nio.ByteBuffer

private const val PORT = 20000

class Server

fun main(args: Array<String>) {
    val server = createServerSocket()

    initServerSocket(server)

    server.bind(InetSocketAddress(Inet4Address.getLocalHost(),PORT),50)

    println("服务器准备就绪~")
    println("服务端信息:${server.inetAddress} P:${server.localPort}")

    // 等待客户端连接
    do{
        // 得到客户端
        val client = server.accept()
        // 客户端构建异步线程
        val clientHandler = ClientHandler(client)
        // 启动线程
        clientHandler.start()
    }while (true)
}

@Throws(IOException::class)
private fun createServerSocket():ServerSocket{
    // serverSocket.bind(InetSocketAddress(Inet4Address.getLocalHost(),PORT),50)
    return ServerSocket()
}

@Throws(IOException::class)
private fun initServerSocket(serverSocket: ServerSocket){
    serverSocket.reuseAddress = true
    serverSocket.receiveBufferSize = 64 * 1000 * 1000
    // 设置ServerSocket#accept超时时间
    // serverSocket.soTimeout = 2000
    serverSocket.setPerformancePreferences(1,1,1)
}

/**
 * 客户端消息处理
 */
private class ClientHandler constructor(private val socket:Socket) : Thread(){

    override fun run() {

        println("新客户端连接：${socket.inetAddress} P：${socket.port}")

        try{
            val inputStream = socket.getInputStream()
            val outputStream = socket.getOutputStream()

            val buffer = ByteArray(256)
            val readCount = inputStream.read(buffer)

            val byteBuffer = ByteBuffer.wrap(buffer,0,readCount)
            println("""Byte:${byteBuffer.get()}
                Short:${byteBuffer.short}
                Int:${byteBuffer.int}
                Char:${byteBuffer.char}
                Boolean:${byteBuffer.get().toInt() == 1}
                Float:${byteBuffer.float}
                Double:${byteBuffer.double}
                String:${String(buffer,byteBuffer.position(),readCount - byteBuffer.position() - 1)}
            """.trimMargin())

            outputStream.write(buffer,0,readCount)
            inputStream.close()
            outputStream.close()

        }catch (e:Exception){
            println("连接异常断开")
        }finally {
            socket.close()
        }

        println("客户端已退出：${socket.inetAddress} P：${socket.localPort}")
    }
}