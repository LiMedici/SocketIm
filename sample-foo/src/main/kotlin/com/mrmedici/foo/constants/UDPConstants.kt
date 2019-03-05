package constants

class UDPConstants{
    companion object {

        val HEADER:ByteArray = ByteArray(8,{7})

        const val PORT_SERVER = 30218

        const val PORT_CLIENT_RESPONSE = 30219
    }
}