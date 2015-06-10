package com.htc.gc.tasks;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;

import android.util.Log;

import com.htc.gc.interfaces.Common;
import com.htc.gc.interfaces.IAutoBackuper;
import com.htc.gc.interfaces.IAutoBackuper.AutoBackupGetAccountCallback;
import com.htc.gc.interfaces.IGCService.ICommandCancel;
import com.htc.gc.internal.GCTask;
import com.htc.gc.internal.Protocol;

public class AutoBackupGetAccountTask extends GCTask {
	private static final int COMMAND_ID = Protocol.AUTOBACKUP_GET_CLOUD_ACCOUNT;
	
	private final IAutoBackuper mThat;
	private final AutoBackupGetAccountCallback mCallback;
	
	public AutoBackupGetAccountTask(IAutoBackuper that, AutoBackupGetAccountCallback callback) {
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
			ByteBuffer bodyBuffer = receiveResponse(stream, COMMAND_ID, header, cancel, Common.DUMP_SENSITIVEDATA);

			byte result = bodyBuffer.get();
			if(result != Common.ErrorCode.ERR_SUCCESS.getVal()) {
				Log.w(Common.TAG, "[" + getClass().toString() + "] Operation fail error: " + Common.ErrorCode.getKey(result).toString() + "(0x" + Integer.toHexString(result) + ")");
				throw new Common.CommonException("Operation fail", Common.ErrorCode.getKey(result));
			}
			
			if(bodyBuffer.remaining() > 1) {
				int strlen = bodyBuffer.remaining() - 1;
				byte[] bufAccount = new byte[strlen];
				bodyBuffer.get(bufAccount);
				
				mCallback.result(mThat, new String(bufAccount, 0, strlen, "UTF-8"));				
			} else {
				mCallback.result(mThat, "");
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
