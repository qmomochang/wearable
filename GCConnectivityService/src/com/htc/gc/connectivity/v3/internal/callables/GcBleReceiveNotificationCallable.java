package com.htc.gc.connectivity.v3.internal.callables;

import java.util.concurrent.Callable;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import com.htc.gc.connectivity.internal.common.CommonBase.GcBleTransceiverErrorCode;
import com.htc.gc.connectivity.v3.internal.common.LongCommandCollector;
import com.htc.gc.connectivity.v3.internal.component.le.GcBleGattAttributeUtil;
import com.htc.gc.connectivity.v3.internal.component.le.GcBleGattAttributes;
import com.htc.gc.connectivity.v3.internal.component.le.GcBleTransceiver;
import com.htc.gc.connectivity.v3.internal.component.le.GcBleTransceiverListener;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.util.Log;



public class GcBleReceiveNotificationCallable implements Callable<BluetoothGattCharacteristic> {

	private final static String TAG = "GcBleReceiveNotificationCallable";
	
	private final static int DEFAULT_CALLABLE_TIMEOUT = 60000;
	
	protected GcBleTransceiver mGcBleTransceiver;
	protected BluetoothDevice mBluetoothDevice;
	protected GcBleGattAttributes.GcV2CommandEnum mCommandID;
	protected boolean m_isLongFormat;
	protected LongCommandCollector mCollector;
	protected int mCallableTimeout;
	private final LinkedBlockingQueue<CallbackObject> mCallbackQueue = new LinkedBlockingQueue<CallbackObject>();

	
	
	private GcBleTransceiverListener mGcBleTransceiverListener = new GcBleTransceiverListener() {
		
		@Override
		public void onDisconnectedFromGattServer(BluetoothDevice device) {
			
			Log.d(TAG, "[MGCC] onDisconnectedFromGattServer device = " + device);

			if (device.equals(mBluetoothDevice)) {
				
				addCallback(new CallbackObject(device, null, GcBleTransceiverErrorCode.ERROR_DISCONNECTED_FROM_GATT_SERVER));
			}
		}
		
		
		
		@Override
		public void onNotificationReceive(BluetoothDevice device, BluetoothGattCharacteristic characteristic) {
			
			Log.d(TAG, "[MGCC] onNotificationReceive!!");
			
			if (device.equals(mBluetoothDevice) && characteristic.getValue()[0] == mCommandID.getID()) {
				addCallback(new CallbackObject(device, characteristic, GcBleTransceiverErrorCode.ERROR_NONE));
			}
		}
		
		
		
		@Override
		public void onError(BluetoothDevice device, BluetoothGattCharacteristic characteristic, GcBleTransceiverErrorCode errorCode) {

			Log.d(TAG, "[MGCC] onError. device = " + device + ", errorCode = " + errorCode);
			Log.d(TAG, "[MGCC] onError. characteristic.getUuid().toString() = " + characteristic.getUuid().toString() + ", Command id:" + mCommandID);

			//Todo: Need to confirm Error code format
			//if (device.equals(mBluetoothDevice) && characteristic.getUuid().toString().equals(mUuidString)) {
				
				///addCallback(new CallbackObject(device, null, errorCode));
			//}
		}
	};
	
	
	
	public GcBleReceiveNotificationCallable(GcBleTransceiver transceiver, BluetoothDevice device, GcBleGattAttributes.GcV2CommandEnum commandID) {

		this(transceiver, device, commandID, DEFAULT_CALLABLE_TIMEOUT);
	}

	
	
	public GcBleReceiveNotificationCallable(GcBleTransceiver transceiver, BluetoothDevice device, GcBleGattAttributes.GcV2CommandEnum commandID, int timeout) {

		mGcBleTransceiver = transceiver;
		mBluetoothDevice = device;
		mCommandID = commandID;
		m_isLongFormat = GcBleGattAttributes.isLongFormat(commandID);

		if (m_isLongFormat) {
			
			mCollector = new LongCommandCollector(mBluetoothDevice, commandID);
		}

		if (timeout > 0) {
			
			mCallableTimeout = timeout;
			
		} else {
			
			mCallableTimeout = DEFAULT_CALLABLE_TIMEOUT;
		}
		
		/// Register listener in constructor in order to avoid notification missing problem.
		mGcBleTransceiver.registerListener(mGcBleTransceiverListener);
	}


	
	@Override
	public BluetoothGattCharacteristic call() throws Exception {

		CallbackObject callbackObject = null;
		BluetoothGattCharacteristic ret = null;
		boolean isComplete = false;
		
		do {

			callbackObject = mCallbackQueue.poll(mCallableTimeout, TimeUnit.MILLISECONDS);
			if (callbackObject != null) {
				
				if (m_isLongFormat) {

					isComplete = mCollector.update(mBluetoothDevice, callbackObject.mCharacteristic);

				} else {

					ret = callbackObject.mCharacteristic;
				}

			} else {

				Log.d(TAG, "[MGCC] Failed to poll callbackObject!!");

				if (mCollector != null) {
					
					mCollector.reset();
				}

				break;
			}

		} while (!isComplete && m_isLongFormat);
		
		mGcBleTransceiver.unregisterListener(mGcBleTransceiverListener);

		if (m_isLongFormat)
			return mCollector.getCharacteristic();
		else
			return ret;
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
