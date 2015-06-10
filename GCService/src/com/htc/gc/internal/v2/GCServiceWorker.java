package com.htc.gc.internal.v2;

import java.util.ArrayList;
import java.util.UUID;

import android.util.Log;

import com.htc.gc.GCService;
import com.htc.gc.GCService.Status;
import com.htc.gc.connectivity.v3.interfaces.IGcConnectivityService;
import com.htc.gc.interfaces.Common;
import com.htc.gc.interfaces.Common.BleCommandException;
import com.htc.gc.interfaces.Common.CancelException;
import com.htc.gc.interfaces.Common.ErrorCallback;
import com.htc.gc.interfaces.Common.NoImpException;
import com.htc.gc.interfaces.Common.OperationCallback;
import com.htc.gc.interfaces.Common.RequestCallback;
import com.htc.gc.interfaces.Common.StatusException;
import com.htc.gc.interfaces.IAuthManager;
import com.htc.gc.interfaces.IAutoBackuper;
import com.htc.gc.interfaces.IDeviceController;
import com.htc.gc.interfaces.IDeviceItem;
import com.htc.gc.interfaces.IGCService;
import com.htc.gc.interfaces.IItemDownloader;
import com.htc.gc.interfaces.IItemOperator;
import com.htc.gc.interfaces.IItemPlayer;
import com.htc.gc.interfaces.IItemQuerier;
import com.htc.gc.interfaces.ILiveViewer;
import com.htc.gc.interfaces.IMediaItem;
import com.htc.gc.interfaces.IStillCapturer;
import com.htc.gc.interfaces.IVideoRecorder;
import com.htc.gc.internal.DeviceItem;


public class GCServiceWorker extends GCServiceWorkerImp {

	public GCServiceWorker(android.content.Context ctx, byte[] appGuid, IGCService gcService) {
		super(ctx, appGuid, gcService);
		
		Log.i(Common.TAG, "[GCServiceWorker2] constructor");
	}
	
	@Override
	public void startup() {
		Log.i(Common.TAG, "[GCServiceWorker2] startup");
		mConn.gcOpen();
	}

	@Override
	public void shutdown() {
		Log.i(Common.TAG, "[GCServiceWorker2] shutdown");
		mConn.gcClose();
	}
	
	@Override
	public synchronized int getReady() {
		return mReady;
	}

	@Override
	public synchronized void setReady(int ready) {
		mReady = ready;
	}
	
	@Override
	public synchronized Common.Context getContext() {
		return mContext;
	}

	@Override
	public synchronized void setContext(Common.Context context) {
		Log.i(Common.TAG, "[GCServiceWorker2] setContext(" + context + ")");
		mContext = context;
	}

	@Override
	public synchronized IMediaItem getLastItem() {
		return mLastItem;
	}

	@Override
	public synchronized void setLastItem(IMediaItem item) {
		mLastItem = item;
	}

	@Override
	public synchronized UUID getCursorValidityKey() {
		return mCursorUniqueKey;
	}

	@Override
	public synchronized void updateCursorValidityKey() {
		mCursorUniqueKey = UUID.randomUUID();
	}
	
	@Override
	public String getAddress() {
		return mAddress;
	}
	
	@Override
	public int getFWVersion() {
		return 0;
	}

	@Override
	public int getBootVersion() {
		return 0;
	}
	
	@Override
	public byte getMCUVersion() {
		return (byte)0;
	}

	@Override
	public String getBleVersion() {
		return mBleVersion;
	}
	
	@Override
	public String getA12Version() {
		return mA12Version;
	}
	
	@Override
	public String getModemVersion() {
		return mModemVersion;
	}
	
	@Override
	public String getMCUVersion2() {
		return mMcuVersion2;
	}
	
	@Override
	public boolean isConnectionLive() {
		return mHeartBeatLive.get();
	}
	
	@Override
	public IDeviceItem getTargetDevice() {
		return mGCService.getTargetDevice();
	}
	
	@Override
	public void standby(final OperationCallback callback) throws Exception {
		Log.i(Common.TAG, "[GCServiceWorker2] standby");

		if(callback == null) throw new NullPointerException();
		if(mGCService.getCurrentConnectionMode() != IGCService.ConnectionMode.Full) throw new StatusException();

		throw new NoImpException();
	}
	
	@Override
	public void forceCloseSocket() {
		Log.i(Common.TAG, "[GCServiceWorker2] forceCloseSocket");
		closeSocket(new CancelException());
	}
	
	@Override
	public void forceDisconnectBle(OperationCallback callback) throws Exception {
		Log.i(Common.TAG, "[GCServiceWorker2] forceDisconnectBle");
		
		if(callback == null) throw new NullPointerException();
		DeviceItem device = (DeviceItem) getTargetDevice();
		if(device == null) throw new NullPointerException();
		
		if(!mConn.gcBleDisconnectForce(device.getDevice())) {
			throw new BleCommandException();
		} else {
			addBleCallback(GCServiceWorker.this, IGcConnectivityService.CB_BLE_DISCONNECT_FORCE_RESULT, callback);
		}
	}
	
	@Override
	public void forceResetAllConnections() {
		Log.i(Common.TAG, "[GCServiceWorker2] forceResetAllConnections");
		closeSocket(new CancelException());
		forceResetWifiStatus();
		forceResetBleStatus();
	}
	
	@Override
	public void rebootGc(final RequestCallback callback) throws Exception {
		if(DEBUG) Log.i(Common.TAG, "[GCServiceWorker2] rebootGc");

		if(callback == null) throw new NullPointerException();
		if(getSocketStatus() != Status.Connected) throw new StatusException(); // to handle upgrade process
		
		boolean isEnableRequestCallbackResponse = getFWVersion() >= Common.ENABLE_REQUESTCALLBACK_RESPONSE_VERSION;
		if(DEBUG) Log.i(Common.TAG, "[GCServiceWorker2] rebootGc, isEnableRequestCallbackResponse= "+isEnableRequestCallbackResponse);
		
		throw new NoImpException();
	}
	
	@Override
	public void addBleCallback(Object that, int callbackID, ErrorCallback callback) {
		synchronized(mBleCallbacks) {
			mBleCallbacks.add(new BleErrorCallback(that, callbackID, callback));
		}		
	}
	
	@Override
	public void addBleEventListener(int callbackID, IBleEventListener listener) {
		synchronized(mBleEventHandler) {
			ArrayList<IBleEventListener> listeners = mBleEventHandler.get(callbackID);
			if(listeners == null) {
				listeners = new ArrayList<IBleEventListener>();
				mBleEventHandler.put(callbackID, listeners);
			}

			listeners.add(listener);
		}
	}

	@Override
	public void removeBleEventListener(int callbackID, IBleEventListener listener) {
		synchronized(mBleEventHandler) {
			ArrayList<IBleEventListener> listeners = mBleEventHandler.get(callbackID);
			if(listeners != null) {
				listeners.remove(listener);
			}
		}
	}

	@Override
	public IDeviceController getController() {
		return mController;
	}

	@Override
	public ILiveViewer getLiveViewer() {
		return mLiveViewer;
	}

	@Override
	public IStillCapturer getCapturer() {
		return mStillCapturer;
	}

	@Override
	public IVideoRecorder getRecorder() {
		return mVideoRecorder;
	}

	@Override
	public IItemQuerier getItemQuerier() {
		return mItemQuerier;
	}

	@Override
	public IItemOperator getItemOperator() {
		return mItemOperator;
	}

	@Override
	public IItemDownloader getItemDownloader() {
		return mItemDownloader;
	}

	@Override
	public IItemPlayer getMediaItemPalyer() {
		return mItemPlayer;
	}
	
	@Override
	public IAuthManager getAuthManager() {
		return mAuthManager;
	}
	
	@Override
	public IAutoBackuper getAutoBackuper() {
		return mAutoBackuper;
	}

	public IGcConnectivityService getConnectivityService() {
		return mConn;
	}
	
	@Override
	public IGCService.ConnectionMode getCurrentConnectionMode() {
		return mGCService.getCurrentConnectionMode();
	}

	@Deprecated
	@Override
	public boolean preCreateWifiP2pGroup() {
		Log.i(Common.TAG, "decrepted [GCServiceWorker2] preCreateWifiP2pGroup");
		
		return false;
		//return mConn.gcCreateWifiP2pGroup();
	}
	
	@Override
	public void removeWifiP2pGroup(OperationCallback callback) throws Exception {
		Log.i(Common.TAG, "[GCServiceWorker2] removeWifiP2pGroup");
		
		if(callback == null) throw new NullPointerException();
		
		if(!mConn.gcRemoveWifiP2pGroup()) {
			throw new BleCommandException();
		} else {
			addBleCallback(GCServiceWorker.this, IGcConnectivityService.CB_REMOVE_WIFI_P2P_GROUP_RESULT, callback);
		}
	}

	@Override
    public boolean disconnectWifiAp() {
		if(mInternetOperationCounterQueue.isEmpty()) {
			Log.i(Common.TAG, "[GCServiceWorker2] disconnectWifiAp");
			return ((GCService)mGCService).disconnectWifiApInternal();	
		} else {
			Log.i(Common.TAG, "[GCServiceWorker2] disconnectWifiAp, mInternetOperationCounterQueue is not empty, so do nothing");
			return false;
		}
    }
	
	@Override
	public void	registInternetOperation(Object instance) throws Exception {
		Log.i(Common.TAG, "registInternetOperation: "+instance);
		
		if(instance == null) throw new NullPointerException();
		
		if(!mInternetOperationCounterQueue.contains(instance)) {
			mInternetOperationCounterQueue.add(instance);
			
			if(mInternetOperationCounterQueue.size() == 1) {
				((GCService)mGCService).reconnectWifiApInternal();
			}
			
		} else {
			Log.e(Common.TAG, "registInternetOperation fail, "+instance+" already in queue");
		}	
	}
	
	@Override
	public void unregistInternetOperation(Object instance) throws Exception {
		Log.i(Common.TAG, "unregistInternetOperation: "+instance);
		
		if(instance == null) throw new NullPointerException();
		
		if(!mInternetOperationCounterQueue.remove(instance)) {
			Log.e(Common.TAG, "unregistInternetOperation fail, can't find "+instance+" in queue");
		} else {
			if(mInternetOperationCounterQueue.isEmpty()) {
				((GCService)mGCService).disconnectWifiApInternal();
			}
		}
	}
}
