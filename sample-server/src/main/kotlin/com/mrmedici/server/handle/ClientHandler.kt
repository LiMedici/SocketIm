package server.handle

import com.mrmedici.clink.box.StringReceivePacket
import com.mrmedici.clink.core.Connector
import com.mrmedici.clink.core.ReceivePacket
import com.mrmedici.clink.core.TYPE_MEMORY_STRING
import com.mrmedici.clink.utils.CloseUtils
import com.mrmedici.foo.Foo
import com.mrmedici.server.handle.DefaultNonConnectorStringPacketChain
import com.mrmedici.server.handle.DefaultPrintConnectorCloseChain
import java.io.*
import java.net.Socket
import java.nio.ByteBuffer
import java.nio.channels.SelectionKey
import java.nio.channels.Selector
import java.nio.channels.SocketChannel
import java.util.concurrent.Executor
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class ClientHandler(private val socketChannel: SocketChannel,
                    private val deliveryPool:Executor,
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

    override fun createNewReceiveFile(): File {
        return Foo.createRandomTemp(cachePath)
    }

    override fun onReceivedPacket(packet: ReceivePacket<*, *>) {
        super.onReceivedPacket(packet)
        when(packet.type()){
            TYPE_MEMORY_STRING -> deliveryStringPacket(packet as StringReceivePacket)
            else -> println("New Packet:${packet.type()}-${packet.length()}")
        }
    }

    private fun deliveryStringPacket(packet:StringReceivePacket){
        deliveryPool.execute{
            stringPacketChain.handle(this@ClientHandler,packet)
        }
    }

    fun exit(){
        CloseUtils.close(this)
        closeChain.handle(this,this)
    }
}