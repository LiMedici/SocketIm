package com.mrmedici.foo.handle

import com.mrmedici.clink.core.Connector

/**
 * 关闭链接链式结构
 */
abstract class ConnectorCloseChain : ConnectorHandlerChain<Connector>()