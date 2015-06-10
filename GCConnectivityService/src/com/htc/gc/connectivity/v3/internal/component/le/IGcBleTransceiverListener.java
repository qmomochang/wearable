package com.htc.gc.connectivity.v3.internal.component.le;

import com.htc.gc.connectivity.internal.common.CommonBase.GcBleTransceiverErrorCode;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;



public interface IGcBleTransceiverListener {

   	public void onBonded(BluetoothDevice device);

   	public void onConnected(BluetoothDevice device);
   	public void onDisconnected(BluetoothDevice device);
   	public void onDisconnectedFromGattServer(BluetoothDevice device);

   	public void onCharacteristicRead(BluetoothDevice device, BluetoothGattCharacteristic characteristic);
   	public void onCharacteristicWrite(BluetoothDevice device, BluetoothGattCharacteristic characteristic);
   	public void onDescriptorWrite(BluetoothDevice device, BluetoothGattDescriptor descriptor);
   	public void onNotificationReceive(BluetoothDevice device, BluetoothGattCharacteristic characteristic);
        
   	public void onError(BluetoothDevice device, BluetoothGattCharacteristic characteristic, GcBleTransceiverErrorCode errorCode);
}
