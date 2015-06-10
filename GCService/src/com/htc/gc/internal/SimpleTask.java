package com.htc.gc.internal;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;

import android.util.Log;

import com.htc.gc.interfaces.Common;
import com.htc.gc.interfaces.Common.OperationCallback;
import com.htc.gc.interfaces.IGCService.ICommandCancel;

public abstract class SimpleTask extends GCTask {
	protected final Object mThat;
	protected final OperationCallback mCallback;
	protected final int mCommand;

	public SimpleTask(Object that, int command, OperationCallback callback) {
		mThat = that;
		mCallback = callback;
		mCommand = command;
	}

	@Override
	public void request(OutputStream stream) throws Exception {
		try {
			super.request(stream);
			sendRequest(stream, mCommand, 0, null, true);
		}
		catch(Common.CommonException e) {
			mCallback.error(e);
		}
		catch(Exception e) {
			mCallback.error(e);
			throw e;
		}
	}

	public void requestInternal(OutputStream stream) throws Exception {
		super.request(stream);
	}

	@Override
	public void response(InputStream stream, ICommandCancel cancel) throws Exception {
		try {
			super.response(stream, cancel);

			Protocol.ResponseHeader header = new Protocol.ResponseHeader();
			ByteBuffer bodyBuffer = receiveResponse(stream, mCommand, header, cancel, true);

			byte result = bodyBuffer.get();
			if(result != Common.ErrorCode.ERR_SUCCESS.getVal()) {
				Log.w(Common.TAG, "[" + getClass().toString() + "] Operation fail error: " + Common.ErrorCode.getKey(result).toString() + "(0x" + Integer.toHexString(result) + ")");
				throw new Common.CommonException("Operation fail", Common.ErrorCode.getKey(result));
			}

			try {
				mCallback.done(mThat);
			}
			catch(Exception e) {
				e.printStackTrace();
				mCallback.error(e);
			}
		}
		catch(Common.CommonException e) {
			mCallback.error(e);
		}
		catch(Exception e) {
			mCallback.error(e);
			throw e;
		}
	}

	public void responseInternal(InputStream stream, ICommandCancel cancel) throws Exception {
		super.response(stream, cancel);
	}

	@Override
	public void error(Exception e) {
		mCallback.error(e);
	}
}
