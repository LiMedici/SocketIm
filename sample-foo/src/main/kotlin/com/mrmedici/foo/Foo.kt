package com.mrmedici.foo

import java.io.File
import java.lang.RuntimeException
import java.util.*

const val CACHE_DIR:String = "cache"

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