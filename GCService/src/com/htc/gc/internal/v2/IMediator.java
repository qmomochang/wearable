package com.htc.gc.internal.v2;

import java.util.UUID;

import android.os.Bundle;

import com.htc.gc.connectivity.v3.interfaces.IGcConnectivityService;
import com.htc.gc.interfaces.Common;
import com.htc.gc.interfaces.Common.ErrorCallback;
import com.htc.gc.interfaces.IDeviceItem;
import com.htc.gc.interfaces.IGCService.ConnectionMode;
import com.htc.gc.interfaces.IMediaItem;

public interface IMediator {
	
	public interface IBleEventListener {
		public void event(int callbackID, Bundle bundle);
	}
	
	public IGcConnectivityService getConnectivityService();
	public ConnectionMode getCurrentConnectionMode();
	public IDeviceItem getTargetDevice();

	public IMediaItem getLastItem();
	public void setLastItem(IMediaItem item);

	public Common.Context getContext();
	public void setContext(Common.Context context);

	public UUID getCursorValidityKey();
	public void updateCursorValidityKey();

	public int getReady();
	public void setReady(int ready);
	
	public void forceResetAllConnections();
	
	public void addBleCallback(Object that, int callbackID, ErrorCallback callback);
	public void addBleEventListener(int callbackID, IBleEventListener listener);
	public void removeBleEventListener(int callbackID, IBleEventListener listener);
}
