package client

import client.bean.ServerInfo
import constants.UDPConstants
import extensions.startWith
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.nio.ByteBuffer
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

private const val LISTEN_PORT = UDPConstants.PORT_CLIENT_RESPONSE

class ClientSearcher{
    companion object {
        fun searchServer(timeout:Long):ServerInfo?{
            println("UDPSearcher Started.")

            // 成功收到回送的栅栏
            val receiveLatch = CountDownLatch(1)

            var listener:Listener? = null
            try {
                listener = listen(receiveLatch)
                sendBroadcast()
                receiveLatch.await(timeout, TimeUnit.MICROSECONDS)
            }catch (e:Exception){
                e.printStackTrace()
            }

            println("UDPSearcher Finished.")
            if(listener == null) return null

            val servers = listener.getServerAndClose()
            if(!servers.isEmpty()) return servers.first()
            return null
        }

        private fun listen(receiveLatch:CountDownLatch): Listener {
            println("UDPSearcher start listen.")
            val startDownLatch = CountDownLatch(1)
            val listener = Listener(LISTEN_PORT,startDownLatch,receiveLatch)
            listener.start()
            startDownLatch.await()
            return listener
        }

        private fun sendBroadcast(){
            println("UDPSearcher sendBroadcast Started.")

            // 作为搜索方，让系统自动分配端口
            val ds = DatagramSocket()

            // 构建一份请求数据
            val byteBuffer = ByteBuffer.allocate(128)
            // 头部
            byteBuffer.put(UDPConstants.HEADER)
            // cmd命名
            byteBuffer.putShort(1)
            // 回送端口信息
            byteBuffer.putInt(LISTEN_PORT)
            // 直接构建Packet
            val requestPacket = DatagramPacket(byteBuffer.array(),byteBuffer.position() + 1)
            // 20000端口，受限广播地址
            requestPacket.address = InetAddress.getByName("255.255.255.255")
            requestPacket.port = UDPConstants.PORT_SERVER
            ds.send(requestPacket)
            ds.close()

            // 完成
            println("UDPSearcher sendBroadcast Finished.")
        }

    }

    private class  Listener constructor(private val listenPort:Int,
                                        private val startDownLatch: CountDownLatch,
                                        private val receiveDownLatch: CountDownLatch) : Thread(){

        private val devices = ArrayList<ServerInfo>()
        private val buffer = ByteArray(128)
        private val minLen = UDPConstants.HEADER.size + 2 + 4
        private var done = false
        private var ds:DatagramSocket ?= null

        override fun run() {
            // 通知已启动
            startDownLatch.countDown()

            try {
                // 监听回送端口
                ds = DatagramSocket(listenPort)
                // 构建接收实体
                val receivePacket = DatagramPacket(buffer, buffer.size)

                while (!done) {

                    // 接收
                    ds!!.receive(receivePacket)

                    // 打印接收到的信息与发送者的信息
                    // 发送者的IP地址与端口号
                    val ip = receivePacket.address.hostAddress
                    val port = receivePacket.port
                    val dataLength = receivePacket.length
                    val data = receivePacket.data

                    val isValid = dataLength >= minLen &&
                            data.startWith(UDPConstants.HEADER)
                    println("UDPSearcher receive from ip:$ip port:$port dataValid:$isValid")

                    if(!isValid) continue

                    val byteBuffer = ByteBuffer.wrap(buffer,UDPConstants.HEADER.size,dataLength - UDPConstants.HEADER.size)
                    val cmd = byteBuffer.short
                    val serverPort = byteBuffer.int
                    if(cmd.toInt() != 2 && serverPort <= 0){
                        println("UDPSearcher receive cmd:$cmd\tserverPort:$serverPort")
                        continue
                    }

                    val sn = String(buffer,minLen,dataLength - minLen)
                    val info = ServerInfo(sn,ip,serverPort)
                    devices.add(info)
                    // 成功接收到一份
                    receiveDownLatch.countDown()
                }
            }catch (e:Exception){
                e.printStackTrace()
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

        fun getServerAndClose():List<ServerInfo>{
            done = true
            close()
            return devices
        }
    }
}