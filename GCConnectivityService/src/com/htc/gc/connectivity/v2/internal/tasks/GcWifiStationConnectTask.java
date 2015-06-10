package com.htc.gc.connectivity.v2.internal.tasks;

import java.lang.reflect.Method;
import java.util.Locale;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import com.htc.gc.connectivity.internal.common.GcConnectivityDevice;
import com.htc.gc.connectivity.v2.interfaces.IGcConnectivityService;
import com.htc.gc.connectivity.v2.internal.callables.GcWifiCreateGroupCallable;
import com.htc.gc.connectivity.v2.internal.callables.GcWifiRemoveGroupCallable;
import com.htc.gc.connectivity.v2.internal.callables.GcWifiStationConnectCallable;
import com.htc.gc.connectivity.v2.internal.common.Common;
import com.htc.gc.connectivity.v2.internal.common.GcConnectivityTask;
import com.htc.gc.connectivity.v2.internal.component.le.GcBleTransceiver;
import com.htc.gc.connectivity.v2.internal.component.wifi.GcWifiTransceiver;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.os.Bundle;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.telephony.TelephonyManager;
import android.util.Log;



public class GcWifiStationConnectTask extends GcConnectivityTask {

	private final static String TAG = "GcWifiStationConnectTask";
	
	private final static int RETRY_TIMES = 3;
	
	protected BluetoothDevice mBluetoothDevice;

	
	
	public GcWifiStationConnectTask(GcBleTransceiver gcBleTransceiver, GcWifiTransceiver gcWifiTransceiver, Messenger messenger, ExecutorService executor, BluetoothDevice device) {

		super(gcBleTransceiver, gcWifiTransceiver, messenger, executor);
		mBluetoothDevice = device;

	}

	
	
	@Override
	public void execute() throws Exception {
		
		super.execute();
		
		super.from();
		
		if (mGcWifiTransceiver != null) {

			Integer result;
			Integer resultWifi;
			Callable<Integer> callable;
			Future<Integer> future;
			String countryCode = getCountryCode();
			int connectMethod = 0;
			int convertUTF8 = 1;
			int reTry = RETRY_TIMES;
			
			/// Version protect, can remove later.
			int versionBle = -1;
			GcConnectivityDevice gcDevice = mGcBleTransceiver.getGcConnectivityDeviceGroup().getDevice(mBluetoothDevice);
			if (gcDevice != null) {

				versionBle = gcDevice.getVersionBle();
			}

			do {
				
				callable = new GcWifiCreateGroupCallable(mGcWifiTransceiver);
				future = mExecutor.submit(callable);
				
				result = future.get();
				if (result == 0) {

					callable = new GcWifiStationConnectCallable(mGcBleTransceiver, mGcWifiTransceiver, mExecutor, mBluetoothDevice, countryCode, connectMethod, convertUTF8);
					future = mExecutor.submit(callable);
					
					resultWifi = future.get();
					if (resultWifi == Common.ERROR_SUCCESS) {
						
						String ipAddress = ((GcWifiStationConnectCallable) callable).getIpAddress();

						if (ipAddress != null) {
							
							reTry = 0;
							sendMessage(true, ipAddress, Common.ERROR_SUCCESS);
							
						} else {
							
							reTry = 0;
							sendMessage(false, null, Common.ERROR_GET_IP);
						}
						
					} else {
						
						callable = new GcWifiRemoveGroupCallable(mGcWifiTransceiver);
						future = mExecutor.submit(callable);
						
						result = future.get();
						if (result == 0) {
							
							reTry--;

							Log.d(TAG, "[MGCC] resultWifi = " + resultWifi + ", connectMethod = " + connectMethod + ", convertUTF8 = " + convertUTF8);
							
							if ((reTry == (RETRY_TIMES - 1)) && 
								(resultWifi != Common.ERROR_P2P_SSID)) {
								
								reTry--;
							}

							/// Version protect, can remove later.
							if (versionBle > 2250) {
								
								connectMethod = 1;
								
								if (reTry > (RETRY_TIMES - 2)) {
									
									convertUTF8 = 1;

								} else {
									
									convertUTF8 = 0;
								}
								
							} else {

								connectMethod = 0;
								
								if (reTry > (RETRY_TIMES - 2)) {
									
									convertUTF8 = 1;

								} else {
									
									convertUTF8 = 0;
								}
							}
							
							if (reTry == 0) {
								
								sendMessage(false, null, (int)resultWifi);
							}
							
						} else {
					
							reTry = 0;
							sendMessage(false, null, Common.ERROR_P2P_GROUP_REMOVE);
						}
					}
					
				} else {

					reTry = 0;
					sendMessage(false, null, Common.ERROR_P2P_GROUP);
				}
				
				Log.d(TAG, "[MGCC] WiFi station connect retry times left = " + reTry);
				
			} while (reTry > 0);
		}
		
		super.to(TAG);
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
	
	
	
	@Override
	public void error(Exception e) {

		sendMessage(false, null, Common.ERROR_FAIL);
	}
}
