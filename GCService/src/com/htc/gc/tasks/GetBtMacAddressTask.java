package com.htc.gc.tasks;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;

import android.util.Log;

import com.htc.gc.interfaces.Common;
import com.htc.gc.interfaces.IDeviceController;
import com.htc.gc.interfaces.IDeviceController.GetBtMacAddressCallback;
import com.htc.gc.interfaces.IGCService;
import com.htc.gc.internal.GCTask;
import com.htc.gc.internal.Protocol;

public class GetBtMacAddressTask extends GCTask {
	private static final int COMMAND_ID = Protocol.SYS_GET_BLE_MACADDRESS;
	
	private static final int MAC_ADDRESS_LEN = 18;
	
	private final IDeviceController mThat;
	private final GetBtMacAddressCallback mCallback;

	public GetBtMacAddressTask(IDeviceController that, GetBtMacAddressCallback callback) {
		mThat = that;
		mCallback = callback;
	}

	@Override
	public void request(OutputStream stream) throws Exception {
		try {
			super.request(stream);
			sendRequest(stream, COMMAND_ID, 0, null, true);
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
			
			byte[] bufAddress = new byte[MAC_ADDRESS_LEN];
			bodyBuffer.get(bufAddress);
			int strlen = 0;
			for(int j = 0; j < MAC_ADDRESS_LEN; j++) {
				if(bufAddress[j] == 0) {
					strlen = j;
					break;
				}
			}
			
			mCallback.result(mThat, new String(bufAddress, 0, strlen, "UTF-8"));
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
