package com.htc.gc.tasks;

import com.htc.gc.interfaces.Common.OperationCallback;
import com.htc.gc.interfaces.IVideoRecorder;
import com.htc.gc.internal.Protocol;
import com.htc.gc.internal.SimpleTask;

public class RecordVideoStopTask extends SimpleTask {
	private static final int COMMAND_ID = Protocol.VIDEO_STOP_RECORD;

	public RecordVideoStopTask(IVideoRecorder that, OperationCallback callback) {
		super(that, COMMAND_ID, callback);
	}
}
