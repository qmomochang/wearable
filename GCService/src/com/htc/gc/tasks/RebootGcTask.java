package com.htc.gc.tasks;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;

import android.util.Log;

import com.htc.gc.interfaces.Common;
import com.htc.gc.interfaces.Common.RequestCallback;
import com.htc.gc.interfaces.IGCService;
import com.htc.gc.interfaces.IGCService.ICommandCancel;
import com.htc.gc.internal.GCTask;
import com.htc.gc.internal.Protocol;

public class RebootGcTask extends GCTask {
	private static final int COMMAND_ID_NORESPONSE = Protocol.SYS_REQUEST_REBOOT;
	private static final int COMMAND_ID_RESPONSE = Protocol.SYS_REQUEST_REBOOT_RESPONSE;

	private final IGCService mThat;
	private final RequestCallback mCallback;
	private final int mCommandId;
	private final boolean mEnableResponse;
	
	public RebootGcTask(IGCService that, RequestCallback callback, boolean enableResponse) {
		mThat = that;
		mCallback = callback;
		mEnableResponse = enableResponse;
		
		if(enableResponse) {
			mCommandId = COMMAND_ID_RESPONSE;
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
				mCallback.done(mThat);
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
			Log.w(Common.TAG, "RebootGcTask skip response part");
		}

	}

	@Override
	public void error(Exception e) {
		mCallback.error(e);
	}
}
