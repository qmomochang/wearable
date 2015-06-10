package com.htc.gc.tasks;

import com.htc.gc.interfaces.Common.OperationCallback;
import com.htc.gc.interfaces.IStillCapturer;
import com.htc.gc.internal.Protocol;
import com.htc.gc.internal.SimpleTask;

public class CaptureTimeLapseResumeTask extends SimpleTask {
	private static final int COMMAND_ID = Protocol.CAPTURE_TIMELAPSECAPTURE_RESUME;

	public CaptureTimeLapseResumeTask(IStillCapturer that, OperationCallback callback) {
		super(that, COMMAND_ID, callback);
	}
}
