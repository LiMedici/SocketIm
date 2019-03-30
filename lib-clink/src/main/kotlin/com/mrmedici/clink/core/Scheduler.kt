package com.mrmedici.clink.core

import java.io.Closeable
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

interface Scheduler : Closeable{
    fun schedule(runnable: Runnable,delay:Long,unit: TimeUnit):ScheduledFuture<*>

    fun delivery(runnable: Runnable);
}