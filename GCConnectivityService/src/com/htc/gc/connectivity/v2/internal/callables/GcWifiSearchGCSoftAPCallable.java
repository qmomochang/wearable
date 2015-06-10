package com.htc.gc.connectivity.v2.internal.callables;

import java.util.concurrent.Callable;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import com.htc.gc.connectivity.internal.common.CommonBase.GcWifiTransceiverErrorCode;
import com.htc.gc.connectivity.v2.internal.component.wifi.GcWifiTransceiver;
import com.htc.gc.connectivity.v2.internal.component.wifi.GcWifiTransceiverListener;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiManager;
import android.util.Log;

public class GcWifiSearchGCSoftAPCallable implements Callable<GcWifiTransceiverErrorCode> {
	private final static String TAG = "GcWifiSearchGCSoftAPCallable";
	private final LinkedBlockingQueue<GcWifiTransceiverErrorCode> mCallbackQueue = new LinkedBlockingQueue<GcWifiTransceiverErrorCode>();
	private static final long TIMEOUT = 10;
	protected GcWifiTransceiver mGcWifiTransceiver;
	protected String mSSID;
	protected String mPasswd;
	
	private GcWifiTransceiverListener mGcWifiTransceiverListener = new GcWifiTransceiverListener() {
		@Override
		public void onWifiScanResultAvailable() {
			Log.d(TAG, "[MGCC] onWifiScanResultAvailable!!");
			addCallback(GcWifiTransceiverErrorCode.ERROR_NONE);
		}
		
		@Override
		public void onError(GcWifiTransceiverErrorCode errorCode) {
			Log.d(TAG, "[MGCC] onError!! code=" + errorCode);
			addCallback(errorCode);
		}
	};
	
	public GcWifiSearchGCSoftAPCallable(GcWifiTransceiver transceiver, String SSID, String passwd) {
		mGcWifiTransceiver = transceiver;
		mSSID = SSID;
		mPasswd = passwd;
	}
	
	@Override
	public GcWifiTransceiverErrorCode call() throws Exception {
		mGcWifiTransceiver.registerListener(mGcWifiTransceiverListener);
		mGcWifiTransceiver.scanSoftAP();
		Log.i(TAG, "[MGCC] scanning for softAP, timeout(sec)="+TIMEOUT);
		GcWifiTransceiverErrorCode errorCode = mCallbackQueue.poll(TIMEOUT, TimeUnit.SECONDS);
		mGcWifiTransceiver.unregisterListener(mGcWifiTransceiverListener);
		if (errorCode == null) {
			Log.w(TAG, "[MGCC] softAP scan timed-out");
			errorCode = GcWifiTransceiverErrorCode.ERROR_WIFI_SCAN_TIMEOUT;
		}
		return errorCode;
	}
	
	protected synchronized void addCallback(GcWifiTransceiverErrorCode errorCode) {
		Log.d(TAG, "[MGCC] addCallback errorCode = " + errorCode);
		if (errorCode != null) {
			mCallbackQueue.add(errorCode);
		}
	}
}
