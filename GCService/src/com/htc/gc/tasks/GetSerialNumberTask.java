package com.htc.gc.tasks;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;

import android.util.Log;

import com.htc.gc.interfaces.Common;
import com.htc.gc.interfaces.IDeviceController;
import com.htc.gc.interfaces.IDeviceController.GetSerialNumberCallback;
import com.htc.gc.interfaces.IGCService;
import com.htc.gc.internal.GCTask;
import com.htc.gc.internal.Protocol;

public class GetSerialNumberTask extends GCTask {
	private static final int COMMAND_ID = Protocol.SYS_GET_SERIAL_NUMBER;

	private final IDeviceController mThat;
	private final GetSerialNumberCallback mCallback;

	public GetSerialNumberTask(IDeviceController that, GetSerialNumberCallback callback) {
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
			ByteBuffer bodyBuffer = receiveResponse(stream, COMMAND_ID, header, cancel, Common.DUMP_SENSITIVEDATA);

			byte result = bodyBuffer.get();
			if(result != Common.ErrorCode.ERR_SUCCESS.getVal()) {
				Log.w(Common.TAG, "[" + getClass().toString() + "] Operation fail error: " + Common.ErrorCode.getKey(result).toString() + "(0x" + Integer.toHexString(result) + ")");
				throw new Common.CommonException("Operation fail", Common.ErrorCode.getKey(result));
			}
			
			byte[] bufSerialNum = new byte[IDeviceController.SERIAL_NUMBER_LEN];
			bodyBuffer.get(bufSerialNum, 0, IDeviceController.SERIAL_NUMBER_LEN);
			
			int modelNameLen = 0;
			for(int j = 0; j < IDeviceController.SERIAL_NUMBER_LEN; j++) {
				if(bufSerialNum[j] == 0) {
					break;
				} else {
					modelNameLen++;
				}
			}
			String modelName = new String(bufSerialNum, 0, modelNameLen, "UTF-8");
			
			int serialNumberLen = 0;
			for(int k = modelNameLen+1; k < IDeviceController.SERIAL_NUMBER_LEN; k++) {
				if(bufSerialNum[k] == 0) {
					break;
				} else {
					serialNumberLen++;					
				}
			}
			String serialNumber = new String(bufSerialNum, modelNameLen+1, serialNumberLen, "UTF-8");
			mCallback.result(mThat, modelName, serialNumber);
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
