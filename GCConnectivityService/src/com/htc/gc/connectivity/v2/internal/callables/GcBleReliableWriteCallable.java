package com.htc.gc.connectivity.v2.internal.callables;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import com.htc.gc.connectivity.v2.internal.component.le.GcBleGattAttributeUtil;
import com.htc.gc.connectivity.v2.internal.component.le.GcBleTransceiver;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.util.Log;



public class GcBleReliableWriteCallable implements Callable<BluetoothGattCharacteristic> {

	private final static String TAG = "GcBleReliableWriteCallable";
	
	protected ExecutorService mExecutor;
	protected GcBleTransceiver mGcBleTransceiver;
	protected BluetoothDevice mBluetoothDevice;
	protected String mUuidString;
	protected byte[] mWriteData;
	
	
	
	public GcBleReliableWriteCallable(GcBleTransceiver gcBleTransceiver, ExecutorService executor, BluetoothDevice device, String uuidString, byte[] writeData) {

		mGcBleTransceiver = gcBleTransceiver;
		mExecutor = executor;
		mBluetoothDevice = device;
		mUuidString = uuidString;
		mWriteData = writeData;
	}



	@Override
	public BluetoothGattCharacteristic call() throws Exception {

		BluetoothGattCharacteristic ret = null;

		if (mGcBleTransceiver != null) {
			
			BluetoothGattCharacteristic result;
			Future<BluetoothGattCharacteristic> futureA, futureB;

			futureA = mExecutor.submit(new GcBleWriteCallable(mGcBleTransceiver, mBluetoothDevice, mUuidString, mWriteData));
			if (futureA.get() != null) {

				futureB = mExecutor.submit(new GcBleReadCallable(mGcBleTransceiver, mBluetoothDevice, mUuidString));

				result = futureB.get();
				
				if (result != null) {

					if (GcBleGattAttributeUtil.compareArray(mWriteData, result.getValue())) {

						ret = result;
					}
				}	
			}
		}
		
		return ret;
	}
}
