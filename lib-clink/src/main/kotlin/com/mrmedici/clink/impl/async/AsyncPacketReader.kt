package com.mrmedici.clink.impl.async

import com.mrmedici.clink.core.Frame
import com.mrmedici.clink.core.IoArgs
import com.mrmedici.clink.core.SendPacket
import com.mrmedici.clink.core.TYPE_COMMAND_HEARTBEAT
import com.mrmedici.clink.core.ds.BytePriorityNode
import com.mrmedici.clink.frames.*
import java.io.Closeable
import java.io.IOException

class AsyncPacketReader(private val provider: PacketProvider) : Closeable {

    @Volatile
    private var args = IoArgs()

    // Frame队列
    @Volatile
    private var node: BytePriorityNode<Frame>? = null
    @Volatile
    private var nodeSize: Int = 0

    // 1,2,3....255
    private var lastIdentifier: Short = 0

    fun requestTakePacket(): Boolean {
        synchronized(this) {
            if (nodeSize >= 1) {
                return true
            }
        }

        val packet = provider.takePacket()
        if (packet != null) {
            val identifier = generateIdentifier()
            val frame = SendHeaderFrame(identifier, packet)
            appendNewFrame(frame)
        }

        synchronized(this) {
            return nodeSize != 0
        }
    }

    fun requestSendHeartbeatFrame():Boolean{
        synchronized(this){
            var x = node
            while (x != null) {
                val frame = x.item
                if(frame.getBodyType() == TYPE_COMMAND_HEARTBEAT){
                    return false
                }
                x = x.next
            }

            appendNewFrame(HeartbeatSendFrame())
            return true
        }
    }

    fun fillData(): IoArgs? {
        val currentFrame: Frame = getCurrentFrame() ?: return null

        try {
            if (currentFrame.handle(args)) {
                // 消费完本帧
                // 尝试基于本帧构建后续帧
                val nextFrame = currentFrame.nextFrame()
                if (nextFrame != null) {
                    appendNewFrame(nextFrame)
                } else if (currentFrame is SendEntityFrame) {
                    // 末尾实体帧
                    // 通知完成
                    provider.completedPacket(currentFrame.packet, true)
                }

                // 从链头弹出
                popCurrentFrame()
            }

            return args
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return null
    }

    @Synchronized
    fun cancel(packet: SendPacket<*>) {
        if (nodeSize == 0) {
            return
        }

        var x = node
        var before: BytePriorityNode<Frame>? = null
        while (x != null) {
            val frame = x.item
            if (frame is AbsSendPacketFrame) {
                if (frame.packet == packet) {
                    val removable = frame.abort()
                    if (removable) {
                        // A B C
                        removeFrame(x, before)
                        if (frame is SendHeaderFrame) {
                            // 头帧,并且未发送任何数据，直接取消后不需要添加取消发送帧
                            break
                        }
                    }

                    // 添加终止帧
                    val cancelFrame = CancelSendFrame(frame.getBodyIdentifier())
                    appendNewFrame(cancelFrame)

                    // 意外终止，返回失败
                    provider.completedPacket(packet, false)
                    break
                }
            }

            before = x
            x = x.next
        }
    }

    @Synchronized
    override fun close() {
        while (node != null){
            val frame = node!!.item
            if(frame is AbsSendPacketFrame){
                val packet = frame.packet
                provider.completedPacket(packet,false)
            }

            node = node!!.next
        }

        nodeSize = 0
        node = null
    }

    @Synchronized
    private fun appendNewFrame(frame: Frame) {
        val newNode = BytePriorityNode(frame)
        if(node != null){
            // 使用优先级别添加链表
            node?.appendWithPriority(newNode)
        }else{
            node = newNode
        }

        nodeSize++

    }

    @Synchronized
    private fun getCurrentFrame(): Frame? {
        if(node == null) return null
        return node!!.item
    }

    @Synchronized
    private fun popCurrentFrame() {
        node = node!!.next
        nodeSize --

        if(node == null){
            requestTakePacket()
        }
    }

    @Synchronized
    private fun removeFrame(x: BytePriorityNode<Frame>, before: BytePriorityNode<Frame>?) {
        if(before == null){
            // A B C
            // B C
            node = x.next
        }else{
            // A B C
            // A C
            before.next = x.next
        }

        nodeSize --

        if(node == null){
            requestTakePacket()
        }
    }


    private fun generateIdentifier(): Short {
        val identifier = ++lastIdentifier
        if (identifier == 255.toShort()) {
            lastIdentifier = 0
        }
        return identifier
    }

    interface PacketProvider {
        fun takePacket(): SendPacket<*>?
        fun completedPacket(packet: SendPacket<*>, isSucceed: Boolean)
    }
}