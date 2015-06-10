package com.htc.gc.tasks;

import com.htc.gc.interfaces.Common.OperationCallback;
import com.htc.gc.interfaces.ILiveViewer;
import com.htc.gc.internal.Protocol;
import com.htc.gc.internal.SimpleTask;

public class LiveViewStopTask extends SimpleTask {
	private static final int COMMAND_ID = Protocol.VIDEO_STOP_LIVESTREAM;

	public LiveViewStopTask(ILiveViewer that, OperationCallback callback) {
		super(that, COMMAND_ID, callback);
	}
}
