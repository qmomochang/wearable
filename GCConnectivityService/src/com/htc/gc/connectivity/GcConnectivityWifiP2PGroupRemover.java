package com.htc.gc.connectivity;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import android.content.Context;
import android.os.Messenger;
import android.util.Log;

import com.htc.gc.connectivity.interfaces.IGcConnectivityWifiP2PGroupRemover;
import com.htc.gc.connectivity.v2.internal.common.GcConnectivityTask;
import com.htc.gc.connectivity.v2.internal.component.wifi.GcWifiTransceiver;
import com.htc.gc.connectivity.v2.internal.tasks.GcWifiGroupTask;

public class GcConnectivityWifiP2PGroupRemover implements IGcConnectivityWifiP2PGroupRemover {
	
	private static final String TAG = "GcConnectivityWifiP2PGroupRemover";
	
	private Context mContext;
	private Messenger mMessenger;
	private ExecutorService mExecutor;
	private GcWifiTransceiver mGcWifiTransceiver;
	
	public GcConnectivityWifiP2PGroupRemover(Context context, Messenger messenger) {
		try {
			mContext = context;
			mMessenger = messenger;
			mExecutor = Executors.newCachedThreadPool();
            mGcWifiTransceiver = new GcWifiTransceiver(context);
            
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public boolean gcRemoveWifiP2pGroupInFinish() {
		Log.d(TAG, "[MGCC] gcRemoveWifiP2pGroup++");
		
		boolean ret = false;
		
		try {
			new Thread(new Runnable(){

				@Override
				public void run() {
					GcConnectivityTask task = null;
					try {
						task = new GcWifiGroupTask(null, mGcWifiTransceiver, mMessenger, mExecutor, false, false, false, true);
						task.execute();
					} catch (Exception e) {
						task.error(e);
					}
				}
			}).start();
			
			ret = true;
		} catch (Exception e) {
			Log.e(TAG, "[MGCC] gcOpen exception: " + e);
		}
		
		Log.d(TAG, "[MGCC] gcRemoveWifiP2pGroup--");
		
		return ret;
	}
}
