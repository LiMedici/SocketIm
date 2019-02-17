package udp

import java.net.DatagramPacket
import java.net.DatagramSocket
import java.util.*

/**
 * UDP提供者,可以提供服务
 */
class UDPProvider

fun main(args: Array<String>) {
    // 生成一份唯一标示
    val sn = UUID.randomUUID().toString()
    val provider = Provider(sn)
    provider.start()

    // 读取任意键信息后可以退出
    System.`in`.read()
    provider.exit()
}

private class Provider constructor(private val sn: String) : Thread() {
    private var done = false
    private var ds: DatagramSocket? = null

    override fun run() {
        println("UDPProvider Started.")

        try{
            // 监听本机20000端口
            ds = DatagramSocket(20000)

            while (!done) {

                // 构建接收实体
                val buffer = ByteArray(512)
                val receivePacket = DatagramPacket(buffer, buffer.size)

                // 接收
                ds!!.receive(receivePacket)

                // 打印接收到的信息与发送者的信息
                // 发送者的IP地址与端口号
                val ip = receivePacket.address.hostAddress
                val port = receivePacket.port
                val dataLength = receivePacket.length
                val data = String(buffer, 0, dataLength)
                println("UDPProvider receive from ip:$ip port:$port data:$data")

                // 解析端口号
                val responsePort = MessageCreator.parsePort(data)
                if(responsePort != -1){
                    // 构建一份回送数据
                    val responseData = MessageCreator.buildWithSn(sn)
                    val responseDataBytes = responseData.toByteArray()
                    // 直接根据发送者构建一份回送消息
                    val responsePacket = DatagramPacket(responseDataBytes, responseDataBytes.size,
                            receivePacket.address, responsePort)
                    ds!!.send(responsePacket)
                }
            }
        }catch (ignore:Exception){
        }finally {
            close()
        }


        // 完成
        println("UDPProvider Finished.")
    }

    private fun close() {
        ds?.close()
        ds = null
    }

    /**
     * 提供结束的方法
     */
    fun exit() {
        done = true
        close()
    }
}