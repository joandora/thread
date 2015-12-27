/**   
 * Copyright © 2015 JOANDORA. All rights reserved.
 * 
 * @Title: JodoThreadFactoryTest.java 
 * @Prject: thread
 * @Package: com.joandora.thread.factory 
 * @version: V1.0   
 */
package com.joandora.thread.factory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * @ClassName: JodoThreadFactoryTest
 * @Description: 测试uncaughtException
 * @author: JOANDORA
 * @date: 2015年12月27日 上午12:16:37
 */
public class JodoThreadFactoryTest {
	public static void main(String args[]) throws InterruptedException {
		JodoThreadFactory jodoThreadFactory = new JodoThreadFactory("JOANDORA");
		ExecutorService es = Executors.newFixedThreadPool(1,jodoThreadFactory);
		// 这里要用execute，否则无法uncaughtException
		es.execute(new Runnable() {
			@Override
			public void run() {
				System.out.println(Integer.parseInt("123"));
				System.out.println(Integer.parseInt("XYZ")); // This will cause NumberFormatException
				System.out.println(Integer.parseInt("456"));
			}
		});
		es.shutdown();
		es.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
	}
}
