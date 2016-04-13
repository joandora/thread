/**   
 * Copyright © 2015 JOANDORA. All rights reserved.
 */
package com.joandora.nio.mycat.client.core;

import java.io.IOException;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.joandora.nio.mycat.client.util.NIOUtils;

/**
 * <p>
 * 网络事件反应器<br>
 * 独立开启：Selector<br>
 * 拥有队列ConcurrentLinkedQueue<SocketChannel><br>
 * </p>
 * 
 * @author JOANDORA
 * @date 2016年4月8日 下午2:28:26
 */
public class NIOReactor extends Thread{

    private static final Logger LOGGER = LoggerFactory.getLogger(NIOReactor.class);

    private final Selector selector;

    private final ConcurrentLinkedQueue<NIOConnection> registerQueue;

    /** 构造器 **/
    public NIOReactor(String name) throws IOException {
	super.setName(name);
	this.selector = Selector.open();
	this.registerQueue = new ConcurrentLinkedQueue<NIOConnection>();
    }

    /** 将NIOConnection放入注册队列中 **/
    final void postRegister(NIOConnection nioConnection) {
	this.registerQueue.offer(nioConnection);
	this.selector.wakeup();
    }

    @Override
    public void run() {
	Set<SelectionKey> keys = null;
	while (true) {
	    try {
		this.selector.select(500L);
		register(this.selector);
		keys = selector.selectedKeys();
		for (SelectionKey key : keys) {
		    if (!key.isValid()) {
			continue;
		    }
		    NIOConnection nioConnection = null;
		    try {
			Object att = key.attachment();
			if (att != null) {
			    nioConnection = (NIOConnection) att;
			    if (key.isReadable()) {
				try {
				    nioConnection.syncRead();
				    nioConnection.registerWrite(this.selector);
				} catch (Exception e) {
				    NIOUtils.closeChannel(nioConnection.getSocketChannel());
				    continue;
				}
			    }
			    if (key.isWritable()) {
				nioConnection.doNextWriteCheck();
			    }
			} else {
			    key.cancel();
			}
		    } catch (CancelledKeyException e) {
			LOGGER.debug("[{}] socket key canceled:{}", nioConnection.getId(), StringUtils.defaultString(e.getMessage()));
		    } catch (Exception e) {
			LOGGER.warn(nioConnection + " " + e);
			break;
		    }
		}
	    } catch (Exception e) {
		LOGGER.error(e.getMessage());
	    } finally {
		if (keys != null) {
		    keys.clear();
		}

	    }
	}
    }

    /**
     * <p>
     * 线程一启动，Selector就开始不断轮询，不过该轮询为空。因为没有ServerSocketChannel与Selector绑定<br>
     * 无限循环队列ConcurrentLinkedQueue<SocketChannel>，
     * 并注意取出SocketChannel注册到Selector，绑定读事件<br>
     * </p>
     */
    private void register(Selector selector) {
	NIOConnection nioConnection = null;
	SocketChannel socketChannel = null;
	if (registerQueue.isEmpty()) {
	    return;
	}
	while ((nioConnection = registerQueue.poll()) != null) {
	    try {
		nioConnection.registerRead(selector);// 注册读事件
	    } catch (Exception e) {
		NIOUtils.closeChannel(socketChannel);
	    }
	}
    }

    public Queue<NIOConnection> getRegisterQueue() {
	return this.registerQueue;
    }
}
