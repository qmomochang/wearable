package com.htc.gc.connectivity.v2.internal.tasks;

import java.util.Calendar;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import com.htc.gc.connectivity.v2.interfaces.IGcConnectivityService;
import com.htc.gc.connectivity.v2.internal.callables.GcBleWriteCallable;
import com.htc.gc.connectivity.v2.internal.common.GcConnectivityTask;
import com.htc.gc.connectivity.v2.internal.component.le.GcBleGattAttributes;
import com.htc.gc.connectivity.v2.internal.component.le.GcBleTransceiver;
import com.htc.gc.connectivity.v2.internal.component.wifi.GcWifiTransceiver;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.os.Bundle;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;



public class GcGpsInfoTask extends GcConnectivityTask {

	private final static String TAG = "GcGpsInfoTask";
	
	public final static int ACTION_SET_GPS_INFO_LTEVENT = 0;
	public final static int ACTION_CLR_GPS_INFO_LTEVENT = 1;
	public final static int ACTION_SET_GPS_INFO = 2;
	
	private BluetoothDevice mBluetoothDevice;
	private int mAction;
	private Calendar mCalendar;
	private double mLongitude;
	private double mLatitude;
	private double mAltitude;
	
	
	
	public GcGpsInfoTask(GcBleTransceiver gcBleTransceiver, GcWifiTransceiver gcWifiTransceiver, Messenger messenger, ExecutorService executor, BluetoothDevice device, int action,
							Calendar calendar, double longitude, double latitude, double altitude) {

		super(gcBleTransceiver, gcWifiTransceiver, messenger, executor);
		
		mBluetoothDevice = device;
		mAction = action;
		
		mCalendar = calendar;
		mLongitude = longitude;
		mLatitude = latitude;
		mAltitude = altitude;
	}

	
	
	@Override
	public void execute() throws Exception {
		
		super.execute();
		
		super.from();
		
		BluetoothGattCharacteristic result;
		Future<BluetoothGattCharacteristic> future;

		if (mAction == ACTION_SET_GPS_INFO_LTEVENT) {
			
			sendMessage(true);

		} else if (mAction == ACTION_CLR_GPS_INFO_LTEVENT) {

			sendMessage(true);

		} else if (mAction == ACTION_SET_GPS_INFO) {
			
			byte[] gpsInfoArray = getGpsInfo();

			future = mExecutor.submit(new GcBleWriteCallable(mGcBleTransceiver, mBluetoothDevice, GcBleGattAttributes.GC_GPS_DATA, gpsInfoArray));
			result = future.get();

			if (result != null) {
				
				sendMessage(true);

			} else {

				sendMessage(false);
			}
		}
		
		super.to(TAG);
	}
	
	
	
	private void sendMessage(boolean result) {
		
		try {
			
			Message outMsg = Message.obtain();
			
			if (mAction == ACTION_SET_GPS_INFO_LTEVENT) {
				
				outMsg.what = IGcConnectivityService.CB_SET_GPS_INFO_LTEVENT_RESULT;

			} else if (mAction == ACTION_CLR_GPS_INFO_LTEVENT) {

				outMsg.what = IGcConnectivityService.CB_CLR_GPS_INFO_LTEVENT_RESULT;

			} else if (mAction == ACTION_SET_GPS_INFO) {
			
				outMsg.what = IGcConnectivityService.CB_SET_GPS_INFO_RESULT;
			}
			
			Bundle outData = new Bundle();
			
			if (result) {
				
				outData.putSerializable(IGcConnectivityService.PARAM_RESULT, IGcConnectivityService.Result.RESULT_SUCCESS);
				
			} else {
				
				outData.putSerializable(IGcConnectivityService.PARAM_RESULT, IGcConnectivityService.Result.RESULT_FAIL);
			}
			
			outMsg.setData(outData);
		
			mMessenger.send(outMsg);

		} catch (RemoteException e) {

			e.printStackTrace();
		}
	}
	
	
	
	private byte[] getGpsInfo() {
		
		byte[] gpsArray = null;
		
		if (mAltitude > 0) {
			
			gpsArray = new byte[25];
			gpsArray[0] = (byte) (mCalendar.get(Calendar.YEAR) & 0xff);
			gpsArray[1] = (byte) ((mCalendar.get(Calendar.YEAR) >> 8) & 0xff);
			gpsArray[2] = (byte) (mCalendar.get(Calendar.MONTH));
			gpsArray[3] = (byte) (mCalendar.get(Calendar.DATE));
			gpsArray[4] = (byte) (mCalendar.get(Calendar.HOUR_OF_DAY));
			gpsArray[5] = (byte) (mCalendar.get(Calendar.MINUTE));
			gpsArray[6] = (byte) (mCalendar.get(Calendar.SECOND));

			int lat_deg = (int) Math.floor(Math.abs(mLatitude));
			double lat_min = Math.round((Math.abs(mLatitude) - lat_deg) * 600000);
			int ns1 = (int) (lat_deg * 100 + lat_min / 10000);
			int ns2 = (int) (lat_min % 10000);
			Log.d(TAG, "[MGCC] lat_deg = " + lat_deg);
			Log.d(TAG, "[MGCC] lat_min = " + lat_min);
			Log.d(TAG, "[MGCC] ns1 = " + ns1);
			Log.d(TAG, "[MGCC] ns2 = " + ns2);
			gpsArray[7] = (byte) ((mLatitude > 0) ? 0 : 1);
			gpsArray[8] = (byte) (ns1 & 0xff);
			gpsArray[9] = (byte) ((ns1 >> 8) & 0xff);
			gpsArray[10] = (byte) (ns2 & 0xff);
			gpsArray[11] = (byte) ((ns2 >> 8) & 0xff);

			int lon_deg = (int) Math.floor(Math.abs(mLongitude));
			double lon_min = Math.round((Math.abs(mLongitude) - lon_deg) * 600000);
			int ew1 = (int) (lon_deg * 100 + lon_min / 10000);
			int ew2 = (int) (lon_min % 10000);
			Log.d(TAG, "[MGCC] lon_deg = " + lon_deg);
			Log.d(TAG, "[MGCC] lon_min = " + lon_min);
			Log.d(TAG, "[MGCC] ew1 = " + ew1);
			Log.d(TAG, "[MGCC] ew2 = " + ew2);
			gpsArray[12] = (byte) ((mLongitude > 0) ? 0 : 1);
			gpsArray[13] = (byte) (ew1 & 0xff);
			gpsArray[14] = (byte) ((ew1 >> 8) & 0xff);
			gpsArray[15] = (byte) (ew2 & 0xff);
			gpsArray[16] = (byte) ((ew2 >> 8) & 0xff);

			int altitude = (int) Math.round(Math.abs(mAltitude));
			Log.d(TAG, "[MGCC] altitude = " + altitude);
			gpsArray[17] = (byte) (altitude & 0xff);
			gpsArray[18] = (byte) ((altitude >> 8) & 0xff);
			gpsArray[19] = (byte) ((altitude >> 16) & 0xff);
			gpsArray[20] = (byte) ((altitude >> 24) & 0xff);
			gpsArray[21] = (byte) 0;
			gpsArray[22] = (byte) 0;
			gpsArray[23] = (byte) 0;
			gpsArray[24] = (byte) 0;
			
		} else {
			
			gpsArray = new byte[17];
			gpsArray[0] = (byte) (mCalendar.get(Calendar.YEAR) & 0xff);
			gpsArray[1] = (byte) ((mCalendar.get(Calendar.YEAR) >> 8) & 0xff);
			gpsArray[2] = (byte) (mCalendar.get(Calendar.MONTH));
			gpsArray[3] = (byte) (mCalendar.get(Calendar.DATE));
			gpsArray[4] = (byte) (mCalendar.get(Calendar.HOUR_OF_DAY));
			gpsArray[5] = (byte) (mCalendar.get(Calendar.MINUTE));
			gpsArray[6] = (byte) (mCalendar.get(Calendar.SECOND));

			int lat_deg = (int) Math.floor(Math.abs(mLatitude));
			double lat_min = Math.round((Math.abs(mLatitude) - lat_deg) * 600000);
			int ns1 = (int) (lat_deg * 100 + lat_min / 10000);
			int ns2 = (int) (lat_min % 10000);
			Log.d(TAG, "[MGCC] lat_deg = " + lat_deg);
			Log.d(TAG, "[MGCC] lat_min = " + lat_min);
			Log.d(TAG, "[MGCC] ns1 = " + ns1);
			Log.d(TAG, "[MGCC] ns2 = " + ns2);
			gpsArray[7] = (byte) ((mLatitude > 0) ? 0 : 1);
			gpsArray[8] = (byte) (ns1 & 0xff);
			gpsArray[9] = (byte) ((ns1 >> 8) & 0xff);
			gpsArray[10] = (byte) (ns2 & 0xff);
			gpsArray[11] = (byte) ((ns2 >> 8) & 0xff);

			int lon_deg = (int) Math.floor(Math.abs(mLongitude));
			double lon_min = Math.round((Math.abs(mLongitude) - lon_deg) * 600000);
			int ew1 = (int) (lon_deg * 100 + lon_min / 10000);
			int ew2 = (int) (lon_min % 10000);
			Log.d(TAG, "[MGCC] lon_deg = " + lon_deg);
			Log.d(TAG, "[MGCC] lon_min = " + lon_min);
			Log.d(TAG, "[MGCC] ew1 = " + ew1);
			Log.d(TAG, "[MGCC] ew2 = " + ew2);
			gpsArray[12] = (byte) ((mLongitude > 0) ? 0 : 1);
			gpsArray[13] = (byte) (ew1 & 0xff);
			gpsArray[14] = (byte) ((ew1 >> 8) & 0xff);
			gpsArray[15] = (byte) (ew2 & 0xff);
			gpsArray[16] = (byte) ((ew2 >> 8) & 0xff);
		}

		return gpsArray;
	}
	
	
	
	@Override
	public void error(Exception e) {

		sendMessage(false);
	}
}
