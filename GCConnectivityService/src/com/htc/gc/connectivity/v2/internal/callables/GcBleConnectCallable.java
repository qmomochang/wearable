package com.htc.gc.connectivity.v2.internal.callables;

import java.util.concurrent.Callable;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import com.htc.gc.connectivity.internal.common.CommonBase.GcBleTransceiverErrorCode;
import com.htc.gc.connectivity.v2.internal.component.le.GcBleTransceiver;
import com.htc.gc.connectivity.v2.internal.component.le.GcBleTransceiverListener;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.util.Log;



public class GcBleConnectCallable implements Callable<Integer> {

	private final static String TAG = "GcBleConnectCallable";
	
	private final static int DEFAULT_CALLABLE_TIMEOUT = 60000;
	
	protected GcBleTransceiver mGcBleTransceiver;
	protected BluetoothDevice mBluetoothDevice;
	
	private final LinkedBlockingQueue<GcBleTransceiverErrorCode> mCallbackQueue = new LinkedBlockingQueue<GcBleTransceiverErrorCode>();
	private Integer mStatus;
	
	
	
	private GcBleTransceiverListener mGcBleTransceiverListener = new GcBleTransceiverListener() {
		
		@Override
		public void onConnected(BluetoothDevice device) {

			Log.d(TAG, "[MGCC] onConnected. device = " + device);
			
			if (device.equals(mBluetoothDevice)) {
				
				addCallback(GcBleTransceiverErrorCode.ERROR_NONE);
			}
		}
		
		
		
		@Override
		public void onDisconnectedFromGattServer(BluetoothDevice device) {
			
			Log.d(TAG, "[MGCC] onDisconnectedFromGattServer device = " + device);

			if (device.equals(mBluetoothDevice)) {
				
				addCallback(GcBleTransceiverErrorCode.ERROR_DISCONNECTED_FROM_GATT_SERVER);
			}
		}

		
		
		@Override
		public void onError(BluetoothDevice device, BluetoothGattCharacteristic characteristic, GcBleTransceiverErrorCode errorCode) {

			Log.d(TAG, "[MGCC] onError. device = " + device + ", errorCode = " + errorCode);

			if (device.equals(mBluetoothDevice)) {
				
				addCallback(errorCode);
			}
		}
	};
	
	
	
	public GcBleConnectCallable(GcBleTransceiver transceiver, BluetoothDevice device) {

		mGcBleTransceiver = transceiver;
		mBluetoothDevice = device;
	}



	@Override
	public Integer call() throws Exception {

		Integer ret = 0;
		
		mGcBleTransceiver.registerListener(mGcBleTransceiverListener);
		
		mStatus = 0;
		
		if (mGcBleTransceiver.connect(mBluetoothDevice, false)) {
			
			GcBleTransceiverErrorCode errorCode = mCallbackQueue.poll(DEFAULT_CALLABLE_TIMEOUT, TimeUnit.MILLISECONDS);
			
			if (errorCode == null) {

				mGcBleTransceiver.disconnectForce(mBluetoothDevice);

				mStatus = -2;
				
			} else if (errorCode != GcBleTransceiverErrorCode.ERROR_NONE) {
				
				mStatus = -1;
			}
			
		} else {
			
			mStatus = -3;
		}
		
		mGcBleTransceiver.unregisterListener(mGcBleTransceiverListener);

		ret = mStatus;

		return ret;
	}
	
	
	
	protected synchronized void addCallback(GcBleTransceiverErrorCode errorCode) {

		Log.d(TAG, "[MGCC] addCallback errorCode = " + errorCode);

		if (errorCode != null) {

			mCallbackQueue.add(errorCode);
		}
	}
}
