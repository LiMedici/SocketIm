package com.mrmedici.clink.core.ds

/**
 * 带优先级的节点，可用于构建链表
 */
class BytePriorityNode<Item>(val item:Item){

    private var priority:Byte = 0
    var next:BytePriorityNode<Item>? = null

    /**
     * 按优先级追加到当前链表中
     */
    fun appendWithPriority(node : BytePriorityNode<Item>){
        if(next == null){
            next = node
        }else{
            var after:BytePriorityNode<Item> = this.next!!
            if(after.priority < node.priority){
                // 从中间位置插入
                this.next = node
                node.next = after
            }else{
                after.appendWithPriority(node)
            }
        }
    }

}