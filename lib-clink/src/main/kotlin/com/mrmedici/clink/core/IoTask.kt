package com.mrmedici.clink.core

import java.nio.channels.SocketChannel

abstract class IoTask(val channel:SocketChannel,val ops:Int){

    abstract fun onProcessIo():Boolean

    abstract fun fireThrowable(e:Throwable)

}