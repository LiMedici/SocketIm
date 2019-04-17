package com.mrmedici.clink.impl.stealing

import com.mrmedici.clink.core.IoTask
import com.mrmedici.clink.impl.IoStealingSelectorProvider
import java.util.*
import java.util.concurrent.LinkedBlockingQueue

class StealingService(private val threads:Array<IoStealingSelectorProvider.IoStealingThread>,
                      private val minSafetyThreshold:Int){

    // 结束标志
    @Volatile
    private var isTerminated = false
    private var queues:Array<Queue<IoTask>> = threads.map(StealingSelectorThread::getReadyTaskQueue).toTypedArray()


    /**
     * 窃取一个任务，排除自己，从他人队列窃取一个任务
     *
     * @param excludeQueue 待排除的队列
     * @return 窃取成功返回的队列，失败返回Null
     */
    fun steal(excludeQueue:Queue<IoTask>): IoTask?{
        val minSafetyThreshold = this.minSafetyThreshold
        val queues = this.queues
        for (queue in queues){
            if(queue === excludeQueue){
                continue
            }

            val size = queue.size
            if(size > minSafetyThreshold){
                val poll: IoTask? = queue.poll()
                if(poll != null){
                    return poll
                }
            }
        }

        return null

    }

    /**
     * 获取一个不繁忙的线程
     *
     * @return StealingSelectorThread
     */
    fun getNotBusyThread():StealingSelectorThread?{
        var targetThread:StealingSelectorThread? = null
        var targetSaturatingCapacity = Long.MAX_VALUE
        for (thread in threads){
            val saturatingCapacity = thread.getSaturatingCapacity()
            if(!(saturatingCapacity == (-1).toLong() || saturatingCapacity >= targetSaturatingCapacity)){
                targetSaturatingCapacity = saturatingCapacity
                targetThread = thread
            }
        }

        return targetThread
    }

    /**
     * 结束操作
     */
    fun shutdownNow(){
        if(isTerminated){
            return
        }

        isTerminated = true
        for (thread in threads){
            thread.exit()
        }
    }

    /**
     * 是否已经结束
     *
     * @return true 已结束
     */
    fun isTerminated():Boolean = isTerminated

    /**
     * 执行一个任务
     * @param task 任务
     */
    fun execute(task: IoTask){

    }

}