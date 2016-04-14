/**
 * Copyright (c) 2006-2015 Hzins Ltd. All Rights Reserved. 
 *  
 * This code is the confidential and proprietary information of   
 * Hzins. You shall not disclose such Confidential Information   
 * and shall use it only in accordance with the terms of the agreements   
 * you entered into with Hzins,http://www.hzins.com.
 *  
 */   
package com.joandora.test.server; 

import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.joandora.test.proxy.JDInvocation;

/** An RPC Server. */
public class JDServer extends ServerAbstract {
    	private static Logger logger = LoggerFactory.getLogger(JDServer.class);
	private boolean verbose;

	/**
	 * Construct an RPC server.
	 * 
	 * @param bindAddress
	 *            the address to bind on to listen for connection
	 * @param port
	 *            the port to listen for connections on
	 * @param numHandlers
	 *            the number of method handler threads to run
	 * @param verbose
	 *            whether each call should be logged
	 */
	public JDServer(String bindAddress, int port,
			int numHandlers, boolean verbose) throws IOException {
		super(bindAddress, port, numHandlers);
		this.verbose = verbose;
	}
//重写Sever类的call方法
	public Serializable call(Class<?> iface, Serializable param,
			long receivedTime) throws IOException {
		try {
			JDInvocation call = (JDInvocation) param; //调用参数 Invocationd对象包含方法名称 形式参数列表和实际参数列表
			if (verbose)
			    logger.info("Call: " + call);
			//从实例缓存中按照接口寻找实例对象
			Object instance = INSTANCE_CACHE.get(iface);
			if (instance == null)
				throw new IOException("interface `" + iface	+ "` not inscribe.");
			//通过Class对象获取Method对象
			Method method = iface.getMethod(call.getMethodName(),
					call.getParameterClasses());
			//取消Java语言访问权限检查
			method.setAccessible(true);

			long startTime = System.currentTimeMillis();
			//调用Method对象的invoke方法
			Object value = method.invoke(instance, call.getParameters());
			int processingTime = (int) (System.currentTimeMillis() - startTime);
			int qTime = (int) (startTime - receivedTime);
			if (LOG.isDebugEnabled()) {
				LOG.debug("Served: " + call.getMethodName()
						+ " queueTime= " + qTime + " procesingTime= "
						+ processingTime);
			}
			if (verbose)
			    logger.info("Return: " + value);

			call.setResult(value); //向Invocation对象设置结果
			return call;
		} catch (InvocationTargetException e) {
			Throwable target = e.getTargetException();
			if (target instanceof IOException) {
				throw (IOException) target;
			} else {
				IOException ioe = new IOException(target.toString());
				ioe.setStackTrace(target.getStackTrace());
				throw ioe;
			}
		} catch (Throwable e) {
			if (!(e instanceof IOException)) {
				LOG.error("Unexpected throwable object ", e);
			}
			IOException ioe = new IOException(e.toString());
			ioe.setStackTrace(e.getStackTrace());
			throw ioe;
		}
	}
}
 