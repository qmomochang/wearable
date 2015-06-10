package com.htc.gc.tasks;

import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import com.htc.gc.interfaces.Common;
import com.htc.gc.interfaces.Common.OperationCallback;
import com.htc.gc.interfaces.IDeviceController;
import com.htc.gc.internal.Protocol;
import com.htc.gc.internal.SimpleTask;

public class UpgradeFirmwareTask extends SimpleTask {
	private static final int COMMAND_ID = Protocol.SYS_FW_UPGRADE;
	
    private final byte mSelectFirmwareFlag;
	private final int mBootVersion;
	private final int mMainCodeVersion;
	private final int mMcuVersion;
	private final int mBleVersion;
	
	public UpgradeFirmwareTask(IDeviceController that, byte selectFirmwareFlag, int bootVersion, int mainCodeVersion, int mcuVersion, int bleVersion, OperationCallback callback) {
		super(that, COMMAND_ID, callback);
		
		mSelectFirmwareFlag = selectFirmwareFlag;
		mBootVersion = bootVersion;
		mMainCodeVersion = mainCodeVersion;
		mMcuVersion = mcuVersion;
		mBleVersion = bleVersion;
	}
	
	@Override
	public void request(OutputStream stream) throws Exception {
		try {
			super.requestInternal(stream);
			
			ByteBuffer bodyBuffer = ByteBuffer.allocate(17);
			bodyBuffer.order(ByteOrder.LITTLE_ENDIAN);
			bodyBuffer.put(mSelectFirmwareFlag);
			bodyBuffer.putInt(mBootVersion);
			bodyBuffer.putInt(mMainCodeVersion);
			bodyBuffer.putInt(mMcuVersion);
			bodyBuffer.putInt(mBleVersion);
			bodyBuffer.position(0);
			
			sendRequest(stream, mCommand, 0, bodyBuffer, true);
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
