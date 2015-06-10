package com.htc.gc.connectivity.v2.internal.callables;

import java.util.concurrent.Callable;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import com.htc.gc.connectivity.internal.common.CommonBase.GcWifiTransceiverErrorCode;
import com.htc.gc.connectivity.v2.internal.component.wifi.GcWifiTransceiver;
import com.htc.gc.connectivity.v2.internal.component.wifi.GcWifiTransceiverListener;

import android.net.wifi.SupplicantState;
import android.net.wifi.WifiInfo;
import android.util.Log;

public class GcWifiConnectToWPA2Callable implements Callable<GcWifiTransceiverErrorCode> {
	private final static String TAG = "GcWifiConnectToWPA2Callable";
	private final LinkedBlockingQueue<GcWifiTransceiverErrorCode> mCallbackQueue = new LinkedBlockingQueue<GcWifiTransceiverErrorCode>();
	protected GcWifiTransceiver mGcWifiTransceiver;
	protected String mSSID;
	protected String mPasswd;
	private final static long TIMEOUT = 60;
	private GcWifiTransceiverListener mGcWifiTransceiverListener = new GcWifiTransceiverListener() {
		
		@Override
		public void onWifiConnected(WifiInfo info) {
			Log.d(TAG, "[MGCCtes] onWifiConnected!! WifiInfo= "+info);
			if (SupplicantState.COMPLETED.equals(info.getSupplicantState())) {
				if (validateConnectedSSID(info.getSSID(), mSSID)) {
					addCallback(GcWifiTransceiverErrorCode.ERROR_NONE);
				} else {
					// if current connected ap is not our target, connect again until time out.
					if (!mGcWifiTransceiver.disconnect()) {
						addCallback(GcWifiTransceiverErrorCode.ERROR_UNKNOWN_STATE);
					}
					if (!mGcWifiTransceiver.connectToWPA2(mSSID, mPasswd)) {
						addCallback(GcWifiTransceiverErrorCode.ERROR_GC_SOFTAP_NOT_FOUND);
					}			
				}				
			} else {
				Log.i(TAG, "[MGCC] onWifiConnected but supplicant state is not COMPLETE, ignore it");
			}
		}
		
		@Override
		public void onWifiConnecting() {
			Log.d(TAG, "[MGCCtes] onWifiConnecting!!");
		}
		
		@Override
		public void onWifiDisconnected() {
			Log.d(TAG, "[MGCCtes] onWifiDisconnected!!");
		}
		
		@Override
		public void onError(GcWifiTransceiverErrorCode errorCode) {
			Log.d(TAG, "[MGCCtes] onError!! code=" + errorCode);
			addCallback(errorCode);
		}
	};
	
	public boolean validateConnectedSSID(String currentSSID, String targetSSID) {
		if (currentSSID != null && currentSSID.equalsIgnoreCase("\""+targetSSID+"\"")) {
			return true;
		} else {
			Log.e(TAG, "[MGCC] FATAL: validateConnectedSSID: target="+targetSSID+", connected=" + currentSSID);
			return false;			
		}
	}	
	
	public GcWifiConnectToWPA2Callable(GcWifiTransceiver transceiver, String SSID, String passwd) {
		mGcWifiTransceiver = transceiver;
		mSSID = SSID;
		mPasswd = passwd;
	}
	
	@Override
	public GcWifiTransceiverErrorCode call() throws Exception {	
		mGcWifiTransceiver.registerListener(mGcWifiTransceiverListener);
		if (!mGcWifiTransceiver.connectToWPA2(mSSID, mPasswd)) {
			addCallback(GcWifiTransceiverErrorCode.ERROR_GC_SOFTAP_NOT_FOUND);
		}
		GcWifiTransceiverErrorCode errorCode = mCallbackQueue.poll(TIMEOUT, TimeUnit.SECONDS);
		mGcWifiTransceiver.unregisterListener(mGcWifiTransceiverListener);
		if (errorCode == null) {
			Log.w(TAG, "[MGCC] GcWifiConnectToWPA2Callable timeout");
			errorCode = GcWifiTransceiverErrorCode.ERROR_UNKNOWN_STATE;
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
