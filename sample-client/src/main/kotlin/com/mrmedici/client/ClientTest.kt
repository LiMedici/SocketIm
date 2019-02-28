package com.mrmedici.client

import client.ClientSearcher
import client.TCPClient
import java.io.IOException
import java.util.concurrent.TimeUnit

var done = false

fun main(args: Array<String>) {
    val info = ClientSearcher.searchServer(10000)
    println("Server:$info")

    if(info == null) return
    var counter = 0
    val tcpClients = ArrayList<TCPClient>()

    for (index in 0 until 100){
        try {
            val tcpClient = TCPClient.startWith(info)
            if (tcpClient == null) {
                println("连接异常")
                continue
            }

            tcpClients.add(tcpClient)
            println("连接成功：${++counter}")
        }catch (e:IOException){
            e.printStackTrace()
            println("连接异常")
        }

        TimeUnit.MILLISECONDS.sleep(20)
    }

    System.`in`.read()

    val runnable = Runnable {
        while(!done){
            tcpClients.forEach { it?.send("Hello") }

            TimeUnit.SECONDS.sleep(1)
        }
    }

    val thread = Thread(runnable)
    thread.start()

    System.`in`.read()

    // 等待当前线程完成
    done = true
    thread.join()

    // 客户端结束操作
    tcpClients.forEach { it?.exit() }
}