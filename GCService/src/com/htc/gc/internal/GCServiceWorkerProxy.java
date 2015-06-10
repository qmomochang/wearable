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

public class GCServiceWorkerProxy implements IGCServiceWorker {
	
	private IGCServiceWorker mServiceWorker = new NullGCServiceWorker();
	
	public IGCServiceWorker getServiceWorker() {
		return mServiceWorker;
	}
	
	public void setServiceWorker(IGCServiceWorker serviceWorker) {
		mServiceWorker = serviceWorker;
	}

	@Override
	public void startup() {
		mServiceWorker.startup();
	}

	@Override
	public void shutdown() {
		mServiceWorker.shutdown();
	}

	@Override
	public String getAddress() {
		return mServiceWorker.getAddress();
	}

	@Override
	public int getFWVersion() {
		return mServiceWorker.getFWVersion();
	}

	@Override
	public int getBootVersion() {
		return mServiceWorker.getBootVersion();
	}

	@Override
	public byte getMCUVersion() {
		return mServiceWorker.getMCUVersion();
	}

	@Override
	public String getBleVersion() {
		return mServiceWorker.getBleVersion();
	}

	@Override
	public String getA12Version() {
		return mServiceWorker.getA12Version();
	}

	@Override
	public String getModemVersion() {
		return mServiceWorker.getModemVersion();
	}

	@Override
	public String getMCUVersion2() {
		return mServiceWorker.getMCUVersion2();
	}

	@Override
	public Context getContext() {
		return mServiceWorker.getContext();
	}

	@Override
	public int getReady() {
		return mServiceWorker.getReady();
	}

	@Override
	public boolean isConnectionLive() {
		return mServiceWorker.isConnectionLive();
	}

	@Override
	public boolean preCreateWifiP2pGroup() {
		return mServiceWorker.preCreateWifiP2pGroup();
	}

	@Override
	public void removeWifiP2pGroup(OperationCallback callback) throws Exception {
		mServiceWorker.removeWifiP2pGroup(callback);
	}

	@Override
	public void forceCloseSocket() {
		mServiceWorker.forceCloseSocket();
	}

	@Override
	public void forceDisconnectBle(OperationCallback callback) throws Exception {
		mServiceWorker.forceDisconnectBle(callback);
	}

	@Override
	public void rebootGc(RequestCallback callback) throws Exception {
		mServiceWorker.rebootGc(callback);
	}

	@Override
	public void standby(OperationCallback callback) throws Exception {
		mServiceWorker.standby(callback);
	}

	@Override
	public boolean disconnectWifiAp() {
		return mServiceWorker.disconnectWifiAp();
	}

	@Override
	public void registInternetOperation(Object instance) throws Exception {
		mServiceWorker.registInternetOperation(instance);
	}

	@Override
	public void unregistInternetOperation(Object instance) throws Exception {
		mServiceWorker.unregistInternetOperation(instance);
	}
	
	@Override
	public void setContext(Common.Context context) {
		mServiceWorker.setContext(context);
	}

	@Override
	public IDeviceController getController() {
		return mServiceWorker.getController();
	}

	@Override
	public IAuthManager getAuthManager() {
		return mServiceWorker.getAuthManager();
	}

	@Override
	public ILiveViewer getLiveViewer() {
		return mServiceWorker.getLiveViewer();
	}

	@Override
	public IStillCapturer getCapturer() {
		return mServiceWorker.getCapturer();
	}

	@Override
	public IVideoRecorder getRecorder() {
		return mServiceWorker.getRecorder();
	}

	@Override
	public IItemQuerier getItemQuerier() {
		return mServiceWorker.getItemQuerier();
	}

	@Override
	public IItemOperator getItemOperator() {
		return mServiceWorker.getItemOperator();
	}

	@Override
	public IItemDownloader getItemDownloader() {
		return mServiceWorker.getItemDownloader();
	}

	@Override
	public IItemPlayer getMediaItemPalyer() {
		return mServiceWorker.getMediaItemPalyer();
	}

	@Override
	public IAutoBackuper getAutoBackuper() {
		return mServiceWorker.getAutoBackuper();
	}

	@Override
	public void decideNextConnectionMove() {
		mServiceWorker.decideNextConnectionMove();
	}

}
