/**   
 * Copyright © 2015 JOANDORA. All rights reserved.
 */
package com.joandora.nio.mycat.client.core;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.joandora.nio.mycat.client.buffer.NIOProcessor;
import com.joandora.nio.mycat.client.conf.ConfigureConstant;
import com.joandora.nio.mycat.client.util.NIOUtils;

/**
 * <p>
 * 包装类：包装了SocketChannel，并实现了读方法
 * </p>
 * 
 * @author JOANDORA
 * @date 2016年4月11日 下午4:35:39
 */
public class NIOHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(NIOHandler.class);

    /** 客户端已建立连接的SocketChannel **/
    private SocketChannel socketChannel;

    /** 注册读事件时的key **/
    private SelectionKey processKey;

    /**
     * 读写数据时的缓冲区<br>
     * 初始时由NIOProcessor分配
     **/
    protected volatile ByteBuffer readBuffer;

    protected volatile ByteBuffer writeBuffer;

    protected final ConcurrentLinkedQueue<ByteBuffer> writeQueue = new ConcurrentLinkedQueue<ByteBuffer>();

    protected long netOutBytes;

    protected int writeAttempts;

    private final AtomicBoolean writing = new AtomicBoolean(false);

    /** 编号 **/
    private long id;

    /** 编号生成器 **/
    private static final AcceptIdGenerator ID_GENERATOR = new AcceptIdGenerator();

    protected NIOProcessor processor;

    protected long lastReadTime;

    private int maxPacketSize = ConfigureConstant.maxPacketSize;

    /**
     * 多线程时会导致<br>
     * java.lang.IllegalStateException: Current state = FLUSHED, new state =
     * CODING_END<br>
     * 解决办法：每个线程都创建自己的加解码器<br>
     */
    private CharsetEncoder encoder;

    private CharsetDecoder decoder;

    public NIOHandler(SocketChannel socketChannel, NIOProcessor nioProcessor) throws IOException {
	setId();
	setSocketChannel(socketChannel);
	this.processor = nioProcessor;
	this.readBuffer = this.processor.getBufferPool().allocate();
	this.lastReadTime = System.currentTimeMillis();
	this.encoder = Charset.forName("UTF-8").newEncoder();
	this.decoder = Charset.forName("UTF-8").newDecoder();
	LOGGER.info("assemble socketChannel-{}", id);
    }

    /**
     * <p>
     * 将参数Selector与SocketChannel进行绑定<br>
     * 并注册读事件<br>
     * key添加附件：nioHandler<br>
     * </p>
     */
    public void registerRead(Selector selector) {
	try {
	    processKey = this.socketChannel.register(selector, SelectionKey.OP_READ, this);
	} catch (ClosedChannelException e) {
	    NIOUtils.closeChannel(socketChannel);
	}
    }

    /**
     * <p>
     * 将参数Selector与SocketChannel进行绑定<br>
     * 并注写读事件<br>
     * key添加附件：nioHandler<br>
     * </p>
     */
    public void registerWrite(Selector selector) {
	try {
	    this.socketChannel.register(selector, SelectionKey.OP_WRITE, this);
	} catch (ClosedChannelException e) {
	    NIOUtils.closeChannel(socketChannel);
	}
    }

    /**
     * <p>
     * 准备好读后，立即调用该方法(阻塞读)
     * </p>
     */
    public void syncRead() throws IOException {
	if (null == this.readBuffer) {
	    // 每个SocketChannel第一次读时，初始化一个缓冲区
	    this.readBuffer = this.processor.getBufferPool().allocate();
	}
	this.readBuffer.clear();
	/***
	 * 1、循环处理字节信息 2、如果不循环处理字节信息的话，那么就得规定客户端传过来的数据长度不能大于缓冲区长度，否则数据读取不完整
	 * 3、当客户端channel关闭后，会不断收到read事件，但没有消息，即read方法返回-1，所以这时服务器端也需要关闭channel，避免死循环无效的处理。
	 */
	while (true) {
	    int got = this.socketChannel.read(this.readBuffer);
	    if (got < 0) {
		NIOUtils.closeChannel(this.socketChannel);
		return;
	    } else if (got == 0) {
		if (!this.socketChannel.isOpen()) {
		    NIOUtils.closeChannel(this.socketChannel);
		    return;
		}
	    } else {
		if (!this.readBuffer.hasRemaining()) {
		    this.readBuffer = checkReadBuffer(this.readBuffer);
		} else {
		    break;
		}
	    }
	}
	// 一定需要调用flip函数，否则读取的数据为空
	this.readBuffer.flip();
	String receiveMsg = decode(this.readBuffer).toString();
	responseMsg2WriteQueue(receiveMsg);
    }

    /**
     * <p>
     * 会进入这个方法一定是参数ByteBuffer在SocketChannel读取数据的时候，缓冲区大小比总数据长度小<br>
     * 然后就会进入这个方法，扩充的策略是如果原ByteBuffer长度的两倍大于允许数据包长度，则为maxPacketSize，
     * 否则为原ByteBuffer长度的两倍<br>
     * 然后将原ByteBuffer内容复制到新的缓冲中，新缓冲position指到原ByteBuffer数据长度，limit指到新buffer长度<br>
     * 然后将原ByteBuffer空间进行回收
     * </p>
     */
    private ByteBuffer checkReadBuffer(ByteBuffer buffer) {
	if (buffer.capacity() >= maxPacketSize) {
	    throw new IllegalArgumentException("Packet size over the limit:16 * 1024 * 1024");
	}
	int size = buffer.capacity() << 1;// 有符号的移位 x<<y = x*2的y次方
	size = (size > maxPacketSize) ? maxPacketSize : size;
	ByteBuffer newBuffer = processor.getBufferPool().allocate(size);
	buffer.position(0);
	newBuffer.put(buffer);
	this.readBuffer = newBuffer;
	this.processor.getBufferPool().recycle(buffer);
	return newBuffer;
    }

    /***
     * <p>
     * 接受客户端传来的消息，将相应的数据放入写队列中
     * </p>
     */
    private void responseMsg2WriteQueue(String receiveMsg) {
	if (!StringUtils.equals(receiveMsg, "纳兰容若")) {
	    return;
	}
	StringBuilder sb = new StringBuilder();
	sb.append("人生若只如初见，何事秋风悲画扇？等闲变却故人心，却道故人心易变。\n");
	sb.append("骊山语罢清宵半，泪雨零铃终不怨。何如薄幸锦衣郎，比翼连枝当日愿。\n");
	sb.append("——纳兰容若《木兰花令》\n");
	ByteBuffer byteBuffer = processor.getBufferPool().allocate();
	byteBuffer.put(sb.toString().getBytes());
	writeQueue.offer(byteBuffer);
    }

    /**
     * 1、写数据的过程中，将标识writing设为true。数据写完置为false<br>
     * 2、如果第一次写完没有数据且写缓冲队列为空，
     */
    public void doNextWriteCheck() {
	if (!writing.compareAndSet(false, true)) {
	    return;
	}
	try {
	    boolean hasNoMoreData = writeFirstDate();
	    writing.set(false);
	    if (hasNoMoreData && writeQueue.isEmpty()) {
		// 如果关注的是写事件
		if ((processKey.isValid() && (processKey.interestOps() & SelectionKey.OP_WRITE) != 0)) {
		    disableWrite();
		}
	    } else {
		// 如果关注的不是写事件。继续下一次写
		if ((processKey.isValid() && (processKey.interestOps() & SelectionKey.OP_WRITE) == 0)) {
		    enableWrite();
		}
	    }
	} catch (IOException e) {
	    LOGGER.error("caught err:{}", e.getMessage());
	    NIOUtils.closeChannel(this.socketChannel);
	}
    }
    /**
     * <p>
     * 
     *
     *
     * </p>
     * @return
     * @throws IOException
     *  
     * @author	zhongqoing
     * @date	2016年4月12日 下午3:20:17
     * @version
     */
    private boolean writeFirstDate() throws IOException {
	int written = 0;
	ByteBuffer buffer = this.writeBuffer;
	if (null != buffer) {
	    while (buffer.hasRemaining()) {
		written = this.socketChannel.write(buffer);
		if (written < 0) {
		    break;
		}
	    }
	    if (buffer.hasRemaining()) {
		return false;
	    } else {
		this.writeBuffer = null;
		buffer.clear();
	    }
	}
	buffer = null;
	while (null != (buffer=this.writeQueue.poll())) {
	    if (buffer.limit() == 0) {
		buffer.clear();
		NIOUtils.closeChannel(this.socketChannel);
		return true;
	    }
	    // 只有数据是从channel读出时，才需要flip。自己新建的buffer不需要flip
	    buffer.flip();
	    while (buffer.hasRemaining()) {
		written = this.socketChannel.write(buffer);
		if (written < 0) {
		    break;
		}
	    }
	    if (buffer.hasRemaining()) {
		this.writeBuffer = buffer;
		return false;
	    } else {
		buffer.clear();
	    }
	}
	return true;
    }

    /**
     * <p>
     * 写禁止
     * </p>
     */
    private void disableWrite() {
	try {
	    SelectionKey key = this.processKey;
	    key.interestOps(key.interestOps() & ~SelectionKey.OP_WRITE);// 取消写事件
									// &~ ->
									// ~按位取反
	} catch (Exception e) {
	    LOGGER.warn("disableWrite fail[cid:{}]:{} ", id, e.getMessage());
	}

    }

    /**
     * <p>
     * 写开启
     * </p>
     */
    private void enableWrite() {
	boolean needWakeup = false;
	try {
	    SelectionKey key = this.processKey;
	    key.interestOps(key.interestOps() | SelectionKey.OP_WRITE);
	    needWakeup = true;
	} catch (Exception e) {
	    LOGGER.warn("enableWrite fail[cid:{}]:{} ", id, e.getMessage());

	}
	if (needWakeup) {
	    processKey.selector().wakeup();
	}
    }

    /**
     * 前端连接ID生成器
     */
    private static class AcceptIdGenerator {

	private static final long MAX_VALUE = 0xffffffffL;

	private long acceptId = 0L;

	private final Object lock = new Object();

	private long getId() {
	    synchronized (lock) {
		if (acceptId >= MAX_VALUE) {
		    acceptId = 0L;
		}
		return ++acceptId;
	    }
	}
    }

    /** 分配客户端连接的id编号 **/
    private void setId() {
	this.id = ID_GENERATOR.getId();
    }

    public ByteBuffer encode(CharBuffer in) throws CharacterCodingException {
	this.encoder.reset();
	return this.encoder.encode(in);
    }

    public CharBuffer decode(ByteBuffer in) throws CharacterCodingException {
	this.decoder.reset();
	return this.decoder.decode(in);
    }

    /******************************** set get ****************************/
    public SocketChannel getSocketChannel() {
	return socketChannel;
    }

    public void setSocketChannel(SocketChannel socketChannel) {
	this.socketChannel = socketChannel;
    }

    public ConcurrentLinkedQueue<ByteBuffer> getWriteQueue() {
	return writeQueue;
    }

    public long getId() {
	return id;
    }

    public NIOProcessor getProcessor() {
	return processor;
    }

    public void setProcessor(NIOProcessor processor) {
	this.processor = processor;
    }
}
