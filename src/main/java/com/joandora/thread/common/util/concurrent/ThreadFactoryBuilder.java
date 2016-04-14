/**   
 * Copyright © 2016 JOANDORA. All rights reserved.
 * 
 * @Title: ThreadFactoryBuilder.java 
 * @Prject: thread
 * @Package: com.joandora.thread.common.util.concurrent 
 * @version: V1.0   
 */
package com.joandora.thread.common.util.concurrent;


import java.lang.Thread.UncaughtExceptionHandler;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicLong;


/** 
 * @ClassName: ThreadFactoryBuilder 
 * @Description: ThreadFactory 构造器
 * @author: JOANDORA
 * @date: 2016年1月27日 下午8:12:49  
 */
public class ThreadFactoryBuilder {
	private String nameFormat = null;
	  private Boolean daemon = null;
	  private Integer priority = null;
	  private UncaughtExceptionHandler uncaughtExceptionHandler = null;
	  private ThreadFactory backingThreadFactory = null;

	  /**
	   * Creates a new {@link ThreadFactory} builder.
	   */
	  public ThreadFactoryBuilder() {}

	  /**
	   * Sets the naming format to use when naming threads ({@link Thread#setName})
	   * which are created with this ThreadFactory.
	   *
	   * @param nameFormat a {@link String#format(String, Object...)}-compatible
	   *     format String, to which a unique integer (0, 1, etc.) will be supplied
	   *     as the single parameter. This integer will be unique to the built
	   *     instance of the ThreadFactory and will be assigned sequentially.
	   * @return this for the builder pattern
	   */
	  public ThreadFactoryBuilder setNameFormat(String nameFormat) {
	    String.format(nameFormat, 0); // fail fast if the format is bad or null
	    this.nameFormat = nameFormat;
	    return this;
	  }

	  /**
	   * Sets daemon or not for new threads created with this ThreadFactory.
	   *
	   * @param daemon whether or not new Threads created with this ThreadFactory
	   *     will be daemon threads
	   * @return this for the builder pattern
	   */
	  public ThreadFactoryBuilder setDaemon(boolean daemon) {
	    this.daemon = daemon;
	    return this;
	  }

	  /**
	   * Sets the priority for new threads created with this ThreadFactory.
	   *
	   * @param priority the priority for new Threads created with this
	   *     ThreadFactory
	   * @return this for the builder pattern
	   */
	  public ThreadFactoryBuilder setPriority(int priority) {
	    // Thread#setPriority() already checks for validity. These error messages
	    // are nicer though and will fail-fast.
	    if(priority >= Thread.MIN_PRIORITY){
	    	System.out.printf( "Thread priority (%s) must be >= %s", priority, Thread.MIN_PRIORITY);
	    }
	    if(priority <= Thread.MAX_PRIORITY){
	    	System.out.printf( "Thread priority (%s) must be <= %s", priority, Thread.MAX_PRIORITY);
	    }
	    this.priority = priority;
	    return this;
	  }

	  /**
	   * Sets the {@link UncaughtExceptionHandler} for new threads created with this
	   * ThreadFactory.
	   *
	   * @param uncaughtExceptionHandler the uncaught exception handler for new
	   *     Threads created with this ThreadFactory
	   * @return this for the builder pattern
	   */
	  public ThreadFactoryBuilder setUncaughtExceptionHandler(
	      UncaughtExceptionHandler uncaughtExceptionHandler) {
		  if(null == uncaughtExceptionHandler){
			  throw new NullPointerException("uncaughtExceptionHandler is null");
		  }
	    this.uncaughtExceptionHandler =uncaughtExceptionHandler;
	    return this;
	  }

	  /**
	   * Sets the backing {@link ThreadFactory} for new threads created with this
	   * ThreadFactory. Threads will be created by invoking #newThread(Runnable) on
	   * this backing {@link ThreadFactory}.
	   *
	   * @param backingThreadFactory the backing {@link ThreadFactory} which will
	   *     be delegated to during thread creation.
	   * @return this for the builder pattern
	   *
	   * @see MoreExecutors
	   */
	  public ThreadFactoryBuilder setThreadFactory(
	      ThreadFactory backingThreadFactory) {
		  if(null == uncaughtExceptionHandler){
			  throw new NullPointerException("backingThreadFactory is null");
		  }
	    this.backingThreadFactory = backingThreadFactory;
	    return this;
	  }

	  /**
	   * Returns a new thread factory using the options supplied during the building
	   * process. After building, it is still possible to change the options used to
	   * build the ThreadFactory and/or build again. State is not shared amongst
	   * built instances.
	   *
	   * @return the fully constructed {@link ThreadFactory}
	   */
	  public ThreadFactory build() {
	    return build(this);
	  }

	  private static ThreadFactory build(ThreadFactoryBuilder builder) {
	    final String nameFormat = builder.nameFormat;
	    final Boolean daemon = builder.daemon;
	    final Integer priority = builder.priority;
	    final UncaughtExceptionHandler uncaughtExceptionHandler =
	        builder.uncaughtExceptionHandler;
	    final ThreadFactory backingThreadFactory =
	        (builder.backingThreadFactory != null)
	        ? builder.backingThreadFactory
	        : Executors.defaultThreadFactory();
	    final AtomicLong count = (nameFormat != null) ? new AtomicLong(0) : null;
	    return new ThreadFactory() {
	      @Override public Thread newThread(Runnable runnable) {
	        Thread thread = backingThreadFactory.newThread(runnable);
	        if (nameFormat != null) {
	          thread.setName(String.format(nameFormat, count.getAndIncrement()));
	        }
	        if (daemon != null) {
	          thread.setDaemon(daemon);
	        }
	        if (priority != null) {
	          thread.setPriority(priority);
	        }
	        if (uncaughtExceptionHandler != null) {
	          thread.setUncaughtExceptionHandler(uncaughtExceptionHandler);
	        }
	        return thread;
	      }
	    };
	  }
}
