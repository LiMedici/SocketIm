package server.handle

import com.mrmedici.clink.box.StringReceivePacket
import com.mrmedici.clink.core.Connector
import com.mrmedici.clink.core.IoContext
import com.mrmedici.clink.core.ReceivePacket
import com.mrmedici.clink.core.TYPE_MEMORY_STRING
import com.mrmedici.clink.utils.CloseUtils
import com.mrmedici.foo.Foo
import com.mrmedici.foo.handle.DefaultNonConnectorStringPacketChain
import com.mrmedici.foo.handle.DefaultPrintConnectorCloseChain
import java.io.*
import java.nio.channels.SocketChannel
import java.util.concurrent.Executor

open class ConnectorHandler(private val socketChannel: SocketChannel,
                       private val cachePath:File):Connector(){

    private val clientInfo = socketChannel.remoteAddress.toString()
    val closeChain = DefaultPrintConnectorCloseChain()
    val stringPacketChain = DefaultNonConnectorStringPacketChain()

    init {
        setup(socketChannel)
    }

    fun getClientInfo() = clientInfo

    override fun onChannelClosed(channel: SocketChannel) {
        super.onChannelClosed(channel)
        closeChain.handle(this,this)
    }

    override fun onReceivedPacket(packet: ReceivePacket<*, *>) {
        super.onReceivedPacket(packet)
        when(packet.type()){
            TYPE_MEMORY_STRING -> deliveryStringPacket(packet as StringReceivePacket)
            else -> println("New Packet:${packet.type()}-${packet.length()}")
        }
    }

    override fun createNewReceiveFile(length:Long,headerInfo:ByteArray?): File {
        return Foo.createRandomTemp(cachePath)
    }

    override fun createNewReceiveDirectOutputStream(length: Long, headerInfo: ByteArray?): OutputStream {
        return ByteArrayOutputStream()
    }

    private fun deliveryStringPacket(packet:StringReceivePacket){
        IoContext.get()?.scheduler?.delivery(Runnable {
            stringPacketChain.handle(this@ConnectorHandler,packet)
        })
    }

    fun exit(){
        CloseUtils.close(this)
    }
}