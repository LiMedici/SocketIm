package client

import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader

fun main(args: Array<String>) {
    val info = ClientSearcher.searchServer(10000)
    println("Server:$info")

    if(info != null){
        var tcpClient:TCPClient? = null
        try{
            tcpClient = TCPClient.startWith(info)
            if(tcpClient == null){
                return
            }

            write(tcpClient)
        }catch (e:IOException){
            e.printStackTrace()
        }finally {
            tcpClient?.exit()
        }
    }
}

@Throws(IOException::class)
private fun write(client:TCPClient){
    // 获取键盘输入流
    val input = BufferedReader(InputStreamReader(System.`in`))

    do{
        // 键盘读取一行
        val str:String = input.readLine()
        // 发送到服务器
        client.send(str)

        if("00bye00".equals(str,true)) {
            break
        }
    }while (true)
}