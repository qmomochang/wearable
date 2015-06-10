package com.htc.gc.tasks;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import android.util.Log;

import com.htc.gc.interfaces.Common;
import com.htc.gc.interfaces.IGCService.ICommandCancel;
import com.htc.gc.internal.GCTask;
import com.htc.gc.internal.Protocol;

public class CheckValidationTask extends GCTask {
	private static final int COMMAND_ID = Protocol.INITIAL_CHECK_VALIDATION;

	public interface ValidationCallback extends Common.ErrorCallback {
		void result(byte[] dcGUID, int protocolVersion, int fwVersion, int bootVersion, byte mcuVersion);
	}

	private final byte[] mAppGuid;
	private final int mVersion;
	private final ValidationCallback mCallback;

	public CheckValidationTask(byte[] appGuid, int version, ValidationCallback callback) {
		mAppGuid = appGuid.clone();
		mVersion = version;
		mCallback = callback;
	}

	@Override
	public void request(OutputStream stream) throws Exception {
		try {
			super.request(stream);

			ByteBuffer bodyBuffer = ByteBuffer.allocate(20);
			bodyBuffer.order(ByteOrder.LITTLE_ENDIAN);

			bodyBuffer.put(mAppGuid);
			bodyBuffer.putInt(mVersion);
			bodyBuffer.position(0);

			sendRequest(stream, COMMAND_ID, 0, bodyBuffer, true);
		}
		catch(Exception e) {
			mCallback.error(e);
			throw e;
		}
	}

	@Override
	public void response(InputStream stream, ICommandCancel cancel) throws Exception {
		try {
			super.response(stream, cancel);

			Protocol.ResponseHeader header = new Protocol.ResponseHeader();
			ByteBuffer bodyBuffer = receiveResponse(stream, COMMAND_ID, header, cancel, true);

			byte result = bodyBuffer.get();
			if(result != Common.ErrorCode.ERR_SUCCESS.getVal()) {
				Log.w(Common.TAG, "[" + getClass().toString() + "] Operation fail error: " + Common.ErrorCode.getKey(result).toString() + "(0x" + Integer.toHexString(result) + ")");
				throw new Common.CommonException("Operation fail", Common.ErrorCode.getKey(result));
			}

			byte[] dcGUID = new byte[16];
			bodyBuffer.get(dcGUID);

			mCallback.result(dcGUID, bodyBuffer.getInt(), bodyBuffer.getInt(), bodyBuffer.getInt(), bodyBuffer.get());
		}
		catch(Common.CommonException e) {
			mCallback.error(e);
		}
		catch(Exception e) {
			mCallback.error(e);
			throw e;
		}
	}

	@Override
	public void error(Exception e) {
		mCallback.error(e);
	}
}
