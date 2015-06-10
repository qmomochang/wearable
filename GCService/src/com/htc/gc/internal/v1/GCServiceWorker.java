package com.htc.gc.internal.v1;

import java.util.ArrayList;
import java.util.UUID;

import android.util.Log;

import com.htc.gc.GCService;
import com.htc.gc.GCService.Status;
import com.htc.gc.connectivity.v2.interfaces.IGcConnectivityService;
import com.htc.gc.interfaces.Common;
import com.htc.gc.interfaces.Common.BleCommandException;
import com.htc.gc.interfaces.Common.CancelException;
import com.htc.gc.interfaces.Common.ErrorCallback;
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
import com.htc.gc.internal.GCTask;
import com.htc.gc.tasks.RebootGcTask;
import com.htc.gc.tasks.StandbyTask;


public class GCServiceWorker extends GCServiceWorkerImp {

	public GCServiceWorker(android.content.Context ctx, byte[] appGuid, IGCService gcService) {
		super(ctx, appGuid, gcService);
		
		Log.i(Common.TAG, "[GCServiceWorker] constructor");
	}
	
	@Override
	public void startup() {
		Log.i(Common.TAG, "[GCServiceWorker] startup");
		mConn.gcOpen();
	}

	@Override
	public void shutdown() {
		Log.i(Common.TAG, "[GCServiceWorker] shutdown");
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
		Log.i(Common.TAG, "[GCServiceWorker] setContext(" + context + ")");
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
		return mFWVersion;
	}

	@Override
	public int getBootVersion() {
		return mBootVersion;
	}
	
	@Override
	public byte getMCUVersion() {
		return mMcuVersion;
	}

	@Override
	public String getBleVersion() {
		return mBleVersion;
	}
	
	@Override
	public String getA12Version() {
		return "";
	}
	
	@Override
	public String getModemVersion() {
		return "";
	}
	
	@Override
	public String getMCUVersion2() {
		return "";
	}
	
	@Override
	public boolean isConnectionLive() {
		return mHeartBeatLive;
	}
	
	@Override
	public IDeviceItem getTargetDevice() {
		return mGCService.getTargetDevice();
	}
	
	@Override
	public void standby(final OperationCallback callback) throws Exception {
		Log.i(Common.TAG, "[GCServiceWorker] standby");

		if(callback == null) throw new NullPointerException();
		if(mGCService.getCurrentConnectionMode() != IGCService.ConnectionMode.Full) throw new StatusException();

		requestCommand(new StandbyTask(mGCService, new OperationCallback() {

			@Override
			public void error(Exception e) {
				callback.error(e);
			}

			@Override
			public void done(Object that) {
				closeSocket(new CancelException());
				forceResetWifiStatus();
				
				callback.done(that);
			}
		}));
	}
	
	@Override
	public void forceCloseSocket() {
		Log.i(Common.TAG, "[GCServiceWorker] forceCloseSocket");
		closeSocket(new CancelException());
	}
	
	@Override
	public void forceDisconnectBle(OperationCallback callback) throws Exception {
		Log.i(Common.TAG, "[GCServiceWorker] forceDisconnectBle");
		
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
		Log.i(Common.TAG, "[GCServiceWorker] forceResetAllConnections");
		closeSocket(new CancelException());
		forceResetWifiStatus();
		forceResetBleStatus();
	}
	
	@Override
	public void rebootGc(final RequestCallback callback) throws Exception {
		if(DEBUG) Log.i(Common.TAG, "[GCServiceWorker] rebootGc");

		if(callback == null) throw new NullPointerException();
		if(getSocketStatus() != Status.Connected) throw new StatusException(); // to handle upgrade process
		
		boolean isEnableRequestCallbackResponse = getFWVersion() >= Common.ENABLE_REQUESTCALLBACK_RESPONSE_VERSION;
		if(DEBUG) Log.i(Common.TAG, "[GCServiceWorker] rebootGc, isEnableRequestCallbackResponse= "+isEnableRequestCallbackResponse);
		
		requestCommand(new RebootGcTask(mGCService, new RequestCallback() {

			@Override
			public void error(Exception e) {
				callback.error(e);
			}

			@Override
			public void requested(Object that) {
				callback.requested(that);
			}

			@Override
			public void done(Object that) {
				closeSocket(new CancelException());
				forceResetWifiStatus();
				
				callback.done(that);
			}
			
		}, isEnableRequestCallbackResponse));
	}

	@Override
	public synchronized void requestCommand(GCTask task) throws Exception  {
		if(getSocketStatus() != Status.Connected) throw new StatusException(); // basic requirement to send command

		mCommandQueue.add(task);
	}

	@Override
	public void addEventListener(int eventID, IEventListener listener) {
		synchronized(mEventHandlers) {
			ArrayList<IEventListener> listeners = mEventHandlers.get(eventID);
			if(listeners == null) {
				listeners = new ArrayList<IEventListener>();
				mEventHandlers.put(eventID, listeners);
			}

			listeners.add(listener);
		}
	}

	@Override
	public void removeEventListener(int eventID, IEventListener listener) {
		synchronized(mEventHandlers) {
			ArrayList<IEventListener> listeners = mEventHandlers.get(eventID);
			if(listeners != null) {
				listeners.remove(listener);
			}
		}
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
		Log.i(Common.TAG, "decrepted [GCServiceWorker] preCreateWifiP2pGroup");
		
		return false;
		//return mConn.gcCreateWifiP2pGroup();
	}
	
	@Override
	public void removeWifiP2pGroup(OperationCallback callback) throws Exception {
		Log.i(Common.TAG, "[GCServiceWorker] removeWifiP2pGroup");
		
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
			Log.i(Common.TAG, "[GCServiceWorker] disconnectWifiAp");
			return ((GCService)mGCService).disconnectWifiApInternal();	
		} else {
			Log.i(Common.TAG, "[GCServiceWorker] disconnectWifiAp, mInternetOperationCounterQueue is not empty, so do nothing");
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
