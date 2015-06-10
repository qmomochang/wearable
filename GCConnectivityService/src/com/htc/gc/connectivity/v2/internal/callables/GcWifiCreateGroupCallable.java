package com.htc.gc.connectivity.v2.internal.callables;

import java.util.concurrent.Callable;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import com.htc.gc.connectivity.internal.common.CommonBase.GcWifiTransceiverErrorCode;
import com.htc.gc.connectivity.v2.internal.component.wifi.GcWifiTransceiver;
import com.htc.gc.connectivity.v2.internal.component.wifi.GcWifiTransceiverListener;

import android.util.Log;



public class GcWifiCreateGroupCallable implements Callable<Integer> {

	private final static String TAG = "GcWifiCreateGroupCallable";
	private final static boolean bPerformanceNotify = true;

	private final static int DEFAULT_CALLABLE_TIMEOUT = 30000;
	
	private final LinkedBlockingQueue<GcWifiTransceiverErrorCode> mCallbackQueue = new LinkedBlockingQueue<GcWifiTransceiverErrorCode>();
	protected GcWifiTransceiver mGcWifiTransceiver;
	protected long mTimePrev;

	
	
	private GcWifiTransceiverListener mGcWifiTransceiverListener = new GcWifiTransceiverListener() {
		
		@Override
		public void onWifiDirectGroupCreated() {

			Log.d(TAG, "[MGCC] onWifiDirectGroupCreated!!");
			addCallback(GcWifiTransceiverErrorCode.ERROR_NONE);
		}
		
		
		
		@Override
		public void onError(GcWifiTransceiverErrorCode errorCode) {

			addCallback(errorCode);
		}
	};
	
	
	
	public GcWifiCreateGroupCallable(GcWifiTransceiver transceiver) {

		mGcWifiTransceiver = transceiver;
	}



	@Override
	public Integer call() throws Exception {

		from();
		
		Integer ret = 0;
		
		mGcWifiTransceiver.registerListener(mGcWifiTransceiverListener);
		
		if (mGcWifiTransceiver.createGroup()) {
			
			GcWifiTransceiverErrorCode errorCode = mCallbackQueue.poll(DEFAULT_CALLABLE_TIMEOUT, TimeUnit.MILLISECONDS);
			
			if (errorCode != GcWifiTransceiverErrorCode.ERROR_NONE) {
					
				ret = -1;
			}
			
		} else {
			
			ret = -1;
		}
		
		mGcWifiTransceiver.unregisterListener(mGcWifiTransceiverListener);

		to(TAG);
		
		return ret;
	}
	
	
	
	protected synchronized void addCallback(GcWifiTransceiverErrorCode errorCode) {

		Log.d(TAG, "[MGCC] addCallback errorCode = " + errorCode);

		if (errorCode != null) {

			mCallbackQueue.add(errorCode);
		}
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
}
