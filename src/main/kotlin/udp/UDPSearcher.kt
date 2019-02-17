package udp

import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

fun main(args: Array<String>) {
    println("UDPSearcher Started.")

    // 作为搜索方，让系统自动分配端口
    val ds = DatagramSocket()

    // 构建一份请求数据
    val sendData = "HelloWorld"
    val sendDataBytes = sendData.toByteArray()
    // 直接构建Packet
    val sendPacket = DatagramPacket(sendDataBytes,sendDataBytes.size)
    // 本机20000端口
    sendPacket.address = InetAddress.getLocalHost()
    sendPacket.port = 20000
    ds.send(sendPacket)


    // 构建接收实体
    val buffer = ByteArray(512)
    val receivePacket = DatagramPacket(buffer,buffer.size)

    // 接收
    ds.receive(receivePacket)

    // 打印接收到的信息与发送者的信息
    // 发送者的IP地址与端口号
    val ip = receivePacket.address.hostAddress
    val port = receivePacket.port
    val dataLength = receivePacket.length
    val data = String(buffer,0,dataLength)
    println("UDPSearcher receive from ip:$ip port:$port data:$data")




    // 完成
    println("UDPSearcher Finished.")
    ds.close()
}