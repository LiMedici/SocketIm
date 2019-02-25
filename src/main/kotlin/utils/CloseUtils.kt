package utils

import java.io.Closeable
import java.io.IOException

class CloseUtils{
    companion object {
        fun close(vararg closeables:Closeable){
            try {
                closeables.forEach { it.close() }
            }catch (e:IOException){
                e.printStackTrace()
            }
        }
    }
}