package com.htc.gc.tasks;

import java.io.OutputStream;
import java.nio.ByteBuffer;

import com.htc.gc.interfaces.Common;
import com.htc.gc.interfaces.Common.OperationCallback;
import com.htc.gc.interfaces.IStillCapturer;
import com.htc.gc.interfaces.IStillCapturer.TimeLapseAutoStop;
import com.htc.gc.internal.Protocol;
import com.htc.gc.internal.SimpleTask;

public class CaptureSetTimeLapseAutoStopTask extends SimpleTask {
	private static final int COMMAND_ID = Protocol.CAPTURE_SET_TIMELAPSE_STOPSETTING;
	
	private final TimeLapseAutoStop mAutoStop;
	
	public CaptureSetTimeLapseAutoStopTask(IStillCapturer that, TimeLapseAutoStop autoStop, OperationCallback callback) {
		super(that, COMMAND_ID, callback);
		
		mAutoStop = autoStop;
	}
	
	@Override
	public void request(OutputStream stream) throws Exception {
		try {
			super.requestInternal(stream);
			
			ByteBuffer bodyBuffer = ByteBuffer.allocate(1);
			bodyBuffer.put(mAutoStop.getVal());
			bodyBuffer.position(0);
			
			sendRequest(stream, mCommand, 0, bodyBuffer, true);
		}
		catch(Common.CommonException e) {
			mCallback.error(e);
		}
		catch(Exception e) {
			mCallback.error(e);
			throw e;
		}
	}
}
