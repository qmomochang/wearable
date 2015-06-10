package com.htc.gc.connectivity.v2.internal.tasks;

import java.util.concurrent.ExecutorService;

import com.htc.gc.connectivity.v2.internal.common.GcConnectivityTask;
import com.htc.gc.connectivity.v2.internal.component.le.GcBleTransceiver;
import com.htc.gc.connectivity.v2.internal.component.wifi.GcWifiTransceiver;

import android.os.Messenger;
import android.util.Log;



public class GcSimpleTask extends GcConnectivityTask {

	private final static String TAG = "GcSimpleTask";
	
	
	
	public GcSimpleTask(GcBleTransceiver gcBleTransceiver, GcWifiTransceiver gcWifiTransceiver, Messenger messenger, ExecutorService executor) {

		super(gcBleTransceiver, gcWifiTransceiver, messenger, executor);
	}

	
	
	@Override
	public void execute() throws Exception {
		
		super.execute();
	}
	
	
	
	@Override
	public void error(Exception e) {

	}
}
