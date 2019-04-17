package com.mrmedici.clink.impl.stealing

import com.mrmedici.clink.core.IoTask
import com.mrmedici.clink.utils.CloseUtils
import java.io.IOException
import java.nio.channels.*
import java.util.*
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import javax.print.attribute.standard.NumberUp
import kotlin.collections.ArrayList

const val VALID_OPS = SelectionKey.OP_READ or SelectionKey.OP_WRITE
const val MAX_ONCE_READ_TASK = 128
const val MAX_ONCE_WRITE_TASK = 128
const val MAX_ONCE_RUN_TASK = MAX_ONCE_READ_TASK + MAX_ONCE_WRITE_TASK

abstract class StealingSelectorThread(private val selector: Selector) : Thread() {

    // 是否处于运行中
    @Volatile
    private var isRunning = true
    // 已就绪任务队列
    private val readyTaskQueue = ArrayBlockingQueue<IoTask>(MAX_ONCE_RUN_TASK)
    // 待注册的任务队列
    private val registerTaskQueue = ConcurrentLinkedQueue<IoTask>()

    // 任务饱和度度量
    private val saturatingCapacity = AtomicLong()
    private val unregisterLocker = AtomicBoolean(false)

    // 用于多线程协同的Service
    // 可能是单线程池
    @Volatile
    private var stealingService: StealingService? = null

    fun setStealingService(stealingService: StealingService){
        this.stealingService = stealingService
    }

    fun getReadyTaskQueue():Queue<IoTask>{
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
     */
    @Throws(UnsupportedOperationException::class)
    fun register(task: IoTask) {
        if(task.ops and (VALID_OPS.inv()) != 0){
            throw UnsupportedOperationException("Unsupported register ops:${task.ops}")
        }

        registerTaskQueue.offer(task)
        selector.wakeup()
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

            if(Thread.currentThread() === this){
                selectorKey.cancel()
            }else{
                synchronized(unregisterLocker){
                    unregisterLocker.set(true)
                    selector.wakeup()
                    selectorKey.cancel()
                    unregisterLocker.set(false)
                }
            }
        }
    }

    /**
     * 将单词就绪的任务缓存加入到总队列中
     *
     * @param readyTaskQueue 总任务队列
     * @param onceReadyTaskCache 单次待执行的任务
     */
    private fun joinTaskQueue(readyTaskQueue: Queue<IoTask>, onceReadyTaskCache: List<IoTask>) {
        readyTaskQueue.addAll(onceReadyTaskCache)
    }


    override fun run() {
        super.run()
        val selector = this.selector
        val readyTaskQueue = this.readyTaskQueue
        val registerTaskQueue = this.registerTaskQueue
        val unregisterLocker = this.unregisterLocker
        val onceReadyReadTaskCache = ArrayList<IoTask>(MAX_ONCE_READ_TASK)
        val onceReadyWriteTaskCache = ArrayList<IoTask>(MAX_ONCE_WRITE_TASK)

        try {
            while (isRunning) {
                // 加入待注册的通道
                consumeRegisterTodoTasks(registerTaskQueue)

                val count = selector.select()

                while (unregisterLocker.get()){
                    Thread.yield()
                }

                if(count == 0){
                    continue
                }


                // 处理已就绪的任务
                val selectedKeys = selector.selectedKeys()
                val iterator = selectedKeys.iterator()

                var onceReadTaskCount = MAX_ONCE_READ_TASK
                var onceWriteTaskCount = MAX_ONCE_WRITE_TASK

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
                            if ((readyOps and SelectionKey.OP_READ) != 0 && onceReadTaskCount-- > 0) {
                                onceReadyReadTaskCache.add(attachment.taskForReadable!!)
                                interestOps = interestOps and (SelectionKey.OP_READ.inv())
                            }

                            // 是否可写
                            if ((readyOps and SelectionKey.OP_WRITE) != 0 && onceWriteTaskCount-- > 0) {
                                onceReadyWriteTaskCache.add(attachment.taskForWritable!!)
                                interestOps = interestOps and (SelectionKey.OP_WRITE.inv())
                            }

                            // TODO 通过 onceReadTaskCount + onceWriteTaskCount 致使已经就绪的任务并没有即时的读取 会不会在下次Select的时候丢失

                            // 取消已就绪的关注
                            selectionKey.interestOps(interestOps)
                        } catch (ignored: CancelledKeyException) {
                            // 当前连接被取消，断开时直接移除相关任务
                            if(attachment.taskForReadable != null){
                                onceReadyReadTaskCache.remove(attachment.taskForReadable!!)
                            }

                            if(attachment.taskForWritable != null){
                                onceReadyWriteTaskCache.remove(attachment.taskForWritable!!)
                            }
                        }
                    }

                    iterator.remove()
                }

                // 判断本次是否有待执行的任务
                if (!onceReadyReadTaskCache.isEmpty()) {
                    // 加入到总队列中
                    joinTaskQueue(readyTaskQueue, onceReadyReadTaskCache)
                    onceReadyReadTaskCache.clear()
                }

                // 判断本次是否有待执行的任务
                if (!onceReadyWriteTaskCache.isEmpty()) {
                    // 加入到总队列中
                    joinTaskQueue(readyTaskQueue, onceReadyWriteTaskCache)
                    onceReadyWriteTaskCache.clear()
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
        }
    }

    /**
     * 消费当前待注册的通道任务
     *
     * @param registerTaskQueue 待注册的通道
     */
    private fun consumeRegisterTodoTasks(registerTaskQueue: Queue<IoTask>) {
        val selector = this.selector
        var registerTask = registerTaskQueue.poll()

        while (registerTask != null) {
            try {
                val channel = registerTask.channel
                val ops = registerTask.ops

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
            } catch (e: ClosedChannelException) {
                registerTask.fireThrowable(e)
            } catch (e: CancelledKeyException) {
                registerTask.fireThrowable(e)
            } catch (e: ClosedSelectorException) {
                registerTask.fireThrowable(e)
            } finally {
                registerTask = registerTaskQueue.poll()
            }
        }


    }


    /**
     * 消费待完成的任务
     */
    private fun consumeTodoTasks(readyTaskQueue: Queue<IoTask>,
                                 registerTaskQueue: ConcurrentLinkedQueue<IoTask>) {
        // 循环把所有任务做完
        var doTask: IoTask? = readyTaskQueue.poll()
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