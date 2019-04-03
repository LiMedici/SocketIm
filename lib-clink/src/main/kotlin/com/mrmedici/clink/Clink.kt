package com.mrmedici.clink

import java.io.BufferedInputStream
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit

class TestThread : Thread(){
    override fun run() {
        super.run()
        while (true){
            val input = BufferedInputStream(System.`in`)
            val bufferReader = BufferedReader(InputStreamReader(input))
            val str:String? = bufferReader.readLine()
            if(str.isNullOrEmpty()){
                break
            }
            TimeUnit.SECONDS.sleep(2)
            println(str)
        }
    }
}


fun main(args: Array<String>) {
    val thread = TestThread()
    thread.start()
    TimeUnit.SECONDS.sleep(10)
    System.`in`.close()
}