package com.htc.gc.connectivity.v3.internal.tasks;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import com.htc.gc.connectivity.internal.common.CommonBase.GcWifiTransceiverErrorCode;
import com.htc.gc.connectivity.v3.interfaces.IGcConnectivityService;
import com.htc.gc.connectivity.v3.interfaces.IGcConnectivityService.WifiConfigBand;
import com.htc.gc.connectivity.v3.interfaces.IGcConnectivityService.WifiConfigSecurity;
import com.htc.gc.connectivity.v3.interfaces.IGcConnectivityService.WifiConfigureType;
import com.htc.gc.connectivity.v3.internal.callables.GcBleReceiveNotificationCallable;
import com.htc.gc.connectivity.v3.internal.callables.GcBleWriteCallable;
import com.htc.gc.connectivity.v3.internal.callables.GcBootUpCallable;
import com.htc.gc.connectivity.v3.internal.callables.GcWifiConnectToWPA2Callable;
import com.htc.gc.connectivity.v3.internal.callables.GcWifiSearchGCSoftAPCallable;
import com.htc.gc.connectivity.v3.internal.common.Common;
import com.htc.gc.connectivity.v3.internal.common.GcConnectivityTask;
import com.htc.gc.connectivity.v3.internal.component.le.GcBleGattAttributes;
import com.htc.gc.connectivity.v3.internal.component.le.GcBleTransceiver;
import com.htc.gc.connectivity.v3.internal.component.wifi.GcWifiTransceiver;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.os.Bundle;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

public class GcSoftAPConnectTask extends GcConnectivityTask {
	
	private final static String TAG = "GcSoftAPConnectTask";
	private String mPasswd;
	protected BluetoothDevice mBluetoothDevice;
	
	public GcSoftAPConnectTask(GcBleTransceiver gcBleTransceiver, GcWifiTransceiver gcWifiTransceiver, Messenger messenger, ExecutorService executor, BluetoothDevice device, String passwd) {
		super(gcBleTransceiver, gcWifiTransceiver, messenger, executor);
		mBluetoothDevice = device;
		mPasswd = passwd;
	}
	
	@Override
	public void execute() throws Exception {
		
		super.execute();
		
		super.from();
		
		if (mGcBleTransceiver != null) {

			Integer bootResult;
			Future<Integer> futureBoot;
			
			futureBoot = mExecutor.submit(new GcBootUpCallable(mGcBleTransceiver, mExecutor, mBluetoothDevice, IGcConnectivityService.BootUpType.BOOTUP_LINUX, mMessenger));
			bootResult = futureBoot.get();
			
			if (bootResult == Common.ERROR_SUCCESS) {
				
				connectWifi();

			} else {
				Log.d(TAG, "[MGCC] boot up is fail");
				sendMessage(IGcConnectivityService.CB_WIFI_CONNECT_RESULT, false, null, bootResult);
			}
		}
		
		super.to(TAG);
	}

	
	
	private void connectWifi() throws Exception {
		Log.d(TAG, "[MGCCtes] Wifi connecting...");
		
		BluetoothGattCharacteristic bleWiFiCfgResult;
		BluetoothGattCharacteristic bleResult;
		BluetoothGattCharacteristic blePWResult;
		
		Future<BluetoothGattCharacteristic> futureA, futureAP, futureB, futureC;
		Future<GcWifiTransceiverErrorCode> futureD, futureE;

		//** GcBleReceiveNotificationCallable starts listening events in constructor, hence no future checking is required until result is needed *//*
		futureA = mExecutor.submit(new GcBleReceiveNotificationCallable(mGcBleTransceiver, mBluetoothDevice, GcBleGattAttributes.GcV2CommandEnum.WIFI_SOFTAP_GETSSID_EVENT));

		futureAP = mExecutor.submit(new GcBleReceiveNotificationCallable(mGcBleTransceiver, mBluetoothDevice, GcBleGattAttributes.GcV2CommandEnum.WIFI_SOFTAP_GETPASSWORD_EVENT));

		futureB = mExecutor.submit(new GcBleReceiveNotificationCallable(mGcBleTransceiver, mBluetoothDevice, GcBleGattAttributes.GcV2CommandEnum.WIFI_CONFIG_STATUS_EVENT, 20000));

		byte bandSelection = 0x01; //2.4GHz (Altek BLE_command_spec_0701)
//		if (mGcWifiTransceiver.isDualBandSupported()) {
//			bandSelection = 0x00; //5GHz (Altek BLE_command_spec_0701)
//		}
//		else {
//			bandSelection = 0x01; //2.4GHz (Altek BLE_command_spec_0701)
//		}

		//since it's 2.4GHz, country code doesn't matter
		//TODO: Fill correct configuration
		byte defaultChannel = 0;
		byte[] serverArray = {WifiConfigureType.WIFI_SOFTAP.getType(), 'W', 'T', WifiConfigBand.WIFI_24G.getBand(), WifiConfigSecurity.WIFI_WPA2.getSecurity(), (byte)defaultChannel};
		futureC = mExecutor.submit(new GcBleWriteCallable(mGcBleTransceiver, mBluetoothDevice, GcBleGattAttributes.GcV2CommandEnum.WIFI_CONFIG_REQUEST, serverArray));
		if ((futureC.get() == null)){
			sendMessage(IGcConnectivityService.CB_WIFI_CONNECT_RESULT, false, null, Common.ERROR_GATT_WRITE);
			return;
		}

		if ((bleWiFiCfgResult = futureB.get()) == null) {
			sendMessage(IGcConnectivityService.CB_WIFI_CONNECT_RESULT, false, null, Common.ERROR_GATT_RECEIVE_NOTIFICATION);
			return;
		}
		
		if ((bleResult = futureA.get()) == null) {
			sendMessage(IGcConnectivityService.CB_WIFI_CONNECT_RESULT, false, null, Common.ERROR_GATT_RECEIVE_NOTIFICATION);
			return;
		}

		if ((blePWResult = futureAP.get()) == null) {
			sendMessage(IGcConnectivityService.CB_WIFI_CONNECT_RESULT, false, null, Common.ERROR_GATT_RECEIVE_NOTIFICATION);
			return;
		}
		
		futureE = mExecutor.submit(new GcWifiSearchGCSoftAPCallable(mGcWifiTransceiver, new String(bleResult.getValue()), new String(blePWResult.getValue())));
		if (futureE.get() != GcWifiTransceiverErrorCode.ERROR_NONE) {
			sendMessage(IGcConnectivityService.CB_WIFI_CONNECT_RESULT, false, null, Common.ERROR_SOFTAP_NOT_FOUND);
			return;
		}

		futureD = mExecutor.submit(new GcWifiConnectToWPA2Callable(mGcWifiTransceiver, new String(bleResult.getValue()), new String(blePWResult.getValue())));
		if (futureD.get() != GcWifiTransceiverErrorCode.ERROR_NONE) {
			sendMessage(IGcConnectivityService.CB_WIFI_CONNECT_RESULT, false, null, Common.ERROR_CONN_FAILURE);
			return;
		}

		String szIP = mGcWifiTransceiver.getDhcpServerIP();
		if (szIP != null) {
			sendMessage(IGcConnectivityService.CB_WIFI_CONNECT_RESULT, true, szIP, 0);
		}
		else {
			sendMessage(IGcConnectivityService.CB_WIFI_CONNECT_RESULT, false, null, Common.ERROR_DHCP_FAILURE);
		}
	}
	
	private void sendMessage(int type, boolean result, String ipAddress, int errorCode) {
		try {
			Log.i(TAG, "[MGCC] sendMessage: result=" + result + ", error=" + errorCode);
			Message outMsg = Message.obtain();
			outMsg.what = type;
			Bundle outData = new Bundle();
			outData.putBoolean(IGcConnectivityService.PARAM_RESULT_SOFTAP, true);
			
			if (result) {
				outData.putSerializable(IGcConnectivityService.PARAM_RESULT, IGcConnectivityService.Result.RESULT_SUCCESS);
				outData.putString(IGcConnectivityService.PARAM_DEVICE_IP_ADDRESS, ipAddress);
			} else {
				outData.putSerializable(IGcConnectivityService.PARAM_RESULT, IGcConnectivityService.Result.RESULT_FAIL);
				outData.putInt(IGcConnectivityService.PARAM_WIFI_ERROR_CODE, errorCode);
			}
			
			outMsg.setData(outData);
			mMessenger.send(outMsg);
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}
	
	
	
	@Override
	public void error(Exception e) {

		sendMessage(IGcConnectivityService.CB_WIFI_CONNECT_RESULT, false, null, Common.ERROR_FAIL);
	}
}
