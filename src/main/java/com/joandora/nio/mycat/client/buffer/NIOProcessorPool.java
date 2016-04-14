/**   
 * Copyright © 2015 JOANDORA. All rights reserved.
 */  
package com.joandora.nio.mycat.client.buffer;

import java.io.IOException;

import com.joandora.nio.mycat.client.conf.ConfigureConstant;

/**
 * <p>
 * 连接、缓冲管理器池
 * </p>
 * @author JOANDORA
 * @date 2016年4月12日 上午9:10:34
 */
public class NIOProcessorPool {
    /**服务器CPU核数**/
    private static int defaultProcessorNum = ConfigureConstant.defaultProcessorNum;
    /**processors数组，大小为服务器CPU核数**/
    private static NIOProcessor[] processors = new NIOProcessor[defaultProcessorNum];
    /**记录上一次获取NIOProcessor的指针**/
    private volatile int nextProcessor;
    
    /**单例模式**/
    private static NIOProcessorPool INSTANCE = new NIOProcessorPool();
    private NIOProcessorPool(){}
    public static final NIOProcessorPool getInstance() {
	return INSTANCE;
    }
    
    static {
	 /**一个缓冲区大小(字节)**/
  	int defaultBufferChunkSize = ConfigureConstant.defaultBufferChunkSize;
  	/**一个缓冲区大小(字节)**/
  	int threadLocalPercent = ConfigureConstant.threadLocalPercent;
  	int processorExecutor = (defaultProcessorNum != 1) ? defaultProcessorNum * 2 : 4;
  	BufferPool bufferPool = new BufferPool(defaultBufferChunkSize*defaultProcessorNum*1000, defaultBufferChunkSize, threadLocalPercent / defaultProcessorNum);
  	NameableExecutor businessExecutor = ExecutorUtil.create("BusinessExecutor", processorExecutor);
  	for (int i = 0; i < processors.length; i++) {
  	    try {
		processors[i] = new NIOProcessor("Processor" + i, bufferPool, businessExecutor);
	    } catch (IOException e) {
		e.printStackTrace();
	    }
  	}
      }
    /**
     * <p>
     * 获取一个NIOProcessor
     * </p>
     */
    public NIOProcessor getNextProcessor() {
	int i = ++nextProcessor;
	if (i >= defaultProcessorNum) {
	    i = nextProcessor = 0;
	}
	return processors[i];
    }
}
