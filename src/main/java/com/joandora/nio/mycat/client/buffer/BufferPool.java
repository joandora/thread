/**   
 * Copyright © 2015 JOANDORA. All rights reserved.
 */ 
package com.joandora.nio.mycat.client.buffer; 

import java.nio.ByteBuffer;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>
 * 缓冲区池
 * </p>
 * @author	JOANDORA
 * @date	2016年4月11日 下午4:50:38
 */
public class BufferPool {
    	private static final Logger LOGGER = LoggerFactory.getLogger(BufferPool.class);
    
    	/**系统自启动线程标识。在调用缓冲区回收时，会检查该标识**/
 	public static final String LOCAL_BUF_THREAD_PREX = "$_";
 	/**threadlocal缓冲对象**/
 	private  final ThreadLocalBufferPool localBufferPool;
 	/**一个缓冲区大小**/
 	private final int chunkSize;
 	/**保存所有创建的缓冲块**/
 	private final ConcurrentLinkedQueue<ByteBuffer> items = new ConcurrentLinkedQueue<ByteBuffer>();
 	/**缓冲池容量**/
 	private final long capactiy;
 	private long sharedOptsCount;
         private AtomicInteger newCreated = new AtomicInteger(0);
 	private final long threadLocalCount;
 	private long totalBytes = 0;
 	private long totalCounts = 0;
 	
 	/***
 	 * bufferSize:defaultBufferChunkSize*defaultProcessorNum*1000<br>
 	 * 	所有缓冲块大小合计<br>
 	 * chunkSize: defaultBufferChunkSize<br>
 	 * 	单个缓冲块大小<br>
 	 * threadLocalPercent: 本地线程占用缓存百分比<br>
 	 * 	ThreadLocalBufferPool内部保存BufferQueue对象，其大小就等于threadLocalPercent * capactiy / 100;
 	 */
 	public BufferPool(long bufferSize, int chunkSize, int threadLocalPercent) {
 		this.chunkSize = chunkSize;
 		long size = bufferSize / chunkSize;
 		size = (bufferSize % chunkSize == 0) ? size : size + 1;
 		this.capactiy = size;
 		this.threadLocalCount = threadLocalPercent * capactiy / 100;
 		for (long i = 0; i < capactiy; i++) {
 		   this.items.offer(createDirectBuffer(chunkSize));
 		}
 		this.localBufferPool = new ThreadLocalBufferPool(this.threadLocalCount);
 	}
 	
 	/**
 	 * 分配缓冲块<br>
 	 * 如果是本地线程(名称以$_开头)，则从ThreadLocal取出创建的缓冲块<br>
 	 * 如果不是本地线程(名称以$_开头)或本地线程无缓冲区，则从BufferPool取出预先创建的缓冲块<br>
 	 * 如果缓冲仍未空，则临时虎藏剑ByteBuffer.allocateDirect(size)
 	 */
 	public ByteBuffer allocate() {
 		ByteBuffer node = null;
 		if (isLocalCacheThread()) {
 			// allocate from threadlocal
 			node = localBufferPool.get().poll();
 			if (node != null) {
 				return node;
 			}
 		}
 		node = items.poll();
 		if (node == null) {
 			//newCreated++;
 			newCreated.incrementAndGet();
 			node = this.createDirectBuffer(chunkSize);
 		}
 		return node;
 	}
 	/**
 	 * 1、如果给定缓冲大小小于等于系统预设的缓冲区大小,则调用allocate()方法<br>
 	 * 2、如果大于则调用createTempBuffer()方法<br>
 	 */
 	public ByteBuffer allocate(int size) {
 		if (size <= this.chunkSize) {
 			return allocate();
 		} else {
 			LOGGER.warn("allocate buffer size large than default chunksize:{} he want {}",this.chunkSize,size);
 			return createTempBuffer(size);
 		}
 	}
 	/**
 	 * 检查当前线程名称是否已$_开头
 	 */
 	private static final boolean isLocalCacheThread() {
 		final String thname = Thread.currentThread().getName();
 		return (thname.length() < LOCAL_BUF_THREAD_PREX.length()) ? false
 				: (thname.charAt(0) == '$' && thname.charAt(1) == '_');

 	}
 	/**
 	 * 1、回收缓冲块<br>2、拒绝回收null、非direct buffer、容量大于chunkSize的缓存(清空缓存块)<br>
 	 * 3、如果是本地线程，则判断ThreadLocal中缓冲队列实时大小是否与当初设置时的要小。<br>
 	 * 	如果小，就将该缓冲块放入ThreadLocal中缓冲队列<br>
 	 * 	如果大，则将ThreadLocal中缓冲队列的3/4替换到BufferPool的缓冲队列<br>
 	 * 4、如果不是本地线程，放入BufferPool缓冲队列<br>
 	 */
 	public void recycle(ByteBuffer buffer) {
 		if (!checkValidBuffer(buffer)) {
 			return;
 		}
 		if (isLocalCacheThread()) {
 			BufferQueue localQueue = localBufferPool.get();
 			if (localQueue.snapshotSize() < threadLocalCount) {
 				localQueue.put(buffer);
 			} else {
 				// recyle 3/4 thread local buffer
 				items.addAll(localQueue.removeItems(threadLocalCount * 3 / 4));
 				items.offer(buffer);
 				sharedOptsCount++;
 			}
 		} else {
 			sharedOptsCount++;
 			items.offer(buffer);
 		}

 	}
 	/**
 	 * 拒绝回收null、非direct buffer、容量大于chunkSize的缓存
 	 */
 	private boolean checkValidBuffer(ByteBuffer buffer) {
		if (buffer == null || !buffer.isDirect()) {
			return false;
		} else if (buffer.capacity() > chunkSize) {
			LOGGER.warn("cant' recycle  a buffer large than my pool chunksize {}",buffer.capacity());
			return false;
		}
		totalCounts++;
		totalBytes += buffer.limit();
		buffer.clear();
		return true;
	}
 	/**
 	 * 返回平均的buffer大小
 	 */
 	public int getAvgBufSize() {
 		if (this.totalBytes < 0) {
 		   this.totalBytes = 0;
 			this.totalCounts = 0;
 			return 0;
 		} else {
 			return (int) (this.totalBytes / this.totalCounts);
 		}
 	}

 	/**
 	 * 从系统(非jvm)分配一个新的byte buffer. 
 	 */
 	private ByteBuffer createDirectBuffer(int size) {
 		return ByteBuffer.allocateDirect(size);
 	}
 	/**
 	 * 从jvm分配一个新的byte buffer. 
 	 */
 	private ByteBuffer createTempBuffer(int size) {
		return ByteBuffer.allocate(size);
	}
 	
 	public int getChunkSize() {
		return chunkSize;
	}

	public long getSharedOptsCount() {
		return sharedOptsCount;
	}

	public long size() {
		return this.items.size();
	}

	public long capacity() {
		return capactiy + newCreated.get();
	}
}
 