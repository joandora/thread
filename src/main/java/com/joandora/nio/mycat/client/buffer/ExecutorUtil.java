/**   
 * Copyright © 2015 JOANDORA. All rights reserved.
 */
package com.joandora.nio.mycat.client.buffer; 

import java.util.concurrent.LinkedTransferQueue;

/**
 * <p>
 * 可自定义名称的线程池-包装类
 * </p>
 * @author	JOANDORA
 * @date	2016年4月11日 下午5:42:05
 */
public class ExecutorUtil {

    public static final NameableExecutor create(String name, int size) {
        return create(name, size, true);
    }

    private static final NameableExecutor create(String name, int size, boolean isDaemon) {
        NameableThreadFactory factory = new NameableThreadFactory(name, isDaemon);
        return new NameableExecutor(name, size, new LinkedTransferQueue<Runnable>(), factory);
    }
}
 