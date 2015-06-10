package com.htc.gc.tasks;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;

import android.util.Log;

import com.htc.gc.interfaces.Common;
import com.htc.gc.interfaces.IGCService.ICommandCancel;
import com.htc.gc.interfaces.IStillCapturer;
import com.htc.gc.interfaces.IStillCapturer.ImageRatio;
import com.htc.gc.interfaces.IStillCapturer.ImgRatioCallback;
import com.htc.gc.internal.GCTask;
import com.htc.gc.internal.Protocol;

public class CaptureImageGetRatioTask extends GCTask {
	private static final int COMMAND_ID = Protocol.CAPTURE_GET_IMG_RATIO;
	
	private final IStillCapturer mThat;
	private final ImgRatioCallback mCallback;
	
	public CaptureImageGetRatioTask(IStillCapturer that, ImgRatioCallback callback) {
		mThat = that;
		mCallback = callback;
	}
	
	@Override
	public void request(OutputStream stream) throws Exception {
		try {
			super.request(stream);

			sendRequest(stream, COMMAND_ID, 0, null, true);
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
			
			byte ratio = bodyBuffer.get();
		
			mCallback.result(mThat, ImageRatio.getKey(ratio));
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
