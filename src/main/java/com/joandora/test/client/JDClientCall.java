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
import java.io.Serializable;

import com.joandora.test.proxy.JDInvocation;

/**
 * <p>
 * A call waiting for a value.
 * </p>
 * 
 * @author zhongqoing
 * @date 2016年4月14日 下午3:53:19
 */
public class JDClientCall {

    private int id; // 调用标示ID

    private Serializable param; // 调用参数

    private JDInvocation value; // 调用返回的值

    private IOException error; // 异常信息

    boolean done; // 调用是否完成

    public JDClientCall(Serializable param) {
	this.param = param;
    }

    /**
     * Indicate when the call is complete and the value or error are available.
     * Notifies by default.
     */
    protected synchronized void callComplete() {
	this.done = true;
	notify(); // 唤醒client等待线程
    }

    /**
     * Set the exception when there is an error. Notify the caller the call is
     * done.
     * 
     * @param error exception thrown by the call; either local or remote
     */
    public synchronized void setException(IOException error) {
	this.error = error;
	callComplete();
    }

    /**
     * Set the return value when there is no error. Notify the caller the call
     * is done.
     * 
     * @param value return value of the call.
     */
    public synchronized void setValue(JDInvocation value) {
	this.value = value;
	callComplete(); // Call.done标志位被置位 即远程调用结果已经准备好 同时调用notify()
			// 通知CClient.call()方法停止等待
    }
    /*****************set-get method********************/
    public int getId() {
        return id;
    }

    
    public void setId(int id) {
        this.id = id;
    }

    
    public Serializable getParam() {
        return param;
    }

    
    public void setParam(Serializable param) {
        this.param = param;
    }

    
    public IOException getError() {
        return error;
    }

    
    public void setError(IOException error) {
        this.error = error;
    }

    
    public boolean isDone() {
        return done;
    }

    
    public void setDone(boolean done) {
        this.done = done;
    }

    
    public JDInvocation getValue() {
        return value;
    }
}
