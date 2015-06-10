package com.htc.gc.connectivity.v3.internal.tasks;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import com.htc.gc.connectivity.v3.interfaces.IGcConnectivityService;
import com.htc.gc.connectivity.v3.internal.callables.GcBleReceiveNotificationCallable;
import com.htc.gc.connectivity.v3.internal.callables.GcBleWriteCallable;
import com.htc.gc.connectivity.v3.internal.callables.GcBootUpCallable;
import com.htc.gc.connectivity.v3.internal.common.Common;
import com.htc.gc.connectivity.v3.internal.common.GcConnectivityTask;
import com.htc.gc.connectivity.v3.internal.component.le.GcBleGattAttributeUtil;
import com.htc.gc.connectivity.v3.internal.component.le.GcBleGattAttributes;
import com.htc.gc.connectivity.v3.internal.component.le.GcBleTransceiver;
import com.htc.gc.connectivity.v3.internal.component.wifi.GcWifiTransceiver;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.os.Bundle;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.text.TextUtils;
import android.util.Log;



public class GcFirmwareUpdateTask extends GcConnectivityTask {

	private final static String TAG = "GcFirmwareUpdateTask";
	
	private final static int RECEIVE_FIRMWARE_UPDATE_RESULT_TIMEOUT = 65000;
	private final static int TRIGGER_FW_UPDATE_RESULT_INDEX = 2;

	private BluetoothDevice mBluetoothDevice;
	private byte mUpdateType;
	private String mRTOSVersion;
	private String mModemVersion;
	private String mMCUVersion;

	public GcFirmwareUpdateTask(GcBleTransceiver gcBleTransceiver, GcWifiTransceiver gcWifiTransceiver, Messenger messenger, ExecutorService executor, BluetoothDevice device, boolean update_rtos, boolean update_modem, boolean update_mcu, String firmwareVersion) {

		super(gcBleTransceiver, gcWifiTransceiver, messenger, executor);
		mUpdateType = 0;
		mBluetoothDevice = device;
		if (update_rtos == true)
		{
			mUpdateType+=IGcConnectivityService.FWUpdatePart.UPDATE_RTOS.getPart();
		}

		if (update_modem == true)
		{
			mUpdateType+=IGcConnectivityService.FWUpdatePart.UPDATE_MODEM.getPart();
		}

		if (update_mcu == true)
		{
			mUpdateType+=IGcConnectivityService.FWUpdatePart.UPDATE_MCU.getPart();
		}
		
		parseVersion(firmwareVersion);
	}

	@Override
	public void execute() throws Exception {

		super.execute();
		super.from();
		
		if (TextUtils.isEmpty(mRTOSVersion) || TextUtils.isEmpty(mModemVersion) || TextUtils.isEmpty(mMCUVersion)) {
			Log.d(TAG, "[MGCC] invalid version");
			sendMessage(false, null);
		} else {
			BluetoothGattCharacteristic result;
			Future<BluetoothGattCharacteristic> future, futureA;
			Future<Integer> futureBoot;
			Integer bootResult;
	
			futureBoot = mExecutor.submit(new GcBootUpCallable(mGcBleTransceiver, mExecutor, mBluetoothDevice, IGcConnectivityService.BootUpType.BOOTUP_LINUX, mMessenger));
			bootResult = futureBoot.get();
	
			if (bootResult == Common.ERROR_SUCCESS)
			{
				futureA = mExecutor.submit(new GcBleReceiveNotificationCallable(mGcBleTransceiver, mBluetoothDevice, GcBleGattAttributes.GcV2CommandEnum.TRIGGER_FWUPDATE_RESULT_EVENT, RECEIVE_FIRMWARE_UPDATE_RESULT_TIMEOUT));
				future = mExecutor.submit(new GcBleWriteCallable(mGcBleTransceiver, mBluetoothDevice,GcBleGattAttributes.GcV2CommandEnum.TRIGGER_FWUPDATE_REQUEST, getTriggerFwUpdateData()));
				result = future.get();
				if (result != null) {
					result = futureA.get();
					if (result != null && (result.getValue().length > TRIGGER_FW_UPDATE_RESULT_INDEX)) {
						byte result_status = result.getValue()[TRIGGER_FW_UPDATE_RESULT_INDEX];
						IGcConnectivityService.TriggerFWUpdateResult status = IGcConnectivityService.TriggerFWUpdateResult.findError(result_status);
						if (status != null )
						{
							if(status.equals(IGcConnectivityService.TriggerFWUpdateResult.SUCCESS_TO_START))
								sendMessage(true, status);
							else
								sendMessage(false, status);
						}
						else {
							sendMessage(true, null);
						}
					} else {
						sendMessage(false, null);
					}
				} else {
					sendMessage(false, null);
				}
			}
			else {
				Log.d(TAG, "[MGCC] boot up is fail");
				sendMessage(false, null);
			}
		}
		super.to(TAG);
	}

	private void sendMessage(boolean result, IGcConnectivityService.TriggerFWUpdateResult error) {
		try {
			Message outMsg = Message.obtain();
			outMsg.what = IGcConnectivityService.CB_TRIGGER_FWUPDATE_RESULT;
			Bundle outData = new Bundle();

			if (result) {
				outData.putSerializable(IGcConnectivityService.PARAM_RESULT, IGcConnectivityService.Result.RESULT_SUCCESS);
			} else {
				outData.putSerializable(IGcConnectivityService.PARAM_RESULT, IGcConnectivityService.Result.RESULT_FAIL);
			}

			if (error != null)
				outData.putSerializable(IGcConnectivityService.PARAM_TRIGGER_FWUPDATE_RESULT, error);

			outMsg.setData(outData);
			mMessenger.send(outMsg);
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void error(Exception e) {

		sendMessage(false, null);
	}
	
	private void parseVersion(String firmwareVersion) {
		try {
			String trimmedFirmwareVersion = firmwareVersion.replace(".", "").replace("_", "");
			mMCUVersion = trimmedFirmwareVersion.substring(0, 4);
			mRTOSVersion = trimmedFirmwareVersion.substring(4, 10);
			mModemVersion = trimmedFirmwareVersion.substring(10);
			
			Log.d(TAG, "[MGCC] parse version ok, mcu:" + mMCUVersion + ", rtos:" + mRTOSVersion + ", modem:" + mModemVersion);
		} catch (Exception e) {
			Log.d(TAG, "[MGCC] parse version from " + firmwareVersion + " fail", e);
		}
	}
	
	private byte[] getTriggerFwUpdateData() {
		byte[] data = new byte[18];
		
		data[0] = mUpdateType;
		
		// rtos: 1-7
		int index = 1;
		for (byte b : getVersionData(mRTOSVersion)) {
			data[index++] = b;
		}
		
		// modem: 8-15
		index = 8;
		for (byte b : getVersionData(mModemVersion)) {
			data[index++] = b;
		}
		
		// mcu: 16-17
		index = 16;
		for (byte b : getVersionData(mMCUVersion)) {
			data[index++] = b;
		}
		
		return data;
	}
	
	private byte[] getVersionData(String version) {
		//FIXME following code only works when version's length is even (which is the current case)
		byte[] data = new byte[version.length() / 2];
		int srcIndex = 0;
		int digit1 = 0;
		int digit2 = 0;
		for (int i = 0 ; i < data.length; i++) {
			digit1 = Character.getNumericValue(version.charAt(srcIndex++));
			digit2 = Character.getNumericValue(version.charAt(srcIndex++));
			data[i] = (byte)(digit1 << 4 | digit2);
		}
		
		return data;
	}
}
