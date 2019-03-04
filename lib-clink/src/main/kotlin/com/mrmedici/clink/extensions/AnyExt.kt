package com.mrmedici.clink.extensions

fun Any.waitK(){
    (this as java.lang.Object).wait()
}

fun Any.notifyK(){
    (this as java.lang.Object).notify()
}

fun Any.notifyAllK(){
    (this as java.lang.Object).notifyAll()
}