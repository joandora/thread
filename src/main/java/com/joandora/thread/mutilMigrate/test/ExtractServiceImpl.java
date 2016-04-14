/**   
 * Copyright © 2016 JOANDORA. All rights reserved.
 * 
 * @Title: ExtractServiceImpl.java 
 * @Prject: thread
 * @Package: com.joandora.thread.mutilMigrate.test 
 * @version: V1.0   
 */
package com.joandora.thread.mutilMigrate.test;

import java.util.ArrayList;
import java.util.List;

import com.joandora.thread.mutilMigrate.thread.ExtractService;

/** 
 * @ClassName: ExtractServiceImpl 
 * @Description: TODO
 * @author: JOANDORA
 * @date: 2016年1月28日 下午9:08:49  
 */
public class ExtractServiceImpl extends ExtractService<Integer>{

	/* (non Javadoc) 
	 * @Title: extractData
	 * @Description: TODO
	 * @return 
	 * @see com.joandora.thread.mutilMigrate.thread.ExtractService#extractData() 
	 */
	@Override
	public List<Integer> extractData() {
		List<Integer> tsetList = new ArrayList<Integer>();
		tsetList.add(1);
		tsetList.add(2);
		tsetList.add(3);
		tsetList.add(4);
		tsetList.add(5);
		return tsetList;
	}

}
