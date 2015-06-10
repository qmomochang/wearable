package com.htc.gc.internal;

import com.htc.gc.interfaces.Common;
import com.htc.gc.interfaces.Common.OperationCallback;
import com.htc.gc.interfaces.Common.RequestCallback;
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

public interface IGCServiceWorker {
	
	public void startup();
	public void shutdown();

	public String getAddress();
	
	public int getFWVersion();
	public int getBootVersion();
	public byte getMCUVersion();
	public String getBleVersion();
	public String getA12Version();
	public String getModemVersion();
	public String getMCUVersion2();
	
	public Common.Context getContext();
	public int getReady();
	public boolean isConnectionLive();
	
	public boolean preCreateWifiP2pGroup();
	public void removeWifiP2pGroup(OperationCallback callback) throws Exception;
	
	public void forceCloseSocket();
	public void forceDisconnectBle(OperationCallback callback) throws Exception;
	
	public void rebootGc(RequestCallback callback) throws Exception;
	public void standby(OperationCallback callback) throws Exception;
	
	public boolean disconnectWifiAp();
	public void	registInternetOperation(Object instance) throws Exception;
	public void unregistInternetOperation(Object instance) throws Exception;
	
	public void setContext(Common.Context context);
	
	public IDeviceController getController();
	
	public IAuthManager getAuthManager();
	
	public ILiveViewer getLiveViewer();
	public IStillCapturer getCapturer();
	public IVideoRecorder getRecorder();

	public IItemQuerier getItemQuerier();
	public IItemOperator getItemOperator();
	public IItemDownloader getItemDownloader();
	public IItemPlayer getMediaItemPalyer();
	
	public IAutoBackuper getAutoBackuper();
	
	public void decideNextConnectionMove();
}
