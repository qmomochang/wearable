package com.htc.gc.internal;

import com.htc.gc.interfaces.Common.Context;
import com.htc.gc.interfaces.Common.OperationCallback;
import com.htc.gc.interfaces.Common.RequestCallback;
import com.htc.gc.interfaces.Common;
import com.htc.gc.interfaces.IAuthManager;
import com.htc.gc.interfaces.IAutoBackuper;
import com.htc.gc.interfaces.IDeviceController;
import com.htc.gc.interfaces.IItemDownloader;
import com.htc.gc.interfaces.IItemOperator;
import com.htc.gc.interfaces.IItemPlayer;
import com.htc.gc.interfaces.IItemQuerier;
import com.htc.gc.interfaces.ILiveViewer;
import com.htc.gc.interfaces.IStillCapturer;
import com.htc.gc.interfaces.IVideoRecorder;

class NullGCServiceWorker implements IGCServiceWorker {
	
	@Override
	public void startup() {	
	}

	@Override
	public void shutdown() {
	}

	@Override
	public String getAddress() {
		return null;
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
		return 0;
	}

	@Override
	public String getBleVersion() {
		return null;
	}

	@Override
	public String getA12Version() {
		return null;
	}

	@Override
	public String getModemVersion() {
		return null;
	}

	@Override
	public String getMCUVersion2() {
		return null;
	}

	@Override
	public Context getContext() {
		return null;
	}

	@Override
	public int getReady() {
		return 0;
	}

	@Override
	public boolean isConnectionLive() {
		return false;
	}

	@Override
	public boolean preCreateWifiP2pGroup() {
		return false;
	}

	@Override
	public void removeWifiP2pGroup(OperationCallback callback) throws Exception {
	}

	@Override
	public void forceCloseSocket() {
	}

	@Override
	public void forceDisconnectBle(OperationCallback callback) throws Exception {
	}

	@Override
	public void rebootGc(RequestCallback callback) throws Exception {
	}

	@Override
	public void standby(OperationCallback callback) throws Exception {
	}

	@Override
	public boolean disconnectWifiAp() {
		return false;
	}

	@Override
	public void registInternetOperation(Object instance) throws Exception {
	}

	@Override
	public void unregistInternetOperation(Object instance) throws Exception {
	}
	
	@Override
	public void setContext(Common.Context context) {
	}

	@Override
	public IDeviceController getController() {
		return null;
	}

	@Override
	public IAuthManager getAuthManager() {
		return null;
	}

	@Override
	public ILiveViewer getLiveViewer() {
		return null;
	}

	@Override
	public IStillCapturer getCapturer() {
		return null;
	}

	@Override
	public IVideoRecorder getRecorder() {
		return null;
	}

	@Override
	public IItemQuerier getItemQuerier() {
		return null;
	}

	@Override
	public IItemOperator getItemOperator() {
		return null;
	}

	@Override
	public IItemDownloader getItemDownloader() {
		return null;
	}

	@Override
	public IItemPlayer getMediaItemPalyer() {
		return null;
	}

	@Override
	public IAutoBackuper getAutoBackuper() {
		return null;
	}

	@Override
	public void decideNextConnectionMove() {
	}

}
