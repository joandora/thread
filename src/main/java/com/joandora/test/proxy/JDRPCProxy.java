/**   
 * Copyright © 2015 JOANDORA. All rights reserved.
 */  
package com.joandora.test.proxy;

import java.io.IOException;
import java.lang.reflect.Proxy;
import java.net.InetSocketAddress;

import javax.net.SocketFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.joandora.test.server.JDServer;
import com.joandora.test.util.JDNetUtils;

/**
 * A simple RPC mechanism.
 * 
 * A <i>protocol</i> is a Java interface. All parameters and return types must
 * be one of:
 * 
 * <ul>
 * <li>a primitive type, <code>boolean</code>, <code>byte</code>,
 * <code>char</code>, <code>short</code>, <code>int</code>, <code>long</code>,
 * <code>float</code>, <code>double</code>, or <code>void</code>;</li>
 * <li>a {@link String};</li>
 * <li>an array of the above types</li>
 * </ul>
 */
/* RPC是对Client和Server的包装 */
public class JDRPCProxy {

    private static final Logger logger = LoggerFactory.getLogger(JDRPCProxy.class);

    /** singleton */
    private JDRPCProxy() {
    }

    /**
     * Construct a client-side proxy object that implements the named protocol,
     * talking to a server at the named address.
     */
    @SuppressWarnings("unchecked")
    public static <T> T getProxy(final Class<T> proxyObject, InetSocketAddress addr, SocketFactory factory, int rpcTimeout) throws IOException {
	Preconditions.checkNotNull(proxyObject);
	// 生成动态代理对象 Invoker代理调用类
	return (T) Proxy.newProxyInstance(proxyObject.getClassLoader(), new Class[] { proxyObject }, new ProxyInvoker<T>(proxyObject, addr, factory, rpcTimeout));
    }

    /**
     * Construct a client-side proxy object with the default SocketFactory
     * <ul>
     * <li>default rpc timeout is 0</li>
     * </ul>
     * 
     * @param proxyObject client-side proxy Object
     * @param addr socket address
     * @return a proxy instance
     * @throws IOException
     */
    public static <T> T getProxy(final Class<T> proxyObject, InetSocketAddress addr) throws IOException {
	return getProxy(proxyObject, addr, JDNetUtils.getDefaultSocketFactory(), 0);
    }

    /**
     * Stop this proxy and release its invoker's resource
     */
    public static void stopProxy(Object proxy) {
	if (proxy != null) {
	    // 返回代理实例的调用处理程序
	    ((ProxyInvoker<?>) Proxy.getInvocationHandler(proxy)).close();
	    logger.debug("stop Proxy!");
	}
    }

    /**
     * Construct a server for a protocol implementation instance listening on a
     * port and address.
     */
    public static JDServer getServer(final String bindAddress, final int port) throws IOException {
	return getServer(bindAddress, port, 1, false);
    }

    /**
     * Construct a server for a protocol implementation instance listening on a
     * port and address, with a secret manager.
     */
    public static JDServer getServer(final String bindAddress, final int port, final int numHandlers, final boolean verbose) throws IOException {
	return new JDServer(bindAddress, port, numHandlers, verbose);
    }
}
