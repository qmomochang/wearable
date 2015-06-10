package com.htc.gc.connectivity.v2.internal.common;

import java.util.concurrent.ExecutorService;

import com.htc.gc.connectivity.v2.interfaces.IGcConnectivityService;
import com.htc.gc.connectivity.v2.internal.component.le.GcBleTransceiver;
import com.htc.gc.connectivity.v2.internal.component.wifi.GcWifiTransceiver;

import android.os.Bundle;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;



public class GcConnectivityTask implements Comparable<GcConnectivityTask> {
	
	private final static String TAG = "GcConnectivityTask";
	private final static boolean bPerformanceNotify = true;
	
	protected Messenger mMessenger;
	protected ExecutorService mExecutor;
	protected GcBleTransceiver mGcBleTransceiver;
	protected GcWifiTransceiver mGcWifiTransceiver;
	protected long mTimePrev;
	
	
	
	public GcConnectivityTask(GcBleTransceiver gcBleTransceiver, GcWifiTransceiver gcWifiTransceiver, Messenger messenger, ExecutorService executor) {
		
		mGcBleTransceiver = gcBleTransceiver;
		mGcWifiTransceiver = gcWifiTransceiver;
		mMessenger = messenger;
		mExecutor = executor;
	}
	
	
	
	@Override
    public int compareTo(GcConnectivityTask that) {

    	return 0;
    }
    
    
	
	public void execute() throws Exception {

	}

	
	
	public void error(Exception e) {
		
		
	}
	
	
	
	protected void from() {
		
		if (bPerformanceNotify) {

			mTimePrev = System.currentTimeMillis();
		}
	}
	
	
	
	protected void to(String task) {

		if (bPerformanceNotify) {

			long timeCurr = System.currentTimeMillis();
			long timeDiff = timeCurr - mTimePrev;
			
			Log.d(TAG, "[MGCC][MPerf] [" + task + "] costs: " + timeDiff + " ms");
			
			sendMessage(task, timeDiff);
		}
	}
	
	
	
	private void sendMessage(String task, long timeCost) {
		
		try {
			
			Message outMsg = Message.obtain();
			outMsg.what = IGcConnectivityService.CB_PERFORMANCE_RESULT;
			Bundle outData = new Bundle();
			
			outData.putString(IGcConnectivityService.PARAM_TASK_NAME, task);
			outData.putLong(IGcConnectivityService.PARAM_TIME_COST_MS, timeCost);
			
			outMsg.setData(outData);
		
			mMessenger.send(outMsg);

		} catch (RemoteException e) {

			e.printStackTrace();
		}
	}
}
