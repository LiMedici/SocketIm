package com.mrmedici.client

import client.ClientSearcher
import client.TCPClient
import com.mrmedici.clink.core.Connector
import com.mrmedici.clink.core.IoContext
import com.mrmedici.clink.core.schedule.IdleTimeoutScheduleJob
import com.mrmedici.clink.impl.IoSelectorProvider
import com.mrmedici.clink.impl.SchedulerImpl
import com.mrmedici.clink.utils.CloseUtils
import com.mrmedici.foo.Foo
import com.mrmedici.foo.handle.ConnectorCloseChain
import server.handle.ConnectorHandler
import java.io.IOException
import java.util.concurrent.TimeUnit

// 不考虑发送消耗，并发量：2000*4/400*1000 = 2w/s 算上来回两次数据解析：4w/s
const val CLIENT_SIZE = 500
const val SEND_THREAD_SIZE = 4
const val SEND_THREAD_DELAY = 400L

@Volatile
var done = false

fun main(args: Array<String>) {
    val cachePath = Foo.getCacheDir("client/test")
    val info = ClientSearcher.searchServer(10000)?:return
    println("Server:$info")

    IoContext.setup()
            .ioProvider(IoSelectorProvider())
            .scheduler(SchedulerImpl(1))
            .start()

    var counter = 0
    val tcpClients = ArrayList<TCPClient>(CLIENT_SIZE)

    val closeChain  = object : ConnectorCloseChain(){
        override fun consume(handler: ConnectorHandler, model: Connector): Boolean {
            tcpClients.remove(handler)
            if(tcpClients.size == 0){
                CloseUtils.close(System.`in`)
            }
            return false
        }
    }

    for (index in 0 until CLIENT_SIZE){
        try {
            val tcpClient = TCPClient.startWith(info,cachePath,false) ?: throw NullPointerException()

            // 添加关闭链式节点
            tcpClient.closeChain.appendLast(closeChain)

            val scheduleJob = IdleTimeoutScheduleJob(10,TimeUnit.SECONDS,tcpClient)
            tcpClient.schedule(scheduleJob)

            tcpClients.add(tcpClient)
            println("连接成功：${++counter}")
        }catch (e:IOException){
            e.printStackTrace()
            println("连接异常")
            break
        }catch (e:NullPointerException){
            e.printStackTrace()
            println("连接异常")
            break
        }
        // 让测试类快速的建立好连接
        // TimeUnit.MILLISECONDS.sleep(20)
    }

    System.`in`.read()

    val runnable = Runnable {
        while(!done){
            val array = tcpClients.toArray(emptyArray<TCPClient>())
            array.forEach { it.send("Hello") }

            try{
                TimeUnit.MILLISECONDS.sleep(SEND_THREAD_DELAY)
            }catch (ignore:Exception){

            }
        }
    }

    val threads = ArrayList<Thread>(SEND_THREAD_SIZE)
    for(index in 0 until SEND_THREAD_SIZE){
        val thread = Thread(runnable)
        thread.start()
        threads.add(thread)
    }

    System.`in`.read()

    // 等待当前线程完成
    done = true

    // 客户端结束操作
    val array = tcpClients.toArray(emptyArray<TCPClient>())
    array.forEach { it.exit() }

    IoContext.close()

    // 强制结束处于等待的线程
    threads.forEach(Thread::interrupt)
}