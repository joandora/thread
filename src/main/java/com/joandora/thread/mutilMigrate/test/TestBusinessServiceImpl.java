/**   
 * Copyright © 2016 JOANDORA. All rights reserved.
 * 
 * @Title: TestBusinessServiceImpl.java 
 * @Prject: thread
 * @Package: com.joandora.thread.mutilMigrate.business.impl 
 * @version: V1.0   
 */
package com.joandora.thread.mutilMigrate.test;

import com.joandora.thread.mutilMigrate.business.BusinessService;

/** 
 * @ClassName: TestBusinessServiceImpl 
 * @Description: 
 * @author: JOANDORA
 * @date: 2016年1月27日 下午9:03:10  
 */
public class TestBusinessServiceImpl<T> implements BusinessService<T>{

	/* (non Javadoc) 
	 * @Title: runTask
	 * @Description: TODO
	 * @param businessParam
	 * @return 
	 * @see com.joandora.thread.mutilMigrate.business.BusinessService#runTask(java.lang.Object) 
	 */
	@Override
	public int runTask(T businessParam) {
		try {
			System.out.println(businessParam);
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return 0;
	}

}
