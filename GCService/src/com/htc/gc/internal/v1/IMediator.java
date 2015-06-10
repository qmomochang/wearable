package com.htc.gc.internal.v1;

import java.nio.ByteBuffer;
import java.util.UUID;

import android.os.Bundle;

import com.htc.gc.connectivity.v2.interfaces.IGcConnectivityService;
import com.htc.gc.interfaces.Common;
import com.htc.gc.interfaces.Common.ErrorCallback;
import com.htc.gc.interfaces.IDeviceItem;
import com.htc.gc.interfaces.IGCService.ConnectionMode;
import com.htc.gc.interfaces.IMediaItem;
import com.htc.gc.internal.GCTask;

public interface IMediator {
	public void requestCommand(GCTask task) throws Exception;


	public interface IEventListener {
		public void event(int eventID, ByteBuffer body);
	}
	
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

	public void addEventListener(int eventID, IEventListener listener);
	public void removeEventListener(int eventID, IEventListener listener);
	
	public void addBleCallback(Object that, int callbackID, ErrorCallback callback);
	public void addBleEventListener(int callbackID, IBleEventListener listener);
	public void removeBleEventListener(int callbackID, IBleEventListener listener);
}
