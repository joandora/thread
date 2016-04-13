/**   
 * Copyright © 2015 JOANDORA. All rights reserved.
 */
package com.joandora.nio.mycat.client.buffer;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.joandora.nio.mycat.client.core.NIOHandler;

/**
 * <p>
 * 连接、缓冲管理器
 * </p>
 * 
 * @author JOANDORA
 * @date 2016年4月11日 下午4:49:20
 */
public class NIOProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(NIOProcessor.class);

    private final String name;

    private final BufferPool bufferPool;

    private final NameableExecutor executor;

    private final ConcurrentMap<Long, NIOHandler> frontends;


    // 前端已连接数
    private AtomicInteger frontendsLength = new AtomicInteger(0);

    public NIOProcessor(String name, BufferPool bufferPool, NameableExecutor executor) throws IOException {
	this.name = name;
	this.bufferPool = bufferPool;
	this.executor = executor;
	this.frontends = new ConcurrentHashMap<Long, NIOHandler>();
    }
    /**
     * 定时执行该方法，回收部分资源。
     */
    public void checkFrontCons() {
	frontendCheck();
    }

    // 前端连接检查
    private void frontendCheck() {
	Iterator<Entry<Long, NIOHandler>> it = frontends.entrySet().iterator();
	while (it.hasNext()) {
	    NIOHandler nioHandler = it.next().getValue();

	    // 删除空连接
	    if (nioHandler == null) {
		it.remove();
		this.frontendsLength.decrementAndGet();
		continue;
	    }
	}
    }
    public int getWriteQueueSize() {
	int total = 0;
	for (NIOHandler fron : frontends.values()) {
	    total += fron.getWriteQueue().size();
	}
	return total;

    }

    public NameableExecutor getExecutor() {
	return this.executor;
    }

    public void addFrontend(NIOHandler c) {
	this.frontends.put(c.getId(), c);
	this.frontendsLength.incrementAndGet();
    }

    public ConcurrentMap<Long, NIOHandler> getFrontends() {
	return this.frontends;
    }

    public int getForntedsLength() {
	return this.frontendsLength.get();
    }
    public String getName() {
	return name;
    }

    public BufferPool getBufferPool() {
	return bufferPool;
    }
}
