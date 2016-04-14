/**   
 * Copyright © 2015 JOANDORA. All rights reserved.
 */    
package com.joandora.test.client; 

import java.util.HashMap;
import java.util.Map;

import javax.net.SocketFactory;

/**
 * <p>
 * Cache a client using its socket factory as the key
 * </p>
 * @author	JOANDORA
 * @date	2016年4月14日 下午2:32:10
 */
public class JDClientCache{
	private Map<SocketFactory, JDClient> clients = new HashMap<SocketFactory, JDClient>();

	/**
	 * get a client from cache<br>
	 * <i>new JDClient(factory)</i> if no cached client exists.<br>
	 */
	public synchronized JDClient getClient(SocketFactory factory) {
		JDClient client = clients.get(factory);
		if (client == null) {
			//创建客户端
			client = new JDClient(factory);
			clients.put(factory, client);
		} else {
			//引用数增加1
			client.incCount();
		}
		return client;
	}

	/**
	 * Stop a client connection<br>
	 * A client is closed only when its reference count becomes zero.
	 */
	public void stopClient(JDClient client) {
		synchronized (this) {
			client.decCount();
			if (client.isZeroReference()) {
				clients.remove(client.getSocketFactory());
			}
		}
		if (client.isZeroReference()) {
			client.stop();
		}
	}
}
 