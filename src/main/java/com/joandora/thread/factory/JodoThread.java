/**   
 * Copyright © 2015 JOANDORA. All rights reserved.
 * 
 * @Title: JodoThread.java 
 * @Prject: thread
 * @Package: com.joandora.thread.factory 
 * @version: V1.0   
 */
package com.joandora.thread.factory;

import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @ClassName: JodoThread
 * @Description: TODO
 * @author: JOANDORA
 * @date: 2015年12月26日 下午11:38:47
 */
public class JodoThread extends Thread {
	private static volatile boolean debugLifecycle = true;
	private static final AtomicInteger alive = new AtomicInteger();
	private final static Logger logger = LoggerFactory.getLogger(JodoThread.class);
	/** 默认线程池名称 **/
	private static final String DEFAULT_NAME = "THREAD-JOANDORA";
	/** 线程序号，每创建一个，值加一 **/
	private static final AtomicInteger created = new AtomicInteger();

	public JodoThread(Runnable runnable) {
		this(runnable, DEFAULT_NAME);
	}

	public JodoThread(Runnable runnable, String poolName) {
		super(runnable, poolName + "-" + created.incrementAndGet());
		// 对于线程来说，即使try catch也无法捕捉异常
		super.setUncaughtExceptionHandler(new JodoUncaughtExceptionHandler());
	}

	@Override
	public void run() {
		// 复制debug标志以确保一致的值
		boolean debug = debugLifecycle;
		if (debug){
			logger.debug("Created " + getName());
		}
		try {
			alive.incrementAndGet();
			super.run();
		} finally {
			alive.decrementAndGet();
			if (debug) {
				logger.debug("Exiting " + getName());
			}
		}
	}

	public static int getThreadsCreated() {
		return created.get();
	}

	public static int getThreadsAlive() {
		return alive.get();
	}

	public static boolean getDebug() {
		return debugLifecycle;
	}

	public static void setDebug(boolean b) {
		debugLifecycle = b;
	}
}
