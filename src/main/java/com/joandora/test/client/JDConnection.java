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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import javax.net.SocketFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.joandora.test.constant.Status;
import com.joandora.test.data.ConnectionHeader;
import com.joandora.test.data.DataOutputBuffer;
import com.joandora.test.exception.RemoteException;
import com.joandora.test.proxy.JDInvocation;
import com.joandora.test.server.ServerAbstract;
import com.joandora.test.util.JDNetUtils;
import com.joandora.test.util.WritableUtils;

/**
 * Thread that reads responses and notifies callers. Each connection owns a
 * socket connected to a remote address. Calls are multiplexed through this
 * socket: responses may be delivered out of order.
 */
public 	class JDConnection extends Thread {
    	private static Logger logger = LoggerFactory.getLogger(JDConnection.class);
	private InetSocketAddress server; // server ip:port      服务端ip:port  
	private ConnectionHeader header; // connection header    连接头信息，该实体类封装了连接协议与用户信息UserGroupInformation
	private final JDConnectionId remoteId; // connection id    连接ID 

	private Socket socket = null; // connected socket        客户端已连接的Socket 
	private DataInputStream in;
	private DataOutputStream out;

	private int rpcTimeout;
	/** connections will be culled if it was idle for maxIdleTime msecs */
	private int maxIdleTime;
	// the max. no. of retries for socket connections
	private int maxRetries;
	private boolean tcpNoDelay; // if T then disable Nagle's Algorithm
	private int pingInterval; // how often sends ping to the server in msecs

	// currently active calls
	private Hashtable<Integer, JDClientCall> calls = new Hashtable<Integer, JDClientCall>(); //待处理的RPC队列 
	/** last I/O activity time */
	private AtomicLong lastActivity = new AtomicLong();                      //最后I/O活跃的时间  
	/** indicate if the connection is closed */
	private AtomicBoolean shouldCloseConnection = new AtomicBoolean();       //连接是否关闭  
	private IOException closeException; // close reason                        连接关闭原因 
	private SocketFactory socketFactory; // how to create sockets
	/**client run flag**/ 
	private AtomicBoolean running = new AtomicBoolean(true); // if client runs
	/**与远程服务器连接的缓存池**/ 
	private Hashtable<JDConnectionId, JDConnection> connections = new Hashtable<JDConnectionId, JDConnection>();  
	
	final static int PING_CALL_ID = -1;
	
	public JDConnection(JDConnectionId remoteId,SocketFactory socketFactory,AtomicBoolean running,Hashtable<JDConnectionId, JDConnection> connections) throws IOException {
		this.remoteId = remoteId;
		this.server = remoteId.getAddress();
		if (server.isUnresolved()) {
			throw new UnknownHostException("unknown host: "
					+ remoteId.getAddress().getHostName());
		}
		this.maxIdleTime = remoteId.getMaxIdleTime();
		this.maxRetries = remoteId.getMaxRetries();
		this.tcpNoDelay = remoteId.getTcpNoDelay();
		this.pingInterval = remoteId.getPingInterval();
		if (logger.isDebugEnabled()) {
			logger.debug("The ping interval is " + this.pingInterval + " ms.");
		}
		this.rpcTimeout = remoteId.getRpcTimeout();
		Class<?> protocol = remoteId.getProtocol();
		header = new ConnectionHeader(protocol == null ? null
				: protocol.getName());     //连接头

		this.setName("IPC Client (" + socketFactory.hashCode()
				+ ") connection to " + remoteId.getAddress().toString()
				+ " from an unknown user");
		this.setDaemon(true);
		this.socketFactory=socketFactory;
		this.running=running;
		this.connections=connections;
	}

	/** Update lastActivity with the current time. */
	//更新最近活动时间
	private void touch() {
		lastActivity.set(System.currentTimeMillis());
	}

	/**
	 * Add a call to this connection's call queue and notify a listener;
	 * synchronized. Returns false if called during shutdown.
	 * @param call to add
	 * @return true if the call was added.
	 */
	synchronized boolean addCall(JDClientCall call) {
		if (shouldCloseConnection.get())
			return false;
		calls.put(call.getId(), call);
		notify();
		return true;
	}

	/**
	 * This class sends a ping to the remote side when timeout on reading.
	 * If no failure is detected, it retries until at least a byte is read.
	 */
	//在SocketInputStream之上添加心跳检查能力
	private class PingInputStream extends FilterInputStream {
		/* constructor */
		protected PingInputStream(InputStream in) {
			super(in);
		}

		/*
		 * Process timeout exception if the connection is not going to be
		 * closed or is not configured to have a RPC timeout, send a ping.
		 * (if rpcTimeout is not set to be 0, then RPC should timeout.
		 * otherwise, throw the timeout exception.
		 */
		//处理超时 如果客户端处于运行状态 调用sendPing()
		private void handleTimeout(SocketTimeoutException e)
				throws IOException {
			if (shouldCloseConnection.get() || !running.get()
					|| rpcTimeout > 0) {
				throw e;
			} else {
				if (logger.isDebugEnabled())
					logger.debug("handle timeout, then send ping...");
				sendPing();
			}
		}

		/**
		 * Read a byte from the stream. Send a ping if timeout on read.
		 * Retries if no failure is detected until a byte is read.
		 * 
		 * @throws IOException
		 *             for any IO problem other than socket timeout
		 */
		public int read() throws IOException {
			do {
				try {
					return super.read();//读取一个字节
				} catch (SocketTimeoutException e) {
					handleTimeout(e);
				}
			} while (true);
		}

		/**
		 * Read bytes into a buffer starting from offset <code>off</code>
		 * Send a ping if timeout on read. Retries if no failure is detected
		 * until a byte is read.
		 * 
		 * @return the total number of bytes read; -1 if the connection is
		 *         closed.
		 */
		public int read(byte[] buf, int off, int len) throws IOException {
			do {
				try {
					return super.read(buf, off, len);
				} catch (SocketTimeoutException e) {
					handleTimeout(e);
				}
			} while (true);
		}
	}

	/**
	 * Update the server address if the address corresponding to the host
	 * name has changed.
	 * 
	 * @return true if an addr change was detected.
	 * @throws IOException
	 *             when the hostname cannot be resolved.
	 */
	private synchronized boolean updateAddress() throws IOException {
		// Do a fresh lookup with the old host name.
		InetSocketAddress currentAddr = JDNetUtils.makeSocketAddr(
				server.getHostName(), server.getPort());

		if (!server.equals(currentAddr)) {
			logger.warn("Address change detected. Old: " + server.toString()
					+ " New: " + currentAddr.toString());
			server = currentAddr;
			return true;
		}
		return false;
	}

	private synchronized void setupConnection() throws IOException {
		short ioFailures = 0;
		short timeoutFailures = 0;
		while (true) {
			try {
				this.socket = socketFactory.createSocket();//创建Socket
				this.socket.setTcpNoDelay(tcpNoDelay);

				//设置连接超时为20s 
				JDNetUtils.connect(this.socket, server, 20000);
				if (rpcTimeout > 0) {
					/** rpcTimeout overwrites pingInterval */
					pingInterval = rpcTimeout;
				}

				this.socket.setSoTimeout(pingInterval);
				return;
			} catch (SocketTimeoutException toe) {
				/*
				 * Check for an address change and update the local
				 * reference. Reset the failure counter if the address was
				 * changed
				 */
				if (updateAddress()) {
					timeoutFailures = ioFailures = 0;
				}
				 /* 设置最多连接重试为45次。 
		         * 总共有20s*45 = 15 分钟的重试时间。 
		         */  
				handleConnectionFailure(timeoutFailures++, 45, toe);
			} catch (IOException ie) {
				if (updateAddress()) {
					timeoutFailures = ioFailures = 0;
				}
				handleConnectionFailure(ioFailures++, maxRetries, ie);
			}
		}
	}

	/**
	 * Connect to the server and set up the I/O streams. It then sends a
	 * header to the server and starts the connection thread that waits for
	 * responses.
	 */
	synchronized void setupIOstreams() throws InterruptedException {
		if (socket != null || shouldCloseConnection.get()) {
			return;
		}

		try {
			if (logger.isDebugEnabled()) {
				logger.debug("Connecting to " + server);
			}
			while (true) {
				setupConnection();                           //建立连接  
				InputStream inStream = JDNetUtils.getInputStream(socket);//SocketInputStream 有超时时间
				OutputStream outStream = JDNetUtils.getOutputStream(socket);
				writeRpcHeader(outStream);//IPC连接魔数（hrpc）协议版本号
				// fall back to simple auth because server told us so.
				header = new ConnectionHeader(header.getProtocol());//连接头
				this.in = new DataInputStream(new BufferedInputStream(
						new PingInputStream(inStream)));//将输入流装饰成DataInputStream  
				this.out = new DataOutputStream(new BufferedOutputStream(
						outStream));                    //将输出流装饰成DataOutputStream
				writeHeader();//发送ConnectionHeader

				//更新活动时间 
				touch();

				//当连接建立时，启动接受线程等待服务端传回数据，注意：Connection继承了Tread  
				start();
				return;
			}
		} catch (IOException e) {
			markClosed(e);
			close();
		}
	}

	private void closeConnection() {
		// close the current connection
		try {
			socket.close();
		} catch (IOException e) {
			logger.warn("Not able to close a socket", e);
		}
		// set socket to null so that the next call to setupIOstreams
		// can start the process of connect all over again.
		socket = null;
	}

	/*
	 * Handle connection failures
	 * 
	 * If the current number of retries is equal to the max number of
	 * retries, stop retrying and throw the exception; Otherwise backoff 1
	 * second and try connecting again.
	 * 
	 * This Method is only called from inside setupIOstreams(), which is
	 * synchronized. Hence the sleep is synchronized; the locks will be
	 * retained.
	 * 
	 * @param curRetries current number of retries
	 * 
	 * @param maxRetries max number of retries allowed
	 * 
	 * @param ioe failure reason
	 * 
	 * @throws IOException if max number of retries is reached
	 */
	private void handleConnectionFailure(int curRetries, int maxRetries,
			IOException ioe) throws IOException {
		closeConnection();

		// throw the exception if the maximum number of retries is reached
		if (curRetries >= maxRetries) {
			throw ioe;
		}

		// otherwise back off and retry
		try {
			Thread.sleep(1000);
		} catch (InterruptedException ignored) {
		}

		logger.info("Retrying connect to server: " + server
				+ ". Already tried " + curRetries + " time(s).");
	}

	/* Write the RPC header */
	//写RPC头
	private void writeRpcHeader(OutputStream outStream) throws IOException {
		DataOutputStream out = new DataOutputStream(
				new BufferedOutputStream(outStream));
		// Write out the header, version and authentication method
		out.write(ServerAbstract.HEADER.array());
		out.write(ServerAbstract.CURRENT_VERSION);
		out.flush();
	}

	/*
	 * Write the protocol header for each connection Out is not synchronized
	 * because only the first thread does this.
	 */
	private void writeHeader() throws IOException {
		// Write out the ConnectionHeader
//		ByteArrayOutputStream 
		DataOutputBuffer buf = new DataOutputBuffer();
		header.write(buf);

		// Write out the payload length
		int bufLen = buf.getLength();
		out.writeInt(bufLen);//头长度
		out.write(buf.getData(), 0, bufLen);
	}

	/*
	 * wait till someone signals us to start reading RPC response or it is
	 * idle too long, it is marked as to be closed, or the client is marked
	 * as not running.
	 * 
	 * Return true if it is time to read a response; false otherwise.
	 */
	//返回false 会导致IPC连接关闭
	private synchronized boolean waitForWork() {
		//1 目前没有正在处理的远程调用 2 连接不需要关闭 3 客户端害处于运行状态
		if (calls.isEmpty() && !shouldCloseConnection.get()
				&& running.get()) {
			long timeout = maxIdleTime
					- (System.currentTimeMillis() - lastActivity.get());
			if (timeout > 0) {
				try {
					// 通过wait等待 可能被到达的数据打断
					//也可能被Client.stop()打断或超时
					wait(timeout);
				} catch (InterruptedException e) {
				}
			}
		}

		if (!calls.isEmpty() && !shouldCloseConnection.get()
				&& running.get()) {
			return true;//需要等待连接上返回的远程调用结果
		} else if (shouldCloseConnection.get()) {
			return false;  //shouldCloseConnection 置位 关闭连接
		} else if (calls.isEmpty()) { // idle connection closed or stopped
			markClosed(null);
			return false;//连接长时间处于关闭状态
		} else { // get stopped but there are still pending requests
			markClosed((IOException) new IOException()
					.initCause(new InterruptedException()));
			return false;
		}
	}

	public InetSocketAddress getRemoteAddress() {
		return server;
	}

	/*
	 * Send a ping to the server if the time elapsed since last I/O activity
	 * is equal to or greater than the ping interval
	 */
	//发送心跳信息
	private synchronized void sendPing() throws IOException {
		long curTime = System.currentTimeMillis();
		if (curTime - lastActivity.get() >= pingInterval) {
			lastActivity.set(curTime);
			synchronized (out) {
				out.writeInt(PING_CALL_ID);//发送-1
				out.flush();
			}
		}
	}

	public void run() {
		if (logger.isDebugEnabled())
			logger.debug(getName() + ": starting, having connections "
					+ connections.size());
		/** wait here for work - read or close connection*/
		while (waitForWork()) {
			receiveResponse();   //如果连接上还有数据要处理 处理数据
		}

		close();//关闭连接

		if (logger.isDebugEnabled())
			logger.debug(getName() + ": stopped, remaining connections "
					+ connections.size());
	}

	/**
	 * Initiates a call by sending the parameter to the remote server. Note:
	 * this is not called from the Connection thread, but by other threads.
	 */
	public void sendParam(JDClientCall call) {
		if (shouldCloseConnection.get()) {
			return;
		}

		DataOutputBuffer d = null;
		ByteArrayOutputStream baos = null;
		ObjectOutputStream data = null;
		try {
			synchronized (this.out) {
				if (logger.isDebugEnabled())
					logger.debug(getName() + " sending #" + call.getId());

				// for serializing the
				// data to be written
				//创建一个缓冲区
				d = new DataOutputBuffer();
				
//				d.writeInt(call.id);
//				call.param.write(d);
//				out.writeInt(call.id);
				
				baos = new ByteArrayOutputStream();
				data = new ObjectOutputStream(baos);
				data.writeInt(call.getId());        //调用标示ID
				data.writeObject(call.getParam());  //调用参数
				data.flush();
				
				byte[] buff = baos.toByteArray();
				int length = buff.length;
				
				out.writeInt(length);        //首先写出数据的长度 
				out.write(buff, 0, length);  //向服务端写数据
				out.flush();
			}
		} catch (IOException e) {
			markClosed(e);
		} finally {
			// the buffer is just an in-memory buffer, but it is still
			// polite to close early
			WritableUtils.closeStream(d, data, baos);
		}
	}  

	/*
	 * Receive a response. Because only one receiver, so no synchronization
	 * on in.
	 */
	private void receiveResponse() {
		if (shouldCloseConnection.get()) {
			return;
		}
		touch();

		try {
			int id = in.readInt(); //阻塞读取id 

			if (logger.isDebugEnabled())
				logger.debug(getName() + " got value #" + id);

			JDClientCall call = calls.get(id); //在calls池中找到发送时的那个对象 

			int state = in.readInt();  //阻塞读取call对象的状态 
			if (state == Status.SUCCESS.state) {
				JDInvocation value = null;
				try {
					ObjectInputStream ois = new ObjectInputStream(in);  //读取数据 
					value = (JDInvocation) ois.readObject();
				} catch (ClassNotFoundException e) {
					logger.warn("not found class: " + e);
				}
				//将读取到的值赋给call对象，同时唤醒Client等待线程
				call.setValue(value);//会调用callComplete（）方法 Client.JDClientCall
				calls.remove(id);      //删除已处理的call
			} else if (state == Status.ERROR.state) {
				call.setException(new RemoteException(WritableUtils
						.readString(in), WritableUtils.readString(in)));
				calls.remove(id);
			} else if (state == Status.FATAL.state) {
				// Close the connection
				markClosed(new RemoteException(
						WritableUtils.readString(in),
						WritableUtils.readString(in)));
			}
		} catch (IOException e) {
			markClosed(e);
		}
	}

	private synchronized void markClosed(IOException e) {
		if (shouldCloseConnection.compareAndSet(false, true)) {
			closeException = e;
			notifyAll();
		}
	}

	/** Close the connection. */
	private synchronized void close() {
		if (!shouldCloseConnection.get()) {
			logger.error("The connection is not in the closed state");
			return;
		}

		// release the resources
		// first thing to do;take the connection out of the connection list
		//从连接列表中移除当前连接
		synchronized (connections) {
			if (connections.get(remoteId) == this) {
				connections.remove(remoteId);
			}
		}

		//关闭输入/输出流
		WritableUtils.closeStream(out, in);

		// clean up all calls
		if (closeException == null) {
			if (!calls.isEmpty()) {
				logger.warn("A connection is closed for no cause and calls are not empty");

				// clean up calls anyway
				closeException = new IOException(
						"Unexpected closed connection");
				cleanupCalls();
			}
		} else {
			// log the info
			if (logger.isDebugEnabled()) {
				logger.debug("closing ipc connection to " + server + ": "
						+ closeException.getMessage(), closeException);
			}

			// cleanup calls
			cleanupCalls();
		}
		if (logger.isDebugEnabled())
			logger.debug(getName() + ": closed");
	}

	//为当前连接上未完成的远程调用设置异常 结束远程远程调用
	private void cleanupCalls() {
		Iterator<Entry<Integer, JDClientCall>> itor = calls.entrySet().iterator();
		while (itor.hasNext()) {
			JDClientCall c = itor.next().getValue();
			c.setException(closeException); // local exception
			itor.remove();
		}
	}
}
 