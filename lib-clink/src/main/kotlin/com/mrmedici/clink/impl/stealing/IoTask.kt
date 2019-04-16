package com.mrmedici.clink.impl.stealing

import com.mrmedici.clink.core.IoProvider
import java.nio.channels.SocketChannel

data class IoTask(val channel:SocketChannel,
                  val ops:Int,
                  val providerCallback:IoProvider.HandleProviderCallback?)