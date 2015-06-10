package com.htc.gc.connectivity.v3.internal.component.le;

import com.htc.gc.connectivity.internal.common.CommonBase.GcBleTransceiverErrorCode;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;



public class GcBleTransceiverListener implements IGcBleTransceiverListener {

	@Override
	public void onBonded(BluetoothDevice device) {
		
	}
	
	@Override
	public void onConnected(BluetoothDevice device) {
		
	}

	@Override
	public void onDisconnected(BluetoothDevice device) {
		
	}

	@Override
	public void onDisconnectedFromGattServer(BluetoothDevice device) {
		
	}

	@Override
	public void onCharacteristicRead(BluetoothDevice device, BluetoothGattCharacteristic characteristic) {
		
	}

	@Override
	public void onCharacteristicWrite(BluetoothDevice device, BluetoothGattCharacteristic characteristic) {
		
	}

	@Override
	public void onDescriptorWrite(BluetoothDevice device, BluetoothGattDescriptor descriptor) {
		
	}

	@Override
	public void onNotificationReceive(BluetoothDevice device, BluetoothGattCharacteristic characteristic) {
		
	}

	@Override
	public void onError(BluetoothDevice device, BluetoothGattCharacteristic characteristic, GcBleTransceiverErrorCode errorCode) {
		
	}
}
