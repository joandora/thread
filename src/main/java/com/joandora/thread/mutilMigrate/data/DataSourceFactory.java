/**   
 * Copyright © 2016 JOANDORA. All rights reserved.
 * 
 * @Title: DataSource.java 
 * @Prject: thread
 * @Package: com.joandora.thread.mutilMigrate 
 * @version: V1.0   
 */
package com.joandora.thread.mutilMigrate.data;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.SynchronousQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @ClassName: DataSource
 * @Description: 数据迁移时 数据队列（阻塞队列）
 * @author: JOANDORA
 * @date: 2016年1月26日 下午10:23:44
 */
public class DataSourceFactory<T>{

	Logger logger = LoggerFactory.getLogger(DataSourceFactory.class);
	
	/** 数据队列 */
	private BlockingQueue<T> dataQueue = new SynchronousQueue<T>();

	public void put(T element) {
		try {
			dataQueue.put(element);
		} catch (InterruptedException e) {
			logger.error("error-----"+e.getMessage());
		}
	}

	public T take() {
		try {
			return (T) dataQueue.take();
		} catch (InterruptedException e) {
			logger.error("error-----"+e.getMessage());
		}
		throw new NullPointerException("take error for null");
	}
}
