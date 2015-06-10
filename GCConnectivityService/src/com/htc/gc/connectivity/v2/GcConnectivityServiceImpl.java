package com.htc.gc.connectivity.v2;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import com.htc.gc.connectivity.v2.internal.common.GcConnectivityTask;
import com.htc.gc.connectivity.v2.internal.component.le.GcBleTransceiver;
import com.htc.gc.connectivity.v2.internal.component.wifi.GcWifiTransceiver;
import com.htc.gc.connectivity.v2.internal.tasks.GcLongTermEventTask;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.os.Messenger;
import android.util.Log;



public class GcConnectivityServiceImpl {

	private final static String TAG = "GcConnectivityServiceImpl";

	protected Context mContext;
	protected Messenger mMessenger;
	protected ExecutorService mExecutor;
	protected BluetoothManager mBluetoothManager;
	protected GcBleTransceiver mGcBleTransceiver;
	protected GcWifiTransceiver mGcWifiTransceiver;
	
	private boolean bServiceAvailable = false;
	
	private Thread mTaskThread;
	private final LinkedBlockingQueue<GcConnectivityTask> mTaskQueue = new LinkedBlockingQueue<GcConnectivityTask>();
	private AtomicBoolean mIsTaskThreadInterrupted = new AtomicBoolean(false);
	
	private Thread mLongTermEventThread;
	private GcLongTermEventTask mGcConnectivityLongTermEventTask;
	
	private int mSkipTaskCount;
	
	
	
	private IGcConnectivityServiceListener mGcConnectivityServiceListener = new IGcConnectivityServiceListener() {

		@Override
		public void onError(int errorCode) {

			Log.d(TAG, "[MGCC] onError errorCode = " + errorCode);

			if (errorCode == 881) {
				
				try {
				
					mSkipTaskCount = mTaskQueue.size();
					
					Log.d(TAG, "[MGCC] onError mSkipTaskCount = " + mSkipTaskCount);

					///clearTaskQueue();

				} catch (Exception e) {

					e.printStackTrace();
				}
			}
		}
	};

	
	
	public GcConnectivityServiceImpl(Context context, Messenger messenger) {
		
		try {

			Log.d(TAG, "[MGCC] onCreate");

			mContext = context;
			mMessenger = messenger;
			mSkipTaskCount = 0;

			// For API level 18 and above, get a reference to BluetoothAdapter through BluetoothManager.
	        if (mBluetoothManager == null) {

	        	mBluetoothManager = (BluetoothManager) mContext.getSystemService(Context.BLUETOOTH_SERVICE);
	            if (mBluetoothManager == null) {
	                Log.e(TAG, "Unable to initialize BluetoothManager.");
	                return;
	            }
	        }
			
			mGcBleTransceiver = new GcBleTransceiver(context, mBluetoothManager);
			mGcWifiTransceiver = new GcWifiTransceiver(context);
			
			mGcConnectivityLongTermEventTask = new GcLongTermEventTask(mGcBleTransceiver, mGcWifiTransceiver, mMessenger, mExecutor, mGcConnectivityServiceListener);
			
			open();
			
		} catch (Exception e) {

			e.printStackTrace();
		}
	}
	
		
	
	protected synchronized void addTask(GcConnectivityTask task) throws Exception {

		Log.d(TAG, "[MGCC] addTask task = " + task);
		
		if (task != null) {

			mTaskQueue.add(task);
		}
	}
	
	
	
	protected synchronized void clearTaskQueue() throws Exception {

		mTaskQueue.clear();
	}
	
	
	
	protected synchronized boolean getServiceAvailable() {
		
		return bServiceAvailable;
	}

	
	
	protected synchronized void setServiceAvailable(boolean value) {
		
		bServiceAvailable = value;
	}

	
	
	protected void registerLTEvent(BluetoothDevice device, String uuidString) {
		
		mGcConnectivityLongTermEventTask.registerUuid(device, uuidString);
	}

	
	
	protected void unregisterLTEvent(BluetoothDevice device, String uuidString) {
		
		mGcConnectivityLongTermEventTask.unregisterUuid(device, uuidString);
	}

	
	
	protected void open() {
		
		Log.d(TAG, "[MGCC] open");

		if (mExecutor == null) {

			mExecutor = Executors.newFixedThreadPool(5);
		}
		
		new Thread(new Runnable() {

			@Override
			public void run() {

				try {

					if (mTaskThread == null) {

						mTaskThread = new Thread(mTaskRunnable, "GcConnectivityTaskThread");
						mTaskThread.start();
					}

					if (mLongTermEventThread == null) {

						mLongTermEventThread = new Thread(mLongTermEventRunnable, "GcConnectivityLongTermEventThread");
						mLongTermEventThread.start();
					}

				} catch (Exception e) {

					Log.d(TAG, "[MGCC] open e" + e);
				}
			}

		}).start();
	}
	
	
	
	protected void close() {
		
		Log.d(TAG, "[MGCC] close");
		
		try {
			
			if (mExecutor != null) {
				
				mExecutor.shutdown();
			}
			
			if (mTaskThread != null) {
				
				mIsTaskThreadInterrupted.set(true);
				
				Log.d(TAG, "[MGCC] waiting for task executing...");
				
				mTaskThread.join();
			}

			if (mLongTermEventThread != null) {
				
				mLongTermEventThread.interrupt();
				mLongTermEventThread.join();
			}
			
			mGcBleTransceiver.deInit();
			
			mGcWifiTransceiver.deInit();

		} catch (Exception e) {

			Log.d(TAG, "[MGCC] close e" + e);
		}
	}
	
	
	
	private final Runnable mTaskRunnable = new Runnable() {

		@Override
		public void run() {

			while (mIsTaskThreadInterrupted.get() == false) {

				GcConnectivityTask task = null;
				
				try {
					
					task = mTaskQueue.poll(500, TimeUnit.MILLISECONDS);
					
					if (task != null) {
						
						Log.d(TAG, "[MGCC] got task, reamin mTaskQueue.size() = " + mTaskQueue.size() + ", mSkipTaskCount = " + mSkipTaskCount);
						
						if (mSkipTaskCount > 0) {

							mSkipTaskCount--;
							
							Log.d(TAG, "[MGCC] Skipping task = " + task);
							task.error(null);
							
						} else {
							
							mSkipTaskCount = 0;

							Log.d(TAG, "[MGCC] Executing task = " + task);
							task.execute();
						}			
						
					}
					
				} catch (Exception e) {

					Log.d(TAG, "[MGCC] mTaskRunnable e = " + e);
					e.printStackTrace();
					
					if (task != null) {
						
						task.error(e);
					}
				}
			}
		}
	};
	
	
	
	private final Runnable mLongTermEventRunnable = new Runnable() {

		@Override
		public void run() {

			while (mLongTermEventThread.isInterrupted() == false) {

				try {

					mGcConnectivityLongTermEventTask.execute();
				
				} catch (InterruptedException e) {
					
					Log.d(TAG, "[MGCC] mLongTermEventRunnable interrupted");
					break;
				} catch (Exception e) {

					Log.d(TAG, "[MGCC] mLongTermEventRunnable e = " + e);
				}

			}
		}
	};
}
