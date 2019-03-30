package com.mrmedici.clink.impl

import java.util.concurrent.ThreadFactory
import java.util.concurrent.atomic.AtomicInteger

class NameableThreadFactory(namePrefix: String) : ThreadFactory {
    private val group: ThreadGroup
    private val threadNumber = AtomicInteger(1)
    private val namePrefix: String

    init {
        val manager = System.getSecurityManager()
        group = if (manager != null) manager.threadGroup
        else Thread.currentThread().threadGroup
        this.namePrefix = namePrefix
    }

    override fun newThread(runnable: Runnable): Thread {
        val thread = Thread(group, runnable, namePrefix + threadNumber.getAndIncrement(), 0)
        if (thread.isDaemon) thread.isDaemon = false
        if (thread.priority != Thread.NORM_PRIORITY) thread.priority = Thread.NORM_PRIORITY
        return thread
    }
}