package com.mrmedici.server

import com.mrmedici.clink.utils.CloseUtils
import java.io.IOException
import java.nio.channels.Selector
import java.nio.channels.ServerSocketChannel
import java.nio.channels.SocketChannel
import java.util.concurrent.CountDownLatch

class ServerAccepter(private val listener: AcceptListener) :
        Thread("Server-Accept-Thread") {

    private var done = false
    private val latch = CountDownLatch(1)
    val selector:Selector = Selector.open()

    fun awaitRunning():Boolean{
        return try {
            latch.await()
            true
        }catch (ignore:InterruptedException){
            false
        }
    }

    override fun run() {
        // 回调已进入运行
        latch.countDown()

        val selector = this.selector

        do {
            try {
                if (selector.select() == 0) {
                    if (done) {
                        break
                    }
                    continue
                }

                val iterator = selector.selectedKeys().iterator()
                while (iterator.hasNext()) {
                    if (done) break

                    val selectionKey = iterator.next()
                    iterator.remove()
                    // 检查当前Key的状态是否是我们关注的
                    // 客户端到达状态
                    if (selectionKey.isValid && selectionKey.isAcceptable) {
                        val serverSocketChannel = selectionKey.channel() as ServerSocketChannel
                        // 非阻塞状态拿到客户端连接
                        val socketChannel = serverSocketChannel.accept()
                        listener.onNewSocketArrived(socketChannel)
                    }
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }
        } while (!done)

        println("ServerAccepter Finished!")
    }

    fun exit() {
        done = true
        // 直接关闭
        CloseUtils.close(selector)
    }
}

interface AcceptListener{
    fun onNewSocketArrived(channel:SocketChannel)
}