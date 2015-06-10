package com.htc.gc.connectivity.v2.internal.tasks;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import com.htc.gc.connectivity.v2.interfaces.IGcConnectivityService;
import com.htc.gc.connectivity.v2.internal.callables.GcBleBondCallable;
import com.htc.gc.connectivity.v2.internal.callables.GcBleConnectCallable;
import com.htc.gc.connectivity.v2.internal.callables.GcBleDisconnectCallable;
import com.htc.gc.connectivity.v2.internal.common.GcConnectivityTask;
import com.htc.gc.connectivity.v2.internal.component.le.GcBleTransceiver;
import com.htc.gc.connectivity.v2.internal.component.wifi.GcWifiTransceiver;

import android.bluetooth.BluetoothDevice;
import android.os.Bundle;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;



public class GcBleConnectTask extends GcConnectivityTask {

	private final static String TAG = "GcBleConnectTask";
	
	protected BluetoothDevice mBluetoothDevice;
	protected boolean bConnect;
	protected boolean bForce;

	
	
	public GcBleConnectTask(GcBleTransceiver gcBleTransceiver, GcWifiTransceiver gcWifiTransceiver, Messenger messenger, ExecutorService executor, BluetoothDevice device, boolean connect) {

		this(gcBleTransceiver, gcWifiTransceiver, messenger, executor, device, connect, false);
	}

	
	
	public GcBleConnectTask(GcBleTransceiver gcBleTransceiver, GcWifiTransceiver gcWifiTransceiver, Messenger messenger, ExecutorService executor, BluetoothDevice device, boolean connect, boolean force) {

		super(gcBleTransceiver, gcWifiTransceiver, messenger, executor);
		mBluetoothDevice = device;
		bConnect = connect;
		bForce = force;
	}

	
	
	@Override
	public void execute() throws Exception {
		
		super.execute();
		
		super.from();
		
		if (mGcBleTransceiver != null) {

			Integer result;
			Callable<Integer> callable;
			Future<Integer> future;
			
			if (bConnect) {
				
				callable = new GcBleConnectCallable(mGcBleTransceiver, mBluetoothDevice);

				future = mExecutor.submit(callable);
				
				if (future.get() != 0) {
					
					sendMessage(IGcConnectivityService.CB_BLE_CONNECT_RESULT, -1);
					
				} else {
					
					callable = new GcBleBondCallable(mGcBleTransceiver, mBluetoothDevice);
					future = mExecutor.submit(callable);

					if (future.get() != 0) {

						sendMessage(IGcConnectivityService.CB_BLE_CONNECT_RESULT, -1);
						
					} else {
						
						sendMessage(IGcConnectivityService.CB_BLE_CONNECT_RESULT, 0);
					}
				}

			} else {

				callable = new GcBleDisconnectCallable(mGcBleTransceiver, mBluetoothDevice, bForce);

				future = mExecutor.submit(callable);
				
				result = future.get();
				Log.d(TAG, "[MGCC] future result = " + result);

				if (bForce) {
					
					sendMessage(IGcConnectivityService.CB_BLE_DISCONNECT_FORCE_RESULT, result);
					
				} else {
					
					sendMessage(IGcConnectivityService.CB_BLE_DISCONNECT_RESULT, result);
				}
			}
		}
		
		super.to(TAG);
	}
	
	
	
	private void sendMessage(int type, int result) {
		
		try {
			
			Message outMsg = Message.obtain();
			outMsg.what = type;
			Bundle outData = new Bundle();
			
			if (result == 0) {
				
				outData.putSerializable(IGcConnectivityService.PARAM_RESULT, IGcConnectivityService.Result.RESULT_SUCCESS);
				
			} else {
				
				outData.putSerializable(IGcConnectivityService.PARAM_RESULT, IGcConnectivityService.Result.RESULT_FAIL);
			}
			
			outData.putParcelable(IGcConnectivityService.PARAM_BLUETOOTH_DEVICE, mBluetoothDevice);
			
			outMsg.setData(outData);
		
			mMessenger.send(outMsg);

		} catch (RemoteException e) {

			e.printStackTrace();
		}
	}
	
	
	
	@Override
	public void error(Exception e) {

		if (bConnect) {
			
			sendMessage(IGcConnectivityService.CB_BLE_CONNECT_RESULT, -1);
			
		} else {
			
			sendMessage(IGcConnectivityService.CB_BLE_DISCONNECT_RESULT, -1);
		}
	}
}
