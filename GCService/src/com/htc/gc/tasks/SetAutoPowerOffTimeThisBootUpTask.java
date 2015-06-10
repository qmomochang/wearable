package com.htc.gc.tasks;

import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import com.htc.gc.interfaces.Common;
import com.htc.gc.interfaces.Common.OperationCallback;
import com.htc.gc.internal.Protocol;
import com.htc.gc.internal.SimpleTask;

public class SetAutoPowerOffTimeThisBootUpTask extends SimpleTask {
	private static final int COMMAND_ID = Protocol.SYS_SET_AUTOPOWEROFF_TIME;
	
	private final short mSeconds;
	
	public SetAutoPowerOffTimeThisBootUpTask(Object that, short seconds, OperationCallback callback) {
		super(that, COMMAND_ID, callback);
		
		mSeconds = seconds;
	}
	
	@Override
	public void request(OutputStream stream) throws Exception {
		try {
			super.requestInternal(stream);
			
			ByteBuffer bodyBuffer = ByteBuffer.allocate(Short.SIZE);
			bodyBuffer.order(ByteOrder.LITTLE_ENDIAN);
			bodyBuffer.putShort(mSeconds);
			
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
