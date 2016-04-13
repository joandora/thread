/**   
 * Copyright © 2015 JOANDORA. All rights reserved.
 */   
package com.joandora.nio.mycat.client.core; 

import java.io.IOException;

/***
 * <p>
 * NIOReactor pool
 * </p>
 */
public class NIOReactorPool {

    private final NIOReactor[] reactors;

    private volatile int nextReactor;
    /***
     * <p>
     * 初始化池<br>
     * 池大小为服务器CPU核数<br>
     * 初始化后立即启动所有 NIOReactor(线程)<br>
     * </p>
     */
    public NIOReactorPool(String name, int poolSize) throws IOException {
	reactors = new NIOReactor[poolSize];
	for (int i = 0; i < poolSize; i++) {
	    NIOReactor reactor = new NIOReactor(name + "-" + i);
	    reactors[i] = reactor;
	    reactor.start();
	}
    }
    /**
     * <p>
     * 获取一个 NIOReactor
     * </p>
     */
    public NIOReactor getNextReactor() {
	int i = ++nextReactor;
	if (i >= reactors.length) {
	    i = nextReactor = 0;
	}
	return reactors[i];
    }
}
 