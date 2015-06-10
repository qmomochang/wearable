package com.htc.gc.tasks;

import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import com.htc.gc.interfaces.Common.DataCallback;
import com.htc.gc.interfaces.IDeviceController.DebugLogDeleteSetting;
import com.htc.gc.interfaces.IDeviceController.DebugLogType;
import com.htc.gc.internal.Protocol;

public class GetErrorLogFromGcTask extends DownloadFragmentTask {
	private static final int COMMAND_ID = Protocol.ENGINEER_GET_ERROR_LOG;
	
	private final DebugLogType mType;
	private final boolean mAutoDelete;
	protected final DataCallback mCallback;
	
	public GetErrorLogFromGcTask(DebugLogType type, boolean autoDelete, DataCallback callback) {
		super(callback, COMMAND_ID);
		
		mType = type;
		mAutoDelete = autoDelete;
		mCallback = callback;
	}
	
	@Override
	public void request(OutputStream stream) throws Exception {
		try {
			if(isCancel()) {
				return;
			}
			
			super.request(stream);
			ByteBuffer bodyBuffer = ByteBuffer.allocate(2);
			bodyBuffer.order(ByteOrder.LITTLE_ENDIAN);
			
			bodyBuffer.put(mType.getVal());
			bodyBuffer.put(mAutoDelete ? DebugLogDeleteSetting.DEBUGLOG_DELETELOG.getVal() : DebugLogDeleteSetting.DEBUGLOG_NOTDELETELOG.getVal());
			bodyBuffer.position(0);
			sendRequest(stream, COMMAND_ID, 0, bodyBuffer, true);
		} catch(Exception e) {
			mCallback.error(e);
			throw e;
		}	
	}
}
