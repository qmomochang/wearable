package com.htc.gc.tasks;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;

import android.util.Log;

import com.htc.gc.interfaces.Common;
import com.htc.gc.interfaces.Common.CommonException;
import com.htc.gc.interfaces.Common.RequestCallback;
import com.htc.gc.interfaces.IDeviceController;
import com.htc.gc.interfaces.IGCService.ICommandCancel;
import com.htc.gc.internal.GCTask;
import com.htc.gc.internal.Protocol;

public class SetGcToOobeModeTask extends GCTask  {
	private static final int COMMAND_ID_NORESPONSE = Protocol.SYS_REQUEST_OOBEMODE;
	private static final int COMMAND_ID_RESOPNSE = Protocol.SYS_REQUEST_OOBEMODE_RESPONSE;
	
	private final IDeviceController mThat;
	private final RequestCallback mCallback;
	private final int mCommandId;
	private final boolean mEnableResponse;
	
	public SetGcToOobeModeTask(IDeviceController that, RequestCallback callback, boolean enableResponse) {
		mThat = that;
		mCallback = callback;
		mEnableResponse = enableResponse;
		if(enableResponse) {
			mCommandId = COMMAND_ID_RESOPNSE;
		} else {
			mCommandId = COMMAND_ID_NORESPONSE;
		}
	}
	
	@Override
	public void request(OutputStream stream) throws Exception {
		try {
			super.request(stream);
			sendRequest(stream, mCommandId, 0, null, true);
			mCallback.requested(mThat);
			
			if(!mEnableResponse) {
				// dummy response
				mCallback.done(mCallback);
			}
		} catch(Common.CommonException e) {
			mCallback.error(e);
		} catch(Exception e) {
			mCallback.error(e);
			throw e;
		}
	}
	
	@Override
	public void response(InputStream stream, ICommandCancel cancel) throws Exception {
		try {
			super.response(stream, cancel);
			
			Protocol.ResponseHeader header = new Protocol.ResponseHeader();
			ByteBuffer bodyBuffer = receiveResponse(stream, mCommandId, header, cancel, true);
			
			if(mEnableResponse) {
				byte result = bodyBuffer.get();
				if(result != Common.ErrorCode.ERR_SUCCESS.getVal()) {
					Log.w(Common.TAG, "[" + getClass().toString() + "] Operation fail error: " + Common.ErrorCode.getKey(result).toString() + "(0x" + Integer.toHexString(result) + ")");
					throw new Common.CommonException("Operation fail", Common.ErrorCode.getKey(result));
				}
				
				try {
					mCallback.done(mThat);
				} catch(Exception e) {
					e.printStackTrace();
					mCallback.error(e);
				}			
			} else {
				Log.w(Common.TAG, "SetGcToOobeModeTask skip response part");
			}
		}
		catch(CommonException e) {
			if(mEnableResponse) {
				mCallback.error(e);
			}
		}
		catch(Exception e) {
			if(mEnableResponse) {
				mCallback.error(e);
			}
			throw e;
		}
	}

	@Override
	public void error(Exception e) {
		mCallback.error(e);
	}
}
