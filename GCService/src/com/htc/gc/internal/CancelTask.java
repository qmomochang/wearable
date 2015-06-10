package com.htc.gc.internal;


import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import com.htc.gc.interfaces.Common;
import com.htc.gc.interfaces.Common.OperationCallback;

public class CancelTask extends SimpleTask {
	private static final int COMMAND_ID = Protocol.SYS_CANCEL_CMD;

	private final int mCancelSequenceID;
	public CancelTask(int cancelSequenceID, OperationCallback callback) {
		super(null, COMMAND_ID, callback);
		
		setPriority(Integer.MAX_VALUE);
		mCancelSequenceID = cancelSequenceID;
	}
	
	@Override
	public void request(OutputStream stream) throws Exception {
		try {
			super.requestInternal(stream);

			ByteBuffer bodyBuffer = ByteBuffer.allocate(4);
			bodyBuffer.order(ByteOrder.LITTLE_ENDIAN);
			bodyBuffer.putInt(mCancelSequenceID);
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

