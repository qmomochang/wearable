package com.htc.gc.tasks;

import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import com.htc.gc.interfaces.Common;
import com.htc.gc.interfaces.Common.OperationCallback;
import com.htc.gc.interfaces.IAutoBackuper;
import com.htc.gc.interfaces.IAutoBackuper.ProviderType;
import com.htc.gc.interfaces.IAutoBackuper.TokenType;
import com.htc.gc.internal.Protocol;
import com.htc.gc.internal.SimpleTask;

public class AutoBackupSetProviderTask extends SimpleTask {
	private static final int COMMAND_ID = Protocol.AUTOBACKUP_SET_PROVIDER;
	
	private final ProviderType mProvider;
	private final TokenType mTokenType;
	private final String mToken;
	
	public AutoBackupSetProviderTask(IAutoBackuper that, ProviderType provider, TokenType tokenType, String token, OperationCallback callback) {
		super(that, COMMAND_ID, callback);
		
		mProvider = provider;
		mTokenType = tokenType;
		mToken = token;
	}

	@Override
	public void request(OutputStream stream) throws Exception {
		try {
			super.requestInternal(stream);

			ByteBuffer bodyBuffer = ByteBuffer.allocate(1+1+mToken.getBytes().length+2);
			bodyBuffer.order(ByteOrder.LITTLE_ENDIAN);
			
			bodyBuffer.put(mProvider.getVal());
			bodyBuffer.put(mTokenType.getVal());
			bodyBuffer.put(mToken.getBytes("UTF-8"));
			bodyBuffer.putChar('\0');

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
