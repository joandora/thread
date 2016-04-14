/**   
 * Copyright © 2016 JOANDORA. All rights reserved.
 * 
 * @Title: MigrateThread.java 
 * @Prject: thread
 * @Package: com.joandora.thread.mutilMigrate.task 
 * @version: V1.0   
 */
package com.joandora.thread.mutilMigrate.task;

import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.joandora.thread.mutilMigrate.business.BusinessService;

/**
 * @ClassName: MigrateThread
 * @Description: Callable 线程，单个线程处理业务逻辑
 * @author: JOANDORA
 * @date: 2016年1月27日 下午8:28:07
 */
public class MigrateThread<T> implements Callable<Integer> {

	Logger logger = LoggerFactory.getLogger(MigrateThread.class);
	
	/** 业务service 接口  */
	private BusinessService<T> businessService;
	/**  业务参数 */
	private T businessParam;

	public MigrateThread(T businessParam) {
		this.businessParam = businessParam;
	}

	@Override
	public Integer call() {
		int countNum = 0;
		try {
			countNum = businessService.runTask(businessParam);
		} catch (Exception e) {
			logger.error("error---" + e.getMessage());
		}
		return countNum;
	}

	public void setBusinessService(BusinessService<T> businessService) {
		this.businessService = businessService;
	}

}
