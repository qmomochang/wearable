package com.htc.gc.tasks;

import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import com.htc.gc.interfaces.Common;
import com.htc.gc.interfaces.IAutoBackuper;
import com.htc.gc.interfaces.Common.OperationCallback;
import com.htc.gc.internal.Protocol;
import com.htc.gc.internal.SimpleTask;

public class AutoBackupSetAccountTask extends SimpleTask {
	private static final int COMMAND_ID = Protocol.AUTOBACKUP_SET_CLOUD_ACCOUNT;
	
	private final String mAccount;
	
	public AutoBackupSetAccountTask(IAutoBackuper that, String account, OperationCallback callback) {
		super(that, COMMAND_ID, callback);
		
		mAccount = account;
	}
	
	@Override
	public void request(OutputStream stream) throws Exception {
		try {
			super.requestInternal(stream);
			
			ByteBuffer bodyBuffer = ByteBuffer.allocate(IAutoBackuper.ACCOUNT_LEN);
			bodyBuffer.order(ByteOrder.LITTLE_ENDIAN);
			
			bodyBuffer.put(mAccount.getBytes("UTF-8"));
			bodyBuffer.put((byte) 0);
			
			bodyBuffer.position(0);
			sendRequest(stream, COMMAND_ID, 0, bodyBuffer, Common.DUMP_SENSITIVEDATA);
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
