/**   
 * Copyright © 2016 JOANDORA. All rights reserved.
 * 
 * @Title: MigrateService.java 
 * @Prject: thread
 * @Package: com.joandora.thread.mutilMigrate 
 * @version: V1.0   
 */
package com.joandora.thread.mutilMigrate.business;

/**
 * @ClassName: MigrateService
 * @Description: 模拟数据迁移Service
 * @author: JOANDORA
 * @date: 2016年1月26日 下午10:03:39
 */
public interface BusinessService<T> {
	int runTask(T businessParam);
}
