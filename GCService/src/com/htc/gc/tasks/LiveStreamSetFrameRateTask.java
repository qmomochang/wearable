package com.htc.gc.tasks;

import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import com.htc.gc.interfaces.Common;
import com.htc.gc.interfaces.Common.OperationCallback;
import com.htc.gc.interfaces.ILiveViewer;
import com.htc.gc.interfaces.ILiveViewer.LiveStreamFrameRate;
import com.htc.gc.internal.Protocol;
import com.htc.gc.internal.SimpleTask;

public class LiveStreamSetFrameRateTask extends SimpleTask {
	private static final int COMMAND_ID = Protocol.SYS_SET_LIVESTREAM_FRAMERATE;
	
	private final LiveStreamFrameRate mRate;
	
	public LiveStreamSetFrameRateTask(ILiveViewer that, LiveStreamFrameRate rate, OperationCallback callback) {
		super(that, COMMAND_ID, callback);
		
		mRate = rate;
	}

	@Override
	public void request(OutputStream stream) throws Exception {
		try {
			super.requestInternal(stream);
			
			ByteBuffer bodyBuffer = ByteBuffer.allocate(2);
			bodyBuffer.order(ByteOrder.LITTLE_ENDIAN);
			bodyBuffer.putShort(mRate.getVal());
			
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
