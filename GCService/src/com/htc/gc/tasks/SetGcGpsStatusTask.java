package com.htc.gc.tasks;

import java.io.OutputStream;
import java.nio.ByteBuffer;

import com.htc.gc.interfaces.Common;
import com.htc.gc.interfaces.Common.OperationCallback;
import com.htc.gc.interfaces.IDeviceController;
import com.htc.gc.interfaces.IDeviceController.GcGpsStatus;
import com.htc.gc.internal.Protocol;
import com.htc.gc.internal.SimpleTask;

public class SetGcGpsStatusTask extends SimpleTask {
	private static final int COMMAND_ID = Protocol.SYS_SET_GPS_STATUS;
	
	private final GcGpsStatus mStauts;
	
	public SetGcGpsStatusTask(IDeviceController that, GcGpsStatus status, OperationCallback callback) {
		super(that, COMMAND_ID, callback);
		
		mStauts = status;
	}
	
	@Override
	public void request(OutputStream stream) throws Exception {
		try {
			super.requestInternal(stream);
			
			ByteBuffer bodyBuffer = ByteBuffer.allocate(1);
			bodyBuffer.put(mStauts.getVal());
			
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
