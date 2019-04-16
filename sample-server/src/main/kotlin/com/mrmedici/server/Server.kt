package server

import com.mrmedici.clink.core.IoContext
import com.mrmedici.clink.impl.IoSelectorProvider
import com.mrmedici.clink.impl.IoStealingSelectorProvider
import com.mrmedici.clink.impl.SchedulerImpl
import com.mrmedici.foo.COMMAND_EXIT
import com.mrmedici.foo.Foo
import com.mrmedici.foo.FooGui
import constants.TCPConstants
import java.io.BufferedReader
import java.io.InputStreamReader

fun main(args: Array<String>) {
    val cachePath = Foo.getCacheDir("server")

    IoContext.setup()
             .ioProvider(IoStealingSelectorProvider(3))
             .scheduler(SchedulerImpl(1))
             .start()

    val tcpServer = TcpServer(TCPConstants.PORT_SERVER,cachePath)
    val isSucceed = tcpServer.start()
    if(!isSucceed){
        println("Start TCP server failed!")
    }

    ServerProvider.start(TCPConstants.PORT_SERVER)

    // 启动Gui界面
    val gui = FooGui("Clink-Server", FooGui.Callback { tcpServer.getStatusString() })
    gui.doShow()

    val bufferedReader = BufferedReader(InputStreamReader(System.`in`))
    var str:String
    do{
        str = bufferedReader.readLine()
        if(str == null || COMMAND_EXIT.equals(str,true)){
            break
        }

        if(str.isEmpty()){
            continue
        }

        tcpServer.broadcast(str)
    }while (true)

    ServerProvider.stop()
    tcpServer.stop()
    IoContext.close()
    gui.doDismiss()
}