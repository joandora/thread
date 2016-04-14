/**   
 * Copyright © 2016 JOANDORA. All rights reserved.
 * 
 * @Title: ExtractService.java 
 * @Prject: thread
 * @Package: com.joandora.thread.mutilMigrate.thread 
 * @version: V1.0   
 */
package com.joandora.thread.mutilMigrate.thread;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.joandora.thread.mutilMigrate.data.DataSourceFactory;

/** 
 * @ClassName: ExtractService 
 * @Description: TODO
 * @author: JOANDORA
 * @date: 2016年1月28日 下午8:46:29  
 */
public abstract class ExtractService<T> implements Runnable{
	
	@Override
	public void run() {
		dataSourceFactory.put(extractData());
	}

	Logger logger = LoggerFactory.getLogger(ExtractService.class);
	
	/** 业务队列 ，手动注入，全局唯一，只能有一个  */
	private DataSourceFactory<List<T>> dataSourceFactory;
	
	public void setDataSourceFactory(DataSourceFactory<List<T>> dataSourceFactory) {
		this.dataSourceFactory = dataSourceFactory;
	}
	
	public void initDataSourece(){
		dataSourceFactory.put(extractData());
	}
	
	public abstract List<T> extractData();
	
}
