package com.htc.gc.connectivity.v3.internal.callables;

import java.util.concurrent.Callable;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import com.htc.gc.connectivity.internal.common.CommonBase.GcWifiTransceiverErrorCode;
import com.htc.gc.connectivity.v3.internal.component.wifi.GcWifiTransceiver;
import com.htc.gc.connectivity.v3.internal.component.wifi.GcWifiTransceiverListener;
import android.util.Log;

public class GcWifiConnectToWPA2Callable implements Callable<GcWifiTransceiverErrorCode> {
	private final static String TAG = "GcWifiConnectToWPA2Callable";
	private final LinkedBlockingQueue<GcWifiTransceiverErrorCode> mCallbackQueue = new LinkedBlockingQueue<GcWifiTransceiverErrorCode>();
	protected GcWifiTransceiver mGcWifiTransceiver;
	protected String mSSID;
	protected String mPasswd;
	
	private GcWifiTransceiverListener mGcWifiTransceiverListener = new GcWifiTransceiverListener() {
		private boolean m_bIsAttemptSent = false;
		@Override
		public void onWifiConnected() {
			Log.d(TAG, "[MGCCtes] onWifiConnected!!");
			m_bIsAttemptSent = true;
			if (mGcWifiTransceiver.validateConnectedSSID(mSSID))
				addCallback(GcWifiTransceiverErrorCode.ERROR_NONE);
			else
				addCallback(GcWifiTransceiverErrorCode.ERROR_UNKNOWN_STATE);
		}
		
		@Override
		public void onWifiConnecting() {
			Log.d(TAG, "[MGCCtes] onWifiConnecting!!");
			m_bIsAttemptSent = true;
		}
		
		@Override
		public void onWifiDisconnected() {
			Log.d(TAG, "[MGCCtes] onWifiDisconnected!! sent=" + m_bIsAttemptSent);
			if (m_bIsAttemptSent)
				addCallback(GcWifiTransceiverErrorCode.ERROR_CONN_BREAK);
		}
		
		@Override
		public void onError(GcWifiTransceiverErrorCode errorCode) {
			Log.d(TAG, "[MGCCtes] onError!! code=" + errorCode);
			addCallback(errorCode);
		}
	};
	
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
		GcWifiTransceiverErrorCode errorCode = mCallbackQueue.poll(Long.MAX_VALUE, TimeUnit.SECONDS);
		mGcWifiTransceiver.unregisterListener(mGcWifiTransceiverListener);
		return errorCode;
	}
	
	protected synchronized void addCallback(GcWifiTransceiverErrorCode errorCode) {
		Log.d(TAG, "[MGCC] addCallback errorCode = " + errorCode);
		if (errorCode != null) {
			mCallbackQueue.add(errorCode);
		}
	}
}
