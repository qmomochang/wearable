package com.htc.gc.connectivity.v2.internal.tasks;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import com.htc.gc.connectivity.v2.internal.callables.GcScanStartCallable;
import com.htc.gc.connectivity.v2.internal.callables.GcScanStopCallable;
import com.htc.gc.connectivity.v2.internal.common.BaseAlarmService;
import com.htc.gc.connectivity.v2.internal.common.Common;
import com.htc.gc.connectivity.v2.internal.common.GcConnectivityTask;
import com.htc.gc.connectivity.v2.internal.common.IAlarmService;
import com.htc.gc.connectivity.v2.internal.component.le.GcBleScanner;
import android.content.Context;
import android.os.Messenger;
import android.util.Log;



public class GcScanTask extends GcConnectivityTask {

	private final static String TAG = "GcScanTask";
	
	private static final int DEFAULT_SCAN_PERIOD_MS = 3000;
	
	private final GcBleScanner mGcBleScanner;
	private final int mScanPeriodMs;
	private final boolean bScan;
	private static BaseAlarmService alarmTimeoutRequest = null;
	
	
	
	public GcScanTask(GcBleScanner gcBleScanner, Messenger messenger, ExecutorService executor, int periodMs, boolean scan) {

		super(null, null, messenger, executor);
		
		mGcBleScanner = gcBleScanner;
		
		if (periodMs <= 0) {

			mScanPeriodMs = DEFAULT_SCAN_PERIOD_MS;

		} else {

			mScanPeriodMs = periodMs;
		}
		
		bScan = scan;
	}

	
	
	@Override
	public void execute() throws Exception {
		
		super.execute();
		
		if (mGcBleScanner != null) {

			Integer result;
			Callable<Integer> callable;
			Future<Integer> future;
			
			Log.d(TAG, "[MGCC] bScan = " + bScan);
			
			if (bScan) {
				
				callable = new GcScanStartCallable(mGcBleScanner, mMessenger);
				future = mExecutor.submit(callable);
				
				result = future.get();
				Log.d(TAG, "[MGCC] future result = " + result);

				if (result == 0) {
					
					addScanTimeoutRequestAlarm(mScanPeriodMs);
				}

			} else {
				
				removeScanTimeoutRequestAlarm();
				
            	callable = new GcScanStopCallable(mGcBleScanner, mMessenger);
    			future = mExecutor.submit(callable);
				
    			result = future.get();
    			Log.d(TAG, "[MGCC] future result = " + result);
			}
			
		}
	}
	
	
	
	private synchronized void addScanTimeoutRequestAlarm(long periodMs) {
    	
        Context context = mGcBleScanner.getContext();
        final int id = Common.ALARM_SCAN_TIMEOUT;
        
        Log.d(TAG, "[MGCC] addScanTimeoutRequestAlarm periodMs = " + periodMs);
        
        if (context == null) return;
        
        if (alarmTimeoutRequest != null) {
        	alarmTimeoutRequest.deinitAlarm(id);
        	alarmTimeoutRequest = null;
        }
        
        if (context != null) {
        	
        	alarmTimeoutRequest = new BaseAlarmService("GcScanTimeout", context);

            try {

        		IAlarmService alarmService = new IAlarmService() {
                	
                    @Override
                    public void onAlarm() {

                    	Log.d(TAG, "[MGCC] onAlarm: ALARM_SCAN_TIMEOUT");
            			Integer result;
            			Callable<Integer> callable;
            			Future<Integer> future;
            			
                        if (alarmTimeoutRequest != null) {
                        	alarmTimeoutRequest.deinitAlarm(id);
                        	alarmTimeoutRequest = null;
                        }
                        
                        try {

                        	callable = new GcScanStopCallable(mGcBleScanner, mMessenger);
                			future = mExecutor.submit(callable);
							
                			result = future.get();
                			Log.d(TAG, "[MGCC] future result = " + result);
						
            			} catch (Exception e) {
            				
							e.printStackTrace();
						}
                    }
        		};
        		
        		alarmTimeoutRequest.initAlarm(System.currentTimeMillis() + periodMs, id, alarmService);
            	
            } catch (Exception e) {
            	
                Log.d(TAG, "[MGCC] addScanTimeoutRequestAlarm e: " + e);
            }
        }
    }
	
	
	
	private synchronized void removeScanTimeoutRequestAlarm() {
    	
        final int id = Common.ALARM_SCAN_TIMEOUT;
        
        if (alarmTimeoutRequest != null) {
        	alarmTimeoutRequest.deinitAlarm(id);
        	alarmTimeoutRequest = null;
        }
	}
	
	
	
	@Override
	public void error(Exception e) {

	}
}
