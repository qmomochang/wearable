package com.htc.gc.connectivity.v3.internal.callables;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import com.htc.gc.connectivity.v3.interfaces.IGcConnectivityService;
import com.htc.gc.connectivity.v3.internal.common.Common;
import com.htc.gc.connectivity.v3.internal.component.le.GcBleGattAttributeUtil;
import com.htc.gc.connectivity.v3.internal.component.le.GcBleGattAttributes;
import com.htc.gc.connectivity.v3.internal.component.le.GcBleTransceiver;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.os.Bundle;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;



public class GcBootUpCallable implements Callable<Integer> {

	private final static String TAG = "GcBootUpCallable";
	private final static boolean bPerformanceNotify = true;
	
	private final static int DEFAULT_RETRY_TIMES = 5;
	
	protected ExecutorService mExecutor;
	protected GcBleTransceiver mGcBleTransceiver;
	protected BluetoothDevice mBluetoothDevice;
	protected long mTimePrev;
	
	private int mRetryTimes = DEFAULT_RETRY_TIMES;
	private IGcConnectivityService.BootUpType mBootupOS;
	protected Messenger mMessenger;
	
	
	public GcBootUpCallable(GcBleTransceiver gcBleTransceiver, ExecutorService executor, BluetoothDevice device, IGcConnectivityService.BootUpType bootos, Messenger messenger) {

		mGcBleTransceiver = gcBleTransceiver;
		mExecutor = executor;
		mBluetoothDevice = device;
		mBootupOS = bootos;
		mMessenger = messenger;
	}



	@Override
	public Integer call() throws Exception {

		from();
		
/*		Integer ret = Common.ERROR_BOOT_UP_GC;

		if (mGcBleTransceiver != null) {
			
			mRetryTimes = DEFAULT_RETRY_TIMES;

			int versionBle = -1;
			
			/// Version protect, can remove later.
			GcConnectivityDevice gcDevice = mGcBleTransceiver.getGcConnectivityDeviceGroup().getDevice(mBluetoothDevice);
			if (gcDevice != null) {

				versionBle = gcDevice.getVersionBle();
			}
			
			do {

				if ((gcDevice != null) && (gcDevice.getGcStateBle() != GcStateBle.GCSTATE_BLE_CONNECTED)) {
					
					Log.d(TAG, "[MGCC] GC bootUp fail because BLE is disconnected");
					return Common.ERROR_BOOT_UP_GC;
				}
				
				ret = bootUp();
				
				if (ret == Common.ERROR_SUCCESS) {
					
					mRetryTimes = 0;
					
				} else {

					mRetryTimes--;

					Log.d(TAG, "[MGCC] GC bootUp mRetryTimes = " + mRetryTimes);
				}

			} while (mRetryTimes > 0);
		}*/
		Integer ret = bootUp();
		to(TAG);
		
		return ret;
	}
	
	
	
	private Integer bootUp() throws Exception {
		
		BluetoothGattCharacteristic result;
		Future<BluetoothGattCharacteristic> futureA, futureC, futureD;
		
		futureA = mExecutor.submit(new GcBleReceiveNotificationCallable(mGcBleTransceiver, mBluetoothDevice, GcBleGattAttributes.GcV2CommandEnum.POWER_ON_STATUS_EVENT, 30000));
		
		byte[] bootGcArray = new byte[2];
	    bootGcArray[0] = (byte) 0x01;
	    bootGcArray[1] = (byte) mBootupOS.getType();
		futureC = mExecutor.submit(new GcBleWriteCallable(mGcBleTransceiver, mBluetoothDevice, GcBleGattAttributes.GcV2CommandEnum.POWER_ON_REQUEST, bootGcArray));
		if (futureC.get() == null) {

			Log.d(TAG, "[MGCC] Boot up GC fails: " + Common.ERROR_GATT_WRITE);
			return Common.ERROR_BOOT_UP_GC;
		}

		result = futureA.get();
		if (result != null) {
				if (GcBleGattAttributeUtil.isFirmwareUpdating(result))
				{
					Log.d(TAG, "[MGCC] Firmware updating+");
					futureD = mExecutor.submit(new GcBleReceiveNotificationCallable(mGcBleTransceiver, mBluetoothDevice, GcBleGattAttributes.GcV2CommandEnum.LAST_FWUPDATE_RESULT_EVENT, 300000));
					result = futureD.get();
					if (result != null)
					{
						byte[] resultValue = result.getValue();
						while ((resultValue[1] != (byte) 4) && (resultValue[1] != (byte) 5))
						{
							Log.d(TAG, "[MGCC] Firmware updating:" + resultValue[1]);
							futureD = mExecutor.submit(new GcBleReceiveNotificationCallable(mGcBleTransceiver, mBluetoothDevice, GcBleGattAttributes.GcV2CommandEnum.LAST_FWUPDATE_RESULT_EVENT, 300000));
							result = futureD.get();
							if (result != null)
							{
								resultValue = result.getValue();
							}
							else
							{
								Log.d(TAG, "[MGCC] Firmware updating no response");
								
								sendFwUpdateResultMessage(false);
								
								return Common.ERROR_FW_UPDATE_FAIL;
							}
						}
						
						Log.d(TAG, "[MGCC] Firmware updating-");
						Log.d(TAG, "[MGCC] After sending command, GC is booted up!!");
						Log.d(TAG, "[MGCC] Last firmware update status:" + result.getValue()[1] );
						
						sendFwUpdateResultMessage(true);
						
						return Common.ERROR_FW_UPDATE_SUCCESS;
						
					} else {
						Log.d(TAG, "[MGCC] Firmware update begin no response");
						
						sendFwUpdateResultMessage(false);
						
						return Common.ERROR_FW_UPDATE_FAIL;
					}
					
				}
				else
				{
					if (mBootupOS.equals(IGcConnectivityService.BootUpType.BOOTUP_RTOS) && GcBleGattAttributeUtil.isBootUpRTOSReady(result))
					{
						Log.d(TAG, "[MGCC] After sending command, GC rtos boots up!!");
						return Common.ERROR_SUCCESS;
					}
					else if (mBootupOS.equals(IGcConnectivityService.BootUpType.BOOTUP_LINUX) && GcBleGattAttributeUtil.isBootUpLinuxReady(result))
					{
						Log.d(TAG, "[MGCC] After sending command, GC linux boots up!!");
						return Common.ERROR_SUCCESS;
					}
					else
					{
						Log.d(TAG, "[MGCC] After sending command, GC boot up is failed!!");
					}

				}
		} else {
			
			Log.d(TAG, "[MGCC] Boot up GC fails TIMEOUT: " + Common.ERROR_GATT_RECEIVE_NOTIFICATION);
		}
		
		return Common.ERROR_BOOT_UP_GC;
	}
	
	
	/*
	private Integer bootUpOld() throws Exception {
		
		Integer ret = Common.ERROR_BOOT_UP_GC;
		
		BluetoothGattCharacteristic result;
		Future<BluetoothGattCharacteristic> futureA, futureB, futureC;
		
		futureA = mExecutor.submit(new GcBleReadCallable(mGcBleTransceiver, mBluetoothDevice, GcBleGattAttributes.GC_BOOT_UP_READY));
		result = futureA.get();
		
		if (result != null) {
			
			if (GcBleGattAttributeUtil.isBootUpReady(result)) {
				
				Log.d(TAG, "[MGCC] GC is already boot up!!");

				ret = Common.ERROR_SUCCESS;
				
			} else {
				
				Log.d(TAG, "[MGCC] GC is standby!!");

				futureA = mExecutor.submit(new GcBleReceiveNotificationCallable(mGcBleTransceiver, mBluetoothDevice, GcBleGattAttributes.GC_BOOT_UP_READY, 2500));
				///futureB = mExecutor.submit(new GcBleSetNotificationCallable(mGcBleTransceiver, mBluetoothDevice, GcBleGattAttributes.GC_BOOT_UP_READY, true));
				///if (futureB.get() == null) {
				
					///unregisterNotify(GcBleGattAttributes.GC_BOOT_UP_READY);
					///return Common.ERROR_GATT_SET_NOTIFICATION;
				///}
				
				byte[] bootGcArray = {(byte) 0x01};
				futureC = mExecutor.submit(new GcBleReliableWriteCallable(mGcBleTransceiver, mExecutor, mBluetoothDevice, GcBleGattAttributes.GC_BOOT_UP_GC, bootGcArray));
				if (futureC.get() == null) {

					///unregisterNotify(GcBleGattAttributes.GC_BOOT_UP_READY);
					Log.d(TAG, "[MGCC] Boot up GC fails: " + Common.ERROR_GATT_WRITE);
					return Common.ERROR_BOOT_UP_GC;
				}
				
				result = futureA.get();
				if (result != null) {
					
					if (GcBleGattAttributeUtil.isBootUpReady(result)) {
						
						Log.d(TAG, "[MGCC] After sending command, GC is boot up!!");
						
						ret = Common.ERROR_SUCCESS;
					}

				} else {
					
					Log.d(TAG, "[MGCC] Boot up GC fails TIMEOUT: " + Common.ERROR_GATT_RECEIVE_NOTIFICATION);
					ret = Common.ERROR_BOOT_UP_GC;
				}
				
				///unregisterNotify(GcBleGattAttributes.GC_BOOT_UP_READY);
			}
			
		} else {
			
			Log.d(TAG, "[MGCC] Boot up GC fails: " + Common.ERROR_GATT_READ);
			ret = Common.ERROR_BOOT_UP_GC;
		}

		return ret;
	}
	*/
	
	
	private void from() {
		
		if (bPerformanceNotify) {

			mTimePrev = System.currentTimeMillis();
		}
	}
	
	
	
	private void to(String task) {

		if (bPerformanceNotify) {

			long timeCurr = System.currentTimeMillis();
			long timeDiff = timeCurr - mTimePrev;
			
			Log.d(TAG, "[MGCC][MPerf] [" + task + "] costs: " + timeDiff + " ms");
		}
	}
	
	private void sendFwUpdateResultMessage(boolean isSuccess) {
		
		try {
			Message outMsg = Message.obtain();
			outMsg.what = IGcConnectivityService.CB_FWUPDATE_RESULT;
			
			Bundle outData = new Bundle();
			outData.putSerializable(IGcConnectivityService.PARAM_RESULT, isSuccess ? IGcConnectivityService.Result.RESULT_SUCCESS : IGcConnectivityService.Result.RESULT_FAIL);
			outMsg.setData(outData);
			
			mMessenger.send(outMsg);

		} catch (RemoteException e) {

			e.printStackTrace();
		}
	}
}
