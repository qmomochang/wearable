package com.htc.gc.connectivity.v2.internal.common;

import java.util.ArrayList;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.util.Log;



public class LongCommandCollector {
    
	private final static String TAG = "LongCommandCollector";

	private BluetoothDevice mBluetoothDevice;
	private String mUuid;
    private ArrayList<byte[]> mReceivedList = new ArrayList<byte[]>();
	private boolean bLastPayloadReceived = false;
	private boolean bAllPayloadReceived = false;
	private int mLength = 0;
	private BluetoothGattCharacteristic mResult = null;
    
	
	public LongCommandCollector(BluetoothDevice device, String uuid) {

		mBluetoothDevice = device;
		mUuid = uuid;
		
		mReceivedList.clear();
	}

	
	
	public boolean update(BluetoothDevice device, BluetoothGattCharacteristic characteristic) {
		
		boolean ret = false;
		
		if (device.equals(mBluetoothDevice) && characteristic.getUuid().toString().equals(mUuid)) {
			
			/** we assume that properties and permissions are exactly the same for a single UUID */
			if (mResult == null)
				mResult = new BluetoothGattCharacteristic(characteristic.getUuid(),
					characteristic.getProperties(), characteristic.getPermissions());
			byte[] value = characteristic.getValue();
			boolean isLast = ((value[0] & 0x80) == 0x80) ? true : false;
			int length = value[0] & 0x7f;
			int pos = value[1];

			Log.d(TAG, "[MGCC] AA bLastPayloadReceived = " + bLastPayloadReceived + ", bAllPayloadReceived = " + bAllPayloadReceived + ", mLength = " + mLength);
			
			mReceivedList.add(value);
			
			if (isLast) {
				
				int lengthCurr = 0;

				bLastPayloadReceived = true;
				mLength = pos + length;
				
				for (int cnt = 0; cnt < mReceivedList.size(); cnt++) {
					
					lengthCurr = lengthCurr + (mReceivedList.get(cnt)[0] & 0x7f);
				}
				
				Log.d(TAG, "[MGCC] AA lengthCurr = " + lengthCurr + ", mLength = " + mLength);
				
				if (mLength == lengthCurr) {
					
					bAllPayloadReceived = true;
				}
				
			} else {
				
				if (bLastPayloadReceived) {
					
					int lengthCurr = 0;

					for (int cnt = 0; cnt < mReceivedList.size(); cnt++) {
						
						lengthCurr = lengthCurr + (mReceivedList.get(cnt)[0] & 0x7f);
					}

					Log.d(TAG, "[MGCC] BB lengthCurr = " + lengthCurr + ", mLength = " + mLength);
					
					if (mLength == lengthCurr) {
						
						bAllPayloadReceived = true;
					}
				}
			}
			
			Log.d(TAG, "[MGCC] bLastPayloadReceived = " + bLastPayloadReceived + ", bAllPayloadReceived = " + bAllPayloadReceived + ", mLength = " + mLength);
			Log.d(TAG, "[MGCC] mReceivedList.size() = " + mReceivedList.size());
			
			if (bLastPayloadReceived && bAllPayloadReceived) {
				
				ret = true;
			}
		}

		return ret;
	}

	
	
	public void reset() {
		
		mReceivedList.clear();
		bLastPayloadReceived = false;
		bAllPayloadReceived = false;
		mLength = 0;
		mResult = null;
	}
	
	public BluetoothGattCharacteristic getCharacteristic() {
		if (mResult == null)
			return null;
		mResult.setValue(this.get());
		return mResult;
	}
	
	public byte[] get() {
		
		byte[] retArray = new byte[mLength];
		
		if (bLastPayloadReceived && bAllPayloadReceived) {
			
			for (int cnt = 0; cnt < mReceivedList.size(); cnt++) {
				
				byte[] value = mReceivedList.get(cnt);
				int length = value[0] & 0x7f;
				int pos = value[1];
				
				for (int idx = 0; idx < length; idx++) {
					
					retArray[pos + idx] = value[idx + 2];
				}
			}
		}
		
		return retArray;
	}
	
	
	
	public String getUuid() {
		
		return mUuid;
	}
}
