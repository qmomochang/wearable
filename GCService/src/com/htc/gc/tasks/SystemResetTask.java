package com.htc.gc.tasks;

import com.htc.gc.interfaces.Common.OperationCallback;
import com.htc.gc.internal.Protocol;
import com.htc.gc.internal.SimpleTask;

public class SystemResetTask extends SimpleTask {
	private static final int COMMAND_ID = Protocol.SYS_RESET;
	
	public SystemResetTask(Object that, OperationCallback callback) {
		super(that, COMMAND_ID, callback);
	}
}
