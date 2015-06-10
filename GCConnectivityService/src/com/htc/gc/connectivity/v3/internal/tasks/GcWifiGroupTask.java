package com.htc.gc.connectivity.v3.internal.tasks;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import com.htc.gc.connectivity.v3.interfaces.IGcConnectivityService;
import com.htc.gc.connectivity.v3.internal.callables.GcWifiCreateGroupCallable;
import com.htc.gc.connectivity.v3.internal.callables.GcWifiRemoveGroupCallable;
import com.htc.gc.connectivity.v3.internal.common.GcConnectivityTask;
import com.htc.gc.connectivity.v3.internal.component.le.GcBleTransceiver;
import com.htc.gc.connectivity.v3.internal.component.wifi.GcWifiTransceiver;

import android.os.Bundle;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;



public class GcWifiGroupTask extends GcConnectivityTask {

	private final static String TAG = "GcWifiGroupTask";
	
	protected boolean bCreate;
	protected boolean bInternalUse;
	protected boolean bForce;
	
	
	
	public GcWifiGroupTask(GcBleTransceiver gcBleTransceiver, GcWifiTransceiver gcWifiTransceiver, Messenger messenger, ExecutorService executor, boolean create, boolean internal) {

		this(gcBleTransceiver, gcWifiTransceiver, messenger, executor, create, internal, false);
	}

	
	
	public GcWifiGroupTask(GcBleTransceiver gcBleTransceiver, GcWifiTransceiver gcWifiTransceiver, Messenger messenger, ExecutorService executor, boolean create, boolean internal, boolean force) {

		super(gcBleTransceiver, gcWifiTransceiver, messenger, executor);
		bCreate = create;
		bInternalUse = internal;
		bForce = force;
	}

	
	
	@Override
	public void execute() throws Exception {
		
		super.execute();
		
		super.from();
		
		if (mGcWifiTransceiver != null) {

			Integer result;
			Callable<Integer> callable;
			Future<Integer> future;
			
			if (bCreate) {
				
				callable = new GcWifiCreateGroupCallable(mGcWifiTransceiver);
				
			} else {
				
				callable = new GcWifiRemoveGroupCallable(mGcWifiTransceiver);
			}
			
			future = mExecutor.submit(callable);
			
			result = future.get();

			if (result == 0) {
				
				sendMessage(true);

			} else {
				
				sendMessage(false);
			}
		}

		super.to(TAG);
	}
	
	
	
	private void sendMessage(boolean result) {
		
		try {
			
			if (bInternalUse) {
				
				return;
			}
			
			Message outMsg = Message.obtain();
			
			if (bCreate) {
				
				outMsg.what = IGcConnectivityService.CB_CREATE_WIFI_P2P_GROUP_RESULT;
				
			} else {
				
				if (bForce) {
					
					outMsg.what = IGcConnectivityService.CB_REMOVE_WIFI_P2P_GROUP_FORCE_RESULT;

				} else {
					
					outMsg.what = IGcConnectivityService.CB_REMOVE_WIFI_P2P_GROUP_RESULT;
				}
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
	
	
	
	@Override
	public void error(Exception e) {

		sendMessage(false);
	}
}
