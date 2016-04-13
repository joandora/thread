/**   
 * Copyright © 2015 JOANDORA. All rights reserved.
 */   
package com.joandora.nio.mycat.client.buffer; 

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * <p>
 * 
 *
 *
 * </p>
 * @author	JOANDORA
 * @date	2016年4月11日 下午5:42:55
 */
public class NameableThreadFactory implements ThreadFactory {
	private final ThreadGroup group;
	private final String namePrefix;
	private final AtomicInteger threadId;
	private final boolean isDaemon;

	public NameableThreadFactory(String name, boolean isDaemon) {
		SecurityManager s = System.getSecurityManager();
		this.group = (s != null) ? s.getThreadGroup() : Thread.currentThread()
				.getThreadGroup();
		this.namePrefix = name;
		this.threadId = new AtomicInteger(0);
		this.isDaemon = isDaemon;
	}

	public Thread newThread(Runnable r) {
		Thread t = new Thread(group, r, namePrefix + threadId.getAndIncrement());
		t.setDaemon(isDaemon);
		return t;
	}
}
 