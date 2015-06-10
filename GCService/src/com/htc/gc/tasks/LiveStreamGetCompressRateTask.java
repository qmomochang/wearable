package com.htc.gc.tasks;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;

import android.util.Log;

import com.htc.gc.interfaces.Common;
import com.htc.gc.interfaces.IGCService.ICommandCancel;
import com.htc.gc.interfaces.ILiveViewer;
import com.htc.gc.interfaces.ILiveViewer.LiveStreamCompressRate;
import com.htc.gc.interfaces.ILiveViewer.LiveStreamCompressRateCallback;
import com.htc.gc.internal.GCTask;
import com.htc.gc.internal.Protocol;

public class LiveStreamGetCompressRateTask extends GCTask {
	private static final int COMMAND_ID = Protocol.SYS_GET_LIVESTREAM_COMPRESSRATE;
	
	private final ILiveViewer mThat;
	private final LiveStreamCompressRateCallback mCallback;
	
	public LiveStreamGetCompressRateTask(ILiveViewer that, LiveStreamCompressRateCallback callback) {
		mThat = that;
		mCallback = callback;
	}
	
	@Override
	public void request(OutputStream stream) throws Exception {
		try {
			super.request(stream);
			
			sendRequest(stream, COMMAND_ID, 0, null, true);
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
			ByteBuffer bodyBuffer = receiveResponse(stream, COMMAND_ID, header, cancel, true);
			
			byte result = bodyBuffer.get();
			if(result != Common.ErrorCode.ERR_SUCCESS.getVal()) {
				Log.w(Common.TAG, "[" + getClass().toString() + "] Operation fail error: " + Common.ErrorCode.getKey(result).toString() + "(0x" + Integer.toHexString(result) + ")");
				throw new Common.CommonException("Operation fail", Common.ErrorCode.getKey(result));
			}
			
			try {
				mCallback.result(mThat, LiveStreamCompressRate.getKey(bodyBuffer.get()));				
			} catch(Exception e) {
				e.printStackTrace();
				mCallback.error(e);
			}
		
		} catch(Common.CommonException e) {
			mCallback.error(e);
		} catch(Exception e) {
			mCallback.error(e);
			throw e;
		}
	}
	
	@Override
	public void error(Exception e) {
		mCallback.error(e);
	}
}
