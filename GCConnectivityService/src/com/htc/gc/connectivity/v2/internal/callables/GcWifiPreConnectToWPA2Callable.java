package com.htc.gc.connectivity.v2.internal.callables;

import java.util.concurrent.Callable;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import android.net.wifi.SupplicantState;
import android.util.Log;

import com.htc.gc.connectivity.internal.common.CommonBase.GcWifiTransceiverErrorCode;
import com.htc.gc.connectivity.v2.internal.component.wifi.GcWifiTransceiver;
import com.htc.gc.connectivity.v2.internal.component.wifi.GcWifiTransceiverListener;

public class GcWifiPreConnectToWPA2Callable implements Callable<GcWifiTransceiverErrorCode> {
	private final static String TAG = "GcWifiPreConnectToWPA2Callable";
	private final LinkedBlockingQueue<GcWifiTransceiverErrorCode> mCallbackQueue = new LinkedBlockingQueue<GcWifiTransceiverErrorCode>();
	protected GcWifiTransceiver mGcWifiTransceiver;
	private static final long TIMEOUT = 10;
	
	private GcWifiTransceiverListener mGcWifiTransceiverListener = new GcWifiTransceiverListener() {
		@Override
		public void onWifiSupplicantDisconnected() {
			Log.d(TAG, "[MGCC] onWifiSupplicantDisconnected");
					
			addCallback(GcWifiTransceiverErrorCode.ERROR_NONE);
		}
		
		@Override
		public void onError(GcWifiTransceiverErrorCode errorCode) {
			Log.d(TAG, "[MGCC] onError!! code=" + errorCode);
			addCallback(errorCode);
		}
	};
	
	public GcWifiPreConnectToWPA2Callable(GcWifiTransceiver transceiver) {
		mGcWifiTransceiver = transceiver;
	}
	
	@Override
	public GcWifiTransceiverErrorCode call() throws Exception {	
		mGcWifiTransceiver.registerListener(mGcWifiTransceiverListener);
		
		SupplicantState state = mGcWifiTransceiver.getWifiInfo().getSupplicantState();
		Log.i(TAG, "[MGCC] GcWifiPreConnectToWPA2Callable, state= "+state);
		

		if (state.equals(SupplicantState.DISCONNECTED)) {
			addCallback(GcWifiTransceiverErrorCode.ERROR_NONE);			
		} else {
			if (!mGcWifiTransceiver.disconnect()) {
				addCallback(GcWifiTransceiverErrorCode.ERROR_UNKNOWN_STATE);	
			}
		}
			
		GcWifiTransceiverErrorCode errorCode = mCallbackQueue.poll(TIMEOUT, TimeUnit.SECONDS);
		mGcWifiTransceiver.unregisterListener(mGcWifiTransceiverListener);
		if (errorCode == null) {
			Log.w(TAG, "[MGCC] GcWifiPreConnectToWPA2Callable timeout");
			errorCode = GcWifiTransceiverErrorCode.ERROR_NONE; // ignore timeout
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
