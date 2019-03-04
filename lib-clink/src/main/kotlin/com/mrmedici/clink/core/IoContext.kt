package com.mrmedici.clink.core

import java.io.IOException

class IoContext(val ioProvider: IoProvider?){

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
    }

    class StartedBoot{

        private var ioProvider:IoProvider? = null

        fun ioProvider(ioProvider: IoProvider):StartedBoot{
            this.ioProvider = ioProvider
            return this
        }

        fun start():IoContext?{
            INSTANCE = IoContext(ioProvider)
            return INSTANCE
        }

    }
}