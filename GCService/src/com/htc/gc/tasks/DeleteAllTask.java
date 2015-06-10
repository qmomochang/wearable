package com.htc.gc.tasks;

import com.htc.gc.interfaces.Common.OperationCallback;
import com.htc.gc.interfaces.IItemOperator;
import com.htc.gc.internal.Protocol;
import com.htc.gc.internal.SimpleTask;

public class DeleteAllTask extends SimpleTask {
	private static final int COMMAND_ID = Protocol.FILE_DELETE_ALL;

	public DeleteAllTask(IItemOperator that, OperationCallback callback) {
		super(that, COMMAND_ID, callback);
	}
}
