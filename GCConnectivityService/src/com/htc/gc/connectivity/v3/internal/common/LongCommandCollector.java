package com.htc.gc.connectivity.v3.internal.common;

import java.util.ArrayList;

import com.htc.gc.connectivity.v3.internal.component.le.GcBleGattAttributes;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.util.Log;



public class LongCommandCollector {
    
	private final static String TAG = "LongCommandCollector";
	private GcBleGattAttributes.GcV2CommandEnum mCommandID;
	private BluetoothDevice mBluetoothDevice;


    //private ArrayList<byte[]> mReceivedList = new ArrayList<byte[]>();
	private boolean bLastPayloadReceived = false;
	private boolean bAllPayloadReceived = false;
	private int mLength = 0;
	private BluetoothGattCharacteristic mResult = null;
	private byte[] mResultArray = null;
	private int mRemain_pack_num = 0;
	private int mLast_pack_size = 0;
    
	
	public LongCommandCollector(BluetoothDevice device, GcBleGattAttributes.GcV2CommandEnum commandID) {

		mBluetoothDevice = device;
		mCommandID = commandID;
		
		//mReceivedList.clear();
	}

	
	
	public boolean update(BluetoothDevice device, BluetoothGattCharacteristic characteristic) {
		
		boolean ret = false;
		/** we assume that those packs will arrive in the order **/
		if (device.equals(mBluetoothDevice) && characteristic.getValue()[0] == mCommandID.getID()) {
			
			/** we assume that properties and permissions are exactly the same for a single UUID */
			if (mResult == null)
				mResult = new BluetoothGattCharacteristic(characteristic.getUuid(),
					characteristic.getProperties(), characteristic.getPermissions());
			byte[] value = characteristic.getValue();
			
			int pack_index = ((value[1] >> 1) & 0x1f);
			boolean isFirstPack = (pack_index == 0)? true : false;
			
			if (isFirstPack == true)
			{
				//This command format only can be applied to current design.
				mLength = ((value[1] & 0x1) * 256) + (value[2] & 0xFF);
				mResultArray = new byte[mLength];
				int fill_first_length = 0;
				
				if (mLength <= Common.PAYLOAD_SIZE - 1)
				{
					fill_first_length = mLength;
					mRemain_pack_num = 0;
					bLastPayloadReceived = true;
					bAllPayloadReceived = true;
				}
				else
				{
					fill_first_length = Common.PAYLOAD_SIZE - 1;
					int except_first_pack_length = mLength - (Common.PAYLOAD_SIZE - 1);
					mLast_pack_size = except_first_pack_length % Common.PAYLOAD_SIZE;
					mRemain_pack_num = except_first_pack_length/Common.PAYLOAD_SIZE + ((mLast_pack_size == 0)? 0 : 1);
				}
				
				for(int i=0; i < fill_first_length; i++)
				{
					mResultArray[i] = value[i+3];
				}
			}
			else
			{
				int fill_other_length = 0;
				if (pack_index == mRemain_pack_num)
				{
					fill_other_length = mLast_pack_size;
					bLastPayloadReceived = true;
					bAllPayloadReceived = true;
				}
				else
				{
				    fill_other_length = Common.PAYLOAD_SIZE;
				}

				for (int i = 0; i < fill_other_length; i ++)
				{
					mResultArray[(Common.PAYLOAD_SIZE - 1) + (pack_index - 1) * Common.PAYLOAD_SIZE + i] = value[i + 2];
				}
			}

			if (bLastPayloadReceived && bAllPayloadReceived) {
				ret = true;
			}

		}

		return ret;
	}

	
	
	public void reset() {
		
		//mReceivedList.clear();
		bLastPayloadReceived = false;
		bAllPayloadReceived = false;
		mLength = 0;
		mResult = null;
		mResultArray = null;
		mRemain_pack_num = 0;
		mLast_pack_size = 0;
	}
	
	public BluetoothGattCharacteristic getCharacteristic() {
		if (mResult == null)
			return null;
		mResult.setValue(this.get());
		return mResult;
	}
	
	public byte[] get() {
		
		byte[] retArray = new byte[mLength];
		if (bLastPayloadReceived && bAllPayloadReceived)
		{
			for (int i = 0; i < mLength; i ++)
				retArray[i] = mResultArray[i];
		}
/*
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
*/
		return retArray;
	}
	
	
	
	public GcBleGattAttributes.GcV2CommandEnum getUuid() {
		
		return mCommandID;
	}
}
