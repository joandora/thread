/**   
 * Copyright © 2015 JOANDORA. All rights reserved.
 */
package com.joandora.nio.mycat.client.util;

import java.io.IOException;
import java.net.Socket;
import java.nio.channels.SocketChannel;

import org.apache.log4j.Logger;

/**
 * <p>
 * 工具类
 * </p>
 * 
 * @author JOANDORA
 * @date 2016年4月11日 下午3:53:38
 */
public class NIOUtils {

    private static final Logger LOGGER = Logger.getLogger(NIOUtils.class);

    /**
     * <p>
     * 当遇到失败时，逐步关闭SocketChannel<br>
     * 先关闭Socket<br>
     * 再关闭SocketChannel<br>
     * </p>
     */
    public static void closeChannel(SocketChannel channel) {
	if (channel == null) {
	    return;
	}
	Socket socket = channel.socket();
	if (socket != null) {
	    try {
		socket.close();
	    } catch (IOException e) {
		LOGGER.error("closeChannel  Error", e);
	    }
	}
	try {
	    channel.close();
	} catch (IOException e) {
	    LOGGER.error("closeChannel  Error", e);
	}
    }
}
