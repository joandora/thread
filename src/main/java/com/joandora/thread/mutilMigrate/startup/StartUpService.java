/**   
 * Copyright © 2016 JOANDORA. All rights reserved.
 * 
 * @Title: StartUpService.java 
 * @Prject: thread
 * @Package: com.joandora.thread.mutilMigrate.business 
 * @version: V1.0   
 */
package com.joandora.thread.mutilMigrate.startup;

import java.util.List;

import com.joandora.thread.mutilMigrate.business.BusinessService;
import com.joandora.thread.mutilMigrate.data.DataSourceFactory;
import com.joandora.thread.mutilMigrate.test.ExtractServiceImpl;
import com.joandora.thread.mutilMigrate.test.TestBusinessServiceImpl;
import com.joandora.thread.mutilMigrate.thread.ExtractService;
import com.joandora.thread.mutilMigrate.thread.MigrateService;

/**
 * @ClassName: StartUpService
 * @Description: TODO
 * @author: JOANDORA
 * @date: 2016年1月27日 下午9:05:02
 */
public class StartUpService<T> {
	
	public void assemble(ExtractService<T> extractService,BusinessService<T> businessService) throws InterruptedException{
		MigrateService<T> migrateService = new MigrateService<T>();
		DataSourceFactory<List<T>> dataSourceFactory = new DataSourceFactory<List<T>>();
		
		
		extractService.setDataSourceFactory(dataSourceFactory);
		migrateService.setDataSourceFactory(dataSourceFactory);
		migrateService.setBusinessService(businessService);
		
		new Thread(extractService).start();
		
		
		migrateService.migreateBatch();
	}
	
	
	public static void main(String[] args) throws InterruptedException {
		TestBusinessServiceImpl businessService = new TestBusinessServiceImpl();
		ExtractService<Integer> extractService = new ExtractServiceImpl();
		StartUpService startUpService = new StartUpService();
		startUpService.assemble(extractService,businessService);
	
		
		
		
	}
}
