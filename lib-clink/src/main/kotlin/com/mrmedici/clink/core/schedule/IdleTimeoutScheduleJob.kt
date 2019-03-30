package com.mrmedici.clink.core.schedule

import com.mrmedici.clink.core.Connector
import com.mrmedici.clink.core.ScheduleJob
import java.util.concurrent.TimeUnit

class IdleTimeoutScheduleJob(idleTime:Long, unit: TimeUnit, connector: Connector) :
        ScheduleJob(idleTime,unit,connector){

    override fun run() {
        val lastActiveTime = connector.getLastActiveTime()
        val idleTimeoutMilliseconds = this.idleTimeoutMilliseconds

        val nextDelay = idleTimeoutMilliseconds - (System.currentTimeMillis() - lastActiveTime)
        if(nextDelay <= 0){
            schedule(idleTimeoutMilliseconds)
            try{
                connector.fireIdleTimeoutEvent()
            }catch (throwable:Throwable){
                connector.fireExceptionCaught(throwable)

            }
        }else{
            schedule(nextDelay)
        }
    }
}