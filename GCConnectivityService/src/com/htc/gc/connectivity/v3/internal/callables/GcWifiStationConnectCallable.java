package com.htc.gc.connectivity.v3.internal.callables;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import com.htc.gc.connectivity.internal.common.CommonBase.GcWifiTransceiverErrorCode;
import com.htc.gc.connectivity.v3.interfaces.IGcConnectivityService;
import com.htc.gc.connectivity.v3.interfaces.IGcConnectivityService.WifiConfigBand;
import com.htc.gc.connectivity.v3.interfaces.IGcConnectivityService.WifiConfigSecurity;
import com.htc.gc.connectivity.v3.interfaces.IGcConnectivityService.WifiConfigureType;
import com.htc.gc.connectivity.v3.internal.common.Common;
import com.htc.gc.connectivity.v3.internal.component.le.GcBleGattAttributeUtil;
import com.htc.gc.connectivity.v3.internal.component.le.GcBleGattAttributes;
import com.htc.gc.connectivity.v3.internal.component.le.GcBleTransceiver;
import com.htc.gc.connectivity.v3.internal.component.wifi.GcWifiTransceiver;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.os.Messenger;
import android.util.Log;



public class GcWifiStationConnectCallable implements Callable<Integer> {

	private final static String TAG = "GcWifiStationConnectCallable";
	private final static boolean bPerformanceNotify = true;
	
	private final LinkedBlockingQueue<Integer> mCallbackQueue = new LinkedBlockingQueue<Integer>();

	private Random mRnd = new Random();
	
	protected ExecutorService mExecutor;
	protected GcBleTransceiver mGcBleTransceiver;
	protected BluetoothDevice mBluetoothDevice;
	protected GcWifiTransceiver mGcWifiTransceiver;
	protected int mConnectMethod;
	protected int mConvertUTF8;
	protected String mCountryCode;
	protected long mTimePrev;
	protected Messenger mMessenger;
	
	private String mIpAddress;
	
	
	
	public GcWifiStationConnectCallable(GcBleTransceiver bleTransceiver, GcWifiTransceiver wifiTransceiver, ExecutorService executor, BluetoothDevice device, String countryCode, int method, int convertUTF8, Messenger messenger) {

		mGcBleTransceiver = bleTransceiver;
		mGcWifiTransceiver = wifiTransceiver;
		mExecutor = executor;
		mBluetoothDevice = device;
		mCountryCode = countryCode;
		mConnectMethod = method;
		mConvertUTF8 = convertUTF8;
		mMessenger = messenger;
		
		mIpAddress = null;
	}



	@Override
	public Integer call() throws Exception {

		from();
		
		Integer ret = Common.ERROR_SUCCESS;
		
		if (mGcBleTransceiver != null) {

			if ((mGcWifiTransceiver != null) && (mGcWifiTransceiver.getP2pGroupState().equals(GcWifiTransceiver.WifiP2pGroupState.STATE_P2P_GROUP_CREATED))) {
				
				Integer bootResult;
				Future<Integer> futureBoot;

				futureBoot = mExecutor.submit(new GcBootUpCallable(mGcBleTransceiver, mExecutor, mBluetoothDevice, IGcConnectivityService.BootUpType.BOOTUP_LINUX, mMessenger));
				bootResult = futureBoot.get();
				
				if (bootResult == Common.ERROR_SUCCESS) {

					ret = connectWifi();

				} else {
					
					ret = bootResult;
				}
				
			} else {

				ret = Common.ERROR_P2P_GROUP;
			}

		} else {
			
			ret = Common.ERROR_FAIL;
		}

		to(TAG);

		return ret;
	}
	
	private static final int TIMEOUT_MS = 60000;
	private static final int UDP_SERVER_PORT = 7777;
	private static final int UDP_PACKET_SIZE = 1024;
	
	private int connectWifi() throws Exception {
		
		Log.d(TAG, "[MGCC] Wifi connecting...");
		
		BluetoothGattCharacteristic result;
		
		Future<BluetoothGattCharacteristic> /*futureA,*/ futureB, futureC,/* futureD,*/ futureF;
		
		byte[] ssidArray;
		byte[] ssidTempArray = mGcWifiTransceiver.getGroupName().getBytes("utf-8");
		if (mConvertUTF8 == 0) {
			
			ssidArray = ssidTempArray;
			
		} else {
			
			ssidArray = checkUTF8ByteArray(ssidTempArray);
			
		}
		futureB = mExecutor.submit(new GcBleWriteCallable(mGcBleTransceiver, mBluetoothDevice, GcBleGattAttributes.GcV2CommandEnum.WIFI_SET_SSID_REQUEST, ssidArray));
		if (futureB.get() == null)
		{
			return Common.ERROR_GATT_WRITE;
		}
		
		byte[] passwordArray = mGcWifiTransceiver.getGroupPassword().getBytes("utf-8");
		futureC = mExecutor.submit(new GcBleWriteCallable(mGcBleTransceiver, mBluetoothDevice, GcBleGattAttributes.GcV2CommandEnum.WIFI_SET_PASSWORD_REQUEST, passwordArray));

		if (/*(futureA.get() == null) || (futureB.get() == null) ||*/ (futureC.get() == null)) {
			return Common.ERROR_GATT_WRITE;
		}
		
		final Future<BluetoothGattCharacteristic> futureD = mExecutor.submit(new GcBleReceiveNotificationCallable(mGcBleTransceiver, mBluetoothDevice, GcBleGattAttributes.GcV2CommandEnum.WIFI_CONFIG_STATUS_EVENT, TIMEOUT_MS));

		char[] tempCD = mCountryCode.toCharArray();
		byte bcd0 = (byte) tempCD[0];
		byte bcd1 = (byte) tempCD[1];
		byte bandSelect = mGcWifiTransceiver.getBandSelect();
		byte wifiChannel = mGcWifiTransceiver.getWifiChannel();
		byte[] staticip = mGcWifiTransceiver.getStaticIP();
		byte[] serverArray = {WifiConfigureType.WIFI_STATION.getType(), bcd1, bcd0, bandSelect, WifiConfigSecurity.WIFI_WPA2.getSecurity(), wifiChannel, 0, 0, 0, 0};

		if (staticip[0] != (byte)0)
		{
			serverArray[6] = staticip[0];
			serverArray[7] = staticip[1];
			serverArray[8] = staticip[2];
			serverArray[9] = staticip[3];
		}

		futureF = mExecutor.submit(new GcBleWriteCallable(mGcBleTransceiver, mBluetoothDevice, GcBleGattAttributes.GcV2CommandEnum.WIFI_CONFIG_REQUEST, serverArray));
		if (futureF.get() == null) {
			return Common.ERROR_GATT_WRITE;
		}
		/*****************************************************/
		/** workaround in case BLE getIP fails++             */
		//FIXME: bad visibility scope
/*		final DatagramSocket socket;
		socket = new DatagramSocket(UDP_SERVER_PORT);
		Thread wifiGetIPThread = new Thread(new Runnable() {
			@Override
			public void run() {
				Log.d(TAG, "[MGCC] getGCIP workaround++");
				try {
					socket.setReuseAddress(true);
					byte[] recevieData = new byte[UDP_PACKET_SIZE];
					DatagramPacket dp = new DatagramPacket(recevieData, recevieData.length);
					socket.receive(dp);
					Log.i(TAG, "[MGCC] getGCIP workaround: UDP received");
					socket.close();
					String sz_gcIP = dp.getAddress().getHostAddress();
					Log.i(TAG, "[MGCC] getGCIP workaround: IP=" + sz_gcIP);
					if (mIpAddress == null && sz_gcIP != null) {
						mIpAddress = sz_gcIP;
						addCallback(Common.ERROR_SUCCESS);
					}
				} catch (Exception e) {
					if (socket != null)
						socket.close();
					Log.w(TAG, "[MGCC] getGCIP workaround failed: " + e.getCause());
					e.printStackTrace();
				}
				Log.d(TAG, "[MGCC] getGCIP workaround--");
			}
		});*/
		Thread BLEGetIPThread = new Thread(new Runnable() {
			@Override
			public void run() {
				Log.d(TAG, "[MGCC] getGCIP++");
				try {
					BluetoothGattCharacteristic result = futureD.get();
					if (result != null) {
						Log.d(TAG, "[MGCC] future result D = " + result.getUuid());
						byte[] connectResult = GcBleGattAttributeUtil.getWifiConnectResult(result);
						if (connectResult[0] == 0x00) {
							String ipAddress = GcBleGattAttributeUtil.getIpAddress(result);
							if (ipAddress.length() > 0) {
								Log.d(TAG, "[MGCC] WIFI connect successful, IP = " + ipAddress);
								mIpAddress = ipAddress;
								addCallback(Common.ERROR_SUCCESS);
							} else {
								Log.d(TAG, "[MGCC] WIFI connect error, got invalid IP");
								addCallback(Common.ERROR_GET_IP);
							}
						} else {
							Log.d(TAG, "[MGCC] WIFI connect error, Error code = " + connectResult[0]);
							addCallback(Integer.valueOf(connectResult[0]));
						}
					} else {
						Log.d(TAG, "[MGCC] future result D = null");
					}
				} catch (Exception e) {
					Log.w(TAG, "[MGCC] getGCIP failed: " + e.getCause());
					e.printStackTrace();
				}
				Log.d(TAG, "[MGCC] getGCIP--");
			}
		});
		//wifiGetIPThread.start();
		BLEGetIPThread.start();
		Integer errCode;
		try {
			errCode = mCallbackQueue.poll(TIMEOUT_MS, TimeUnit.MILLISECONDS);
		} catch (InterruptedException e) {
			Log.e(TAG, "[MGCC] poll callback timed out!!");
			return Common.ERROR_GATT_RECEIVE_NOTIFICATION;
		}
		Log.d(TAG, "[MGCC] getIP errCode=" + errCode);
		//socket.close();
		if (BLEGetIPThread.isAlive())
			BLEGetIPThread.interrupt();
		if ((errCode != null) && (errCode == Common.ERROR_SUCCESS)) {
			
			return Common.ERROR_SUCCESS;

		} else {
			
			if (checkP2PSsid(ssidTempArray)) {

				if (errCode == null) {

					return Common.ERROR_WIFI_TIMEOUT;
					
				} else {
					
					return errCode;
				}

			} else {
				
				Log.w(TAG, "[MGCC] ivalid p2p ssid");
				
				return Common.ERROR_P2P_SSID;
			}
		}
		/** workaround in case BLE getIP fails--             */
		/*****************************************************/
	}
	
	
	
	protected synchronized void addCallback(Integer errorCode) {

		Log.d(TAG, "[MGCC] addCallback errorCode = " + errorCode);

		if (errorCode != null) {

			mCallbackQueue.add(errorCode);
		}
	}
	
	
	
	public String getIpAddress() {
		
		return mIpAddress;
	}
	
	
	
	private void from() {
		
		if (bPerformanceNotify) {

			mTimePrev = System.currentTimeMillis();
		}
	}
	
	
	
	private void to(String task) {

		if (bPerformanceNotify) {

			long timeCurr = System.currentTimeMillis();
			long timeDiff = timeCurr - mTimePrev;
			
			Log.d(TAG, "[MGCC][MPerf] [" + task + "] costs: " + timeDiff + " ms");
		}
	}
	
	
	
	private boolean checkP2PSsid(byte[] ssidArray) {
		
		boolean ret = true;
		
		if (ssidArray.length <= 0) {
			
			return false;
		}

		for (int cnt = 0; cnt < ssidArray.length; cnt++) {

			if ((cnt + 1) < ssidArray.length) {

				if (mConvertUTF8 == 0) {

					/// Detect "\x", "\"" or "\\" in SSID array.
					if ((ssidArray[cnt] == 0x5C) &&
						((ssidArray[cnt + 1] == 0x5C) || (ssidArray[cnt + 1] == 0x78) || (ssidArray[cnt + 1] == 0x22))) {

						return false;
					}
					
				} else {
					
					/// Detect "\x" in SSID array.
					if ((ssidArray[cnt] == 0x5C) &&	(ssidArray[cnt + 1] == 0x78)) {

						return false;
					}
				}
			}
		}
		
		return ret;
	}
	
	
	
	private byte[] checkUTF8ByteArray(byte[] ssidArray) {
		
		byte[] retTempArray = new byte[32];
		byte[] oneChar = new byte[2];
		
		if (ssidArray.length <= 0) {
			
			return ssidArray;
		}

		/// For retTempArray[]
		int idx = 0;
		for (int cnt = 0; cnt < ssidArray.length; cnt++) {
			
			if ((cnt + 3) < ssidArray.length) {

				/// Detect "\x" in SSID array and translate to correct UTF-8 format.
				if ((ssidArray[cnt] == 0x5C) &&	(ssidArray[cnt + 1] == 0x78)) {

					oneChar[0] = ssidArray[cnt + 2];
					oneChar[1] = ssidArray[cnt + 3];
					
					retTempArray[idx] = convertToUTF8Byte(oneChar);
					
					///Log.d(TAG, "[MGCC] retTempArray[" + idx + "] = " + String.format("%x", retTempArray[idx]));
					
					/// Check if it is utf-8 non-ASCII format
					if ((retTempArray[idx] & 0x80) != 0x80) {
						
						return ssidArray;
					}
					
					cnt += 3;

				} else {

					retTempArray[idx] = ssidArray[cnt];
				}

			} else {
				
				retTempArray[idx] = ssidArray[cnt];
			}
			
			idx++;
		}
		
		/// Remove no use bytes
		byte[] retArray = new byte[idx];
		for (int i = 0; i < idx; i++) {
			
			retArray[i] = retTempArray[i];
		}
		
		return retArray;
	}
	
	
	
	private byte convertToUTF8Byte(byte[] oneChar) {
		
		byte ret = 0;
		int val;
		
		try {

			String temp = String.format("%c%c", oneChar[0], oneChar[1]);
		
			val = (int) Integer.parseInt(temp, 16);
			ret = (byte) (val & 0x00ff);
		
		} catch (Exception e) {
			
			Log.d(TAG, "[MGCC] convertToUTF8Byte e = " + e);
			ret = 0;
		}
		
		return ret;
	}
}