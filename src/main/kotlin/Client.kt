import org.omg.CORBA.LocalObject
import java.io.*
import java.net.*
import java.nio.ByteBuffer

private const val PORT = 20000
private const val LOCAL_PORT = 20001

class Client

@Throws(IOException::class)
fun main(args: Array<String>){
    val socket = createSocket()

    initSocket(socket)

    // 链接到本地20000端口，超时时间设置为3秒，超出则抛出异常
    socket.connect(InetSocketAddress(Inet4Address.getLocalHost(), PORT), 3000)

    println("已发起服务器连接，并进入后续流程~")
    println("客户端信息:${socket.localAddress} P:${socket.localPort}")
    println("服务端信息:${socket.remoteSocketAddress} P:${socket.port}")

    try {
        // 发送接收数据
        todo(socket)
    } catch (e: IOException) {
        e.printStackTrace()
        println("异常关闭")
    }

    socket.close()
    println("客户端已退出~")
}

@Throws(IOException::class)
private fun createSocket():Socket{
    val socket = Socket()
    socket.bind(InetSocketAddress(Inet4Address.getLocalHost(),LOCAL_PORT))
    return socket
}

@Throws(SocketException::class)
private fun initSocket(socket:Socket){
    // 设置读取超时时间为3秒
    socket.soTimeout = 3000
    // 是否复用未完全关闭的Socket地址，对于指定Bind操作后的套接字有效
    socket.reuseAddress = true

    // 是否开启Nagle算法
    socket.tcpNoDelay = true
    // 是否需要在长时间无数据响应时发送确认数据(类似心跳包)，时间大约为2小时
    socket.keepAlive = true

    socket.setSoLinger(true,20)

    socket.oobInline = false

    // 设置接收发送缓冲器大小
    socket.receiveBufferSize = 64 * 1024 * 1024
    socket.sendBufferSize = 64 * 1024 * 1024
    // 设置性能参数：短连接，延迟，带宽的重要性
    socket.setPerformancePreferences(1,1,1)
}

private fun todo(client:Socket){
    // 构建Socket输出流
    val outputStream = client.getOutputStream()

    // 得到Socket输入流
    val inputStream = client.getInputStream()

    val buffer = ByteArray(128)

    val byteBuffer = ByteBuffer.wrap(buffer)

    // Byte
    val byte:Byte = 1
    byteBuffer.put(byte)
    // Short
    val short:Short = 2
    byteBuffer.putShort(short)
    // Int
    val int:Int = 4
    byteBuffer.putInt(int)
    // Char
    val char:Char = 'a'
    byteBuffer.putChar(char)
    // Boolean
    val bool = false
    val boolByte:Byte = if(bool){1}else{0}
    byteBuffer.put(boolByte)
    // Float
    val float = 3.1415926f
    byteBuffer.putFloat(float)
    // Double
    val double = 33.32243424243
    byteBuffer.putDouble(double)
    // String
    val str = "Hello您好"
    byteBuffer.put(str.toByteArray())
    // 发送到服务器
    outputStream.write(buffer,0,byteBuffer.position() + 1)

    // 接收服务器返回
    val readCount = inputStream.read(buffer)
    println("收到数量：$readCount")

    // 资源释放
    outputStream.close()
    inputStream.close()

}