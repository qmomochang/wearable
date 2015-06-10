package com.htc.gc.interfaces;

import com.htc.gc.interfaces.Common.Context;
import com.htc.gc.interfaces.Common.Mode;
import com.htc.gc.interfaces.Common.OperationCallback;
import com.htc.gc.interfaces.Common.RequestCallback;
import com.htc.gc.internal.CancelTask;

public interface IGCService {
	enum ConnectionMode {
		Full,
		Partial,
		Disconnected,
	}
	
	enum WifiConnectMode {
		None,
		WifiP2p,
		SoftAp
	}
	
	interface ScanDeviceResultListener extends ErrorListener {
		public void onDeviceFound(IDeviceItem item);
		public void onConnectedDeviceFound(IDeviceItem item);
		public void onScanComplete();
	}
	
	interface ConnectionModeListener extends ErrorListener {
		public void onDisconnectedMode();
		public void onPartialMode();
		public void onFullMode();
	}
	
	interface StandaloneStatusListener {
		public void onStandalone();
	}
	
	interface HeartBeatListener {
		public void onHeartBeat(IGCService that, boolean live);
	}
	
	interface ReadyStatusListener {
		public void onChange(int ready);
	}
	
	interface PowerStateListener {
		public void onPowerOn(boolean powerOn);
	}
	
	interface FwSupportedFunctionListener {
		public void onChange(int supportedFuncs);
	}

	interface ErrorListener {
		public void onError(IGCService that, Exception e);
	}
	
	interface SyncDataListener {
		public void onSync(IGCService that, Mode mode, Context context, long timecode, int totalFrameCount, int ready, IMediaItem lastItem);
	}
	
	public interface ICancel {
		public boolean isCancel();
	}

	public interface ICommandCancel extends ICancel {
		public void requestCancel(CancelTask task);
	}
	
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
	
	public boolean startDeviceScan(int timeOutMs, ScanDeviceResultListener l);
	public boolean stopDeviceScan();
	
	public IDeviceItem getTargetDevice();
	public void setTargetDevice(IDeviceItem item);
	public Common.DeviceVersion getTargetDeviceVersion();
	
	public boolean isSocketConnected();
	public boolean isSocketDisconnected();
	public boolean isWifiConnected();
	public boolean isWifiDisconnected();
	public boolean isBleVerified();
	
	public boolean isStandalone();
	
	public ConnectionMode getExpectedConnectionMode();	
	public void setExpectedConnectionMode(ConnectionMode mode);
	public ConnectionMode getCurrentConnectionMode();
	
	public boolean isSoftApEnable();
	public void enableSoftAp(boolean enable);
	
	public WifiConnectMode getSuggestWifiMode();
	
	public void rebootGc(RequestCallback callback) throws Exception;
	public void standby(OperationCallback callback) throws Exception;
	
	public boolean reconnectWifiAp();
	public boolean disconnectWifiAp();
	public void	registInternetOperation(Object instance) throws Exception;
	public void unregistInternetOperation(Object instance) throws Exception;
	
	public void removeWifiP2pGroupInFinish(OperationCallback callback) throws Exception;
	
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
	
	public void setCurrentConnectionModeListener(ConnectionModeListener l);
	public void setStandaloneStatusListener(StandaloneStatusListener l);
	public void setSyncInitDataListener(SyncDataListener l);
	public void setErrorListener(ErrorListener l);
	public void setHeartBeatListener(HeartBeatListener l);
	public void setReadyStatusListener(ReadyStatusListener l);
	public void setFwSupportedFunctionListener(FwSupportedFunctionListener l);
	public void setPowerStateChangeListener(PowerStateListener l);
}
