package com.htc.gc.tasks;

import java.io.OutputStream;
import java.nio.ByteBuffer;

import com.htc.gc.interfaces.Common;
import com.htc.gc.interfaces.Common.OperationCallback;
import com.htc.gc.interfaces.IStillCapturer;
import com.htc.gc.interfaces.IStillCapturer.FaceCountTimes;
import com.htc.gc.internal.Protocol;
import com.htc.gc.internal.SimpleTask;

public class CaptureSetFaceCountTimesSettingTask extends SimpleTask {
	private static final int COMMAND_ID = Protocol.CAPTURE_SET_FACEDETECT_TIMES;
	
	private final FaceCountTimes mCountTimes;
	
	public CaptureSetFaceCountTimesSettingTask(IStillCapturer that, FaceCountTimes countTimes, OperationCallback callback) {
		super(that, COMMAND_ID, callback);
		
		mCountTimes = countTimes;
	}
	
	@Override
	public void request(OutputStream stream) throws Exception {
		try {
			super.requestInternal(stream);
			
			ByteBuffer bodyBuffer = ByteBuffer.allocate(1);
			bodyBuffer.put(mCountTimes.getVal());
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
