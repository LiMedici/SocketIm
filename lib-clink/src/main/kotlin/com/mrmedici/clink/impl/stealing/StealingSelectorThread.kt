package com.mrmedici.clink.impl.stealing

import com.mrmedici.clink.core.IoProvider
import com.mrmedici.clink.utils.CloseUtils
import java.io.IOException
import java.nio.channels.*
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.atomic.AtomicLong

const val VALID_OPS = SelectionKey.OP_READ or SelectionKey.OP_WRITE

abstract class StealingSelectorThread(private val selector: Selector) : Thread() {

    // 是否处于运行中
    @Volatile
    private var isRunning = true
    // 已就绪任务队列
    private val readyTaskQueue = LinkedBlockingQueue<IoTask>()
    // 待注册的任务队列
    private val registerTaskQueue = LinkedBlockingQueue<IoTask>()
    // 单次就绪的任务缓存，随后一次性加入到就绪队列中
    private val onceReadyTaskCache = ArrayList<IoTask>()

    // 任务饱和度度量
    private val saturatingCapacity = AtomicLong()
    // 用于多线程协同的Service
    // 可能是单线程池
    @Volatile
    private var stealingService: StealingService? = null

    fun setStealingService(stealingService: StealingService){
        this.stealingService = stealingService
    }

    fun getReadyTaskQueue():LinkedBlockingQueue<IoTask>{
        return readyTaskQueue
    }

    /**
     * 获取饱和度
     * 暂时的饱和度量是使用任务执行的次数来定
     *
     * @return -1 已失效
     */
    fun getSaturatingCapacity():Long{
        return if(selector.isOpen){
            saturatingCapacity.get()
        }else{
            -1
        }
    }

    /**
     * 将通道注册到当前的Selector中
     * @param channel 通道
     * @param ops 关注的行为
     * @param callback 触发时的回调
     * @return 是否注册成功
     */
    fun register(channel: SocketChannel, ops: Int, callback: IoProvider.HandleProviderCallback): Boolean {
        return when {
            channel.isOpen -> {
                val ioTask = IoTask(channel, ops, callback)
                registerTaskQueue.offer(ioTask)
                true
            }
            else -> false
        }
    }


    /**
     * 取消注册，原理类似于注册操作在队列中添加一份取消注册的任务；并将副本变量清空
     *
     * @param channel 通道
     */
    fun unregister(channel: SocketChannel) {
        val selectorKey = channel.keyFor(selector)
        selectorKey?.attachment()?.let {
            // 关闭前可使用Attach简单判断是否已处于队列中
            selectorKey.attach(null)
            // 添加取消操作
            val ioTask = IoTask(channel, 0, null)
            registerTaskQueue.offer(ioTask)
        }
    }

    /**
     * 将单词就绪的任务缓存加入到总队列中
     *
     * @param readyTaskQueue 总任务队列
     * @param onceReadyTaskCache 单次待执行的任务
     */
    private fun joinTaskQueue(readyTaskQueue: LinkedBlockingQueue<IoTask>, onceReadyTaskCache: List<IoTask>) {
        readyTaskQueue.addAll(onceReadyTaskCache)
    }


    override fun run() {
        super.run()
        val selector = this.selector
        val readyTaskQueue = this.readyTaskQueue
        val registerTaskQueue = this.registerTaskQueue
        val onceReadyTaskCache = this.onceReadyTaskCache

        try {
            while (isRunning) {
                // 加入待注册的通道
                consumeRegisterTodoTasks(registerTaskQueue)

                // 检查一次
                if (selector.selectNow() == 0) {
                    Thread.yield()
                    continue
                }


                // 处理已就绪的任务
                val selectedKeys = selector.selectedKeys()
                val iterator = selectedKeys.iterator()

                // 迭代已就绪的任务
                while (iterator.hasNext()) {
                    val selectionKey = iterator.next()
                    val attachment = selectionKey.attachment()
                    // 检查有效性
                    if (selectionKey.isValid && attachment is KeyAttachment) {

                        try {
                            val readyOps = selectionKey.readyOps()
                            var interestOps = selectionKey.interestOps()

                            // 是否可读
                            if ((readyOps and SelectionKey.OP_READ) != 0) {
                                onceReadyTaskCache.add(attachment.taskForReadable!!)
                                interestOps = interestOps and (SelectionKey.OP_READ.inv())
                            }

                            // 是否可写
                            if ((readyOps and SelectionKey.OP_WRITE) != 0) {
                                onceReadyTaskCache.add(attachment.taskForWritable!!)
                                interestOps = interestOps and (SelectionKey.OP_WRITE.inv())
                            }

                            // 取消已就绪的关注
                            selectionKey.interestOps(interestOps)
                        } catch (ignored: CancelledKeyException) {
                            // 当前连接被取消，断开时直接移除相关任务
                            onceReadyTaskCache.remove(attachment.taskForReadable)
                            onceReadyTaskCache.remove(attachment.taskForWritable)
                        }
                    }

                    iterator.remove()
                }

                // 判断本次是否有待执行的任务
                if (!onceReadyTaskCache.isEmpty()) {
                    // 加入到总队列中
                    joinTaskQueue(readyTaskQueue, onceReadyTaskCache)
                    onceReadyTaskCache.clear()
                }

                // 消费总队列的任务
                consumeTodoTasks(readyTaskQueue, registerTaskQueue)
            }
        } catch (ignored: ClosedSelectorException) {

        } catch (e: IOException) {
            CloseUtils.close(selector)
        } finally {
            readyTaskQueue.clear()
            registerTaskQueue.clear()
            onceReadyTaskCache.clear()
        }
    }

    /**
     * 消费当前待注册的通道任务
     *
     * @param registerTaskQueue 待注册的通道
     */
    private fun consumeRegisterTodoTasks(registerTaskQueue: LinkedBlockingQueue<IoTask>) {
        val selector = this.selector
        var registerTask = registerTaskQueue.poll()

        while (registerTask != null) {
            try {
                val channel = registerTask.channel
                val ops = registerTask.ops
                if (ops == 0) {
                    // Cancel
                    val key: SelectionKey? = channel.keyFor(selector)
                    key?.cancel()
                } else if (ops and VALID_OPS.inv() == 0) {
                    var key: SelectionKey? = channel.keyFor(selector)
                    if (key == null) {
                        key = channel.register(selector, ops, KeyAttachment())
                    } else {
                        key.interestOps(key.interestOps() or ops)
                    }

                    val attachment = key!!.attachment()
                    if (attachment is KeyAttachment) {
                        attachment.attach(ops, registerTask)
                    } else {
                        // 外部关闭，直接取消
                        key.cancel()
                    }
                }
            } catch (ignored: ClosedChannelException) {

            } catch (ignored: CancelledKeyException) {

            } catch (ignored: ClosedSelectorException) {

            } finally {
                registerTask = registerTaskQueue.poll()
            }
        }


    }


    /**
     * 消费待完成的任务
     */
    private fun consumeTodoTasks(readyTaskQueue: LinkedBlockingQueue<IoTask>,
                                 registerTaskQueue: LinkedBlockingQueue<IoTask>) {
        // 循环把所有任务做完
        var doTask:IoTask? = readyTaskQueue.poll()
        while (doTask != null){
            // 增加饱和度
            saturatingCapacity.incrementAndGet()
            // 做任务
            if(processTask(doTask)){
                // 做完工作后添加待注册的列表
                registerTaskQueue.add(doTask)
            }

            // 下个任务
            doTask = readyTaskQueue.poll()
        }

        // 窃取其它的任务
        val stealingService:StealingService? = this.stealingService
        if(stealingService != null){
            doTask = stealingService.steal(readyTaskQueue)
            while (doTask != null){
                saturatingCapacity.incrementAndGet()
                if(processTask(doTask)){
                    registerTaskQueue.offer(doTask)
                }

                doTask = stealingService.steal(readyTaskQueue)
            }
        }

    }

    /**
     * 调用子类执行任务操作
     *
     * @param task 任务
     * @return 执行任务后是否需要再次添加该任务
     */
    abstract fun processTask(task: IoTask): Boolean

    fun exit() {
        isRunning = false
        CloseUtils.close(selector)
        interrupt()
    }

    class KeyAttachment {
        // 可读时执行的任务
        var taskForReadable: IoTask? = null
        // 可写时执行的任务
        var taskForWritable: IoTask? = null

        fun attach(ops: Int, task: IoTask) {
            if (ops == SelectionKey.OP_READ) {
                taskForReadable = task
            } else {
                taskForWritable = task
            }
        }
    }
}