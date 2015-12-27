/**   
 * Copyright © 2015 JOANDORA. All rights reserved.
 * 
 * @Title: JodoThreadFactory.java 
 * @Prject: thread
 * @Package: com.joandora.thread.factory 
 * @version: V1.0   
 */
package com.joandora.thread.factory;

import java.util.concurrent.ThreadFactory;

/** 
 * @ClassName: JodoThreadFactory 
 * @Description:
 * 线程池Executors.newFixedThreadPool,Executors.newSingleThreadExecutor是由一个默认的ThreadFactory来创建线程的。
 * <br>
 * 这里我们自己重新实现，创建线程的工作交给我们自己的另一个实现JodoThread				
 * @author: JOANDORA
 * @date: 2015年12月26日 下午11:33:18  
 */
public class JodoThreadFactory implements ThreadFactory{
	/** 线程池名称  **/
	private final String poolName;
	
	/** 构造器  **/
	public JodoThreadFactory(String poolName){
		this.poolName = poolName;
	}
	
	@Override
	public Thread newThread(Runnable runnable) {
		return new JodoThread(runnable,poolName);
	}

}
