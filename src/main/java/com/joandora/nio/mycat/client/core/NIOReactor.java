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

    private final ConcurrentLinkedQueue<NIOHandler> registerQueue;

    /** 构造器 **/
    public NIOReactor(String name) throws IOException {
	super.setName(name);
	this.selector = Selector.open();
	this.registerQueue = new ConcurrentLinkedQueue<NIOHandler>();
    }

    /** 将nioHandler放入注册队列中 **/
    final void postRegister(NIOHandler nioHandler) {
	this.registerQueue.offer(nioHandler);
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
		    NIOHandler nioHandler = null;
		    try {
			Object att = key.attachment();
			if (att != null) {
			    nioHandler = (NIOHandler) att;
			    if (key.isReadable()) {
				try {
				    nioHandler.syncRead();
				    nioHandler.registerWrite(this.selector);
				} catch (Exception e) {
				    NIOUtils.closeChannel(nioHandler.getSocketChannel());
				    continue;
				}
			    }
			    if (key.isWritable()) {
				nioHandler.doNextWriteCheck();
			    }
			} else {
			    key.cancel();
			}
		    } catch (CancelledKeyException e) {
			LOGGER.debug("[{}] socket key canceled:{}", nioHandler.getId(), StringUtils.defaultString(e.getMessage()));
		    } catch (Exception e) {
			LOGGER.warn(nioHandler + " " + e);
		    }catch(final Throwable e){
			// 防止发生如OOM等异常时，NIOReactor仍能继续运行
			if(null != nioHandler && null != nioHandler.getSocketChannel()){
			    NIOUtils.closeChannel(nioHandler.getSocketChannel());
			}
			LOGGER.error("caught err: {}",e.getMessage());
			continue;
		    }
		}
	    } catch (Exception e) {
		LOGGER.error("caught err: {}",e.getMessage());
	    }catch(final Throwable e){
		// 防止发生如内存溢出等异常时，NIOReactor仍能继续运行
		LOGGER.error("caught err: {}",e.getMessage());
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
	NIOHandler nioHandler = null;
	SocketChannel socketChannel = null;
	if (registerQueue.isEmpty()) {
	    return;
	}
	while ((nioHandler = registerQueue.poll()) != null) {
	    try {
		nioHandler.registerRead(selector);// 注册读事件
	    } catch (Exception e) {
		NIOUtils.closeChannel(socketChannel);
	    }
	}
    }

    public Queue<NIOHandler> getRegisterQueue() {
	return this.registerQueue;
    }
}
