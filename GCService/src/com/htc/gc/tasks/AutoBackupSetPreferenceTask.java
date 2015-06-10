package com.htc.gc.tasks;

import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import com.htc.gc.interfaces.Common;
import com.htc.gc.interfaces.Common.OperationCallback;
import com.htc.gc.interfaces.IAutoBackuper;
import com.htc.gc.interfaces.IAutoBackuper.OptionCheck;
import com.htc.gc.internal.Protocol;
import com.htc.gc.internal.SimpleTask;

public class AutoBackupSetPreferenceTask extends SimpleTask {
	private static final int COMMAND_ID = Protocol.AUTOBACKUP_SET_PREFERENCE;
	
	private final OptionCheck mEnableAutoBackup;
	private final OptionCheck mBackupWhenACPluggedIn;
	private final OptionCheck mDeleteAfterBackingup;
	
	public AutoBackupSetPreferenceTask(IAutoBackuper that, OptionCheck enableAutoBackup, OptionCheck backupWhenACPluggedIn, OptionCheck deleteAfterBackingup, OperationCallback callback) {
		super(that, COMMAND_ID, callback);
		
		mEnableAutoBackup = enableAutoBackup;
		mBackupWhenACPluggedIn = backupWhenACPluggedIn;
		mDeleteAfterBackingup = deleteAfterBackingup;
	}

	@Override
	public void request(OutputStream stream) throws Exception {
		try {
			super.requestInternal(stream);
			
			ByteBuffer bodyBuffer = ByteBuffer.allocate(3);
			bodyBuffer.order(ByteOrder.LITTLE_ENDIAN);
			
			bodyBuffer.put(mEnableAutoBackup.getVal());
			bodyBuffer.put(mBackupWhenACPluggedIn.getVal());
			bodyBuffer.put(mDeleteAfterBackingup.getVal());

			bodyBuffer.position(0);
			sendRequest(stream, COMMAND_ID, 0, bodyBuffer, true);
		}
		catch(Common.CommonException e) {
			mCallback.error(e);
		}
		catch(Exception e) {
			mCallback.error(e);
			throw e;
		}		
	}
}
