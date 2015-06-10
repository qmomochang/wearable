package com.htc.gc.tasks;

import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import com.htc.gc.interfaces.Common;
import com.htc.gc.interfaces.IAutoBackuper;
import com.htc.gc.interfaces.Common.OperationCallback;
import com.htc.gc.internal.Protocol;
import com.htc.gc.internal.SimpleTask;

@Deprecated
public class AutoBackupDeselectWifiApTask extends SimpleTask {
	private static final int COMMAND_ID = Protocol.AUTOBACKUP_ERASE_AP_AUTH;
	
	private final String mSSID;
	
	public AutoBackupDeselectWifiApTask(IAutoBackuper that, String ssid, OperationCallback callback) {
		super(that, COMMAND_ID, callback);

		mSSID = ssid;
	}

	@Override
	public void request(OutputStream stream) throws Exception {
		try {
			super.requestInternal(stream);
			
			ByteBuffer bodyBuffer = ByteBuffer.allocate(IAutoBackuper.SSID_LEN);
			bodyBuffer.order(ByteOrder.LITTLE_ENDIAN);
			
			bodyBuffer.put(mSSID.getBytes("UTF-8"));
			for(int i = 0; i < (IAutoBackuper.SSID_LEN - mSSID.getBytes("UTF-8").length); i++) {
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
