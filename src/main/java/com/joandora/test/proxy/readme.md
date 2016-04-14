# com.joandora.test.proxy.RPCProxyWrapper
* wrapper for client and server side
* wraper tool is jdk static proxy
* invoke method is com.joandora.test.proxy.ProxyInvoker.ProxyInvoker<T>

######## how to use
* if you want a proxy(client),you can use RPCProxyWrapper.getProxy(Echo.class, InetSocketAddress);
* if you want a server,you can use RPCProxyWrapper.getServer(host, port);

# com.joandora.test.util.JDNetUtils
* write data to DataOutput
* read data from DataInput
* Closes the stream ignoring IOException


