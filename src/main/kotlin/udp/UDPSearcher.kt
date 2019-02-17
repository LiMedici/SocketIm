package udp

import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.util.concurrent.CountDownLatch

/**
 * UDP搜索者，可以搜索服务
 */
private const val LISTEN_PORT = 30000

class UDPSearcher

fun main(args: Array<String>) {
    println("UDPSearcher Started.")

    val listener = listen()
    sendBroadcast()

    // 读取任意键信息后可以退出
    System.`in`.read()

    val devices = listener.getDevicesAndClose()
    devices.forEach { println("Device:$it") }

    // 完成
    println("UDPSearcher Finished.")
}

private fun listen():Listener{
    println("UDPSearcher start listen.")
    val countDownLatch = CountDownLatch(1)
    val listener = Listener(LISTEN_PORT,countDownLatch)
    listener.start()

    countDownLatch.await()
    return listener
}

private fun sendBroadcast(){
    println("UDPSearcher sendBroadcast Started.")

    // 作为搜索方，让系统自动分配端口
    val ds = DatagramSocket()

    // 构建一份请求数据
    val sendData = MessageCreator.buildWithPort(LISTEN_PORT)
    val sendDataBytes = sendData.toByteArray()
    // 直接构建Packet
    val sendPacket = DatagramPacket(sendDataBytes,sendDataBytes.size)
    // 20000端口，受限广播地址
    sendPacket.address = InetAddress.getByName("255.255.255.255")
    sendPacket.port = 20000
    ds.send(sendPacket)
    ds.close()

    // 完成
    println("UDPSearcher sendBroadcast Finished.")
}

private data class Device(private val ip:String,private val port:Int,private val sn:String)

private class  Listener constructor(private val listenPort:Int,
                                    private val countDownLatch: CountDownLatch) : Thread(){
    private val devices = ArrayList<Device>()
    private var done = false
    private var ds:DatagramSocket ?= null

    override fun run() {
        // 通知已启动
        countDownLatch.countDown()

        try {
            // 监听回送端口
            ds = DatagramSocket(listenPort)

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
                println("UDPSearcher receive from ip:$ip port:$port data:$data")

                val sn = MessageCreator.parseSn(data)
                if (sn != null) {
                    devices.add(Device(ip, port, sn))
                }
            }
        }catch (ignored:Exception){

        }finally {
            close()
        }


        // 完成
        println("UDPSearcher Listener Finished.")
    }

    private fun close(){
        ds?.close()
        ds = null
    }

    fun getDevicesAndClose():List<Device>{
        done = true
        close()
        return devices
    }
}