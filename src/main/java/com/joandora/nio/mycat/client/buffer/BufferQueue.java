/**   
 * Copyright © 2015 JOANDORA. All rights reserved.
 */
package com.joandora.nio.mycat.client.buffer;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * <p>
 * 缓冲队列
 * </p>
 * 
 * @author JOANDORA
 * @date 2016年4月11日 下午4:52:16
 */
public class BufferQueue {

    private final long total;

    private final LinkedList<ByteBuffer> items = new LinkedList<ByteBuffer>();

    public BufferQueue(long capacity) {
	this.total = capacity;
    }

    /**
     * 返回内部缓存队列大小
     */
    public int snapshotSize() {
	return this.items.size();
    }
    /**
     * 将内部缓冲队列删除指定书目的缓冲块，然后将删除的缓冲块作为list返回<br>
     */
    public Collection<ByteBuffer> removeItems(long count) {
	List<ByteBuffer> removed = new ArrayList<ByteBuffer>();
	Iterator<ByteBuffer> itor = items.iterator();
	while (itor.hasNext()) {
	    removed.add(itor.next());
	    itor.remove();
	    if (removed.size() >= count) {
		break;
	    }
	}
	return removed;
    }

    /**
     * 
     * @param buffer
     * @throws InterruptedException
     */
    public void put(ByteBuffer buffer) {
	this.items.offer(buffer);
	if (items.size() > total) {
	    throw new java.lang.RuntimeException("bufferQueue size exceeded ,maybe sql returned too many records ,cursize:" + items.size());

	}
    }

    public ByteBuffer poll() {
	return items.poll();
    }

    public boolean isEmpty() {
	return items.isEmpty();
    }
}
