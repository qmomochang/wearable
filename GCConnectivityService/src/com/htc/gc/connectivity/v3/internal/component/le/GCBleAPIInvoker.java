package com.htc.gc.connectivity.v3.internal.component.le;

import java.util.concurrent.CountDownLatch;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.content.Context;
import android.os.Handler;
import android.util.Log;

class GCBleAPIInvoker {

	private static final String TAG = "GCBleAPIInvoker";

	private final boolean mRunOnMainThread;
	private final Handler mMainThreadHandler;

	private BluetoothGatt mGattHolder = null;

	public GCBleAPIInvoker(Context context, boolean runOnMainThread) {
		mRunOnMainThread = runOnMainThread;
		if (mRunOnMainThread) {
			mMainThreadHandler = new Handler(context.getMainLooper());
		} else {
			mMainThreadHandler = null;
		}
	}

	public BluetoothGatt connectGatt(final BluetoothDevice device,
			final Context context, final boolean auto,
			final BluetoothGattCallback callback) {
		if (mRunOnMainThread) {

			mGattHolder = null;
			final CountDownLatch latch = new CountDownLatch(1);

			mMainThreadHandler.post(new Runnable() {

				@Override
				public void run() {
					Log.d(TAG, "[MGCC] device.connectGatt()+++");
					mGattHolder = device.connectGatt(context, auto, callback);
					Log.d(TAG, "[MGCC] device.connectGatt()---");

					latch.countDown();
				}
			});
			try {
				latch.await();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		} else {
			Log.d(TAG, "[MGCC] device.connectGatt()+++");
			mGattHolder = device.connectGatt(context, auto, callback);
			Log.d(TAG, "[MGCC] device.connectGatt()---");
		}
		
		return mGattHolder;
	}

	public void disconnect(final BluetoothGatt gatt) {
		if (mRunOnMainThread) {

			final CountDownLatch latch = new CountDownLatch(1);

			mMainThreadHandler.post(new Runnable() {

				@Override
				public void run() {
					Log.d(TAG, "[MGCC] gatt.disconnect()+++");
					gatt.disconnect();
					Log.d(TAG, "[MGCC] gatt.disconnect()---");

					latch.countDown();
				}
			});
			try {
				latch.await();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

		} else {
			Log.d(TAG, "[MGCC] gatt.disconnect()+++");
			gatt.disconnect();
			Log.d(TAG, "[MGCC] gatt.disconnect()---");
		}
	}
}
