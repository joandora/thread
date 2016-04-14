/**   
 * Copyright © 2015 JOANDORA. All rights reserved.
 */  
package com.joandora.nio.mycat.client.buffer; 

/**
 * <p>
 * threadlocal缓冲对象<br>
 * 变量为BufferQueue
 * </p>
 * @author	JOANDORA
 * @date	2016年4月11日 下午4:51:38
 */
public class ThreadLocalBufferPool extends ThreadLocal<BufferQueue> {
	private final long size;

	public ThreadLocalBufferPool(long size) {
		this.size = size;
	}

	protected synchronized BufferQueue initialValue() {
		return new BufferQueue(size);
	}
}
 