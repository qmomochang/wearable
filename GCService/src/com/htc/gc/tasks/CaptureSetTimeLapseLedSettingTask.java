package com.htc.gc.tasks;

import java.io.OutputStream;
import java.nio.ByteBuffer;

import com.htc.gc.interfaces.Common;
import com.htc.gc.interfaces.Common.OperationCallback;
import com.htc.gc.interfaces.IStillCapturer.LedSwitch;
import com.htc.gc.internal.Protocol;
import com.htc.gc.internal.SimpleTask;

public class CaptureSetTimeLapseLedSettingTask extends SimpleTask {
	private static final int COMMAND_ID = Protocol.CAPTURE_SET_TIMELAPSE_LED_STATUS;
	
	private final LedSwitch mOnOff;
	
	public CaptureSetTimeLapseLedSettingTask(Object that, LedSwitch onOff, OperationCallback callback) {
		super(that, COMMAND_ID, callback);

		mOnOff = onOff;
	}
	
	@Override
	public void request(OutputStream stream) throws Exception {
		try {
			super.requestInternal(stream);
			
			ByteBuffer bodyBuffer = ByteBuffer.allocate(1);
			bodyBuffer.put(mOnOff.getVal());
			
			bodyBuffer.position(0);
			sendRequest(stream, COMMAND_ID, 0, bodyBuffer, true);
		} catch(Common.CommonException e) {
			mCallback.error(e);
		} catch(Exception e) {
			mCallback.error(e);
			throw e;
		}
	}
}
