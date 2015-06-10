package com.htc.dummy.connectivity;

import android.content.Context;
import android.os.Messenger;
import com.htc.gc.connectivity.interfaces.IGcConnectivityWifiP2PGroupRemover;

public class GcDummyConnectivityWifiP2PGroupRemover implements IGcConnectivityWifiP2PGroupRemover {
	
	private final Context mContext;
	private final Messenger mMessenger;
	
	public GcDummyConnectivityWifiP2PGroupRemover(Context context, Messenger messenger) {
		mContext = context;
		mMessenger = messenger;
	}

	@Override
	public boolean gcRemoveWifiP2pGroupInFinish() {
		// TODO Auto-generated method stub
		return false;
	}
}
