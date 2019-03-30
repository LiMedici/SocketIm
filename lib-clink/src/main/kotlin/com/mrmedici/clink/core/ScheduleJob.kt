package com.mrmedici.clink.core

import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

abstract class ScheduleJob : Runnable{
    protected val idleTimeoutMilliseconds:Long
    protected val connector:Connector

    @Volatile
    private var scheduler:Scheduler? = null
    @Volatile
    private var scheduledFuture:ScheduledFuture<*>? = null

    constructor(idleTime:Long,unit:TimeUnit,connector: Connector){
        idleTimeoutMilliseconds = unit.toMillis(idleTime)
        this.connector = connector
    }

    @Synchronized
    fun schedule(scheduler: Scheduler){
        this.scheduler = scheduler
        schedule(idleTimeoutMilliseconds)
    }

    @Synchronized
    fun unSchedule(){
        if(scheduler != null){
            scheduler = null
        }

        if(scheduledFuture != null){
            scheduledFuture?.cancel(true)
            scheduledFuture = null
        }
    }

    @Synchronized
    protected fun schedule(timeoutMilliseconds:Long){
        scheduledFuture = scheduler?.schedule(this,timeoutMilliseconds,TimeUnit.MILLISECONDS)
    }
}