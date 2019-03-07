package com.mrmedici.clink.core

import java.io.OutputStream

/**
 * 接收包的定义
 */
abstract class ReceivePacket<T : OutputStream> : Packet<T>(){
}