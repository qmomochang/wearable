package com.htc.gc.tasks;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.Calendar;

import android.util.Log;

import com.htc.gc.interfaces.Common;
import com.htc.gc.interfaces.IAutoBackuper;
import com.htc.gc.interfaces.IAutoBackuper.AutoBackupStatusCallback;
import com.htc.gc.interfaces.IAutoBackuper.ProviderType;
import com.htc.gc.interfaces.IGCService.ICommandCancel;
import com.htc.gc.internal.GCTask;
import com.htc.gc.internal.Protocol;

public class AutoBackupGetCurrentStatusTask extends GCTask {
	private static final int COMMAND_ID = Protocol.AUTOBACKUP_GET_INFO;
	
	private final IAutoBackuper mThat;
	private final AutoBackupStatusCallback mCallback;
	
	public AutoBackupGetCurrentStatusTask(IAutoBackuper that, AutoBackupStatusCallback callback) {
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

			ProviderType provider = ProviderType.getKey(bodyBuffer.get());
			int	count = bodyBuffer.getInt();
			short time = bodyBuffer.getShort();
			short date = bodyBuffer.getShort();
			
			Calendar dateTime = Calendar.getInstance();
			dateTime.set(Calendar.YEAR, ((date & 0xFE00) >> 9) + 1980);
			dateTime.set(Calendar.MONTH, ((date & 0x01E0) >> 5) - 1);
			dateTime.set(Calendar.DAY_OF_MONTH, date & 0x001F);

			dateTime.set(Calendar.HOUR_OF_DAY, (time & 0xF800) >> 11);
			dateTime.set(Calendar.MINUTE, (time & 0x07E0) >> 5);
			dateTime.set(Calendar.SECOND, ((time & 0x001F) >> 0) << 1);
			dateTime.set(Calendar.MILLISECOND, 0);
			
			mCallback.result(mThat, provider, count, dateTime);
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
