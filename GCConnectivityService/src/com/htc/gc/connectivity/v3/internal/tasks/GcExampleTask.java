package com.htc.gc.connectivity.v3.internal.tasks;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import com.htc.gc.connectivity.v3.internal.callables.GcExampleCallable;
import com.htc.gc.connectivity.v3.internal.component.le.GcBleTransceiver;
import com.htc.gc.connectivity.v3.internal.component.wifi.GcWifiTransceiver;

import android.os.Messenger;
import android.util.Log;



public class GcExampleTask extends GcSimpleTask {

	private final static String TAG = "GcExampleTask";
	
	
	
	public GcExampleTask(GcBleTransceiver gcBleTransceiver, GcWifiTransceiver gcWifiTransceiver, Messenger messenger, ExecutorService executor) {

		super(gcBleTransceiver, gcWifiTransceiver, messenger, executor);
	}

	
	
	@Override
	public void execute() throws Exception {
		
		super.execute();
		
		Callable<Integer> callable = new GcExampleCallable();
		Future<Integer> futureA = mExecutor.submit(callable);
		Future<Integer> futureB = mExecutor.submit(callable);
		Future<Integer> futureC = mExecutor.submit(callable);
		
		Log.d(TAG, "[MGCC] futureA = " + futureA.get());
		Log.d(TAG, "[MGCC] futureB = " + futureB.get());
		Log.d(TAG, "[MGCC] futureC = " + futureC.get());
		
		Log.d(TAG, "[MGCC] Task completed!!");
	}
	
	
	
	@Override
	public void error(Exception e) {

	}
}
