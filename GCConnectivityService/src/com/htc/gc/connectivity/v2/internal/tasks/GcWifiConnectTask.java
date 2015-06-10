package com.htc.gc.connectivity.v2.internal.tasks;

import java.lang.reflect.Method;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import com.htc.gc.connectivity.v2.interfaces.IGcConnectivityService;
import com.htc.gc.connectivity.v2.internal.callables.GcBleReceiveNotificationCallable;
import com.htc.gc.connectivity.v2.internal.callables.GcBleReliableWriteCallable;
import com.htc.gc.connectivity.v2.internal.callables.GcBleWriteCallable;
import com.htc.gc.connectivity.v2.internal.callables.GcBootUpCallable;
import com.htc.gc.connectivity.v2.internal.common.Common;
import com.htc.gc.connectivity.v2.internal.common.GcConnectivityTask;
import com.htc.gc.connectivity.v2.internal.component.le.GcBleGattAttributeUtil;
import com.htc.gc.connectivity.v2.internal.component.le.GcBleGattAttributes;
import com.htc.gc.connectivity.v2.internal.component.le.GcBleTransceiver;
import com.htc.gc.connectivity.v2.internal.component.wifi.GcWifiTransceiver;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.Context;
import android.os.Bundle;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.telephony.TelephonyManager;
import android.util.Log;



public class GcWifiConnectTask extends GcConnectivityTask {

	private final static String TAG = "GcWifiConnectTask";
	
	protected BluetoothDevice mBluetoothDevice;
	private Random mRnd = new Random();
	
	
	
	public GcWifiConnectTask(GcBleTransceiver gcBleTransceiver, GcWifiTransceiver gcWifiTransceiver, Messenger messenger, ExecutorService executor, BluetoothDevice device) {

		super(gcBleTransceiver, gcWifiTransceiver, messenger, executor);
		mBluetoothDevice = device;

	}

	
	
	@Override
	public void execute() throws Exception {
		
		super.execute();
		
		super.from();
		
		if (mGcBleTransceiver != null) {

			if ((mGcWifiTransceiver != null) && (mGcWifiTransceiver.getP2pGroupState().equals(GcWifiTransceiver.WifiP2pGroupState.STATE_P2P_GROUP_CREATED))) {
				
				Integer result;
				Future<Integer> future;
				
				future = mExecutor.submit(new GcBootUpCallable(mGcBleTransceiver, mExecutor, mBluetoothDevice));
				result = future.get();
				
				if (result == Common.ERROR_SUCCESS) {
					
					connectWifi();

				} else {
					
					sendMessage(false, null, result);
				}
				
			} else {
				
				sendMessage(false, null, Common.ERROR_P2P_GROUP);
			}

		} else {
			
			sendMessage(false, null, Common.ERROR_FAIL);
		}
		
		super.to(TAG);
	}

	
	
	private void connectWifi() throws Exception {
		
		Log.d(TAG, "[MGCC] Wifi connecting...");
		
		BluetoothGattCharacteristic result;
		
		Future<BluetoothGattCharacteristic> futureA, futureB, futureC, futureD, futureE, futureF, futureG;
		
		///byte[] serverArray = {(byte) 0x01, (byte) 0x00};
		char[] tempCD = getCountryCode().toCharArray();
		byte bcd0 = (byte) tempCD[0];
		byte bcd1 = (byte) tempCD[1];
		byte[] serverArray = {(byte) 0x01, (byte) 0x00, bcd1, bcd0};
		futureA = mExecutor.submit(new GcBleWriteCallable(mGcBleTransceiver, mBluetoothDevice, GcBleGattAttributes.GC_WIFI_SERVER_BAND, serverArray));
		
		byte[] ssidArray = mGcWifiTransceiver.getGroupName().getBytes("utf-8");
		futureB = mExecutor.submit(new GcBleWriteCallable(mGcBleTransceiver, mBluetoothDevice, GcBleGattAttributes.GC_PHONE_SSID, ssidArray));
		
		byte[] passwordArray = mGcWifiTransceiver.getGroupPassword().getBytes("utf-8");
		futureC = mExecutor.submit(new GcBleWriteCallable(mGcBleTransceiver, mBluetoothDevice, GcBleGattAttributes.GC_PHONE_PASSWORD, passwordArray));

		if ((futureA.get() == null) || (futureB.get() == null) || (futureC.get() == null)) {

			sendMessage(false, null, Common.ERROR_GATT_WRITE);
			return;
		}
		
		futureD = mExecutor.submit(new GcBleReceiveNotificationCallable(mGcBleTransceiver, mBluetoothDevice, GcBleGattAttributes.GC_PHONE_WIFI_ERROR, 40000));
		///futureE = mExecutor.submit(new GcBleSetNotificationCallable(mGcBleTransceiver, mBluetoothDevice, GcBleGattAttributes.GC_PHONE_WIFI_ERROR, true));
		///if (futureE.get() == null) {

			///sendMessage(false, null, Common.ERROR_GATT_SET_NOTIFICATION);
			///unregisterNotify(GcBleGattAttributes.GC_PHONE_WIFI_ERROR);
			///return;
		///}

		byte rndValue = (byte) (Math.abs(mRnd.nextInt()) % 16);
		byte band = (byte) (((rndValue & 0x0f) << 4) | 0x01);
		byte[] cfgArray = {(byte) band, (byte) 4, (byte) 1};
		futureF = mExecutor.submit(new GcBleReliableWriteCallable(mGcBleTransceiver, mExecutor, mBluetoothDevice, GcBleGattAttributes.GC_PHONE_WIFI_CFG, cfgArray));
		if (futureF.get() == null) {

			sendMessage(false, null, Common.ERROR_GATT_WRITE);
			///unregisterNotify(GcBleGattAttributes.GC_PHONE_WIFI_ERROR);
			return;
		}

		result = futureD.get();
		
		if (result != null) {

			Log.d(TAG, "[MGCC] future result D = " + result.getUuid());
			
			byte[] connectResult = GcBleGattAttributeUtil.getWifiConnectResult(result);
			
			if (connectResult[1] == 0x00) {
				
				String ipAddress = GcBleGattAttributeUtil.getIpAddress(result);
				
				Log.d(TAG, "[MGCC] WIFI connect successful, IP = " + ipAddress);
				
				sendMessage(true, ipAddress, 0);

			} else {

				Log.d(TAG, "[MGCC] WIFI connect error, Error code = " + connectResult[1]);
				
				sendMessage(false, null, (int)connectResult[1]);
			}
			
		} else {
			
			sendMessage(false, null, Common.ERROR_GATT_RECEIVE_NOTIFICATION);
		}
		
		///unregisterNotify(GcBleGattAttributes.GC_PHONE_WIFI_ERROR);
	}
	
	
	
	private String getCountryCode() {
		
		String countryCode = "00";
		String telCD = null;
		
		Context context = mGcBleTransceiver.getContext();
		String[] isoCountries = Locale.getISOCountries();
		
		String systemCDCrda = null;
		String systemCDDefault = null;

		try {

			Class<?> klass;
			klass = Class.forName("android.os.SystemProperties");
			
			if (klass != null) {
				
				final Method method = klass.getDeclaredMethod("get", String.class);

				if (method != null) {
					
					systemCDCrda = (String) method.invoke(null, "wlan.crda.country");
					systemCDDefault = (String) method.invoke(null, "wifi.country");
				}
			}

			Log.d(TAG, "[MGCC] systemCDCrda = " + systemCDCrda);
			Log.d(TAG, "[MGCC] systemCDDefault = " + systemCDDefault);
			
		} catch (Exception e) {

			e.printStackTrace();
		}

		
		TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
		
		if (telephonyManager == null) {
			
			return countryCode;
		}
		
		Log.d(TAG, "[MGCC] telephonyManager.getSimState() = " + telephonyManager.getSimState());
		Log.d(TAG, "[MGCC] telephonyManager.getNetworkType() = " + telephonyManager.getNetworkType());
		Log.d(TAG, "[MGCC] telephonyManager.getPhoneType() = " + telephonyManager.getPhoneType());
		Log.d(TAG, "[MGCC] telephonyManager.getNetworkCountryIso() = " + telephonyManager.getNetworkCountryIso());
		Log.d(TAG, "[MGCC] telephonyManager.getSimCountryIso() = " + telephonyManager.getSimCountryIso());
		
		if (telephonyManager.getSimState() == TelephonyManager.SIM_STATE_READY) {
			
			if ((telephonyManager.getPhoneType() == TelephonyManager.PHONE_TYPE_GSM) ||
				(telephonyManager.getPhoneType() == TelephonyManager.PHONE_TYPE_SIP)) {
				
				telCD = telephonyManager.getNetworkCountryIso().toUpperCase();
				
			} else if (telephonyManager.getPhoneType() == TelephonyManager.PHONE_TYPE_CDMA){
				
				telCD = telephonyManager.getSimCountryIso().toUpperCase();
			}
		}
		
		if ((telCD == null) || ((telCD != null) && (telCD.length() != 2))) {
			
			if ((systemCDCrda != null) && (systemCDCrda.length() == 2)) {
				
				telCD = systemCDCrda.toUpperCase();
			}
		}
		
		if ((telCD != null) && (telCD.length() == 2)) {

			for (int cnt = 0; cnt < isoCountries.length; cnt++) {
				
				if (telCD.equals(isoCountries[cnt])) {
					
					countryCode = telCD;
					break;
				}
			}

		}

		Log.d(TAG, "[MGCC] Final country code = " + countryCode);
		
		return countryCode;
	}
	
    
	
	private void sendMessage(boolean result, String ipAddress, int errorCode) {
		
		try {
			
			Message outMsg = Message.obtain();
			outMsg.what = IGcConnectivityService.CB_WIFI_CONNECT_RESULT;
			Bundle outData = new Bundle();
			
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

		sendMessage(false, null, Common.ERROR_FAIL);
	}
}
