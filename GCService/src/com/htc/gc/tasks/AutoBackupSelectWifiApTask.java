package com.htc.gc.tasks;

import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import com.htc.gc.interfaces.Common;
import com.htc.gc.interfaces.Common.OperationCallback;
import com.htc.gc.interfaces.IAutoBackuper;
import com.htc.gc.interfaces.IAutoBackuper.SecurityType;
import com.htc.gc.internal.Protocol;
import com.htc.gc.internal.SimpleTask;

@Deprecated
public class AutoBackupSelectWifiApTask extends SimpleTask {
	private static final int COMMAND_ID = Protocol.AUTOBACKUP_SET_AP_CONFIG;
	
	private final SecurityType mSecurity;
	private final String mSSID;
	private final String mKey;
	
	public AutoBackupSelectWifiApTask(IAutoBackuper that, SecurityType security, String ssid, String key, OperationCallback callback) {
		super(that, COMMAND_ID, callback);
		
		mSecurity = security;
		mSSID = ssid;
		mKey = key;
	}

	@Override
	public void request(OutputStream stream) throws Exception {
		try {
			super.requestInternal(stream);
			
			ByteBuffer bodyBuffer = ByteBuffer.allocate(1+IAutoBackuper.SSID_LEN+IAutoBackuper.KEY_LEN);
			bodyBuffer.order(ByteOrder.LITTLE_ENDIAN);
			
			bodyBuffer.put(mSecurity.getVal());
			
			bodyBuffer.put(mSSID.getBytes("UTF-8"));
			for(int i = 0; i < (IAutoBackuper.SSID_LEN - mSSID.getBytes("UTF-8").length); i++) {
				bodyBuffer.put((byte) 0);
			}
			
			bodyBuffer.put(mKey.getBytes("UTF-8"));
			for(int i = 0; i < (IAutoBackuper.KEY_LEN - mKey.getBytes("UTF-8").length); i++) {
				bodyBuffer.put((byte) 0);
			}

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
