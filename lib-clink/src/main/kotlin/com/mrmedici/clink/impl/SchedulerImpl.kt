package com.mrmedici.clink.impl

import com.mrmedici.clink.core.Scheduler
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

class SchedulerImpl(poolSize:Int) : Scheduler{

    private val scheduledThreadPool = Executors.newScheduledThreadPool(poolSize,
            NameableThreadFactory("Scheduler-Thread-"))

    private val deliveryPool = Executors.newFixedThreadPool(4,
            NameableThreadFactory("Delivery-Thread-"))

    override fun schedule(runnable: Runnable, delay: Long, unit: TimeUnit): ScheduledFuture<*> {
        return scheduledThreadPool.schedule(runnable,delay,unit)
    }

    override fun delivery(runnable: Runnable) {
        deliveryPool.execute(runnable)
    }

    override fun close() {
        scheduledThreadPool.shutdownNow()
        deliveryPool.shutdownNow()
    }
}