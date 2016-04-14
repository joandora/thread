/**
 * Copyright (c) 2006-2015 Hzins Ltd. All Rights Reserved. 
 *  
 * This code is the confidential and proprietary information of   
 * Hzins. You shall not disclose such Confidential Information   
 * and shall use it only in accordance with the terms of the agreements   
 * you entered into with Hzins,http://www.hzins.com.
 *  
 */
package com.joandora.test.client;

import java.io.IOException;
import java.net.InetSocketAddress;

import com.joandora.test.constant.RpcConstant;

/**
 * This class holds the address and the user ticket. The client connections to
 * servers are uniquely identified by <remoteAddress, protocol, ticket>
 */
public class JDConnectionId {

    private InetSocketAddress address; // 连接实例的Socket地址

    private Class<?> protocol; // 连接的协议

    private int rpcTimeout;

    private int maxIdleTime;

    private int maxRetries;

    private boolean tcpNoDelay; 

    private int pingInterval; 
    /**
     * addr:客户端连接地址<br>protocol:代理类<br>rpcTimeout:rpc超时时间<br>
     * maxIdleTime:最大空闲时间<br>maxRetries:最大重试次数<br>tcpNoDelay:tcp延时<br>
     * pingInterval:心跳间隔<br>
     **/
    public JDConnectionId(InetSocketAddress address, Class<?> protocol, int rpcTimeout,
	    int maxIdleTime, int maxRetries, boolean tcpNoDelay, int pingInterval) {
	this.protocol = protocol;
	this.address = address;
	this.rpcTimeout = rpcTimeout;
	this.maxIdleTime = maxIdleTime;
	this.maxRetries = maxRetries;
	this.tcpNoDelay = tcpNoDelay;
	this.pingInterval = pingInterval;
    }
    /**
     * addr:客户端连接地址<br>protocol:代理类<br>rpcTimeout:rpc超时时间<br>
     * maxIdleTime:最大空闲时间<br>maxRetries:最大重试次数<br>tcpNoDelay:tcp延时<br>
     * pingInterval:心跳间隔<br>
     **/
    public static JDConnectionId getConnectionId(InetSocketAddress addr, Class<?> protocol, int rpcTimeout) throws IOException {
	return new JDConnectionId(addr, protocol, rpcTimeout, 10000, 10, false, RpcConstant.DEFAULT_PING_INTERVAL);
    }

    private static boolean isEqual(Object a, Object b) {
	return a == null ? b == null : a.equals(b);
    }

    @Override
    public boolean equals(Object obj) {
	if (obj == this) {
	    return true;
	}
	if (obj instanceof JDConnectionId) {
	    JDConnectionId that = (JDConnectionId) obj;
	    return isEqual(this.address, that.address) && this.maxIdleTime == that.maxIdleTime && this.maxRetries == that.maxRetries && this.pingInterval == that.pingInterval && isEqual(this.protocol, that.protocol) && this.rpcTimeout == that.rpcTimeout && this.tcpNoDelay == that.tcpNoDelay;
	}
	return false;
    }
    
    private static final int PRIME = 16777619;
    @Override
    public int hashCode() {
	int result = 1;
	result = PRIME * result + ((address == null) ? 0 : address.hashCode());
	result = PRIME * result + maxIdleTime;
	result = PRIME * result + maxRetries;
	result = PRIME * result + pingInterval;
	result = PRIME * result + ((protocol == null) ? 0 : protocol.hashCode());
	result = PRIME * rpcTimeout;
	result = PRIME * result + (tcpNoDelay ? 1231 : 1237);
	return result;
    }

    public InetSocketAddress getAddress() {
	return address;
    }

    public Class<?> getProtocol() {
	return protocol;
    }

    public int getRpcTimeout() {
	return rpcTimeout;
    }

    public int getMaxIdleTime() {
	return maxIdleTime;
    }

    public int getMaxRetries() {
	return maxRetries;
    }

    public boolean getTcpNoDelay() {
	return tcpNoDelay;
    }

    public int getPingInterval() {
	return pingInterval;
    }
}
