/**
 * Copyright (c) 2006-2015 Hzins Ltd. All Rights Reserved. 
 *  
 * This code is the confidential and proprietary information of   
 * Hzins. You shall not disclose such Confidential Information   
 * and shall use it only in accordance with the terms of the agreements   
 * you entered into with Hzins,http://www.hzins.com.
 *  
 */
package com.joandora.test.proxy;

import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;

import javax.net.SocketFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.joandora.test.client.JDClientCache;
import com.joandora.test.client.JDClient;
import com.joandora.test.client.JDConnectionId;

/**
 * <p>
 * 动态代理的实现类 实现InvocationHandler接口
 * </p>
 * 
 * @author zhongqoing
 * @date 2016年4月14日 下午2:30:18
 */
public class ProxyInvoker<T> implements InvocationHandler {
    private static final Logger logger = LoggerFactory.getLogger(RPCProxyWrapper.class);
    /**JDClient cache**/
    private static JDClientCache CLIENT_CACHE = new JDClientCache();

    private JDConnectionId connectionId;
    /**proxy object**/
    private Class<T> iface;

    private JDClient jdClient;

    private boolean isClosed = false;

    // iface代表协议
    public ProxyInvoker(final Class<T> iface, InetSocketAddress address, SocketFactory factory, int rpcTimeout) throws IOException {
    	this.iface = iface;
    	this.connectionId = JDConnectionId.getConnectionId(address, iface, rpcTimeout);
    	this.jdClient = CLIENT_CACHE.getClient(factory);
    }

    /**rpc invoke**/
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
    	long startTime = System.currentTimeMillis();
    	// 调用Client call方法
    	InvocationEntity value = jdClient.call(new InvocationEntity(iface, method, args), this.connectionId);
    	long callTime = System.currentTimeMillis() - startTime;
    	logger.info("call server successed,[{}ms],[{}],[{}]",callTime,method.getName(),args);
    	return value.getResult();
    }

    /** close the IPC client that's responsible for this invoker's RPCs **/
    public synchronized void close() {
    	if (!isClosed) {
    		isClosed = true;
    		CLIENT_CACHE.stopClient(jdClient);
    	}
    }
}
