/**   
 * Copyright © 2015 JOANDORA. All rights reserved.
 */
package com.joandora.nio.mycat.client.main;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.util.Iterator;

/**
 * <p>
 * 测试客户端类
 * </p>
 */
public class MainClient implements Runnable {

    private final static String BIND_IP = "127.0.0.1";

    private final static int SERVER_PORT = 8066;

    /**
     * 多线程时会导致<br>
     * java.lang.IllegalStateException: Current state = FLUSHED, new state =
     * CODING_END<br>
     * 解决办法：每个线程都创建自己的加解码器<br>
     */
    private CharsetEncoder encoder;

    private CharsetDecoder decoder;

    public static void main(String[] args) {
	// 种多个线程发起Socket客户端连接请求
	for (int i = 0; i < 1000; i++) {
	    CharsetEncoder encoder = Charset.forName("UTF-8").newEncoder();
	    CharsetDecoder decoder = Charset.forName("UTF-8").newDecoder();
	    new Thread(new MainClient(encoder, decoder)).start();
	}
    }

    public MainClient(CharsetEncoder encoder, CharsetDecoder decoder) {
	this.encoder = encoder;
	this.decoder = decoder;
    }

    @Override
    public void run() {
	SocketChannel channel = null;
	Selector selector = null;
	try {
	    channel = SocketChannel.open();
	    channel.configureBlocking(false);
	    // 请求连接
	    channel.connect(new InetSocketAddress(BIND_IP, SERVER_PORT));
	    selector = Selector.open();
	    channel.register(selector, SelectionKey.OP_CONNECT);
	    boolean isOver = false;

	    while (!isOver) {
		selector.select();
		Iterator ite = selector.selectedKeys().iterator();
		while (ite.hasNext()) {
		    SelectionKey key = (SelectionKey) ite.next();
		    ite.remove();

		    if (key.isValid() && key.isConnectable()) {
			SocketChannel sc = (SocketChannel) key.channel();
			if (sc.finishConnect()) {
			    sc.register(selector, SelectionKey.OP_READ);
			    channel.write(encode(CharBuffer.wrap("纳兰容若")));
			} else {
			    key.cancel();
			}

		    } else if (key.isValid() && key.isReadable()) {
			ByteBuffer byteBuffer = ByteBuffer.allocate(1024);
			channel.read(byteBuffer);
			byteBuffer.flip();
			CharBuffer charBuffer = decode(byteBuffer);
			String answer = charBuffer.toString();
			System.out.println(answer);

		    }
		}
	    }
	} catch (IOException e) {
	    e.printStackTrace();
	} finally {
	    if (channel != null) {
		try {
		    channel.close();
		} catch (IOException e) {
		    e.printStackTrace();
		}
	    }

	    if (selector != null) {
		try {
		    selector.close();
		} catch (IOException e) {
		    e.printStackTrace();
		}
	    }
	}
    }

    public ByteBuffer encode(CharBuffer in) throws CharacterCodingException {
	this.encoder.reset();
	return this.encoder.encode(in);
    }

    public CharBuffer decode(ByteBuffer in) throws CharacterCodingException {
	this.decoder.reset();
	return this.decoder.decode(in);
    }

}
