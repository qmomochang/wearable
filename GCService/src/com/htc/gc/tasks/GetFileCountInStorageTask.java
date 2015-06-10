package com.htc.gc.tasks;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;

import android.util.Log;

import com.htc.gc.interfaces.Common;
import com.htc.gc.interfaces.Common.CommonException;
import com.htc.gc.interfaces.Common.Filter;
import com.htc.gc.interfaces.IDeviceController;
import com.htc.gc.interfaces.IDeviceController.StorageFileCountCallback;
import com.htc.gc.interfaces.IGCService;
import com.htc.gc.internal.GCTask;
import com.htc.gc.internal.Protocol;

public class GetFileCountInStorageTask extends GCTask {
	private static final int COMMAND_ID = Protocol.FILE_GET_STORAGE_COUNTS;
	
	private final IDeviceController mThat;
	private final StorageFileCountCallback mCallback;
	private final Filter mFilter;
	public GetFileCountInStorageTask(IDeviceController that, Filter filter, StorageFileCountCallback callback) {
		mThat = that;
		mCallback = callback;
		
		mFilter = filter;
	}
	
	@Override
	public void request(OutputStream stream) throws Exception {
		try {
			super.request(stream);
			
			ByteBuffer bodyBuffer = ByteBuffer.allocate(1);
			bodyBuffer.put(mFilter.getVal());
			
			bodyBuffer.position(0);
			sendRequest(stream, COMMAND_ID, 0, bodyBuffer, true);
		} catch(Exception e) {
			mCallback.error(e);
			throw e;
		}	
	}
	
	@Override
	public void response(InputStream stream, IGCService.ICommandCancel cancel) throws Exception {
		try {
			super.response(stream, cancel);
			
			Protocol.ResponseHeader header = new Protocol.ResponseHeader();
			ByteBuffer bodyBuffer = receiveResponse(stream, COMMAND_ID, header, cancel, true);
			
			byte result = bodyBuffer.get();
			if(result != Common.ErrorCode.ERR_SUCCESS.getVal()) {
				Log.w(Common.TAG, "[" + getClass().toString() + "] Operation fail error: " + Common.ErrorCode.getKey(result).toString() + "(0x" + Integer.toHexString(result) + ")");
				throw new Common.CommonException("Operation fail", Common.ErrorCode.getKey(result));
			}
		
			mCallback.result(mThat, mFilter, bodyBuffer.getInt());
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
	public void error(Exception e) {
		mCallback.error(e);
	}

}
