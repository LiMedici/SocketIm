package com.mrmedici.clink.core

import java.io.IOException

class IoContext(val ioProvider: IoProvider?,
                val scheduler: Scheduler?){

    companion object {

        private var INSTANCE:IoContext? = null

        fun get():IoContext?{
            return INSTANCE
        }

        fun setup() = StartedBoot()

        @Throws(IOException::class)
        fun close(){
            INSTANCE?.callClose()
        }
    }

    @Throws(IOException::class)
    private fun callClose() {
        ioProvider?.close()
        scheduler?.close()
    }

    class StartedBoot{

        private var ioProvider:IoProvider? = null
        private var scheduler:Scheduler? = null

        fun ioProvider(ioProvider: IoProvider):StartedBoot{
            this.ioProvider = ioProvider
            return this
        }

        fun scheduler(scheduler: Scheduler):StartedBoot{
            this.scheduler = scheduler
            return this
        }

        fun start():IoContext?{
            INSTANCE = IoContext(ioProvider,scheduler)
            return INSTANCE
        }

    }
}