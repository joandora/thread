/**   
 * Copyright © 2015 JOANDORA. All rights reserved.
 */  
package com.joandora.test.constant;


/**
 * Status of a Hadoop IPC call.
 */
public enum Status {
	SUCCESS(0), ERROR(1), FATAL(-1);

	public int state;

	private Status(int state) {
		this.state = state;
	}
}
