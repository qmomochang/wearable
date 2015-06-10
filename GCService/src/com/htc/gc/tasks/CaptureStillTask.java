package com.htc.gc.tasks;

import java.io.OutputStream;
import java.nio.ByteBuffer;

import com.htc.gc.interfaces.Common;
import com.htc.gc.interfaces.Common.OperationCallback;
import com.htc.gc.interfaces.IStillCapturer;
import com.htc.gc.internal.Protocol;
import com.htc.gc.internal.SimpleTask;

public class CaptureStillTask extends SimpleTask {
	private static final int COMMAND_ID = Protocol.CAPTURE_SHUTTER_PRESS;
	
	private static final int CAPTURE_MODE_AUTO = 0x00;
	
	public CaptureStillTask(IStillCapturer that, OperationCallback callback) {
		super(that, COMMAND_ID, callback);
	}

	@Override
	public void request(OutputStream stream) throws Exception {
		try {
			super.requestInternal(stream);

			ByteBuffer bodyBuffer = ByteBuffer.allocate(1);
			bodyBuffer.put((byte) CAPTURE_MODE_AUTO);
			bodyBuffer.position(0);

			sendRequest(stream, COMMAND_ID, 0, bodyBuffer, true);
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
