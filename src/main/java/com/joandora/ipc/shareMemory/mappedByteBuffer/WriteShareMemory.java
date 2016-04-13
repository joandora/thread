/**   
 * Copyright © 2015 JOANDORA. All rights reserved.
 */
package com.joandora.ipc.shareMemory.mappedByteBuffer;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.nio.channels.FileLock;

/**
 * <p>
 * 往 "共享内存" 写入数据
 * </p>
 * 
 * @author JOANDORA
 * @date 2016年4月13日 下午6:55:32
 */
public class WriteShareMemory {

    /** 文件大小 **/
    private int fileSize = 1024;

    private MappedByteBuffer mbb;

    private FileChannel fc;

    /**
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
	WriteShareMemory map = new WriteShareMemory();
	map.init();
	if (map.getLock()) {
	    try {
		map.putBuffer();
		System.out.println("write done！");
	    } catch (Exception e) {
		e.printStackTrace();
	    }
	} else {
	    System.out.println("can't get lock");
	}
    }

    public void init() throws Exception {
	RandomAccessFile raf = new RandomAccessFile("c:/mbb.lock", "rw");
	fc = raf.getChannel();
	mbb = fc.map(MapMode.READ_WRITE, 0, fileSize);
    }

    public void putBuffer() {
	// 清除文件内容
	clearBuffer();
	// 从文件的第二个字节开始，依次写入 1-9 数字，第一个字节指明了当前操作的位置
	for (int index = 1; index < 10; index++) {
	    mbb.putInt(index);
	}
    }

    public void clearBuffer() {
	// 清除文件内容
	for (int i = 0; i < fileSize; i++) {
	    mbb.put(i, (byte) 0);
	}
    }

    public boolean getLock() {
	FileLock lock = null;
	try {
	    lock = fc.tryLock();
	} catch (IOException e) {
	    e.printStackTrace();
	}
	if (lock == null) {
	    return false;
	} else {
	    return true;
	}
    }
}
