package com.htc.gc.tasks;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;

import android.util.Log;

import com.htc.gc.interfaces.Common;
import com.htc.gc.interfaces.Common.ErrorCode;
import com.htc.gc.interfaces.Common.OperationCallback;
import com.htc.gc.interfaces.IDeviceController;
import com.htc.gc.interfaces.IGCService.ICommandCancel;
import com.htc.gc.internal.GCTask;
import com.htc.gc.internal.Protocol;

public class ModeSetTask extends GCTask {
	private static final int COMMAND_ID = Protocol.SYS_SET_FUNCTION_MODE;

	private final IDeviceController mThat;
	private final Common.Mode mMode;
	private final SetControllerModeCallback mCallback;

	public interface SetControllerModeCallback extends OperationCallback {
		void noChange();
	}

	public ModeSetTask(IDeviceController that, Common.Mode mode, SetControllerModeCallback callback) {
		mThat = that;
		mMode = mode;
		mCallback = callback;
	}

	@Override
	public void request(OutputStream stream) throws Exception {
		try {
			super.request(stream);

			ByteBuffer bodyBuffer = ByteBuffer.allocate(1);

			if(mMode == Common.Mode.Browse) bodyBuffer.put((byte) Protocol.PROP_FUNCTIONMODE_BROWSE);
			else if(mMode == Common.Mode.Control) bodyBuffer.put((byte) Protocol.PROP_FUNCTIONMODE_CONTROL);
			else throw new Common.CommonException("Mode does not support", ErrorCode.ERR_SYSTEM_ERROR);

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

	@Override
	public void response(InputStream stream, ICommandCancel cancel) throws Exception {
		try {
			super.response(stream, cancel);

			Protocol.ResponseHeader header = new Protocol.ResponseHeader();
			ByteBuffer bodyBuffer = receiveResponse(stream, COMMAND_ID, header, cancel, true);

			byte result = bodyBuffer.get();
			if(result != Common.ErrorCode.ERR_SUCCESS.getVal() && result != Common.ErrorCode.ERR_SAME_MODE.getVal()) {
				Log.w(Common.TAG, "[" + getClass().toString() + "] Operation fail error: " + Common.ErrorCode.getKey(result).toString() + "(0x" + Integer.toHexString(result) + ")");
				throw new Common.CommonException("Operation fail", Common.ErrorCode.getKey(result));
			}

			try {
				mCallback.done(mThat);
				if(result == Common.ErrorCode.ERR_SAME_MODE.getVal()) mCallback.noChange();
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

	@Override
	public void error(Exception e) {
		mCallback.error(e);
	}

}
