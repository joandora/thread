package com.joandora.test.client;


import java.io.IOException;
import java.io.Serializable;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.SocketTimeoutException;
import java.util.Hashtable;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.net.SocketFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.joandora.test.exception.RemoteException;
import com.joandora.test.proxy.JDInvocation;
import com.joandora.test.server.ServerAbstract;
import com.joandora.test.util.JDNetUtils;

/** A client for an IPC service. <br> 
 * IPC calls take a single {@link JDInvocation} as a parameter, and return a {@link JDInvocation} as their value.  <br>
 * 
 * @see ServerAbstract
 */
public class JDClient {
	public static final Logger logger = LoggerFactory.getLogger(JDClient.class);
	/**与远程服务器连接的缓存池**/ 
	private Hashtable<JDConnectionId, JDConnection> connections = new Hashtable<JDConnectionId, JDConnection>();  

	/**client run flag**/ 
	private AtomicBoolean running = new AtomicBoolean(true); // if client runs

	private SocketFactory socketFactory; // how to create sockets
	private int refCount = 1;

	
	public static final int PING_CALL_ID = -1;
	
	/**
	 * Increment this client's reference count
	 */
	public synchronized void incCount() {
		refCount++;
	}

	/**
	 * Decrement this client's reference count
	 */
	public synchronized void decCount() {
		refCount--;
	}
  
	/**
	 * Return if this client has no reference
	 * @return true if this client has no reference; false otherwise
	 */
	public synchronized boolean isZeroReference() {
		return refCount == 0;
	}



	/**
	 * Construct an IPC client whose values are of the given
	 * {@link SocketFactory} class.
	 */
	public JDClient(SocketFactory factory) {
		this.socketFactory = factory;
	}

	/**
	 * Construct an IPC client with the default SocketFactory
	 */
	public JDClient() {
		this(JDNetUtils.getDefaultSocketFactory());
	}
 
	/**
	 * Return the socket factory of this client
	 * 
	 * @return this client's socket factory
	 */
	public SocketFactory getSocketFactory() {
		return socketFactory;
	}

	/**
	 * Stop all threads related to this client. No further calls may be made
	 * using this client.
	 */
	public void stop() {
		if (logger.isDebugEnabled()) {
			logger.debug("Stopping client");
		}

		if (!running.compareAndSet(true, false)) {
			return;
		}

		// wake up all connections
		synchronized (connections) {
			for (JDConnection conn : connections.values()) {
				conn.interrupt();//中断线程
			}
		}

		// wait until all connections are closed
		while (!connections.isEmpty()) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
			}
		}
	}
  
	/**
	 * Make a call, passing <code>param</code>, to the IPC server running at
	 * <code>address</code> which is servicing the <code>protocol</code>
	 * protocol, with the <code>ticket</code> credentials,
	 * <code>rpcTimeout</code> as timeout and <code>conf</code> as configuration
	 * for this connection, returning the value. Throws exceptions if there are
	 * network problems or if the remote code threw an exception.
	 */
	public Serializable call(JDInvocation param, InetSocketAddress addr,
			Class<?> protocol, int rpcTimeout) throws InterruptedException,
			IOException {
		JDConnectionId remoteId = JDConnectionId.getConnectionId(addr, protocol,
				rpcTimeout);
		return call(param, remoteId);
	}

	/**
	 * Make a call, passing <code>param</code>, to the IPC server defined by
	 * <code>remoteId</code>, returning the value. Throws exceptions if there
	 * are network problems or if the remote code threw an exception.
	 */
	public JDInvocation call(JDInvocation invoked, JDConnectionId remoteId)
			throws InterruptedException, IOException {
		JDClientCall call = new JDClientCall(invoked);    //将传入的数据封装成call对象 Serializable接口
		//已经向服务器端 RPCHeader ConnectionHeader验证
		JDConnection connection = getConnection(remoteId, call); //获得一个连接  
		connection.sendParam(call); // 向服务端发送Call对象
		boolean interrupted = false;
		synchronized (call) {
			while (!call.isDone()) {
				try {
					call.wait(); //等待结果的返回，在Call类的callComplete()方法里有notify()方法用于唤醒线程  
				} catch (InterruptedException ie) {
					// save the fact that we were interrupted
					interrupted = true;
				}
			}

			if (interrupted) {
				//因中断异常而终止，设置标志interrupted为true 
				Thread.currentThread().interrupt();
			}

			if (call.getError() != null) {
				if (call.getError() instanceof RemoteException) {
					call.getError().fillInStackTrace();
					throw call.getError();
				} else {
					/* local exception use the connection because it will
					 * reflect an ip change, unlike the remoteId
					 */
					throw wrapException(connection.getRemoteAddress(),
							call.getError());
				}
			} else {
				return call.getValue(); //返回结果数据  
			}
		}
	}

	/**
	 * Take an IOException and the address we were trying to connect to and
	 * return an IOException with the input exception as the cause. The new
	 * exception provides the stack trace of the place where the exception is
	 * thrown and some extra diagnostics information. If the exception is
	 * ConnectException or SocketTimeoutException, return a new one of the same
	 * type; Otherwise return an IOException.
	 * 
	 * @param addr
	 *            target address
	 * @param exception
	 *            the relevant exception
	 * @return an exception to throw
	 */
	private IOException wrapException(InetSocketAddress addr,
			IOException exception) {
		if (exception instanceof ConnectException) {
			// connection refused; include the host:port in the error
			return (ConnectException) new ConnectException("Call to " + addr
					+ " failed on connection exception: " + exception)
					.initCause(exception);
		} else if (exception instanceof SocketTimeoutException) {
			return (SocketTimeoutException) new SocketTimeoutException(
					"Call to " + addr + " failed on socket timeout exception: "
							+ exception).initCause(exception);
		} else {
			return (IOException) new IOException("Call to " + addr
					+ " failed on local exception: " + exception)
					.initCause(exception);

		}
	}

	// for unit testing only
	Set<JDConnectionId> getConnectionIds() {
		synchronized (connections) {
			return connections.keySet();
		}
	}

	/**
	 * Get a connection from the pool, or create a new one and add it to the
	 * pool. Connections to a given ConnectionId are reused.
	 */
	private JDConnection getConnection(JDConnectionId remoteId, JDClientCall call)
			throws IOException, InterruptedException {
		if (!running.get()) {
			//如果client关闭了  
			throw new IOException("The client is stopped");
		}
		JDConnection connection;
		//如果connections连接池中有对应的连接对象，就不需重新创建了；如果没有就需重新创建一个连接对象。  
		//但请注意，该连接对象只是存储了remoteId的信息，其实还并没有和服务端建立连接。 
		do {
			synchronized (connections) {
				connection = connections.get(remoteId);
				if (connection == null) {
					connection = new JDConnection(remoteId,socketFactory,running,connections);
					connections.put(remoteId, connection);
				}
			}
		} while (!connection.addCall(call));//将call对象放入对应连接中的calls池

		// we don't invoke the method below inside "synchronized (connections)"
		// block above. The reason for that is if the server happens to be slow,
		// it will take longer to establish a connection and that will slow the
		// entire system down.
		//这句代码才是真正的完成了和服务端建立连接
		connection.setupIOstreams();
		return connection;
	}
}
