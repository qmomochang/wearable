package com.htc.gc.tasks;

import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import com.htc.gc.interfaces.Common;
import com.htc.gc.interfaces.Common.OperationCallback;
import com.htc.gc.interfaces.IStillCapturer;
import com.htc.gc.internal.Protocol;
import com.htc.gc.internal.SimpleTask;

public class TimeLapseSetRateTask extends SimpleTask {
	private static final int COMMAND_ID = Protocol.CAPTURE_SET_TIMELAPSE_RATE;

	private final int mRate;

	public TimeLapseSetRateTask(IStillCapturer that, int sec, OperationCallback callback) {
		super(that, COMMAND_ID, callback);
		mRate = sec;
	}

	@Override
	public void request(OutputStream stream) throws Exception {
		try {
			super.requestInternal(stream);

			ByteBuffer bodyBuffer = ByteBuffer.allocate(4);
			bodyBuffer.order(ByteOrder.LITTLE_ENDIAN);
			bodyBuffer.putInt(mRate);

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
