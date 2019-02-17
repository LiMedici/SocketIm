package udp

const val SN_HEADER = "收到暗号，我是(SN)："
const val PORT_HEADER = "这是暗号，请回电端口(Port)："

class MessageCreator{

    companion object {

        @JvmStatic
        fun buildWithPort(port:Int):String{
            return PORT_HEADER + port
        }

        @JvmStatic
        fun parsePort(data:String):Int{
            if(data.startsWith(PORT_HEADER)){
                return data.substring(PORT_HEADER.length).toInt()
            }

            return -1
        }

        @JvmStatic
        fun buildWithSn(sn:String):String{
            return SN_HEADER + sn
        }

        @JvmStatic
        fun parseSn(data:String):String?{
            if(data.startsWith(SN_HEADER)){
                return data.substring(SN_HEADER.length)
            }

            return null
        }
    }
}