/**   
 * Copyright © 2015 JOANDORA. All rights reserved.
 */
package com.joandora.nio.mycat.client.main;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.joandora.nio.mycat.client.buffer.BufferPool;
import com.joandora.nio.mycat.client.conf.ConfigureConstant;
import com.joandora.nio.mycat.client.core.NIOAcceptor;
import com.joandora.nio.mycat.client.core.NIOReactorPool;

/**
 * <p>
 * 启动测试类
 * </p>
 * 
 * @author JOANDORA
 * @date 2016年4月8日 下午4:05:35
 */
public class MainStartup {

    private static final Logger LOGGER = LoggerFactory.getLogger(MainStartup.class);

    private final static String BIND_SERVER_IP = "127.0.0.1";

    private final static int BIND_SERVER_PORT = 8066;

    public static void main(String[] args) throws Exception {
	NIOReactorPool reactorPool = new NIOReactorPool(BufferPool.LOCAL_BUF_THREAD_PREX + "NIOREACTOR", ConfigureConstant.defaultProcessorNum);

	NIOAcceptor server = new NIOAcceptor(BufferPool.LOCAL_BUF_THREAD_PREX + "NioAcceptorThread", BIND_SERVER_IP, BIND_SERVER_PORT, reactorPool);
	server.start();
	LOGGER.info("{}已启动,正在监听端口:{} ", server.getName(), BIND_SERVER_PORT);
    }
}
