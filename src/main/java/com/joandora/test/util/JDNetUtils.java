package com.joandora.test.util;


import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.nio.channels.SocketChannel;
import java.util.Enumeration;

import javax.net.SocketFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.joandora.test.SocketIOWithTimeout;
import com.joandora.test.data.SocketInputStream;
import com.joandora.test.data.SocketOutputStream;
/***
 * <p>
 * socket tool
 * </p>
 * @author	JOANDORA 
 * @date	2016年4月14日 下午2:21:07
 */
public class JDNetUtils {
	private static final Logger logger = LoggerFactory.getLogger(JDNetUtils.class);
	
	/**
	 * Get the default socket factory: SocketFactory.getDefault()
	 */
	public static SocketFactory getDefaultSocketFactory() {
		return SocketFactory.getDefault();
	}

	/**
	 * Create a socket address with the given host and port. <br>
	 */
	public static InetSocketAddress getSocketAddr(String host, int port) {
		InetSocketAddress addr;
		try {
			InetAddress iaddr = InetAddress.getByName(host);
			// if there is a static entry for the host, make the returned
			// address look like the original given host
			iaddr = InetAddress.getByAddress(host, iaddr.getAddress());
			addr = new InetSocketAddress(iaddr, port);
		} catch (UnknownHostException e) {
			addr = InetSocketAddress.createUnresolved(host, port);
		}
		return addr;
	}

	/**
	 * @see #getInputStream(Socket, long)
	 */
	public static InputStream getInputStream(Socket socket) throws IOException {
		return (socket.getChannel() == null) ? socket.getInputStream()
				: new SocketInputStream(socket, socket.getSoTimeout());
	}

	/**
	 * @see #getOutputStream(Socket, long)
	 */
	public static OutputStream getOutputStream(Socket socket)
			throws IOException {
		return (socket.getChannel() == null) ? socket.getOutputStream()
				: new SocketOutputStream(socket, socket.getSoTimeout());
	}

	/**
	 * SocketChannel ch = socket.getChannel();<br>
	 * <ul>
	 * <li>ch is null,then invoke {@link Socket#connect(SocketAddress, int)}</li>
	 * <li>ch is non-null,then connect is implemented using Hadoop's selectors.<br>
	 * 	This is done mainly to avoid Sun's
	 * connect implementation from creating thread-local selectors, since Hadoop
	 * does not have control on when these are closed and could end up taking
	 * all the available file descriptors.
	 * </li>
	 * </ul>
	 */
	public static void connect(Socket socket, SocketAddress socketAddress,int timeout) throws IOException {
		if (socket == null || socketAddress == null || timeout < 0) {
			throw new IllegalArgumentException("Illegal argument for connect()");
		}
		SocketChannel ch = socket.getChannel();

		if (ch == null) {
			// let the default implementation handle it.
			socket.connect(socketAddress, timeout);
		} else {
			logger.debug("SocketChannel is null");
			SocketIOWithTimeout.connect(ch, socketAddress, timeout);
		}
		// There is a very rare case allowed by the TCP specification, such that
		// if we are trying to connect to an endpoint on the local machine,
		// and we end up choosing an ephemeral port equal to the destination port,
		// we will actually end up getting connected to ourself (ie any data we
		// send just comes right back). This is only possible if the target
		// daemon is down, so we'll treat it like connection refused.
		// a sample:Socket[addr=192.168.13.61/192.168.13.61,port=4453,localport=52177]
		if (socket.getLocalPort() == socket.getPort()&& socket.getLocalAddress().equals(socket.getInetAddress())) {
			socket.close();
			throw new ConnectException("Detected a loopback TCP socket, disconnecting it");
		}
	}

	/**
	 * Get the local host address.
	 * @return the site address or `127.0.0.1` 
	 */
	public static String getLocalAddress() {
		String host = "127.0.0.1";
		Enumeration<NetworkInterface> ifaces = null;
		try {
			ifaces = NetworkInterface.getNetworkInterfaces();
			while (ifaces.hasMoreElements()) {
				NetworkInterface ni = ifaces.nextElement();
				Enumeration<InetAddress> ips = ni.getInetAddresses();
				while (ips.hasMoreElements()) {
					InetAddress addr = ips.nextElement();
					if (addr.isSiteLocalAddress())
						return host = addr.getHostAddress();
				}
			}
		} catch (Exception e) {
		}
		return host;
	}
}
