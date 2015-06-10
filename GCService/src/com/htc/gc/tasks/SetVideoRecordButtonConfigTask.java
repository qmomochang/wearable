package com.htc.gc.tasks;

import java.io.OutputStream;
import java.nio.ByteBuffer;

import com.htc.gc.interfaces.Common.OperationCallback;
import com.htc.gc.interfaces.Common;
import com.htc.gc.interfaces.IDeviceController;
import com.htc.gc.interfaces.IDeviceController.VideoRecBtnConfig;
import com.htc.gc.internal.Protocol;
import com.htc.gc.internal.SimpleTask;

public class SetVideoRecordButtonConfigTask extends SimpleTask {
	private static final int COMMAND_ID = Protocol.SYS_SET_VIDEO_REC_BUTTON_CONFIG;
	
	private final VideoRecBtnConfig mConfig;
	
	public SetVideoRecordButtonConfigTask(IDeviceController that, VideoRecBtnConfig config, OperationCallback callback) {
		super(that, COMMAND_ID, callback);
		
		mConfig = config;
	}
	
	@Override
	public void request(OutputStream stream) throws Exception {
		try {
			super.requestInternal(stream);
			
			ByteBuffer bodyBuffer = ByteBuffer.allocate(1);
			bodyBuffer.put(mConfig.getVal());
			
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
