package com.htc.gc.connectivity.v3.internal.tasks;

import java.lang.reflect.Method;
import java.util.Calendar;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import com.htc.gc.connectivity.interfaces.IGcConnectivityServiceBase.WifiAP;
import com.htc.gc.connectivity.v3.interfaces.IGcConnectivityService;
import com.htc.gc.connectivity.v3.interfaces.IGcConnectivityService.BackupProcessStatus;
import com.htc.gc.connectivity.v3.interfaces.IGcConnectivityService.BackupProviderIdIndex;
import com.htc.gc.connectivity.v3.interfaces.IGcConnectivityService.BackupTokenType;
import com.htc.gc.connectivity.v3.interfaces.IGcConnectivityService.WifiConfigureType;
import com.htc.gc.connectivity.v3.internal.callables.GcBleReceiveNotificationCallable;
import com.htc.gc.connectivity.v3.internal.callables.GcBleReliableWriteCallable;
import com.htc.gc.connectivity.v3.internal.callables.GcBleWriteCallable;
import com.htc.gc.connectivity.v3.internal.callables.GcBootUpCallable;
import com.htc.gc.connectivity.v3.internal.common.Common;
import com.htc.gc.connectivity.v3.internal.common.GcConnectivityTask;
import com.htc.gc.connectivity.v3.internal.component.le.GcBleGattAttributeUtil;
import com.htc.gc.connectivity.v3.internal.component.le.GcBleGattAttributes;
import com.htc.gc.connectivity.v3.internal.component.le.GcBleTransceiver;
import com.htc.gc.connectivity.v3.internal.component.wifi.GcWifiTransceiver;

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
	public final static int ACTION_SET_AUTO_BACKUP_AP_SCAN_START = 6;
	public final static int ACTION_SET_AUTO_BACKUP_AP_SCAN_STOP = 7;

	private final static int COMMAND_PADDING_AUTH_TOKEN = 4;
	private final static int COMMAND_LENGTH_CONNECTAP   = 10;
	
	private BluetoothDevice mBluetoothDevice;
	private int mAction;
	private String mSsidString;
	private String mPassword;
	private byte mSecurity;
	private int mPort;
	private String mProxyString;
	private int mOption;
	private BackupProviderIdIndex mPidx;
	private BackupTokenType mTokenType;
	private String mToken;
	
	
	
	public GcAutoBackupTask(GcBleTransceiver gcBleTransceiver, GcWifiTransceiver gcWifiTransceiver, Messenger messenger, ExecutorService executor, BluetoothDevice device, int action,
		 String ssid, String passwd, byte security, int port, String proxy, BackupProviderIdIndex pidx, BackupTokenType token_type, String token, int option) {

		super(gcBleTransceiver, gcWifiTransceiver, messenger, executor);
		
		mBluetoothDevice = device;
		mAction = action;
		mSsidString = ssid;

		mPassword = passwd;
		if ((mPassword == null) || ((mPassword != null) && (mPassword.length() <= 0))) {
			
			mPassword = "\0";
		}

		mSecurity = security;
		mPort = port;
		mProxyString = proxy;
		mPidx = pidx;
		mTokenType = token_type;
		mToken = token;
		mOption = option;
	}
	
	public GcAutoBackupTask(GcBleTransceiver gcBleTransceiver, GcWifiTransceiver gcWifiTransceiver, Messenger messenger, ExecutorService executor, BluetoothDevice device, int action, int option) {

		/// ACTION_SET_AUTO_BACKUP_LTEVENT, ACTION_CLR_AUTO_BACKUP_LTEVENT, ACTION_SET_AUTO_BACKUP_AP_SCAN, ACTION_GET_AUTO_BACKUP_STATUS
		this(gcBleTransceiver, gcWifiTransceiver, messenger, executor, device, action, null, null, (byte) 0, 0, null, null, null, null, option);
	}

	
	
	public GcAutoBackupTask(GcBleTransceiver gcBleTransceiver, GcWifiTransceiver gcWifiTransceiver, Messenger messenger, ExecutorService executor, BluetoothDevice device, int action,
		 String ssid, String passwd, byte security) {
	
		/// ACTION_SET_AUTO_BACKUP_AP, ACTION_CLR_AUTO_BACKUP_AP
		this(gcBleTransceiver, gcWifiTransceiver, messenger, executor, device, action, ssid, passwd, security, 0, null, null, null, null, -1);
	}

	
	
	public GcAutoBackupTask(GcBleTransceiver gcBleTransceiver, GcWifiTransceiver gcWifiTransceiver, Messenger messenger, ExecutorService executor, BluetoothDevice device, int action, String ssid, int port, String proxy, byte security) {

		/// ACTION_SET_AUTO_BACKUP_PROXY, ACTION_GET_AUTO_BACKUP_PROXY
		this(gcBleTransceiver, gcWifiTransceiver, messenger, executor, device, action, ssid, null, security, port, proxy, null, null, null, -1);
	}

	public GcAutoBackupTask(GcBleTransceiver gcBleTransceiver, GcWifiTransceiver gcWifiTransceiver, Messenger messenger, ExecutorService executor, BluetoothDevice device, int action, BackupProviderIdIndex pidx, BackupTokenType token_type, String token) {

		/// ACTION_GET_AUTO_BACKUP_AUTH_TOKEN
		this(gcBleTransceiver, gcWifiTransceiver, messenger, executor, device, action, null, null, (byte) 0, 0, null, pidx, token_type, token, -1);
	}
	
	@Override
	public void execute() throws Exception {
		
		super.execute();
		
		super.from();
		
		switch(mAction)
		{
		case ACTION_SET_AUTO_BACKUP_LTEVENT:
		case ACTION_CLR_AUTO_BACKUP_LTEVENT:
			sendSuccessMessage(Common.ERROR_SUCCESS);
			break;
		case ACTION_SET_AUTO_BACKUP_AP:
			Integer result;
			Future<Integer> future;
			
			future = mExecutor.submit(new GcBootUpCallable(mGcBleTransceiver, mExecutor, mBluetoothDevice, IGcConnectivityService.BootUpType.BOOTUP_LINUX, mMessenger));
			result = future.get();
			
			if (result == Common.ERROR_SUCCESS) {
				connectAP();
			} else {
				sendFailMessage(result);
			}
			break;
		case ACTION_CLR_AUTO_BACKUP_AP:
			future = mExecutor.submit(new GcBootUpCallable(mGcBleTransceiver, mExecutor, mBluetoothDevice, IGcConnectivityService.BootUpType.BOOTUP_LINUX, mMessenger));
			result = future.get();
			
			if (result == Common.ERROR_SUCCESS) {
				eraseAP();
			} else {
				sendFailMessage(result);
			}
			break;
		case ACTION_SET_AUTO_BACKUP_PROXY:
			future = mExecutor.submit(new GcBootUpCallable(mGcBleTransceiver, mExecutor, mBluetoothDevice, IGcConnectivityService.BootUpType.BOOTUP_LINUX, mMessenger));
			result = future.get();
			
			if (result == Common.ERROR_SUCCESS) {
				setProxy();
			} else {
				sendFailMessage(result);
			}
			break;
		case ACTION_GET_AUTO_BACKUP_PROXY:
			future = mExecutor.submit(new GcBootUpCallable(mGcBleTransceiver, mExecutor, mBluetoothDevice, IGcConnectivityService.BootUpType.BOOTUP_LINUX, mMessenger));
			result = future.get();
			
			if (result == Common.ERROR_SUCCESS) {
				getProxy();
			} else {
				sendFailMessage(result);
			}
			break;
		case ACTION_SET_AUTO_BACKUP_AP_SCAN_START:
		case ACTION_SET_AUTO_BACKUP_AP_SCAN_STOP:
			future = mExecutor.submit(new GcBootUpCallable(mGcBleTransceiver, mExecutor, mBluetoothDevice, IGcConnectivityService.BootUpType.BOOTUP_LINUX, mMessenger));
			result = future.get();
			
			if (result == Common.ERROR_SUCCESS) {
				scanAP();
			} else {
				sendFailMessage(result);
			}
			break;
		default:
			break;
		}

		super.to(TAG);
	}
	
	
	
	private void connectAP() throws Exception {
		
		Log.d(TAG, "[MGCC] AP connecting...");

		BluetoothGattCharacteristic result;

		Future<BluetoothGattCharacteristic> futureA, futureB, futureC, futureD, futureF;
		
		//Send Configuration
		char[] tempCD = getCountryCode().toCharArray();
		byte[] ssidArray = mSsidString.getBytes("utf-8");
		byte[] passwordArray = mPassword.getBytes("utf-8");
		byte[] configWifiArray = new byte[COMMAND_LENGTH_CONNECTAP];
		configWifiArray[0] = WifiConfigureType.WIFI_CONN_AP.getType();
		configWifiArray[1] = (byte) tempCD[0];
		configWifiArray[2] = (byte) tempCD[1];
		//configWifiArray[3] = (byte) 0; //2.4G! Select Band later
		configWifiArray[4] = mSecurity;
	    //5: Channel 6-9:Static IP are reserved

		//Leave the late byte is '\0' +
		byte[] ssid_add_empty     = new byte[ssidArray.length + 1];
		byte[] password_add_empty = new byte[passwordArray.length + 1];

		for (int i = 0; i < ssidArray.length; i++)
		{
			ssid_add_empty[i] = ssidArray[i];
		}

		for (int i = 0; i < passwordArray.length; i++)
		{
			password_add_empty[i] = passwordArray[i];
		}
		//Leave the late byte is '\0' -

		futureA = mExecutor.submit(new GcBleWriteCallable(mGcBleTransceiver, mBluetoothDevice, GcBleGattAttributes.GcV2CommandEnum.WIFI_SET_SSID_REQUEST, ssid_add_empty));
		if ((futureA.get() == null)) {
			sendFailMessage(Common.ERROR_GATT_WRITE);
			return;
		}

	    futureB = mExecutor.submit(new GcBleWriteCallable(mGcBleTransceiver, mBluetoothDevice, GcBleGattAttributes.GcV2CommandEnum.WIFI_SET_PASSWORD_REQUEST, password_add_empty));
		if ((futureB.get() == null)) {
			sendFailMessage(Common.ERROR_GATT_WRITE);
			return;
		}

	    futureC = mExecutor.submit(new GcBleWriteCallable(mGcBleTransceiver, mBluetoothDevice, GcBleGattAttributes.GcV2CommandEnum.WIFI_CONFIG_REQUEST, configWifiArray));
		if ((futureC.get() == null)) {
			sendFailMessage(Common.ERROR_GATT_WRITE);
			return;
		}

		futureD = mExecutor.submit(new GcBleReceiveNotificationCallable(mGcBleTransceiver, mBluetoothDevice, GcBleGattAttributes.GcV2CommandEnum.WIFI_CONFIG_STATUS_EVENT));
		result = futureD.get();
		if(result != null)
		{
			 byte[] resultValue = result.getValue();
			 if (resultValue[1] == (byte)0)
			 {
				 sendSuccessMessage(Common.ERROR_SUCCESS);
			 }
			 else
			 {
				 sendSuccessMessage(resultValue[1]);
			 }
		}
		else
		{
			sendFailMessage(Common.ERROR_GATT_RECEIVE_NOTIFICATION);
		}
	}
	
	
	
	private void eraseAP() throws Exception {
		
		Log.d(TAG, "[MGCC] AP erasing...");

		BluetoothGattCharacteristic result;
		Future<BluetoothGattCharacteristic> futureA, futureB;
		
		//futureA = mExecutor.submit(new GcBleReceiveNotificationCallable(mGcBleTransceiver, mBluetoothDevice, GcBleGattAttributes.GC_AUTO_BACKUP_RESPONSE, 20000));
		
		char[] temp0 = mSsidString.toCharArray();
		byte[] ssidArray = new byte[temp0.length + 3];
		ssidArray[0] = mSecurity;
		ssidArray[1] = (byte) temp0.length;
		for (int cnt = 0; cnt < temp0.length; cnt++) {
			ssidArray[cnt + 2] = (byte) temp0[cnt];
		}
		futureB = mExecutor.submit(new GcBleWriteCallable(mGcBleTransceiver, mBluetoothDevice, GcBleGattAttributes.GcV2CommandEnum.WIFI_ERASE_AP_CONFIG_REQUEST, ssidArray));

		if (futureB.get() == null) {

			sendFailMessage(Common.ERROR_GATT_WRITE);
			return;
		}
		else
		{
			sendSuccessMessage(Common.ERROR_SUCCESS);
		}
/*
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
		}*/
	}
	
	
	
	private void setProxy() throws Exception {

		Log.d(TAG, "[MGCC] Setting proxy...");
		/*
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
		*/
	}
	
	
	
	private void getProxy() throws Exception {

		Log.d(TAG, "[MGCC] Getting proxy...");
		/*
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
		*/
	}
	
	
	
	private void scanAP() throws Exception {
		
		Log.d(TAG, "[MGCC] AP scanning...");

		BluetoothGattCharacteristic result;
		Future<BluetoothGattCharacteristic> futureA, futureB;

		byte[] actionArray = new byte[2];
		if (ACTION_SET_AUTO_BACKUP_AP_SCAN_START == mAction)
		{
			actionArray[0] = (byte) 0;
			actionArray[1] = (byte) mOption;
		}
		else
		{
			actionArray[0] = (byte) 1;
		}

		futureB = mExecutor.submit(new GcBleWriteCallable(mGcBleTransceiver, mBluetoothDevice, GcBleGattAttributes.GcV2CommandEnum.WIFI_SCAN_REQUEST, actionArray));

		if (futureB.get() == null) {
			sendFailMessage(Common.ERROR_GATT_WRITE);
		}
		else
		{
			sendSuccessMessage(Common.ERROR_SUCCESS);
		}
	}
	
	
	
	private void sendSuccessMessage(int errorCode) {
		
		try {
			
			Message outMsg = Message.obtain();
			
			outMsg.what = getCorrespondingMessage();
			
			Bundle outData = new Bundle();
			
			outData.putSerializable(IGcConnectivityService.PARAM_RESULT, IGcConnectivityService.Result.RESULT_SUCCESS);
			outData.putInt(IGcConnectivityService.PARAM_AUTO_BACKUP_ERROR_CODE, errorCode);
			
			outMsg.setData(outData);
		
			mMessenger.send(outMsg);

		} catch (RemoteException e) {

			e.printStackTrace();
		}
	}
	
	
	private void sendSuccessMessage(int errorCode,
			BackupProcessStatus backupProcessStatus,
			BackupProviderIdIndex backupProviderIndex, int unbackupItemNumber,
			int totalItemNumber, Calendar lastBackupDateTime) {
		
		try {
			
			Message outMsg = Message.obtain();
			
			outMsg.what = IGcConnectivityService.CB_GET_AUTO_BACKUP_STATUS_RESULT;
			
			Bundle outData = new Bundle();
			
			outData.putSerializable(IGcConnectivityService.PARAM_RESULT, IGcConnectivityService.Result.RESULT_SUCCESS);
			outData.putInt(IGcConnectivityService.PARAM_AUTO_BACKUP_ERROR_CODE, errorCode);
			outData.putSerializable(IGcConnectivityService.PARAM_AUTO_BACKUP_PROCESS_STATUS, backupProcessStatus);
			outData.putSerializable(IGcConnectivityService.PARAM_AUTO_BACKUP_PROVIDER_INDEX, backupProviderIndex);
			outData.putInt(IGcConnectivityService.PARAM_AUTO_BACKUP_UNBACKUP_ITEM_NUMBER, unbackupItemNumber);
			outData.putInt(IGcConnectivityService.PARAM_AUTO_BACKUP_TOTAL_ITEM_NUMBER, totalItemNumber);
			outData.putSerializable(IGcConnectivityService.PARAM_AUTO_BACKUP_LAST_BACKUP_DATE_TIME, lastBackupDateTime);
			
			outMsg.setData(outData);
		
			mMessenger.send(outMsg);

		} catch (RemoteException e) {

			e.printStackTrace();
		}

	}
	
	
	private void sendFailMessage(int errorCode) {
		
		try {
			
			Message outMsg = Message.obtain();
			
			outMsg.what = getCorrespondingMessage();
			
			Bundle outData = new Bundle();
			
			outData.putSerializable(IGcConnectivityService.PARAM_RESULT, IGcConnectivityService.Result.RESULT_FAIL);
			outData.putInt(IGcConnectivityService.PARAM_AUTO_BACKUP_ERROR_CODE, errorCode);
			
			outMsg.setData(outData);
		
			mMessenger.send(outMsg);

		} catch (RemoteException e) {

			e.printStackTrace();
		}
	}
	
	
	private int getCorrespondingMessage() {
		switch (mAction) {
		case ACTION_SET_AUTO_BACKUP_LTEVENT:
			return IGcConnectivityService.CB_SET_AUTO_BACKUP_LTEVENT_RESULT;
			
		case ACTION_CLR_AUTO_BACKUP_LTEVENT:
			return IGcConnectivityService.CB_CLR_AUTO_BACKUP_LTEVENT_RESULT;
			
		case ACTION_SET_AUTO_BACKUP_AP:
			return IGcConnectivityService.CB_SET_AUTO_BACKUP_AP_RESULT;
			
		case ACTION_CLR_AUTO_BACKUP_AP:
			return IGcConnectivityService.CB_CLR_AUTO_BACKUP_AP_RESULT;
			
		case ACTION_SET_AUTO_BACKUP_PROXY:
			return IGcConnectivityService.CB_SET_AUTO_BACKUP_PROXY_RESULT;
			
		case ACTION_GET_AUTO_BACKUP_PROXY:
			return IGcConnectivityService.CB_GET_AUTO_BACKUP_PROXY_RESULT;
			
		case ACTION_SET_AUTO_BACKUP_AP_SCAN_START:
			return IGcConnectivityService.CB_SET_AUTO_BACKUP_AP_SCAN_START_RESULT;
			
		case ACTION_SET_AUTO_BACKUP_AP_SCAN_STOP:
			return IGcConnectivityService.CB_SET_AUTO_BACKUP_AP_SCAN_STOP_RESULT;
			
		default:
			Log.e(TAG, "[MGCC] unknown action");
			return -1;
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

		sendFailMessage(Common.ERROR_FAIL);
	}
}
