package com.htc.gc.connectivity.v2.internal.callables;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import com.htc.gc.connectivity.interfaces.IGcConnectivityDevice.GcStateBle;
import com.htc.gc.connectivity.internal.common.GcConnectivityDevice;
import com.htc.gc.connectivity.v2.internal.common.Common;
import com.htc.gc.connectivity.v2.internal.component.le.GcBleGattAttributeUtil;
import com.htc.gc.connectivity.v2.internal.component.le.GcBleGattAttributes;
import com.htc.gc.connectivity.v2.internal.component.le.GcBleTransceiver;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
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
	
	
	
	public GcBootUpCallable(GcBleTransceiver gcBleTransceiver, ExecutorService executor, BluetoothDevice device) {

		mGcBleTransceiver = gcBleTransceiver;
		mExecutor = executor;
		mBluetoothDevice = device;
	}



	@Override
	public Integer call() throws Exception {

		from();
		
		Integer ret = Common.ERROR_BOOT_UP_GC;

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
				
				/// Version protect, can remove later.
				if (versionBle > 2250) {
					ret = bootUp();
				} else {
					ret = bootUpOld();
				}
				
				if (ret == Common.ERROR_SUCCESS) {
					
					mRetryTimes = 0;
					
				} else {

					mRetryTimes--;

					Log.d(TAG, "[MGCC] GC bootUp mRetryTimes = " + mRetryTimes);
				}

			} while (mRetryTimes > 0);
		}
		
		to(TAG);
		
		return ret;
	}
	
	
	
	private Integer bootUp() throws Exception {
		
		Integer ret = Common.ERROR_BOOT_UP_GC;
		
		BluetoothGattCharacteristic result;
		Future<BluetoothGattCharacteristic> futureA, futureC;
		
		futureA = mExecutor.submit(new GcBleReceiveNotificationCallable(mGcBleTransceiver, mBluetoothDevice, GcBleGattAttributes.GC_BOOT_UP_READY, 3000));
		
		byte[] bootGcArray = {(byte) 0x01};
		futureC = mExecutor.submit(new GcBleReliableWriteCallable(mGcBleTransceiver, mExecutor, mBluetoothDevice, GcBleGattAttributes.GC_BOOT_UP_GC, bootGcArray));
		if (futureC.get() == null) {

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
		
		return ret;
	}
	
	
	
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
}
