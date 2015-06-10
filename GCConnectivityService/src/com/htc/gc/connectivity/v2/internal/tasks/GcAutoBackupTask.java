package com.htc.gc.connectivity.v2.internal.tasks;

import java.lang.reflect.Method;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import com.htc.gc.connectivity.interfaces.IGcConnectivityServiceBase.WifiAP;
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



public class GcAutoBackupTask extends GcConnectivityTask {

	private final static String TAG = "GcAutoBackupTask";
	
	public final static int ACTION_SET_AUTO_BACKUP_LTEVENT = 0;
	public final static int ACTION_CLR_AUTO_BACKUP_LTEVENT = 1;
	public final static int ACTION_SET_AUTO_BACKUP_AP = 2;
	public final static int ACTION_CLR_AUTO_BACKUP_AP = 3;
	public final static int ACTION_SET_AUTO_BACKUP_PROXY = 4;
	public final static int ACTION_GET_AUTO_BACKUP_PROXY = 5;
	public final static int ACTION_SET_AUTO_BACKUP_AP_SCAN = 6;
	public final static int ACTION_GET_AUTO_BACKUP_STATUS = 7;
	
	private BluetoothDevice mBluetoothDevice;
	private int mAction;
	private WifiAP mWifiAP;
	private String mSsidString;
	private String mPassword;
	private byte mSecurity;
	private int mPort;
	private String mProxyString;
	private int mOption;
	
	
	
	public GcAutoBackupTask(GcBleTransceiver gcBleTransceiver, GcWifiTransceiver gcWifiTransceiver, Messenger messenger, ExecutorService executor, BluetoothDevice device, int action,
			WifiAP ap, String ssid, String passwd, byte security, int port, String proxy, int option) {

		super(gcBleTransceiver, gcWifiTransceiver, messenger, executor);
		
		mBluetoothDevice = device;
		mAction = action;
		mWifiAP = ap;
		mSsidString = ssid;

		mPassword = passwd;
		if ((mPassword == null) || ((mPassword != null) && (mPassword.length() <= 0))) {
			
			mPassword = "\0";
		}

		mSecurity = security;
		mPort = port;
		mProxyString = proxy;
		mOption = option;
	}

	
	
	public GcAutoBackupTask(GcBleTransceiver gcBleTransceiver, GcWifiTransceiver gcWifiTransceiver, Messenger messenger, ExecutorService executor, BluetoothDevice device, int action, int option) {

		/// ACTION_SET_AUTO_BACKUP_LTEVENT, ACTION_CLR_AUTO_BACKUP_LTEVENT, ACTION_SET_AUTO_BACKUP_AP_SCAN, ACTION_GET_AUTO_BACKUP_STATUS
		this(gcBleTransceiver, gcWifiTransceiver, messenger, executor, device, action, null, null, null, (byte) 0, 0, null, option);
	}

	
	
	public GcAutoBackupTask(GcBleTransceiver gcBleTransceiver, GcWifiTransceiver gcWifiTransceiver, Messenger messenger, ExecutorService executor, BluetoothDevice device, int action,
			WifiAP ap, String ssid, String passwd, byte security) {
	
		/// ACTION_SET_AUTO_BACKUP_AP, ACTION_CLR_AUTO_BACKUP_AP
		this(gcBleTransceiver, gcWifiTransceiver, messenger, executor, device, action, ap, ssid, passwd, security, 0, null, -1);
	}

	
	
	public GcAutoBackupTask(GcBleTransceiver gcBleTransceiver, GcWifiTransceiver gcWifiTransceiver, Messenger messenger, ExecutorService executor, BluetoothDevice device, int action, String ssid, int port, String proxy, byte security) {

		/// ACTION_SET_AUTO_BACKUP_PROXY, ACTION_GET_AUTO_BACKUP_PROXY
		this(gcBleTransceiver, gcWifiTransceiver, messenger, executor, device, action, null, ssid, null, security, port, proxy, -1);
	}

	
	
	@Override
	public void execute() throws Exception {
		
		super.execute();
		
		super.from();
		
		if (mAction == ACTION_SET_AUTO_BACKUP_LTEVENT) {
		
			sendMessage(true, Common.ERROR_SUCCESS);
			
		} else if (mAction == ACTION_CLR_AUTO_BACKUP_LTEVENT) {
			
			sendMessage(true, Common.ERROR_SUCCESS);

		} else if (mAction == ACTION_SET_AUTO_BACKUP_AP) {
			
			Integer result;
			Future<Integer> future;
			
			future = mExecutor.submit(new GcBootUpCallable(mGcBleTransceiver, mExecutor, mBluetoothDevice));
			result = future.get();
			
			if (result == Common.ERROR_SUCCESS) {
				
				connectAP();

			} else {
				
				sendMessage(false, result);
			}
			
		} else if (mAction == ACTION_CLR_AUTO_BACKUP_AP) {
			
			Integer result;
			Future<Integer> future;
			
			future = mExecutor.submit(new GcBootUpCallable(mGcBleTransceiver, mExecutor, mBluetoothDevice));
			result = future.get();
			
			if (result == Common.ERROR_SUCCESS) {
				
				eraseAP();

			} else {
				
				sendMessage(false, result);
			}
			
		} else if (mAction == ACTION_SET_AUTO_BACKUP_PROXY) {

			Integer result;
			Future<Integer> future;
			
			future = mExecutor.submit(new GcBootUpCallable(mGcBleTransceiver, mExecutor, mBluetoothDevice));
			result = future.get();
			
			if (result == Common.ERROR_SUCCESS) {
				
				setProxy();

			} else {
				
				sendMessage(false, result);
			}

		} else if (mAction == ACTION_GET_AUTO_BACKUP_PROXY) {

			Integer result;
			Future<Integer> future;
			
			future = mExecutor.submit(new GcBootUpCallable(mGcBleTransceiver, mExecutor, mBluetoothDevice));
			result = future.get();
			
			if (result == Common.ERROR_SUCCESS) {
				
				getProxy();

			} else {
				
				sendMessage(false, result);
			}
			
		} else if (mAction == ACTION_SET_AUTO_BACKUP_AP_SCAN) {

			Integer result;
			Future<Integer> future;
			
			future = mExecutor.submit(new GcBootUpCallable(mGcBleTransceiver, mExecutor, mBluetoothDevice));
			result = future.get();
			
			if (result == Common.ERROR_SUCCESS) {
				
				scanAP();

			} else {
				
				sendMessage(false, result);
			}

		} else if (mAction == ACTION_GET_AUTO_BACKUP_STATUS) {

			Integer result;
			Future<Integer> future;
			
			future = mExecutor.submit(new GcBootUpCallable(mGcBleTransceiver, mExecutor, mBluetoothDevice));
			result = future.get();
			
			if (result == Common.ERROR_SUCCESS) {
				
				getStatus();

			} else {
				
				sendMessage(false, result);
			}
		}
		
		super.to(TAG);
	}
	
	
	
	private void connectAP() throws Exception {
		
		Log.d(TAG, "[MGCC] AP connecting...");
		
		BluetoothGattCharacteristic result;

		Future<BluetoothGattCharacteristic> futureA, futureB, futureC, futureD, futureF;
		
		char[] tempCD = getCountryCode().toCharArray();
		byte bcd0 = (byte) tempCD[0];
		byte bcd1 = (byte) tempCD[1];
		byte[] serverArray = {(byte) 0x01, (byte) 0x00, bcd1, bcd0};
		futureA = mExecutor.submit(new GcBleWriteCallable(mGcBleTransceiver, mBluetoothDevice, GcBleGattAttributes.GC_WIFI_SERVER_BAND, serverArray));
		
		byte[] ssidArray = mSsidString.getBytes("utf-8");
		futureB = mExecutor.submit(new GcBleWriteCallable(mGcBleTransceiver, mBluetoothDevice, GcBleGattAttributes.GC_PHONE_SSID, ssidArray));
		
		byte[] passwordArray = mPassword.getBytes("utf-8");
		futureC = mExecutor.submit(new GcBleWriteCallable(mGcBleTransceiver, mBluetoothDevice, GcBleGattAttributes.GC_PHONE_PASSWORD, passwordArray));

		if ((futureA.get() == null) || (futureB.get() == null) || (futureC.get() == null)) {

			sendMessage(false, Common.ERROR_GATT_WRITE);
			return;
		}
		
		futureD = mExecutor.submit(new GcBleReceiveNotificationCallable(mGcBleTransceiver, mBluetoothDevice, GcBleGattAttributes.GC_PHONE_WIFI_ERROR));
		///futureE = mExecutor.submit(new GcBleSetNotificationCallable(mGcBleTransceiver, mBluetoothDevice, GcBleGattAttributes.GC_PHONE_WIFI_ERROR, true));
		///if (futureE.get() == null) {

			///sendMessage(false, Common.ERROR_GATT_SET_NOTIFICATION);
			///return;
		///}

		byte b1 = mSecurity;
		byte b2 = 0;
		if (mWifiAP.equals(WifiAP.AP_NORMAL)) {
			b2 = 4;
		} else if (mWifiAP.equals(WifiAP.AP_HOTSPOT)) {
			b2 = 5;
		}

		byte[] cfgArray = {(byte) 1, (byte) b1, (byte) b2};
		futureF = mExecutor.submit(new GcBleReliableWriteCallable(mGcBleTransceiver, mExecutor, mBluetoothDevice, GcBleGattAttributes.GC_PHONE_WIFI_CFG, cfgArray));
		if (futureF.get() == null) {

			sendMessage(false, Common.ERROR_GATT_WRITE);
			return;
		}

		result = futureD.get();
		
		if (result != null) {

			byte[] connectResult = GcBleGattAttributeUtil.getWifiConnectResult(result);
			
			if (connectResult[1] == 0x00) {
				
				Log.d(TAG, "[MGCC] AP connect successful");
				
				sendMessage(true, 0);

			} else {
				
				Log.d(TAG, "[MGCC] AP connect error, Error code = " + connectResult[1]);

				sendMessage(true, (int)connectResult[1]);
			}
			
		} else {
			
			sendMessage(false, Common.ERROR_GATT_RECEIVE_NOTIFICATION);
		}
	}
	
	
	
	private void eraseAP() throws Exception {
		
		Log.d(TAG, "[MGCC] AP erasing...");
		
		BluetoothGattCharacteristic result;
		Future<BluetoothGattCharacteristic> futureA, futureB;
		
		futureA = mExecutor.submit(new GcBleReceiveNotificationCallable(mGcBleTransceiver, mBluetoothDevice, GcBleGattAttributes.GC_AUTO_BACKUP_RESPONSE, 20000));
		
		char[] temp0 = mSsidString.toCharArray();
		byte[] ssidArray = new byte[temp0.length + 2];
		ssidArray[0] = mSecurity;
		ssidArray[1] = (byte) temp0.length;
		for (int cnt = 0; cnt < temp0.length; cnt++) {
			ssidArray[cnt + 2] = (byte) temp0[cnt];
		}
		futureB = mExecutor.submit(new GcBleWriteCallable(mGcBleTransceiver, mBluetoothDevice, GcBleGattAttributes.GC_AUTO_BACKUP_ERASE_AP, ssidArray));

		if (futureB.get() == null) {

			sendMessage(false, Common.ERROR_GATT_WRITE);
			return;
		}
		
		result = futureA.get();
		
		if (result != null) {
			
			if (result.getUuid().toString().equals(GcBleGattAttributes.GC_AUTO_BACKUP_RESPONSE)) {
				
				byte[] value = result.getValue();
				
				if (value[0] == 0x05) {
					
					sendMessage(true, (int)value[1]);

				} else {
					
					sendMessage(false, Common.ERROR_FAIL);
				}
				
			} else {
				
				sendMessage(false, Common.ERROR_FAIL);
			}

		} else {
			
			sendMessage(false, Common.ERROR_GATT_RECEIVE_NOTIFICATION);
		}
	}
	
	
	
	private void setProxy() throws Exception {

		Log.d(TAG, "[MGCC] Setting proxy...");
		
		BluetoothGattCharacteristic result;
		Future<BluetoothGattCharacteristic> futureA, futureB;
		
		futureA = mExecutor.submit(new GcBleReceiveNotificationCallable(mGcBleTransceiver, mBluetoothDevice, GcBleGattAttributes.GC_AUTO_BACKUP_RESPONSE, 20000));
		
		char[] temp0 = mSsidString.toCharArray();
		char[] temp1 = mProxyString.toCharArray();
		byte[] proxyArray = new byte[temp0.length + temp1.length + 6];
		
		proxyArray[0] = (byte) 0;
		proxyArray[1] = (byte) (mPort & 0xff);
		proxyArray[2] = (byte) ((mPort >> 8) & 0xff);
		proxyArray[3] = mSecurity;
		proxyArray[4] = (byte) temp0.length;
		for (int cnt = 0; cnt < temp0.length; cnt++) {
			proxyArray[cnt + 5] = (byte) temp0[cnt];
		}
		proxyArray[temp0.length + 5] = (byte) temp1.length;
		for (int cnt = 0; cnt < temp1.length; cnt++) {
			proxyArray[cnt + temp0.length + 6] = (byte) temp1[cnt];
		}

		futureB = mExecutor.submit(new GcBleWriteCallable(mGcBleTransceiver, mBluetoothDevice, GcBleGattAttributes.GC_AUTO_BACKUP_PROXY, proxyArray));

		if (futureB.get() == null) {

			sendMessage(false, Common.ERROR_GATT_WRITE);
			return;
		}
		
		result = futureA.get();
		
		if (result != null) {
			
			if (result.getUuid().toString().equals(GcBleGattAttributes.GC_AUTO_BACKUP_RESPONSE)) {
				
				byte[] value = result.getValue();
				
				if (value[0] == 0x04) {
					
					sendMessage(true, (int)value[1]);

				} else {
					
					sendMessage(false, Common.ERROR_FAIL);
				}
				
			} else {
				
				sendMessage(false, Common.ERROR_FAIL);
			}

		} else {
			
			sendMessage(false, Common.ERROR_GATT_RECEIVE_NOTIFICATION);
		}
	}
	
	
	
	private void getProxy() throws Exception {

		Log.d(TAG, "[MGCC] Getting proxy...");
		
		BluetoothGattCharacteristic result;
		Future<BluetoothGattCharacteristic> futureA, futureB;
		
		futureA = mExecutor.submit(new GcBleReceiveNotificationCallable(mGcBleTransceiver, mBluetoothDevice, GcBleGattAttributes.GC_AUTO_BACKUP_GET_PROXY, 20000));
		
		char[] temp0 = mSsidString.toCharArray();
		byte[] proxyArray = new byte[temp0.length + 6];
		
		proxyArray[0] = (byte) 1;
		proxyArray[1] = (byte) 0;
		proxyArray[2] = (byte) 0;
		proxyArray[3] = mSecurity;
		proxyArray[4] = (byte) temp0.length;
		for (int cnt = 0; cnt < temp0.length; cnt++) {
			proxyArray[cnt + 5] = (byte) temp0[cnt];
		}
		proxyArray[temp0.length + 5] = (byte) 0;

		futureB = mExecutor.submit(new GcBleWriteCallable(mGcBleTransceiver, mBluetoothDevice, GcBleGattAttributes.GC_AUTO_BACKUP_PROXY, proxyArray));

		if (futureB.get() == null) {

			sendMessage(false, Common.ERROR_GATT_WRITE);
			return;
		}
		
		result = futureA.get();
		
		if (result != null) {
			
			if (result.getUuid().toString().equals(GcBleGattAttributes.GC_AUTO_BACKUP_GET_PROXY)) {
				
				byte[] value = result.getValue();
				int port = (int) GcBleGattAttributeUtil.byteArrayToShort(value, 1);
				String proxy = GcBleGattAttributeUtil.getProxy(result);

				sendMessage(true, (int)value[0], proxy, port);
				
			} else {
				
				sendMessage(false, Common.ERROR_FAIL);
			}

		} else {
			
			sendMessage(false, Common.ERROR_GATT_RECEIVE_NOTIFICATION);
		}
	}
	
	
	
	private void scanAP() throws Exception {
		
		Log.d(TAG, "[MGCC] AP scanning...");
		
		BluetoothGattCharacteristic result;
		Future<BluetoothGattCharacteristic> futureA, futureB;
		
		futureA = mExecutor.submit(new GcBleReceiveNotificationCallable(mGcBleTransceiver, mBluetoothDevice, GcBleGattAttributes.GC_AUTO_BACKUP_RESPONSE, 60000));

		byte[] actionArray = {(byte) 2, (byte) mOption};
		futureB = mExecutor.submit(new GcBleWriteCallable(mGcBleTransceiver, mBluetoothDevice, GcBleGattAttributes.GC_AUTO_BACKUP_ACTION, actionArray));

		if (futureB.get() == null) {

			sendMessage(false, Common.ERROR_GATT_WRITE);
			return;
		}
		
		result = futureA.get();
		
		if (result != null) {
			
			if (result.getUuid().toString().equals(GcBleGattAttributes.GC_AUTO_BACKUP_RESPONSE)) {
				
				byte[] value = result.getValue();
				
				if (value[0] == 0x02) {
					
					sendMessage(true, (int)value[1]);

				} else {
					
					sendMessage(false, Common.ERROR_FAIL);
				}

			} else {
				
				sendMessage(false, Common.ERROR_FAIL);
			}

		} else {
			
			sendMessage(false, Common.ERROR_GATT_RECEIVE_NOTIFICATION);
		}
	}
	
	
	
	private void getStatus() throws Exception {
		
		Log.d(TAG, "[MGCC] Get status...");
		
		BluetoothGattCharacteristic result;
		Future<BluetoothGattCharacteristic> futureA, futureB;
		
		futureA = mExecutor.submit(new GcBleReceiveNotificationCallable(mGcBleTransceiver, mBluetoothDevice, GcBleGattAttributes.GC_AUTO_BACKUP_RESPONSE, 60000));

		byte[] actionArray = {(byte) 6, (byte) mOption};
		futureB = mExecutor.submit(new GcBleWriteCallable(mGcBleTransceiver, mBluetoothDevice, GcBleGattAttributes.GC_AUTO_BACKUP_ACTION, actionArray));

		if (futureB.get() == null) {

			sendMessage(false, Common.ERROR_GATT_WRITE);
			return;
		}
		
		result = futureA.get();
		
		if (result != null) {
			
			if (result.getUuid().toString().equals(GcBleGattAttributes.GC_AUTO_BACKUP_RESPONSE)) {
				
				byte[] value = result.getValue();
				
				if (value[0] == 0x06) {
					
					sendMessage(true, (int)value[1]);

				} else {
					
					sendMessage(false, Common.ERROR_FAIL);
				}

			} else {
				
				sendMessage(false, Common.ERROR_FAIL);
			}

		} else {
			
			sendMessage(false, Common.ERROR_GATT_RECEIVE_NOTIFICATION);
		}
	}
	
	
	
	private void sendMessage(boolean result, int errorCode) {
		
		sendMessage(result, errorCode, null, -1);
	}
	
	
	
	private void sendMessage(boolean result, int errorCode, String proxy, int port) {
		
		try {
			
			Message outMsg = Message.obtain();
			
			if (mAction == ACTION_SET_AUTO_BACKUP_LTEVENT) {
				
				outMsg.what = IGcConnectivityService.CB_SET_AUTO_BACKUP_LTEVENT_RESULT;
				
			} else if (mAction == ACTION_CLR_AUTO_BACKUP_LTEVENT) {
				
				outMsg.what = IGcConnectivityService.CB_CLR_AUTO_BACKUP_LTEVENT_RESULT;

			} else if (mAction == ACTION_SET_AUTO_BACKUP_AP) {
				
				outMsg.what = IGcConnectivityService.CB_SET_AUTO_BACKUP_AP_RESULT;
				
			} else if (mAction == ACTION_CLR_AUTO_BACKUP_AP) {
				
				outMsg.what = IGcConnectivityService.CB_CLR_AUTO_BACKUP_AP_RESULT;
				
			} else if (mAction == ACTION_SET_AUTO_BACKUP_PROXY) {
				
				outMsg.what = IGcConnectivityService.CB_SET_AUTO_BACKUP_PROXY_RESULT;

			} else if (mAction == ACTION_GET_AUTO_BACKUP_PROXY) {

				outMsg.what = IGcConnectivityService.CB_GET_AUTO_BACKUP_PROXY_RESULT;
				
			} else if (mAction == ACTION_SET_AUTO_BACKUP_AP_SCAN) {
				
				outMsg.what = IGcConnectivityService.CB_SET_AUTO_BACKUP_AP_SCAN_RESULT;

			} else if (mAction == ACTION_GET_AUTO_BACKUP_STATUS) {
				
				outMsg.what = IGcConnectivityService.CB_GET_AUTO_BACKUP_STATUS_RESULT;
			}
			
			Bundle outData = new Bundle();
			
			if (result) {
				
				outData.putSerializable(IGcConnectivityService.PARAM_RESULT, IGcConnectivityService.Result.RESULT_SUCCESS);
				outData.putInt(IGcConnectivityService.PARAM_AUTO_BACKUP_ERROR_CODE, errorCode);
				
			} else {
				
				outData.putSerializable(IGcConnectivityService.PARAM_RESULT, IGcConnectivityService.Result.RESULT_FAIL);
				outData.putInt(IGcConnectivityService.PARAM_AUTO_BACKUP_ERROR_CODE, errorCode);
			}
			
			if (proxy != null) {
				
				outData.putString(IGcConnectivityService.PARAM_AP_PROXY, proxy);
			}
			
			if (port >= 0) {
				
				outData.putInt(IGcConnectivityService.PARAM_AP_PORT, port);
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

		sendMessage(false, Common.ERROR_FAIL);
	}
}
