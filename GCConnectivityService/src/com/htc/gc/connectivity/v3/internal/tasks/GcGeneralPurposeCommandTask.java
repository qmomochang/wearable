package com.htc.gc.connectivity.v3.internal.tasks;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import com.htc.gc.connectivity.v3.interfaces.IGcConnectivityService;
import com.htc.gc.connectivity.v3.internal.callables.GcBleReceiveNotificationCallable;
import com.htc.gc.connectivity.v3.internal.callables.GcBleWriteCallable;
import com.htc.gc.connectivity.v3.internal.callables.GcBootUpCallable;
import com.htc.gc.connectivity.v3.internal.common.Common;
import com.htc.gc.connectivity.v3.internal.common.GcConnectivityTask;
import com.htc.gc.connectivity.v3.internal.component.le.GcBleGattAttributes;
import com.htc.gc.connectivity.v3.internal.component.le.GcBleTransceiver;
import com.htc.gc.connectivity.v3.internal.component.wifi.GcWifiTransceiver;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.os.Messenger;
import android.util.Log;



public abstract class GcGeneralPurposeCommandTask extends GcConnectivityTask {
	
	public static final byte APP_ID_AUTOBACKUP = (byte) 0x01;
    public static final byte APP_ID_BROADCAST = (byte) 0x02;
    public static final byte APP_ID_SMS = (byte) 0x03;
    
    private static final String TAG = "GcGeneralPurposeCommandTask";
	
	public static class NotifyData {
		public byte appId;
		public String table;
		public String key;
		public String value;
	}
	
	public static class MessageNotifyData {
		public byte appId;
		public String messageType;
		public String message;
		public byte[] messageRawData;
	}
	
	protected int mWriteError;
	protected int mReadError;
	protected String mReadValue;
	
	public GcGeneralPurposeCommandTask(GcBleTransceiver gcBleTransceiver, GcWifiTransceiver gcWifiTransceiver, Messenger messenger, ExecutorService executor) {
		super(gcBleTransceiver, gcWifiTransceiver, messenger, executor);
	}
	
	protected boolean bootup(BluetoothDevice bluetoothDevice) throws InterruptedException, ExecutionException {
		Future<Integer> futureBoot = mExecutor.submit(new GcBootUpCallable(mGcBleTransceiver, mExecutor, bluetoothDevice, IGcConnectivityService.BootUpType.BOOTUP_LINUX, mMessenger));
		Integer bootResult = futureBoot.get();
		return bootResult == Common.ERROR_SUCCESS;
	}
	
	protected boolean write(BluetoothDevice bluetoothDevice, byte appId, String table, String key, String value) throws InterruptedException, ExecutionException {
		Future<BluetoothGattCharacteristic> futureNotify = mExecutor.submit(
				new GcBleReceiveNotificationCallable(mGcBleTransceiver, bluetoothDevice, GcBleGattAttributes.GcV2CommandEnum.GENERAL_PURPOSE_WRITE_REQUEST));
		byte[] writeData = getWriteData(appId, table, key, value);
		Future<BluetoothGattCharacteristic> future = mExecutor.submit(
				new GcBleWriteCallable(mGcBleTransceiver, bluetoothDevice, GcBleGattAttributes.GcV2CommandEnum.GENERAL_PURPOSE_WRITE_REQUEST, writeData));
		
		if (future.get() == null) {
			Log.d(TAG, "[MGCC] write ble fail");
			mWriteError = Common.ERROR_GATT_WRITE;
			return false;
		}
		BluetoothGattCharacteristic result = futureNotify.get();
		if (result == null) {
			Log.d(TAG, "[MGCC] receive notification fail");
			mWriteError = Common.ERROR_GATT_RECEIVE_NOTIFICATION;
			return false;
		}
		
		NotifyData notifyData = parseNotifyData(result.getValue());
		if (notifyData.appId != appId) {
			Log.d(TAG, "[MGCC] unmatch app id:" + appId + " <-> " + notifyData.appId);
			mWriteError = Common.ERROR_FAIL;
			return false;
		}
		if (notifyData.key.equals("err_str") && !notifyData.value.equals("ok")) {
			Log.d(TAG, "[MGCC] notify err:" + notifyData.value);
			mWriteError = Common.ERROR_FAIL;
			return false;
		}
		
		mWriteError = Common.ERROR_SUCCESS;
		return true;
	}
	
	protected boolean read(BluetoothDevice bluetoothDevice, byte appId, String table, String key) throws InterruptedException, ExecutionException {
		Future<BluetoothGattCharacteristic> futureNotify = mExecutor.submit(
				new GcBleReceiveNotificationCallable(mGcBleTransceiver, bluetoothDevice, GcBleGattAttributes.GcV2CommandEnum.GENERAL_PURPOSE_READ_REQUEST));
		byte[] writeData = getWriteData(appId, table, key, "");
		Future<BluetoothGattCharacteristic> future = mExecutor.submit(
				new GcBleWriteCallable(mGcBleTransceiver, bluetoothDevice, GcBleGattAttributes.GcV2CommandEnum.GENERAL_PURPOSE_READ_REQUEST, writeData));
		
		if (future.get() == null) {
			Log.d(TAG, "[MGCC] write ble fail");
			mReadError = Common.ERROR_GATT_WRITE;
			return false;
		}
		BluetoothGattCharacteristic result = futureNotify.get();
		if (result == null) {
			Log.d(TAG, "[MGCC] receive notification fail");
			mReadError = Common.ERROR_GATT_RECEIVE_NOTIFICATION;
			return false;
		}
		NotifyData notifyData = parseNotifyData(result.getValue());
		if (notifyData.appId != appId) {
			Log.d(TAG, "[MGCC] unmatch app id:" + appId + " <-> " + notifyData.appId);
			mWriteError = Common.ERROR_FAIL;
			return false;
		}
		if (!notifyData.key.equals(key)) {
			Log.d(TAG, "[MGCC] unmatch key:" + key + " <-> " + notifyData.key);
			mReadError = Common.ERROR_FAIL;
			return false;
		}
		
		mReadValue = notifyData.value;
		mReadError = Common.ERROR_SUCCESS;
		return true;
	}
	
	private byte[] getWriteData(byte appId, String table, String key, String value) {
		final byte[] tableBytes = table.getBytes();
		final byte[] keyBytes = key.getBytes();
		final byte[] valueBytes = value.getBytes();
		
		byte[] data = new byte[1 + (tableBytes.length + 1) + (keyBytes.length + 1) + (valueBytes.length + 1)];
		int dataIndex = 0;
		
		data[dataIndex] = appId;
		dataIndex += 1;
		
		System.arraycopy(tableBytes, 0, data, dataIndex, tableBytes.length);
		dataIndex += (tableBytes.length + 1);
		
		System.arraycopy(keyBytes, 0, data, dataIndex, keyBytes.length);
		dataIndex += (keyBytes.length + 1);
		
		System.arraycopy(valueBytes, 0, data, dataIndex, valueBytes.length);
		
		return data;
	}
	
	private NotifyData parseNotifyData(byte[] data) {
		NotifyData result = new NotifyData();
		
		result.appId = data[0];
		
		int beginIndex = 1;
		result.table = parseComponent(data, beginIndex);
		beginIndex += (result.table.length() + 1);
		
		result.key = parseComponent(data, beginIndex);
		beginIndex += (result.key.length() + 1);
		
		result.value = parseComponent(data, beginIndex);
		
		return result;
	}
	
	private static String parseComponent(byte[] data, int beginIndex) {
		int endIndex = beginIndex;
		while (endIndex < data.length && data[endIndex] != (byte) 0x0) {
			endIndex++;
		}
		
		byte[] componentData = new byte[endIndex - beginIndex];
		System.arraycopy(data, beginIndex, componentData, 0, componentData.length);
		
		return new String(componentData);
	}
	
	public static MessageNotifyData parseMessageNotifyData(byte[] data) {
		MessageNotifyData result = new MessageNotifyData();
		
		result.appId = data[0];
		
		int beginIndex = 1;
		result.messageType = parseComponent(data, beginIndex);
		beginIndex += (result.messageType.length() + 1);
		
		result.message = parseComponent(data, beginIndex);
		
		result.messageRawData = new byte[data.length - beginIndex];
		System.arraycopy(data, beginIndex, result.messageRawData, 0, result.messageRawData.length);
		
		return result;
	}
}
