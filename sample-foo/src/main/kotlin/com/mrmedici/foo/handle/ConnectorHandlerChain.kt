package com.mrmedici.foo.handle

import server.handle.ConnectorHandler

abstract class ConnectorHandlerChain<Model>{

    @Volatile
    private var next:ConnectorHandlerChain<Model>? = null


    // TODO 精辟代码
    fun appendLast(newChain: ConnectorHandlerChain<Model>):ConnectorHandlerChain<Model>{
        if(newChain == this || this.javaClass === newChain.javaClass){
            return this
        }

        synchronized(this){
            if(next == null){
                next = newChain
                return newChain
            }

            return next!!.appendLast(newChain)
        }
    }

    // TODO 精辟代码
    fun remove(clx:Class<out ConnectorHandlerChain<Model>>):Boolean{
        if(this.javaClass === clx){
            // 自己不能移除自己
            return false
        }

        synchronized(this){
            return when {
                next == null -> false
                next!!.javaClass === clx -> {
                    next = next!!.next
                    true
                }
                else -> next!!.remove(clx)
            }
        }
    }

    @Synchronized
    fun handle(handler:ConnectorHandler, model:Model):Boolean{
        val next:ConnectorHandlerChain<Model>? = this.next

        // 自己消费
        // 对局部变量next赋值放在自己消费之前。
        // 防止自己未消费，只是添加一个功能节点，被刚添加的节点捕获到
        if(consume(handler,model)){
            return true
        }

        val consumed = next!=null && next.handle(handler,model)
        if(consumed){
            return true
        }

        return consumeAgain(handler,model)
    }

    protected abstract fun consume(handler:ConnectorHandler, model:Model):Boolean

    protected open fun consumeAgain(handler:ConnectorHandler, model:Model):Boolean{
        return false
    }

}