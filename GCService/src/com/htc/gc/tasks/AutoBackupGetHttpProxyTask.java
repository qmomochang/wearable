package com.htc.gc.tasks;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import android.util.Log;

import com.htc.gc.interfaces.Common;
import com.htc.gc.interfaces.IAutoBackuper;
import com.htc.gc.interfaces.IAutoBackuper.AutoBackupGetHttpProxyCallback;
import com.htc.gc.interfaces.IGCService.ICommandCancel;
import com.htc.gc.internal.GCTask;
import com.htc.gc.internal.Protocol;

@Deprecated
public class AutoBackupGetHttpProxyTask extends GCTask {
	private static final int COMMAND_ID = Protocol.AUTOBACKUP_GET_HTTP_PROXY;
	
	private final IAutoBackuper mThat;
	private final String mSSID;
	private final AutoBackupGetHttpProxyCallback mCallback;
	
	public AutoBackupGetHttpProxyTask(IAutoBackuper that, String ssid, AutoBackupGetHttpProxyCallback callback) {
		mThat = that;
		mSSID = ssid;
		mCallback = callback;
	}
	
	@Override
	public void request(OutputStream stream) throws Exception {
		try {
			super.request(stream);
		
			ByteBuffer bodyBuffer = ByteBuffer.allocate(IAutoBackuper.SSID_LEN);
			bodyBuffer.order(ByteOrder.LITTLE_ENDIAN);
			bodyBuffer.put(mSSID.getBytes("UTF-8"));
			for(int i = 0; i < (IAutoBackuper.SSID_LEN - mSSID.getBytes("UTF-8").length); i++) {
				bodyBuffer.put((byte) 0);
			}
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
			
			int strlen = IAutoBackuper.PROXY_NAME_LEN;
			byte[] bufName = new byte[IAutoBackuper.PROXY_NAME_LEN];
			bodyBuffer.get(bufName, 0, IAutoBackuper.PROXY_NAME_LEN);
			for(int j = 0; j < IAutoBackuper.PROXY_NAME_LEN; j++) {
				if(bufName[j] == 0) {
					strlen = j;
					break;
				}
			}
			
			int port = (int)bodyBuffer.getChar(); // unsigned short
			
			mCallback.result(mThat, new String(bufName, 0, strlen, "UTF-8"), port);
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
