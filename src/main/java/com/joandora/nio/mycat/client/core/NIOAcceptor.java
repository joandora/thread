/**   
 * Copyright © 2015 JOANDORA. All rights reserved.
 */
package com.joandora.nio.mycat.client.core;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.StandardSocketOptions;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Set;





import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.joandora.nio.mycat.client.buffer.NIOProcessor;
import com.joandora.nio.mycat.client.buffer.NIOProcessorPool;
import com.joandora.nio.mycat.client.util.NIOUtils;

/**
 * 1、功能：与客户端建立连接,然后连接交由NIOReactor处理<br>
 * 	独立开启：Selector、ServerSocketChannel
 * 	NIOAcceptor与NIOReactor内部的线程之间通过队列ConcurrentLinkedQueue<SocketChannel>进行通信
 * 2、处理过程：负责监听客户端的连接，然后把连接成功后得到的SocketChannel封装到FrontendConnection，
 * 然后再交给NIOReactor处理<br>
 * 3、绑定端口为8066<br>
 * 4、是一个线程<br>
 * 5、run方法只接受accept类型的key<br>
 * 6、连接之后将SocketChannel封装进FrontendConnection，并交由NIOReactor处理<br>
 */
public class NIOAcceptor extends Thread {

    private static final Logger LOGGER = LoggerFactory.getLogger(NIOAcceptor.class);
    /**輪詢器**/
    private final Selector selector;
    /**服务端socket通道**/
    private final ServerSocketChannel serverChannel;
    /**NIOReactor池，处理客户端连接**/
    private final NIOReactorPool reactorPool;

    /***
     * <p>
     * 唯一构造器<br>
     * 创建Selector<br>
     * 创建ServerSocketChannel<br>
     * ServerSocketChannel绑定ip和8066端口<br>
     * ServerSocketChannel注册到Selector,并关注accept事件<br>
     * </p>
     */
    public NIOAcceptor(String name, String bindIp, int port, NIOReactorPool reactorPool) throws IOException {
	super.setName(name);
	/**************** NIO服务端基本步骤-begin ********************/
	this.selector = Selector.open();
	this.serverChannel = ServerSocketChannel.open();
	this.serverChannel.configureBlocking(false);
	// 设置TCP属性
	serverChannel.setOption(StandardSocketOptions.SO_REUSEADDR, true);
	serverChannel.setOption(StandardSocketOptions.SO_RCVBUF, 1024 * 16 * 2);
	//backlog参数限制accept队列最大长度，超出将拒绝连接。顺序为FIFO
	serverChannel.bind(new InetSocketAddress(bindIp, port), 5000);
	this.serverChannel.register(selector, SelectionKey.OP_ACCEPT);
	/**************** NIO服务端基本步骤-end ********************/
	this.reactorPool = reactorPool;
    }
    /**
     * 只处理客户端连接事件<br>
     * 有客户端连接，就交由NIOReactor进行调度<br>
     */
    @Override
    public void run() {
	while (true) {
	    try {
		// 防止cpu不断轮询，使cpu负载过高
		this.selector.select(1000L);
		Set<SelectionKey> keys = this.selector.selectedKeys();
		try {
		    for (SelectionKey key : keys) {
			if (key.isValid() && key.isAcceptable()) {
			    accept();
			} else {
			    key.cancel();
			}
		    }
		} finally {
		    keys.clear();
		}
	    } catch (Exception e) {
		LOGGER.error(StringUtils.defaultString(e.getMessage()));
	    }
	}
    }
    /**
     * <p>
     * 当有客户端连接时，将创建的SocketChannel放入NIOReactor的队列中<br> 
     * 调用ServerSocketChannel的accept事件：<br> 
     * 1、获取SocketChannel
     * 2、让客户端获取finishConnect成功事件
     * </p>
     */
    private void accept() {
	SocketChannel socketChannel = null;
	try {
	    socketChannel = serverChannel.accept();
	    socketChannel.configureBlocking(false);

	    NIOProcessor nioProcessor = NIOProcessorPool.getInstance().getNextProcessor();
	    NIOConnection nioConnection = new NIOConnection(socketChannel,nioProcessor);
	    NIOReactor reactor = reactorPool.getNextReactor();
	    reactor.postRegister(nioConnection);

	} catch (Exception e) {
	    LOGGER.warn(StringUtils.defaultString(e.getMessage()));
	    NIOUtils.closeChannel(socketChannel);
	}
    }
}
