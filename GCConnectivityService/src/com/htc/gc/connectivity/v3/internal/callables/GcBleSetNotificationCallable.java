package com.htc.gc.connectivity.v3.internal.callables;

import java.util.concurrent.Callable;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import com.htc.gc.connectivity.internal.common.CommonBase.GcBleTransceiverErrorCode;
import com.htc.gc.connectivity.v3.internal.component.le.GcBleGattAttributes;
import com.htc.gc.connectivity.v3.internal.component.le.GcBleTransceiver;
import com.htc.gc.connectivity.v3.internal.component.le.GcBleTransceiverListener;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.util.Log;



public class GcBleSetNotificationCallable implements Callable<BluetoothGattCharacteristic> {

	private final static String TAG = "GcBleSetNotificationCallable";
	
	private final static int DEFAULT_CALLABLE_TIMEOUT = 20000;
	
	private final static int DEFAULT_RETRY_TIMES = 10;

	protected GcBleTransceiver mGcBleTransceiver;
	protected BluetoothDevice mBluetoothDevice;
	protected GcBleGattAttributes.GcV2CommandEnum mCommandID;
	protected boolean mEnable;

	private final LinkedBlockingQueue<CallbackObject> mCallbackQueue = new LinkedBlockingQueue<CallbackObject>();
	private int mRetryTimes = DEFAULT_RETRY_TIMES;
	
	
	
	private GcBleTransceiverListener mGcBleTransceiverListener = new GcBleTransceiverListener() {
		
		@Override
		public void onDisconnectedFromGattServer(BluetoothDevice device) {
			
			Log.d(TAG, "[MGCC] onDisconnectedFromGattServer device = " + device);

			if (device.equals(mBluetoothDevice)) {
				
				addCallback(new CallbackObject(device, null, GcBleTransceiverErrorCode.ERROR_DISCONNECTED_FROM_GATT_SERVER));
			}
		}
		
		
		
		@Override
		public void onDescriptorWrite(BluetoothDevice device, BluetoothGattDescriptor descriptor) {
			
			Log.d(TAG, "[MGCC] onDescriptorWrite!!");

			if (device.equals(mBluetoothDevice) && descriptor.getCharacteristic().getUuid().toString().equals(GcBleGattAttributes.getUuid(mCommandID))) {
				
				addCallback(new CallbackObject(device, descriptor.getCharacteristic(), GcBleTransceiverErrorCode.ERROR_NONE));
			}
		}
		
		
		
		@Override
		public void onError(BluetoothDevice device, BluetoothGattCharacteristic characteristic, GcBleTransceiverErrorCode errorCode) {

			Log.d(TAG, "[MGCC] onError. device = " + device + ", errorCode = " + errorCode);
			Log.d(TAG, "[MGCC] onError. characteristic.getUuid().toString() = " + characteristic.getUuid().toString() + ", command ID:" + mCommandID);

			//TODO: Confirm Error code type
			if (device.equals(mBluetoothDevice) && characteristic.getUuid().toString().equals(GcBleGattAttributes.getUuid(mCommandID))) {
				
				addCallback(new CallbackObject(device, null, errorCode));
			}
		}
	};
	
	
	
	public GcBleSetNotificationCallable(GcBleTransceiver transceiver, BluetoothDevice device, GcBleGattAttributes.GcV2CommandEnum commandID, boolean enable) {

		mGcBleTransceiver = transceiver;
		mBluetoothDevice = device;
		mCommandID = commandID;
		mEnable = enable;
	}



	@Override
	public BluetoothGattCharacteristic call() throws Exception {

		CallbackObject callbackObject = null;
		
		mGcBleTransceiver.registerListener(mGcBleTransceiverListener);

		mRetryTimes = DEFAULT_RETRY_TIMES;
		
		do {

			int retValue = mGcBleTransceiver.setGcNotification(mBluetoothDevice, mCommandID, mEnable, 0);
			if (retValue < 0) {
				
				mGcBleTransceiver.unregisterListener(mGcBleTransceiverListener);
				return null;
			}
			
			callbackObject = mCallbackQueue.poll(DEFAULT_CALLABLE_TIMEOUT, TimeUnit.MILLISECONDS);
			
			if (callbackObject == null) {
				
				mRetryTimes = 0;
				
			} else {

				if (callbackObject.mErrorCode.equals(GcBleTransceiverErrorCode.ERROR_NONE) || 
					callbackObject.mErrorCode.equals(GcBleTransceiverErrorCode.ERROR_DISCONNECTED_FROM_GATT_SERVER)) {
						
					mRetryTimes = 0;

				} else {
					
					mRetryTimes--;
				}
				
				Log.d(TAG, "[MGCC] errorCode = " + callbackObject.mErrorCode + ", mRetryTimes = " + mRetryTimes);
			}

		} while (mRetryTimes > 0);

		mGcBleTransceiver.unregisterListener(mGcBleTransceiverListener);

		if (callbackObject == null) {
			
			return null;

		} else {
			
			return callbackObject.mCharacteristic;
		}
	}
	
	
	
	protected synchronized void addCallback(CallbackObject callbackObject) {

		Log.d(TAG, "[MGCC] addCallback!!"); 
		
		mCallbackQueue.add(callbackObject);
	}
	
	
	
	private class CallbackObject {
		
		public final BluetoothDevice mDevice;
		public final BluetoothGattCharacteristic mCharacteristic;
		public final GcBleTransceiverErrorCode mErrorCode;
		
		public CallbackObject(BluetoothDevice device, BluetoothGattCharacteristic characteristic, GcBleTransceiverErrorCode errorCode) {
			
			mDevice = device;
			mCharacteristic = characteristic;
			mErrorCode = errorCode;
		}
	}
}
