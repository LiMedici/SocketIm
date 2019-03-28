package server

import com.mrmedici.foo.extensions.startWith
import constants.UDPConstants
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.nio.ByteBuffer
import java.util.*

class ServerProvider {
    companion object {
        private var PROVIDER_INSTANCE: Provider? = null

        fun start(port: Int) {
            stop()
            val sn = UUID.randomUUID().toString()
            val provider = Provider(sn, port)
            provider.start()
            PROVIDER_INSTANCE = provider
        }

        fun stop() {
            PROVIDER_INSTANCE?.let {
                it.exit()
            }

            PROVIDER_INSTANCE = null
        }

        private class Provider(sn: String, port: Int) : Thread("Server-UDPProvider-Thread") {

            private val sn: ByteArray = sn.toByteArray()
            private val port: Int = port
            private var done = false
            private var ds: DatagramSocket? = null

            private val buffer: ByteArray = ByteArray(128)

            override fun run() {
                println("UDPProvider Started.")

                try {
                    // 监听端口
                    ds = DatagramSocket(UDPConstants.PORT_SERVER)

                    // 构建接收实体
                    val receivePacket = DatagramPacket(buffer, buffer.size)
                    while (!done) {

                        // 接收
                        ds!!.receive(receivePacket)

                        // 打印接收到的信息与发送者的信息
                        // 发送者的IP地址与端口号
                        val clientIp = receivePacket.address.hostAddress
                        val clientPort = receivePacket.port
                        val dataLength = receivePacket.length
                        val clientData = receivePacket.data

                        val isValid = dataLength >= UDPConstants.HEADER.size + 2 + 4 &&
                                clientData.startWith(UDPConstants.HEADER)

                        println("UDPProvider receive from clientIp:$clientIp clientPort:$clientPort dataValid:$isValid")

                        if(!isValid) continue

                        // 解析命令与回送端口
                        var index = UDPConstants.HEADER.size
                        val cmd = ((clientData[index++].toInt().and(0xFF).shl(8))).or((clientData[index++].toInt().and(0xFF))).toShort()
                        val responsePort = (clientData[index++].toInt().and(0xFF).shl(24).or(
                                clientData[index++].toInt().and(0xFF).shl(16)).or(
                                clientData[index++].toInt().and(0xFF).shl(8)).or(
                                clientData[index++].toInt().and(0xFF)))
                        if (cmd.toInt() == 1 && responsePort > 0) {
                            // 构建一份回送数据
                            val byteBuffer = ByteBuffer.wrap(buffer)
                            byteBuffer.put(UDPConstants.HEADER)
                            byteBuffer.putShort(2)
                            byteBuffer.putInt(port)
                            byteBuffer.put(sn)

                            val len = byteBuffer.position()
                            // 直接根据发送者构建一份回送消息
                            val responsePacket = DatagramPacket(buffer, len,
                                    receivePacket.address, responsePort)
                            ds!!.send(responsePacket)
                            println("ServerProvider response to:$clientIp\tport:$responsePort\tdataLen:$len")
                        }else{
                            println("ServerProvider receive cmd nonsupport; cmd:$cmd\tport:$port")
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
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
    }
}