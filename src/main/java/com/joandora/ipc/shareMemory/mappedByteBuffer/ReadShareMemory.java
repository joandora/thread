/**
/**   
 * Copyright © 2015 JOANDORA. All rights reserved.
 */
package com.joandora.ipc.shareMemory.mappedByteBuffer;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.FileChannel.MapMode;

/**
 * <p>
 * 从 "共享内存" 读出数据
 * </p>
 * 
 * @author JOANDORA
 * @date 2016年4月13日 下午6:56:31
 */
public class ReadShareMemory {

    /** 文件大小 **/
    private int fileSize = 1024;

    private MappedByteBuffer mbb;

    private FileChannel fc;

    public void init() throws Exception {
	RandomAccessFile raf = new RandomAccessFile("c:/mbb.lock", "rw");
	fc = raf.getChannel();
	mbb = fc.map(MapMode.READ_WRITE, 0, fileSize);
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

    public void getBuffer() {
	for (int i = 2; i < 10; i++) {
	    System.out.println("程序 ReadShareMemory：" + " 读出数据：" + mbb.getInt());
	}
    }

    /**
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
	ReadShareMemory map = new ReadShareMemory();
	if (map.getLock()) {
	    try {
		map.getBuffer();
	    } catch (Exception e) {
		e.printStackTrace();
	    }
	} else {
	    System.out.println("can't get lock");
	}
    }
}
