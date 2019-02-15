import java.io.*
import java.net.Inet4Address
import java.net.InetSocketAddress
import java.net.Socket

class Client

fun main(args: Array<String>){
    val socket = Socket()

    // 设置超时时间为3000ms
    socket.soTimeout = 3000

    // 连接本地服务器，端口号2000；超时时间3000ms
    socket.connect(InetSocketAddress(Inet4Address.getLocalHost(), 2000), 3000)

    println("已发起服务器连接，并进入后续流程~")
    println("客户端信息:${socket.localAddress} P:${socket.localPort}")
    println("服务端信息:${socket.remoteSocketAddress} P:${socket.port}")

    try {
        // 发送接收数据
        todo(socket)
    } catch (e: IOException) {
        e.printStackTrace()
        println("客户端已经关闭")
    }

    socket.close()
    println("客户端已退出~")
}

private fun todo(client:Socket){
    // 构建键盘输入流
    val input = System.`in`
    val bufferReader = BufferedReader(InputStreamReader(input))

    // 构建Socket输出流，并转换为打印流
    val outputStream = client.getOutputStream()
    val socketPrintStream = PrintStream(outputStream)

    // 得到Socket输入流，并转换为BufferedReader
    val inputStream = client.getInputStream()
    val socketBufferedReader = BufferedReader(InputStreamReader(inputStream))

    var flag = true

    do{
        // 键盘读取一行
        val line = bufferReader.readLine()
        socketPrintStream.println(line)

        // 从服务器读取一行
        val echo = socketBufferedReader.readLine()
        if("bye".equals(echo,true)){
            flag = false
        } else {
            println(echo)
        }
        println()
    }while (flag)

    // 资源释放
    socketPrintStream.close()
    socketBufferedReader.close()

}