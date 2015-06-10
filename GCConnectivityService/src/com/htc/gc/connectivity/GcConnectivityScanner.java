package com.htc.gc.connectivity;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.os.Bundle;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

import com.htc.gc.connectivity.interfaces.IGcConnectivityScanner;
import com.htc.gc.connectivity.interfaces.IGcConnectivityDevice.GcVersion;
import com.htc.gc.connectivity.v2.interfaces.IGcConnectivityService;
import com.htc.gc.connectivity.v2.internal.common.GcConnectivityTask;
import com.htc.gc.connectivity.v2.internal.component.le.GcBleScanner;
import com.htc.gc.connectivity.v2.internal.component.le.GcBleScannerListener;
import com.htc.gc.connectivity.v2.internal.tasks.GcScanTask;

public class GcConnectivityScanner implements IGcConnectivityScanner {
	
	private static final String TAG = "GcConnectivityScanner";
	
	private Context mContext;
	private Messenger mMessenger;
	private ExecutorService mExecutor;
	private GcBleScanner mGcBleScanner;
	
	private Thread mTaskThread;
	private final LinkedBlockingQueue<GcConnectivityTask> mTaskQueue = new LinkedBlockingQueue<GcConnectivityTask>();
	private AtomicBoolean mIsTaskThreadInterrupted = new AtomicBoolean(false);
	
	private GcBleScannerListener mGcBleScannerListener;
	
	public GcConnectivityScanner(Context context, Messenger messenger) {
		try {
			mContext = context;
			mMessenger = messenger;
			
			// For API level 18 and above, get a reference to BluetoothAdapter through BluetoothManager.
	        BluetoothManager bluetoothManager = (BluetoothManager) mContext.getSystemService(Context.BLUETOOTH_SERVICE);
            if (bluetoothManager == null) {
                Log.e(TAG, "Unable to initialize BluetoothManager.");
                return;
            }
	        
            mGcBleScanner = new GcBleScanner(context, bluetoothManager);
			
			mGcBleScannerListener = new GcBleScannerListener(){
				
				@Override
				public void onScanHit(BluetoothDevice device, GcVersion deviceVersion) {
					Log.d(TAG, "[MGCC] onScanHit. device = " + device);
					
					try {
						
						Message outMsg = Message.obtain();
						outMsg.what = IGcConnectivityService.CB_BLE_SCAN_RESULT;
						Bundle outData = new Bundle();
						outData.putSerializable(IGcConnectivityService.PARAM_RESULT, ScanResult.SCAN_RESULT_HIT);
						outData.putParcelable(IGcConnectivityService.PARAM_BLUETOOTH_DEVICE, device);
						outData.putSerializable(IGcConnectivityScanner.PARAM_BLUETOOTH_DEVICE_VERSION, deviceVersion);
						outMsg.setData(outData);
					
						mMessenger.send(outMsg);

					} catch (RemoteException e) {

						e.printStackTrace();
					}
				}

				@Override
				public void onScanHitConnected(BluetoothDevice device, GcVersion deviceVersion) {
					Log.d(TAG, "[MGCC] onScanHitConnected. device = " + device);
					
					try {
						
						Message outMsg = Message.obtain();
						outMsg.what = IGcConnectivityService.CB_BLE_SCAN_RESULT;
						Bundle outData = new Bundle();
						outData.putSerializable(IGcConnectivityService.PARAM_RESULT, ScanResult.SCAN_RESULT_HIT_CONNECTED);
						outData.putParcelable(IGcConnectivityService.PARAM_BLUETOOTH_DEVICE, device);
						outData.putSerializable(IGcConnectivityScanner.PARAM_BLUETOOTH_DEVICE_VERSION, deviceVersion);
						outMsg.setData(outData);
					
						mMessenger.send(outMsg);

					} catch (Exception e) {

						e.printStackTrace();
					}
				}
			};
			mGcBleScanner.registerListener(mGcBleScannerListener);
			
			open();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public boolean gcOpen() {
		Log.d(TAG, "[MGCC] gcOpen++");
		
		boolean ret = false;
		
		try {
			open();
			
			ret = true;
		} catch (Exception e) {
			Log.e(TAG, "[MGCC] gcOpen exception: " + e);
		}
		
		Log.d(TAG, "[MGCC] gcOpen--");
		
		return ret;
	}

	@Override
	public boolean gcClose() {
		Log.d(TAG, "[MGCC] gcClose++");
		
		boolean ret = false;
		
		try {
			close();
			
			ret = true;
		} catch (Exception e) {
			Log.e(TAG, "[MGCC] gcClose exception: " + e);
		}

		Log.d(TAG, "[MGCC] gcClose--");
		
		return ret;
	}

	@Override
	public boolean gcScan(int period) {
		Log.d(TAG, "[MGCC] gcScan++");
		
		boolean ret = false;
		
		try {
			GcConnectivityTask task = new GcScanTask(mGcBleScanner, mMessenger, mExecutor, period, true);
			addTask(task);
			
			ret = true;
		} catch (Exception e) {
			Log.d(TAG, "[MGCC] gcScan exception: " + e);
		}

		Log.d(TAG, "[MGCC] gcScan--");

		return ret;
	}

	@Override
	public boolean gcStopScan() {
		Log.d(TAG, "[MGCC] gcStopScan++");
		
		boolean ret = false;

		try {
			GcConnectivityTask task = new GcScanTask(mGcBleScanner, mMessenger, mExecutor, 0, false);
			addTask(task);
			
			ret = true;
		} catch (Exception e) {
			Log.d(TAG, "[MGCC] gcStopScan exception: " + e);
		}

		Log.d(TAG, "[MGCC] gcStopScan--");

		return ret;
	}
	
	private void open() {
		if (mExecutor == null) {
			mExecutor = Executors.newCachedThreadPool();
		}
		
		new Thread(new Runnable() {

			@Override
			public void run() {

				try {
					if (mTaskThread == null) {
						Runnable taskRunnable = new Runnable() {

							@Override
							public void run() {
								while (mIsTaskThreadInterrupted.get() == false) {
									GcConnectivityTask task = null;
									try {
										task = mTaskQueue.poll(500, TimeUnit.MILLISECONDS);
										if (task != null) {
											Log.d(TAG, "[MGCC] Executing task = " + task);
											task.execute();
										}
									} catch (Exception e) {
										Log.d(TAG, "[MGCC] taskRunnable e = " + e);
										e.printStackTrace();
										
										if (task != null) {
											task.error(e);
										}
									}
								}
							}
						};
						mTaskThread = new Thread(taskRunnable, "GcConnectivityScannerTaskThread");
						mTaskThread.start();
					}
				} catch (Exception e) {

					Log.d(TAG, "[MGCC] open e" + e);
				}
			}

		}).start();
	}

	private void close() {
		try {
			if (mExecutor != null) {
				mExecutor.shutdown();
			}

			if (mTaskThread != null) {
				
				mIsTaskThreadInterrupted.set(true);
				
				Log.d(TAG, "[MGCC] waiting for task executing...");
				
				mTaskThread.join();
			}
		} catch (InterruptedException e) {
			Log.d(TAG, "[MGCC] close e" + e);
		}
	}
	
	private void addTask(GcConnectivityTask task) throws Exception {
		Log.d(TAG, "[MGCC] addTask task = " + task);
		if (task != null) {
			mTaskQueue.add(task);
		}
	}
}
