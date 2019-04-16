package client

import com.mrmedici.clink.box.FileSendPacket
import com.mrmedici.clink.core.Connector
import com.mrmedici.clink.core.IoContext
import com.mrmedici.clink.core.schedule.IdleTimeoutScheduleJob
import com.mrmedici.clink.impl.IoSelectorProvider
import com.mrmedici.clink.impl.IoStealingSelectorProvider
import com.mrmedici.clink.impl.SchedulerImpl
import com.mrmedici.clink.utils.CloseUtils
import com.mrmedici.foo.COMMAND_EXIT
import com.mrmedici.foo.Foo
import com.mrmedici.foo.handle.ConnectorCloseChain
import server.handle.ConnectorHandler
import java.io.BufferedReader
import java.io.File
import java.io.IOException
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit

fun main(args: Array<String>) {
    val cachePath = Foo.getCacheDir("client")
    val info = ClientSearcher.searchServer(10000)?:return
    println("Server:$info")

    IoContext.setup()
            .ioProvider(IoStealingSelectorProvider(3))
            .scheduler(SchedulerImpl(1))
            .start()

    if(info != null){
        var tcpClient:TCPClient? = null
        try{
            tcpClient = TCPClient.startWith(info,cachePath)
            if(tcpClient == null){
                return
            }

            tcpClient.closeChain.appendLast(object:ConnectorCloseChain(){
                override fun consume(handler: ConnectorHandler, model: Connector): Boolean {
                    // TODO 关闭键盘输入，readLine阻塞不能停止
                    CloseUtils.close(System.`in`)
                    return true
                }
            })

            val scheduleJob = IdleTimeoutScheduleJob(10,TimeUnit.SECONDS,tcpClient)
            tcpClient.schedule(scheduleJob)

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

    do {
        // 键盘读取一行
        val str: String? = input.readLine()
        if (str == null || COMMAND_EXIT.equals(str, true)) {
            break
        }

        if (str.isEmpty()) {
            continue
        }

        // abc
        // --f url
        if (str.startsWith("--f")) {
            val array = str.split(" ")
            if (array.size == 2) {
                val filePath = array[1]
                val file = File(filePath)
                if (file.exists() && file.isFile) {
                    val packet = FileSendPacket(file)
                    client.send(packet)
                    continue
                }
            }
        }

        // 发送字符串
        client.send(str)
    } while (true)
}