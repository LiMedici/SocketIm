package com.mrmedici.foo.handle

import com.mrmedici.clink.box.StringReceivePacket

/**
 * 字符串包链式结构
 */
abstract class ConnectorStringPacketChain : ConnectorHandlerChain<StringReceivePacket>()