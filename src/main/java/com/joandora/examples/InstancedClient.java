package com.joandora.examples;


import java.net.InetSocketAddress;

import com.joandora.test.proxy.JDRPCProxy;
import com.joandora.test.util.JDNetUtils;

public class InstancedClient {
	/**
	 * The Server Instance Main.
	 * @param args
	 * 		[port, host]
	 * 		--port	if not configured, the default is {@link InstancedServer.PORT}
	 * 		--host	if not configured, default is the first record of
	 * 				running the command `ifconfig` or `ipconfig`
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		String host = JDNetUtils.getLocalAddress();
		int port = InstancedServer.PORT;
		if (args.length > 0)
			port = Integer.parseInt(args[0]);
		if (args.length > 1)
			host = args[1];

		InetSocketAddress addr = JDNetUtils.makeSocketAddr(host, port);
		// 创建代理
		Echo proxy = JDRPCProxy.getProxy(Echo.class, addr);
		System.out.println("VVVVVVVVVV: " + proxy.who());
		proxy.from("jakkkkkkki");

		ICode proxyCode = JDRPCProxy.getProxy(ICode.class, addr);
		Code v = proxyCode.generate("bbbbbbbbbbbbbbb");
		System.out.println(v.getLink().length());
	}

}
