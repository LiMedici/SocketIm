package com.mrmedici.foo

import java.io.File
import java.lang.RuntimeException
import java.util.*

const val CACHE_DIR:String = "cache"
const val COMMAND_EXIT = "00bye00"

const val COMMAND_GROUP_JOIN = "--m g join"
const val COMMAND_GROUP_LEAVE = "--m g leave"
const val DEFAULT_GROUP_NAME = "SOCKET"

class Foo{
    companion object {
        fun getCacheDir(dir: String):File {
            val path = System.getProperty("user.dir") + (File.separator + CACHE_DIR + File.separator + dir)
            val file = File(path)
            if(!file.exists()){
                if(!file.mkdirs()){
                    throw RuntimeException("Create path error:$path")
                }
            }

            return file
        }

        fun createRandomTemp(parent:File):File{
            val string = "${UUID.randomUUID()}.temp"
            val file = File(parent,string)
            file.createNewFile()
            return file
        }

    }

}