package client

import java.io.IOException

fun main(args: Array<String>) {
    val info = ClientSearcher.searchServer(10000)
    println("Server:$info")

    if(info != null){
        try{
            TCPClient.linkWith(info)
        }catch (e:IOException){
            e.printStackTrace()
        }
    }
}