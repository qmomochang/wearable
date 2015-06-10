package com.htc.gc.tasks;

import java.io.OutputStream;
import java.nio.ByteBuffer;

import com.htc.gc.interfaces.Common;
import com.htc.gc.interfaces.Common.OperationCallback;
import com.htc.gc.interfaces.IStillCapturer;
import com.htc.gc.interfaces.IStillCapturer.WideAngleMode;
import com.htc.gc.internal.Protocol;
import com.htc.gc.internal.SimpleTask;

public class CaptureSetWideAngleModeTask extends SimpleTask {
	private static final int COMMAND_ID = Protocol.CAPTURE_SET_WIDE_ANGLE_MODE;
	
	private final WideAngleMode mStatus;
	
	public CaptureSetWideAngleModeTask(IStillCapturer that, WideAngleMode status, OperationCallback callback) {
		super(that, COMMAND_ID, callback);
		
		mStatus = status;
	}
	
	@Override
	public void request(OutputStream stream) throws Exception {
		try {
			super.requestInternal(stream);
			
			ByteBuffer bodyBuffer = ByteBuffer.allocate(1);
			bodyBuffer.put(mStatus.getVal());
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
