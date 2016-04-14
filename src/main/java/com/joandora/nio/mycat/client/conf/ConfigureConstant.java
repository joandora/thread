/**   
 * Copyright © 2015 JOANDORA. All rights reserved.
 */   
package com.joandora.nio.mycat.client.conf; 

/**
 * <p>
 * 保存系统配置参数
 * </p>
 * @author	JOANDORA
 * @date	2016年4月12日 上午9:21:31
 */
public class ConfigureConstant {
    /**读：允许读的数据最大长度**/
    public static int maxPacketSize = 16 * 1024 * 1024;
    /**服务器CPU核数**/
    public static int defaultProcessorNum = Runtime.getRuntime().availableProcessors();
    /**一个缓冲区大小(字节)**/
    public static int defaultBufferChunkSize = 1024;
    /**缓冲块中，本地线程占有的百分比**/
    public static int threadLocalPercent = 100;
}
 