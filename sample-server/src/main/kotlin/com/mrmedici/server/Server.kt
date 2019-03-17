package server

import com.mrmedici.clink.core.IoContext
import com.mrmedici.clink.impl.IoSelectorProvider
import com.mrmedici.foo.Foo
import constants.TCPConstants
import java.io.BufferedReader
import java.io.InputStreamReader

fun main(args: Array<String>) {
    val cachePath = Foo.getCacheDir("server")

    IoContext.setup()
             .ioProvider(IoSelectorProvider())
             .start()

    val tcpServer = TcpServer(TCPConstants.PORT_SERVER,cachePath)
    val isSucceed = tcpServer.start()
    if(!isSucceed){
        println("Start TCP server failed!")
    }

    ServerProvider.start(TCPConstants.PORT_SERVER)

    val bufferedReader = BufferedReader(InputStreamReader(System.`in`))
    var str:String
    do{
        str = bufferedReader.readLine()
        if(str == null) continue

        if(!"00bye00".equals(str,true)) {
            tcpServer.broadcast(str)
        }

        if("00bye00".equals(str,true)){
            break
        }
    }while (true)

    ServerProvider.stop()
    tcpServer.stop()
    IoContext.close()
}