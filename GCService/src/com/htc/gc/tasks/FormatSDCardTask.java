package com.htc.gc.tasks;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import android.util.Log;

import com.htc.gc.interfaces.Common;
import com.htc.gc.interfaces.Common.CommonException;
import com.htc.gc.interfaces.Common.OperationCallback;
import com.htc.gc.interfaces.IDeviceController;
import com.htc.gc.interfaces.IDeviceController.SDCardFormatType;
import com.htc.gc.interfaces.IGCService.ICommandCancel;
import com.htc.gc.internal.GCTask;
import com.htc.gc.internal.Protocol;

public class FormatSDCardTask extends GCTask {
	private static final int COMMAND_ID = Protocol.SYS_FORMAT_SD_CARD;

	private final IDeviceController mThat;
	private final OperationCallback mCallback;
	private final SDCardFormatType mType;

	public FormatSDCardTask(IDeviceController that, SDCardFormatType type, OperationCallback callback) {
		mThat = that;
		mCallback = callback;
		mType = type;
	}

	@Override
	public void request(OutputStream stream) throws Exception {
		try {
			super.request(stream);
			
			ByteBuffer bodyBuffer = ByteBuffer.allocate(4);
			bodyBuffer.order(ByteOrder.LITTLE_ENDIAN);
			if (mType == SDCardFormatType.FILESYS_FORMAT_FULL)
				bodyBuffer.put((byte)0x00);
			else if (mType == SDCardFormatType.FILESYS_FORMAT_QUICK)
				bodyBuffer.put((byte)0x01);
			else
				throw new IllegalArgumentException("Format type not recognizable: " + mType);
			bodyBuffer.position(0);

			sendRequest(stream, COMMAND_ID, 0, bodyBuffer, true);
		}
		catch(CommonException e) {
			mCallback.error(e);
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
			} else {
				mCallback.done(mThat);
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

	@Override
	public void error(Exception e) {
		mCallback.error(e);
	}
}
