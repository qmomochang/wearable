package com.htc.gc.tasks;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;

import android.util.Log;

import com.htc.gc.interfaces.Common;
import com.htc.gc.interfaces.IDeviceController;
import com.htc.gc.interfaces.IDeviceController.BatteryInfoCallback;
import com.htc.gc.interfaces.IGCService;
import com.htc.gc.internal.GCTask;
import com.htc.gc.internal.Protocol;

public class GetBatteryInfoTask extends GCTask {
	private static final int COMMAND_ID = Protocol.SYS_GET_BATTERY_LEVEL;

	private final IDeviceController mThat;
	private final BatteryInfoCallback mCallback;

	public GetBatteryInfoTask(IDeviceController that, BatteryInfoCallback callback) {
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

			byte level = bodyBuffer.get();
			boolean ac = (level & Protocol.BATTERY_LEVEL_AC_POWER) == Protocol.BATTERY_LEVEL_AC_POWER ? true: false;
			try {
				if(ac) {
					byte chargingLevel = bodyBuffer.get();
					mCallback.result(mThat, ac, chargingLevel);
				} else {
					mCallback.result(mThat, ac, level);					
				}				
			} catch(Exception e) {
				e.printStackTrace();
				mCallback.error(e);				
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
