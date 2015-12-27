/**   
 * Copyright © 2015 JOANDORA. All rights reserved.
 * 
 * @Title: JodoUncaughtExceptionHandler.java 
 * @Prject: thread
 * @Package: com.joandora.thread.factory 
 * @version: V1.0   
 */
package com.joandora.thread.factory;

import java.lang.Thread.UncaughtExceptionHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @ClassName: JodoUncaughtExceptionHandler
 * @Description: 线程中处理未受检查异常类
 * @author: JOANDORA
 * @date: 2015年12月26日 下午11:55:28
 */
public class JodoUncaughtExceptionHandler implements UncaughtExceptionHandler {
	private final static Logger logger = LoggerFactory.getLogger(JodoThread.class);

	@Override
	public void uncaughtException(Thread t, Throwable e) {
		logger.debug("UNCAUGHT in thread " + t.getName(), e);
	}

}
