package com.htc.gc.connectivity.v3.internal.tasks;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import com.htc.gc.connectivity.v3.interfaces.IGcConnectivityService;
import com.htc.gc.connectivity.v3.internal.callables.GcBleReadCallable;
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
import android.util.Log;



public class GcGetAllFwVersionTask extends GcConnectivityTask {

	private final static String TAG = "GcGetAllFwVersionTask";
	private BluetoothDevice mBluetoothDevice;
	private final static int A12_FW_FIRST_INDEX = 1;
	private final static int A12_FW_LAST_INDEX  = 7;
	private final static int MOD_FW_LAST_INDEX = 15;
	private final static int MCU_FW_LAST_INDEX = 17;
	
	
	
	public GcGetAllFwVersionTask(GcBleTransceiver gcBleTransceiver, GcWifiTransceiver gcWifiTransceiver, Messenger messenger, ExecutorService executor, BluetoothDevice device) {

		super(gcBleTransceiver, gcWifiTransceiver, messenger, executor);
		
		mBluetoothDevice = device;
	}

	
	
	@Override
	public void execute() throws Exception {
		
		super.execute();
		
		super.from();
		Future<Integer> futureBoot;
		Integer bootResult;
		futureBoot = mExecutor.submit(new GcBootUpCallable(mGcBleTransceiver, mExecutor, mBluetoothDevice, IGcConnectivityService.BootUpType.BOOTUP_RTOS, mMessenger));
		bootResult = futureBoot.get();
		
		if (bootResult == Common.ERROR_SUCCESS)
		{
			BluetoothGattCharacteristic result = null;
			Future<BluetoothGattCharacteristic> futureA, futureB;
			byte[] tmp_date = new byte[0];

			futureA = mExecutor.submit(new GcBleReceiveNotificationCallable(mGcBleTransceiver, mBluetoothDevice, GcBleGattAttributes.GcV2CommandEnum.GET_VERSION_EVENT));
			futureB = mExecutor.submit(new GcBleWriteCallable(mGcBleTransceiver, mBluetoothDevice, GcBleGattAttributes.GcV2CommandEnum.GET_VERSION_EVENT, tmp_date));
			result = futureB.get();
			if ( result != null)
			{
				result = futureA.get();
				if (result != null)
				{
					if (result.getValue().length > MCU_FW_LAST_INDEX)
					{
						//Parse firmware version information
						byte[] all_version = result.getValue();
						String a12_fw_version = parse_a12_version(all_version);
						String modem_fw_version = parse_modem_version2(all_version);
						String mcu_fw_version = parse_mcu_version(all_version);
						sendMessage(true, a12_fw_version, modem_fw_version, mcu_fw_version);
					}
					else
					{
						Log.d(TAG, "[MGCC] invalid data length:" + result.getValue().length);
						sendMessage(false, null, null, null);
					}
				}
				else
				{
					Log.d(TAG, "[MGCC] receive notification fail");
					sendMessage(false, null, null, null);
				}
			}
			else
			{
				Log.d(TAG, "[MGCC] write fail");
				sendMessage(false, null, null, null);
			}
		}
		else
		{
			Log.d(TAG, "[MGCC] boot up is fail");
			sendMessage(false, null, null, null);
		}
		super.to(TAG);
	}
	
	
	
	private void sendMessage(boolean result, String a12_version, String mod_version, String mcu_version) {
		
		try {

			Message outMsg = Message.obtain();
			outMsg.what = IGcConnectivityService.CB_GET_ALL_FW_VERSION_RESULT;
			Bundle outData = new Bundle();
			
			if (result) {
				
				outData.putSerializable(IGcConnectivityService.PARAM_RESULT, IGcConnectivityService.Result.RESULT_SUCCESS);
				
			} else {
				
				outData.putSerializable(IGcConnectivityService.PARAM_RESULT, IGcConnectivityService.Result.RESULT_FAIL);
			}
			
			if (a12_version != null) {
				
				outData.putSerializable(IGcConnectivityService.PARAM_A12_FW_VERSION, a12_version);
			}

			if (mod_version != null) {
				
				outData.putSerializable(IGcConnectivityService.PARAM_MOD_FW_VERSION, mod_version);
			}

			if (mcu_version != null) {
				
				outData.putSerializable(IGcConnectivityService.PARAM_MCU_FW_VERSION, mcu_version);
			}

			outMsg.setData(outData);

			mMessenger.send(outMsg);

		} catch (RemoteException e) {

			e.printStackTrace();
		}
	}
	
	private String parse_a12_version(byte[] input)
	{
		//A12 version (x.xx.xxx.xxxxxxxx)
		String result = new String();

		for (int i = A12_FW_FIRST_INDEX; i <= A12_FW_LAST_INDEX; i ++)
		{
			if (input[i] == (byte)0xff)
				break;

			result = result + ((0xF0 & input[i])>>4);
			if (i == 1 || i == 2)
				result = result + ".";
			result = result + (0x0F & input[i]);
			if (i == 3)
				result = result + ".";
		}

		return result;
	}

	private String parse_modem_version(byte[] input)
	{
		//modem version (x.x.xxxxx@xxxxx_xx.xx)
		String result = new String();

		for (int i = (A12_FW_LAST_INDEX + 1); i <= MOD_FW_LAST_INDEX; i ++)
		{
			result = result + ((0xF0 & input[i])>>4);
			if (i == (A12_FW_LAST_INDEX + 1))
				result = result + ".";
            if (i == (A12_FW_LAST_INDEX + 4))
            	result = result + "@";
			result = result + (0x0F & input[i]);
			if (i == (A12_FW_LAST_INDEX + 1) || i == (A12_FW_LAST_INDEX + 7))
				result = result + ".";
			if (i == (A12_FW_LAST_INDEX + 6))
				result = result + "_";
		}

		return result;
	}
	
	private String parse_modem_version2(byte[] input)
	{
		//apps image version (A.B)
		//radio version (0.0.50362@RSTUV_00.01)
		//modem version (0ABRSTUV)
		
		int beginIndex = A12_FW_LAST_INDEX + 1;
		int A = (0x0F & input[beginIndex]);
		int B = (0xF0 & input[beginIndex + 1]) >> 4;
		int R = (0x0F & input[beginIndex + 1]);
		int S = (0xF0 & input[beginIndex + 2]) >> 4;
		int T = (0x0F & input[beginIndex + 2]);
		int U = (0xF0 & input[beginIndex + 3]) >> 4;
		int V = (0x0F & input[beginIndex + 3]);
		
		StringBuilder result = new StringBuilder("0");
		result.append(A);
		result.append(B);
		result.append(R);
		result.append(S);
		result.append(T);
		result.append(U);
		result.append(V);
		
		return result.toString();
	}
	
	private String parse_mcu_version(byte[] input)
	{
		//MCU version (x.x.xx)
		String result = new String();

		for (int i = (MOD_FW_LAST_INDEX+1); i <= MCU_FW_LAST_INDEX; i ++)
		{
			result = result + ((0xF0 & input[i])>>4);
			if (i == (MOD_FW_LAST_INDEX+1))
				result = result + ".";
			result = result + (0x0F & input[i]);
			if (i == (MOD_FW_LAST_INDEX+1))
				result = result + ".";
		}

		return result;
	}
	
	@Override
	public void error(Exception e) {

		sendMessage(false, null, null, null);
	}
}
