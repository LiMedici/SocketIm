package client

import com.mrmedici.clink.box.FileSendPacket
import com.mrmedici.clink.core.IoContext
import com.mrmedici.clink.impl.IoSelectorProvider
import com.mrmedici.foo.Foo
import java.io.BufferedReader
import java.io.File
import java.io.IOException
import java.io.InputStreamReader

fun main(args: Array<String>) {
    val cachePath = Foo.getCacheDir("client")

    IoContext.setup()
            .ioProvider(IoSelectorProvider())
            .start()

    val info = ClientSearcher.searchServer(10000)
    println("Server:$info")

    if(info != null){
        var tcpClient:TCPClient? = null
        try{
            tcpClient = TCPClient.startWith(info,cachePath)
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

    IoContext.close()
}

@Throws(IOException::class)
private fun write(client:TCPClient){
    // 获取键盘输入流
    val input = BufferedReader(InputStreamReader(System.`in`))

    do{
        // 键盘读取一行
        val str:String = input.readLine()
        if(str == null ||
                str.isEmpty() ||
                "00bye00".equals(str,true)) {
            break
        }

        // abc
        // --f url
        if(str.startsWith("--f")){
            val array = str.split(" ")
            if(array.size == 2){
                val filePath = array[1]
                val file = File(filePath)
                if(file.exists() && file.isFile){
                    val packet = FileSendPacket(file)
                    client.send(packet)
                    continue
                }
            }
        }

        // 发送字符串
        client.send(str)
    }while (true)
}