package com.htc.gc.tasks;

import java.io.OutputStream;
import java.nio.ByteBuffer;

import com.htc.gc.interfaces.Common;
import com.htc.gc.interfaces.Common.OperationCallback;
import com.htc.gc.interfaces.IDeviceController.DebugLogSetting;
import com.htc.gc.internal.Protocol;
import com.htc.gc.internal.SimpleTask;

public class SetDebugLogEnableSettingTask extends SimpleTask {
	private static final int COMMAND_ID = Protocol.ENGINEER_SET_DEBUGLOG_ENABLE;
	
	private final DebugLogSetting mSetting;
	
	public SetDebugLogEnableSettingTask(Object that, DebugLogSetting setting, OperationCallback callback) {
		super(that, COMMAND_ID, callback);
		
		mSetting = setting;
	}
	
	@Override
	public void request(OutputStream stream) throws Exception {
		try {
			super.requestInternal(stream);
			
			ByteBuffer bodyBuffer = ByteBuffer.allocate(1);
			bodyBuffer.put(mSetting.getVal());
			
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
