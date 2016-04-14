/**   
 * Copyright © 2016 JOANDORA. All rights reserved.
 * 
 * @Title: MigrateServiceImpl.java 
 * @Prject: thread
 * @Package: com.joandora.thread.mutilMigrate 
 * @version: V1.0   
 */
package com.joandora.thread.mutilMigrate.thread;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.joandora.thread.common.util.concurrent.ThreadFactoryBuilder;
import com.joandora.thread.mutilMigrate.business.BusinessService;
import com.joandora.thread.mutilMigrate.data.DataSourceFactory;
import com.joandora.thread.mutilMigrate.task.MigrateThread;

/**
 * @ClassName: MigrateServiceImpl
 * @Description: 模拟多线程 迁移数据
 * @author: JOANDORA
 * @date: 2016年1月26日 下午10:07:12
 */
public class MigrateService<T> {
	
	/** 业务队列 ，手动注入，全局唯一，只能有一个  */
	private DataSourceFactory<List<T>> dataSourceFactory;
	
	public void setDataSourceFactory(DataSourceFactory<List<T>> dataSourceFactory) {
		this.dataSourceFactory = dataSourceFactory;
	}

	/** 日志处理 */
	Logger logger = LoggerFactory.getLogger(MigrateService.class);

	/** 抽取数据后的处理 service */
	private BusinessService<T> businessService;

	/** 初始化线程数 **/
	private static int THREAD_POOL_INIT = Runtime.getRuntime()
			.availableProcessors() * 20;
	/** 一个线程池初始化任务数 **/
	private static int THREAD_POOL_TASK_NUM = 0;

	public void migreateBatch() throws InterruptedException {

		// 初始化线程工厂，实现线程池名自定义
		final ThreadFactory threadFactory = new ThreadFactoryBuilder()
				.setNameFormat("MigrateMultiThread-%d").setDaemon(true).build();
		// 初始化线程池
		ExecutorService executorService = Executors.newFixedThreadPool(
				THREAD_POOL_INIT, threadFactory);
		// 包装线程池-已取得子线程完成情况
		CompletionService<Integer> completionService = new ExecutorCompletionService<Integer>(executorService);

		int currentLoopIndex = 0;
		while (Boolean.TRUE) {
			List<T> dataList = null;
			// 当没有数据时，线程会阻塞
			dataList = dataSourceFactory.take();
			long queryBeginTime = System.currentTimeMillis();
			THREAD_POOL_TASK_NUM = dataList.size();
			for (int i = 0; i < THREAD_POOL_TASK_NUM; i++) {
				MigrateThread<T> runner = new MigrateThread<T>(dataList.get(i));
				runner.setBusinessService(businessService);
				completionService.submit(runner);
			}
			long queryRuntimeTime = (System.currentTimeMillis() - queryBeginTime);
			logger.info("第%s批线程(查询-装配)完成! spending time = %sms，投保单共：%s条",currentLoopIndex, queryRuntimeTime, THREAD_POOL_TASK_NUM);
			int countNum = 0;
			long insertBeginTime = System.currentTimeMillis();
			for (int i = 0; i < THREAD_POOL_TASK_NUM; i++) {
				try {
					countNum = countNum + completionService.take().get();
				} catch (ExecutionException e) {
					e.printStackTrace();
				}
			}
			long insertRuntimeTime = (System.currentTimeMillis() - insertBeginTime) / 1000;
			BigDecimal runtimeSpeed = new BigDecimal(countNum).divide(new BigDecimal(insertRuntimeTime), 2);
			logger.info("第%s批线程处理完成! spending time = %ss，速度%s条/s，处理记录共：%s条",currentLoopIndex, insertRuntimeTime,runtimeSpeed.toString(), countNum);
		}
		executorService.shutdown();
		executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
		logger.info("|||||||||||||||||||||||||||||||||||||||||||||||||||||||||||");
		logger.info(" [MigrateService] is done! Congratulations！----------------");
		logger.info("|||||||||||||||||||||||||||||||||||||||||||||||||||||||||||");
	}

	public void setBusinessService(BusinessService<T> businessService) {
		this.businessService = businessService;
	}
}
