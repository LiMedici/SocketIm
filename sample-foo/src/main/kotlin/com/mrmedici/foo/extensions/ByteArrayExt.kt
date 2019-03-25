package com.mrmedici.foo.extensions

import com.mrmedici.clink.utils.ByteUtils

fun ByteArray.startWith(byteArray: ByteArray):Boolean{
    return ByteUtils.startsWith(this,byteArray)
}