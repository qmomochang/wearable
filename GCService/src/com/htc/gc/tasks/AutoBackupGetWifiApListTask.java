package com.htc.gc.tasks;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;

import android.util.Log;

import com.htc.gc.interfaces.Common;
import com.htc.gc.interfaces.IAutoBackuper;
import com.htc.gc.interfaces.IAutoBackuper.AutoBackupWifiApListCallback;
import com.htc.gc.interfaces.IAutoBackuper.ScanMode;
import com.htc.gc.interfaces.IAutoBackuper.SecurityType;
import com.htc.gc.interfaces.IAutoBackuper.WifiApInfo;
import com.htc.gc.interfaces.IGCService.ICommandCancel;
import com.htc.gc.internal.GCTask;
import com.htc.gc.internal.Protocol;

public class AutoBackupGetWifiApListTask extends GCTask {
	private static final int COMMAND_ID = Protocol.AUTOBACKUP_GET_SCANLIST;
	
	private final IAutoBackuper mThat;
	private final ScanMode mMode;
	private final AutoBackupWifiApListCallback mCallback;
	
	public AutoBackupGetWifiApListTask(IAutoBackuper that, ScanMode mode, AutoBackupWifiApListCallback callback) {
		mThat = that;
		mMode = mode;
		mCallback = callback;
	}
	
	@Override
	public void request(OutputStream stream) throws Exception {
		try {
			super.request(stream);

			ByteBuffer bodyBuffer = ByteBuffer.allocate(1);
			bodyBuffer.put(mMode.getVal());
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
			
			int apCount = bodyBuffer.remaining() / (IAutoBackuper.SSID_LEN + IAutoBackuper.RSSI_LEN + 1 /* Security */);

			ArrayList<WifiApInfo> list = new ArrayList<WifiApInfo>();
			int strlen = IAutoBackuper.SSID_LEN;
			for(int i = 0; i < apCount; i++) {
				byte[] bufSSID = new byte[IAutoBackuper.SSID_LEN];
				bodyBuffer.get(bufSSID, 0, IAutoBackuper.SSID_LEN);
				for(int j = 0; j < IAutoBackuper.SSID_LEN; j++) {
					if(bufSSID[j] == 0) {
						strlen = j;
						break;
					}
				}
											
				list.add(new WifiApInfo(new String(bufSSID, 0, strlen, "UTF-8"), bodyBuffer.getShort(), SecurityType.getKey(bodyBuffer.get()), bodyBuffer.get() == IAutoBackuper.AUTHORIZATED));
			}
			
			mCallback.result(mThat, list);
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
