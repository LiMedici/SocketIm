package com.mrmedici.foo

import java.io.File
import java.lang.RuntimeException
import java.util.*

// 绑定Stream到一个命令连接(带参数)
const val COMMAND_CONNECTOR_BIND = "--m c bind "
// 创建对话房间
const val COMMAND_AUDIO_CREATE_ROOM = "--m a create"
// 加入对话房间(带参数)
const val COMMAND_AUDIO_JOIN_ROOM = "--m a join "
// 主动离开对话房间
const val COMMAND_AUDIO_LEAVE_ROOM = "--m a leave"

// 回送服务器上的唯一标志(带参数)
const val COMMAND_INFO_NAME = "--i server "
// 回送语音群名(带参数)
const val COMMAND_INFO_AUDIO_ROOM = "--i a room "
// 回送语音开始(带参数)
const val COMMAND_INFO_AUDIO_START = "--i a start "
// 回送语音结束
const val COMMAND_INFO_AUDIO_STOP = "--i a stop"
// 回送语音操作错误
const val COMMAND_INFO_AUDIO_ERROR = "--i a error"


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