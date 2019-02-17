package udp

import java.net.DatagramPacket
import java.net.DatagramSocket

fun main(args: Array<String>) {
    println("UDPProvider Started.")

    // 作为接收者，指定一个端口用于数据接收
    val ds = DatagramSocket(20000)

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
    println("UDPProvider receive from ip:$ip port:$port data:$data")


    // 构建一份回送数据
    val responseData = "Receive data with len:$dataLength"
    val responseDataBytes = responseData.toByteArray()
    // 直接根据发送者构建一份回送消息
    val responsePacket = DatagramPacket(responseDataBytes,responseDataBytes.size,
            receivePacket.address,receivePacket.port)
    ds.send(responsePacket)

    // 完成
    println("UDPProvider Finished.")
    ds.close()

}