package com.htc.gc;

import java.io.File;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import android.bluetooth.BluetoothDevice;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.util.Log;

import com.htc.dummy.connectivity.GcDummyConnectivityScanner;
import com.htc.dummy.connectivity.GcDummyConnectivityWifiP2PGroupRemover;
import com.htc.gc.connectivity.GcConnectivityScanner;
import com.htc.gc.connectivity.GcConnectivityWifiP2PGroupRemover;
import com.htc.gc.connectivity.interfaces.IGcConnectivityDevice;
import com.htc.gc.connectivity.interfaces.IGcConnectivityDevice.GcVersion;
import com.htc.gc.connectivity.interfaces.IGcConnectivityScanner;
import com.htc.gc.connectivity.interfaces.IGcConnectivityScanner.ScanResult;
import com.htc.gc.connectivity.interfaces.IGcConnectivityServiceBase.Result;
import com.htc.gc.connectivity.interfaces.IGcConnectivityWifiP2PGroupRemover;
import com.htc.gc.connectivity.v3.interfaces.IGcConnectivityService;
import com.htc.gc.interfaces.Common;
import com.htc.gc.interfaces.Common.BleCommandException;
import com.htc.gc.interfaces.Common.Context;
import com.htc.gc.interfaces.Common.DeviceVersion;
import com.htc.gc.interfaces.Common.OperationCallback;
import com.htc.gc.interfaces.Common.RequestCallback;
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
import com.htc.gc.interfaces.IStillCapturer;
import com.htc.gc.interfaces.IVideoRecorder;
import com.htc.gc.internal.DeviceItem;
import com.htc.gc.internal.GCAuthManagerProxy;
import com.htc.gc.internal.GCAutoBackuperProxy;
import com.htc.gc.internal.GCDeviceControllerProxy;
import com.htc.gc.internal.GCItemDownloaderProxy;
import com.htc.gc.internal.GCItemOperatorProxy;
import com.htc.gc.internal.GCItemPlayerProxy;
import com.htc.gc.internal.GCItemQuerierProxy;
import com.htc.gc.internal.GCLiveViewerProxy;
import com.htc.gc.internal.GCServiceWorkerProxy;
import com.htc.gc.internal.GCStillCapturerProxy;
import com.htc.gc.internal.GCVideoRecorderProxy;
import com.htc.gc.internal.NetworkHelper;

public class GCService implements IGCService {
	
	public enum Status {
		Disconnected,
		Connecting,
		Connected,
		Verifying,
		Verified,
		Disconnecting,
		Error,
	}
	
	private static final boolean DEBUG = Common.DEBUG;
	protected static final String DEBUG_SOFTAP_FLAG_NAME = "SOFTAP";
	
	private final android.content.Context mCtx;
	private final byte[] mAppGuid;
	
	private Object mAccessLock;
	private Messenger mMessenger;
	private volatile IGcConnectivityScanner mScanner;
	private volatile IGcConnectivityWifiP2PGroupRemover mWifiP2PGroupRemover;
	
	protected IGCService.ScanDeviceResultListener mScanDeviceResultListener;
	protected IGCService.ConnectionModeListener mConnectionModeListener;
	protected IGCService.StandaloneStatusListener mStandaloneStatusListener;
	protected IGCService.SyncDataListener mSyncDataListener;
	protected IGCService.HeartBeatListener mHeartbeatListener;
	protected IGCService.ErrorListener mErrorListener;
	protected IGCService.ReadyStatusListener mReadyStatusListener;
	protected IGCService.PowerStateListener mPowerStateListener;
	protected IGCService.FwSupportedFunctionListener mFwSupportedFunctionListener;
	protected OperationCallback mRemoveWifiP2PGroupInFinishCallback;
	
	protected volatile IDeviceItem mTargetDevice;
	
	protected IGCService.ConnectionMode mCurrentConnectionMode = IGCService.ConnectionMode.Disconnected;
	protected ReentrantReadWriteLock mConnectionModeLock = new ReentrantReadWriteLock();
	protected IGCService.ConnectionMode mExpectedConnectionMode = IGCService.ConnectionMode.Disconnected;
	protected ReentrantReadWriteLock mExpectedConnectionModeLock = new ReentrantReadWriteLock();
	
	private Status mBleStatus = Status.Disconnected;
	private ReentrantReadWriteLock mBleStatusLock = new ReentrantReadWriteLock();
	private Status mWifiStatus = Status.Disconnected;
	private ReentrantReadWriteLock mWifiStatusLock = new ReentrantReadWriteLock();
	private Status mSocketStatus = Status.Disconnected;
	private ReentrantReadWriteLock mSocketStatusLock = new ReentrantReadWriteLock();
	
	protected boolean mIsSoftApEnable = false;
	protected ReentrantReadWriteLock mSoftApEnableFlagLock = new ReentrantReadWriteLock();
	
	protected AtomicBoolean mIsStandalone = new AtomicBoolean(true);
	
	private Handler mRunOnUiHandler = new Handler(Looper.getMainLooper());
	
	private GCDeviceControllerProxy mDeviceController = new GCDeviceControllerProxy();
	private GCAuthManagerProxy mAuthManager = new GCAuthManagerProxy();
	private GCLiveViewerProxy mLiveViewer = new GCLiveViewerProxy();
	private GCStillCapturerProxy mStillCapturer = new GCStillCapturerProxy();
	private GCVideoRecorderProxy mVideoRecorder = new GCVideoRecorderProxy();
	private GCItemQuerierProxy mItemQuerier = new GCItemQuerierProxy();
	private GCItemOperatorProxy mItemOperator = new GCItemOperatorProxy();
	private GCItemDownloaderProxy mItemDownloader = new GCItemDownloaderProxy();
	private GCItemPlayerProxy mItemPlayer = new GCItemPlayerProxy();
	private GCAutoBackuperProxy mAutoBackuper = new GCAutoBackuperProxy();
	
	private GCServiceWorkerProxy mWorker = new GCServiceWorkerProxy();
	
	protected GCService(android.content.Context ctx, byte[] appGuid) {
		
		mCtx = ctx;
		mAppGuid = appGuid;
		
		mAccessLock = new Object();
		mMessenger = new Messenger(createConnectionHandler());
		
		Log.i(Common.TAG, "[GCService] constructor");
	}
	
	public static IGCService createInstance(android.content.Context ctx, byte[] appGuid) {
		return new GCService(ctx, appGuid);
	}

	@Override
	public void startup() {
		mWorker.startup();
	}

	@Override
	public void shutdown() {
		mWorker.shutdown();
	}

	@Override
	public String getAddress() {
		return mWorker.getAddress();
	}

	@Override
	public int getFWVersion() {
		return mWorker.getFWVersion();
	}

	@Override
	public int getBootVersion() {
		return mWorker.getBootVersion();
	}

	@Override
	public byte getMCUVersion() {
		return mWorker.getMCUVersion();
	}

	@Override
	public String getBleVersion() {
		return mWorker.getBleVersion();
	}
	
	@Override
	public String getA12Version() {
		return mWorker.getA12Version();
	}
	
	@Override
	public String getModemVersion() {
		return mWorker.getModemVersion();
	}
	
	@Override
	public String getMCUVersion2() {
		return mWorker.getMCUVersion2();
	}

	@Override
	public Context getContext() {
		return mWorker.getContext();
	}

	@Override
	public int getReady() {
		return mWorker.getReady();
	}

	@Override
	public boolean isConnectionLive() {
		return mWorker.isConnectionLive();
	}

	@Override
	public boolean preCreateWifiP2pGroup() {
		return mWorker.preCreateWifiP2pGroup();
	}

	@Override
	public void removeWifiP2pGroup(OperationCallback callback) throws Exception {
		mWorker.removeWifiP2pGroup(callback);
	}

	@Override
	public void forceCloseSocket() {
		mWorker.forceCloseSocket();
	}

	@Override
	public void forceDisconnectBle(OperationCallback callback) throws Exception {
		mWorker.forceDisconnectBle(callback);
	}

	@Override
	public boolean startDeviceScan(int timeOutMs, ScanDeviceResultListener l) {
		Log.i(Common.TAG, "[GCService] startDeviceScan");
		mScanDeviceResultListener = l;
		
		return getScanner().gcScan(timeOutMs);
	}

	@Override
	public boolean stopDeviceScan() {
		Log.i(Common.TAG, "[GCService] stopDeviceScan");
		
		return getScanner().gcStopScan();
	}

	@Override
	public IDeviceItem getTargetDevice() {
		return mTargetDevice;	
	}
	
	@Override
	public void setTargetDevice(IDeviceItem item) {
		if(item != null) {
			Log.i(Common.TAG, "[GCService] setTargetDevice= " + item.getDeviceBluetoothAddress());
			
			if (item.getDeviceVersion() == DeviceVersion.GC1) {
				if (mWorker.getServiceWorker() instanceof com.htc.gc.internal.v1.GCServiceWorker) {
					// already target to gc1 device
				} else {
					if (mWorker.getServiceWorker() instanceof com.htc.gc.internal.v2.GCServiceWorker) {
						Log.i(Common.TAG, "[GCService] setTargetDevice shutdown old service worker2 ++");
						forceCloseSocket();
						try {
							forceDisconnectBle(new OperationCallback(){

								@Override
								public void error(Exception e) {
								}

								@Override
								public void done(Object that) {
								}});
						} catch (Exception e) {
							Log.e(Common.TAG, "[GCService] setTargetDevice exception raised when force disconnect ble: ", e);
						}
						mWorker.shutdown();
						Log.i(Common.TAG, "[GCService] setTargetDevice shutdown old service worker2 --");
					}
					
					mWorker.setServiceWorker(new com.htc.gc.internal.v1.GCServiceWorker(mCtx, mAppGuid, this));
					
					mDeviceController.setDeviceController(mWorker.getController());
					mAuthManager.setAuthManager(mWorker.getAuthManager());
					mLiveViewer.setLiveViewer(mWorker.getLiveViewer());
					mStillCapturer.setStillCapturer(mWorker.getCapturer());
					mVideoRecorder.setVideoRecorder(mWorker.getRecorder());
					mItemQuerier.setItemQuerier(mWorker.getItemQuerier());
					mItemOperator.setItemOperator(mWorker.getItemOperator());
					mItemDownloader.setItemDownloader(mWorker.getItemDownloader());
					mItemPlayer.setItemPlayer(mWorker.getMediaItemPalyer());
					mAutoBackuper.setAutoBackuper(mWorker.getAutoBackuper());
				}
			} else if (item.getDeviceVersion() == DeviceVersion.GC2) {
				if (mWorker.getServiceWorker() instanceof com.htc.gc.internal.v2.GCServiceWorker) {
					// already target to gc2 device
				} else {
					if (mWorker.getServiceWorker() instanceof com.htc.gc.internal.v1.GCServiceWorker) {
						Log.i(Common.TAG, "[GCService] setTargetDevice shutdown old service worker ++");
						forceCloseSocket();
						try {
							forceDisconnectBle(new OperationCallback(){

								@Override
								public void error(Exception e) {
								}

								@Override
								public void done(Object that) {
								}});
						} catch (Exception e) {
							Log.e(Common.TAG, "[GCService] setTargetDevice exception raised when force disconnect ble: ", e);
						}
						mWorker.shutdown();
						Log.i(Common.TAG, "[GCService] setTargetDevice shutdown old service worker --");
					}
					
					mWorker.setServiceWorker(new com.htc.gc.internal.v2.GCServiceWorker(mCtx, mAppGuid, this));
					
					mDeviceController.setDeviceController(mWorker.getController());
					mAuthManager.setAuthManager(mWorker.getAuthManager());
					mLiveViewer.setLiveViewer(mWorker.getLiveViewer());
					mStillCapturer.setStillCapturer(mWorker.getCapturer());
					mVideoRecorder.setVideoRecorder(mWorker.getRecorder());
					mItemQuerier.setItemQuerier(mWorker.getItemQuerier());
					mItemOperator.setItemOperator(mWorker.getItemOperator());
					mItemDownloader.setItemDownloader(mWorker.getItemDownloader());
					mItemPlayer.setItemPlayer(mWorker.getMediaItemPalyer());
					mAutoBackuper.setAutoBackuper(mWorker.getAutoBackuper());
				}
			} else {
				Log.w(Common.TAG, "[GCService] setTargetDevice to unknown device");
			}
			
		} else {
			Log.i(Common.TAG, "[GCService] setTargetDevice to null");
		}
		
		mTargetDevice = item;
	}
	
	@Override
	public Common.DeviceVersion getTargetDeviceVersion() {
		return mTargetDevice != null ? mTargetDevice.getDeviceVersion() : Common.DeviceVersion.Unknown;
	}

	@Override
	public boolean isSocketConnected() {
		return getSocketStatus().equals(Status.Connected);
	}

	@Override
	public boolean isSocketDisconnected() {
		return getSocketStatus().equals(Status.Disconnected);
	}

	@Override
	public boolean isWifiConnected() {
		return getWifiStatus().equals(Status.Connected);
	}

	@Override
	public boolean isWifiDisconnected() {
		return getWifiStatus().equals(Status.Disconnected);
	}

	@Override
	public boolean isBleVerified() {
		return getBleStatus().equals(Status.Verified);
	}

	@Override
	public boolean isStandalone() {
		return mIsStandalone.get();
	}
	
	void setStandalone(boolean isStandalone) {
		mIsStandalone.set(isStandalone);
	}

	@Override
	public ConnectionMode getExpectedConnectionMode() {
		mExpectedConnectionModeLock.readLock().lock();
		try {
			return mExpectedConnectionMode;
		} finally {
			mExpectedConnectionModeLock.readLock().unlock();
		}
	}

	@Override
	public void setExpectedConnectionMode(ConnectionMode mode) {
		Log.i(Common.TAG, "[GCService] setExpectedConnectionMode= "+mode.toString());
		mExpectedConnectionModeLock.writeLock().lock();
		try {
			mExpectedConnectionMode = mode;
		} finally {
			mExpectedConnectionModeLock.writeLock().unlock();
		}
		
		mWorker.decideNextConnectionMove();
	}

	@Override
	public ConnectionMode getCurrentConnectionMode() {
		mConnectionModeLock.readLock().lock();
		try {
			return mCurrentConnectionMode;	
		} finally {
			mConnectionModeLock.readLock().unlock();
		}
	}
	
	void setCurrentConnectionMode(ConnectionMode mode) {
		mConnectionModeLock.writeLock().lock();
		try {
			mCurrentConnectionMode = mode;	
		} finally {
			mConnectionModeLock.writeLock().unlock();
		}
	}
	
	public void resetExpectedConnectionMode(IGCService.ConnectionMode mode) {
		mExpectedConnectionModeLock.writeLock().lock();
		try {
			Log.i(Common.TAG, "[GCService] resetExpectedConnectionMode to "+mode.toString());
			mExpectedConnectionMode = mode;
		} finally {
			mExpectedConnectionModeLock.writeLock().unlock();
		}
	}

	@Override
	public boolean isSoftApEnable() {
		mSoftApEnableFlagLock.readLock().lock();
		try {
			return mIsSoftApEnable;
		} finally {
			mSoftApEnableFlagLock.readLock().unlock();
		}
	}

	@Override
	public void enableSoftAp(boolean enable) {
		Log.i(Common.TAG, "[GCService] enableSoftAp= "+enable);
		
		mSoftApEnableFlagLock.writeLock().lock();
		try {
			mIsSoftApEnable = enable;	
		} finally {
			mSoftApEnableFlagLock.writeLock().unlock();
		}
	}

	@Override
	public WifiConnectMode getSuggestWifiMode() {
		Log.i(Common.TAG, "[GCService] getSuggestWifiMode");
		
		return NetworkHelper.isNetworkCountryIsoReady(getContentContext()) ? IGCService.WifiConnectMode.WifiP2p : IGCService.WifiConnectMode.SoftAp;
	}

	@Override
	public void rebootGc(RequestCallback callback) throws Exception {
		mWorker.rebootGc(callback);
	}

	@Override
	public void standby(OperationCallback callback) throws Exception {
		mWorker.startup();
	}

	@Override
	public boolean reconnectWifiAp() {
		Log.i(Common.TAG, "[GCService] reconnectWifiAp");
		return reconnectWifiApInternal();
	}

	@Override
	public boolean disconnectWifiAp() {
		return mWorker.disconnectWifiAp();
	}

	@Override
	public void registInternetOperation(Object instance) throws Exception {
		mWorker.registInternetOperation(instance);
	}

	@Override
	public void unregistInternetOperation(Object instance) throws Exception {
		mWorker.unregistInternetOperation(instance);
	}
	
	@Override
	public void removeWifiP2pGroupInFinish(OperationCallback callback) throws Exception {
		Log.i(Common.TAG, "[GCService] removeWifiP2pGroupInFinish");
		
		if(callback == null) throw new NullPointerException();
		
		if(!getWifiP2PGroupRemover().gcRemoveWifiP2pGroupInFinish()) {
			throw new BleCommandException();
		} else {
			mRemoveWifiP2PGroupInFinishCallback = callback;
		}
	}
	
	@Override
	public void setContext(Common.Context context) {
		Log.i(Common.TAG, "[GCService] setContext(" + context + ")");
		
		mWorker.setContext(context);
	}

	@Override
	public IDeviceController getController() {
		return mDeviceController;
	}

	@Override
	public IAuthManager getAuthManager() {
		return mAuthManager;
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
	public IAutoBackuper getAutoBackuper() {
		return mAutoBackuper;
	}

	@Override
	public void setCurrentConnectionModeListener(ConnectionModeListener l) {
		mConnectionModeListener = l;
	}
	
	public ConnectionModeListener getCurrentConnectionModeListener() {
		return mConnectionModeListener;
	}

	@Override
	public void setStandaloneStatusListener(StandaloneStatusListener l) {
		mStandaloneStatusListener = l;
	}
	
	StandaloneStatusListener getStandaloneStatusListener() {
		return mStandaloneStatusListener;
	}

	@Override
	public void setSyncInitDataListener(SyncDataListener l) {
		mSyncDataListener = l;
	}
	
	public SyncDataListener getSyncInitDataListener() {
		return mSyncDataListener;
	}

	@Override
	public void setErrorListener(ErrorListener l) {
		mErrorListener = l;
	}
	
	public ErrorListener getErrorListener() {
		return mErrorListener;
	}

	@Override
	public void setHeartBeatListener(HeartBeatListener l) {
		mHeartbeatListener = l;
	}
	
	public HeartBeatListener getHeartBeatListener() {
		return mHeartbeatListener;
	}

	@Override
	public void setReadyStatusListener(ReadyStatusListener l) {
		mReadyStatusListener = l;
	}
	
	public ReadyStatusListener getReadyStatusListener() {
		return mReadyStatusListener;
	}

	@Override
	public void setFwSupportedFunctionListener(FwSupportedFunctionListener l) {
		mFwSupportedFunctionListener = l;
	}
	
	public FwSupportedFunctionListener getFwSupportedFunctionListener() {
		return mFwSupportedFunctionListener;
	}

	@Override
	public void setPowerStateChangeListener(PowerStateListener l) {
		mPowerStateListener = l;
	}
	
	public PowerStateListener getPowerStateChangeListener() {
		return mPowerStateListener;
	}

	private Handler createConnectionHandler() {
		return new Handler(Looper.getMainLooper()) {
			@Override
			public void handleMessage(Message msg) {
				mRunOnUiHandler.post(new ConnectionCallbackRunnable(msg));
			}
		};
	}
	
	private class ConnectionCallbackRunnable implements Runnable {
		private Message mMsg = new Message();
		public ConnectionCallbackRunnable(Message msg) {
			mMsg.copyFrom(msg);
		}
		
		@Override
		public void run() {
			Bundle bundle = mMsg.getData();
			Result result = null;
			switch (mMsg.what) {
				case IGcConnectivityScanner.CB_BLE_SCAN_RESULT:
					ScanResult scanResult = (ScanResult) bundle.getSerializable(IGcConnectivityScanner.PARAM_RESULT);
					Log.i(Common.TAG, "[GCService] Scan Device Result= "+scanResult.toString());
					if(scanResult.equals(ScanResult.SCAN_RESULT_HIT)) {
						BluetoothDevice device = (BluetoothDevice) bundle.getParcelable(IGcConnectivityScanner.PARAM_BLUETOOTH_DEVICE);
						IGcConnectivityDevice.GcVersion deviceVersion = (IGcConnectivityDevice.GcVersion) bundle.getSerializable(IGcConnectivityScanner.PARAM_BLUETOOTH_DEVICE_VERSION);
						if(device != null) Log.i(Common.TAG, "[GCService] Device found= "+device.getAddress() + ", " + deviceVersion);
						Common.DeviceVersion itemVersion = Common.DeviceVersion.Unknown;
						if (deviceVersion == GcVersion.GC1) {
							itemVersion = Common.DeviceVersion.GC1;
						} else if (deviceVersion == GcVersion.GC2) {
							itemVersion = Common.DeviceVersion.GC2;
						}
						IDeviceItem item = new DeviceItem(device, itemVersion);
						if(getTargetDevice() != null && getTargetDevice().getDeviceBluetoothAddress().equals(device.getAddress())) {
							DeviceItem targetDevice = (DeviceItem) getTargetDevice();
							targetDevice.setDeviceName(item.getDeviceName());
						}
						if(mScanDeviceResultListener != null) {
							mScanDeviceResultListener.onDeviceFound(item);
						} else {
							Log.w(Common.TAG, "mScanDeviceResultListener is null");
						}
					} else if(scanResult.equals(ScanResult.SCAN_RESULT_HIT_CONNECTED)) {
						BluetoothDevice device = (BluetoothDevice) bundle.getParcelable(IGcConnectivityScanner.PARAM_BLUETOOTH_DEVICE);
						IGcConnectivityDevice.GcVersion deviceVersion = (IGcConnectivityDevice.GcVersion) bundle.getSerializable(IGcConnectivityScanner.PARAM_BLUETOOTH_DEVICE_VERSION);
						if(device != null) Log.i(Common.TAG, "[GCService] Connected Device found= "+device.getAddress() + ", " + deviceVersion);
						Common.DeviceVersion itemVersion = Common.DeviceVersion.Unknown;
						if (deviceVersion == GcVersion.GC1) {
							itemVersion = Common.DeviceVersion.GC1;
						} else if (deviceVersion == GcVersion.GC2) {
							itemVersion = Common.DeviceVersion.GC2;
						}
						IDeviceItem item = new DeviceItem(device, itemVersion);
						if(mScanDeviceResultListener != null) {
							mScanDeviceResultListener.onConnectedDeviceFound(item);
						}  else {
							Log.w(Common.TAG, "mScanDeviceResultListener is null");
						}
					} else if(scanResult.equals(ScanResult.SCAN_RESULT_COMPLETE)) {
						if(mScanDeviceResultListener != null) {
							mScanDeviceResultListener.onScanComplete();
						} else {
							Log.w(Common.TAG, "mScanDeviceResultListener is null");
						}
						
						Log.i(Common.TAG, "[GCService] Scan Device complete, remove listener= "+mScanDeviceResultListener);
						mScanDeviceResultListener = null;
					} else if(scanResult.equals(ScanResult.SCAN_RESULT_ERROR)) {
						if(mScanDeviceResultListener != null) {
							mScanDeviceResultListener.onError(GCService.this, new Common.ScanBleException());
						} else {
							Log.w(Common.TAG, "mScanDeviceResultListener is null");
						}
						
						Log.i(Common.TAG, "[GCService] Scan Device error, remove listener= "+mScanDeviceResultListener);
						mScanDeviceResultListener = null;
					}
				break;
				
				case IGcConnectivityWifiP2PGroupRemover.CB_REMOVE_WIFI_P2P_GROUP_IN_FINISH_RESULT:
					result = (Result) bundle.getSerializable(IGcConnectivityWifiP2PGroupRemover.PARAM_RESULT);
					Log.i(Common.TAG, "[GCService] Remove Wifi P2P Group In Finish Result= "+result);
					
					// for M8 and EYE both only can scan at background after GO is removed, so reconnect at here
					reconnectWifiAp();
					
					if(mRemoveWifiP2PGroupInFinishCallback != null) {
						if(result.equals(Result.RESULT_SUCCESS)) {
							mRemoveWifiP2PGroupInFinishCallback.done(this);
						} else {
							mRemoveWifiP2PGroupInFinishCallback.error(new BleCommandException());
						}
						mRemoveWifiP2PGroupInFinishCallback = null;
					} else {
						Log.e(Common.TAG, "[GCService] Remove Wifi P2P Group In Finish callback is null");
					}
					break;
				
				default:
					Log.w(Common.TAG, "[GCService] Unknown BLE Callback: "+mMsg.what);
					break;
			}
		}
	}
	
	public Status getSocketStatus() {
		mSocketStatusLock.readLock().lock();
		try {
			return mSocketStatus;
		} finally {
			mSocketStatusLock.readLock().unlock();
		}
	}
	
	public void setSocketStatus(Status status) {
		mSocketStatusLock.writeLock().lock();
		try {
			mSocketStatus = status;
		} finally {
			mSocketStatusLock.writeLock().unlock();
		}
		
		updateCurrentConnectionMode();
	}
	
	public Status getWifiStatus() {
		mWifiStatusLock.readLock().lock();
		try {
			return mWifiStatus;
		} finally {
			mWifiStatusLock.readLock().unlock();
		}
	}
	
	public void setWifiStatus(Status status) {
		mWifiStatusLock.writeLock().lock();
		try {
			mWifiStatus = status;
		} finally {
			mWifiStatusLock.writeLock().unlock();
		}
			
		updateCurrentConnectionMode();
	}
	
	public Status getBleStatus() {
		mBleStatusLock.readLock().lock();
		try {
			return mBleStatus;
		} finally {
			mBleStatusLock.readLock().unlock();
		}
	}
	
	public void setBleStatus(Status status) {
		mBleStatusLock.writeLock().lock();
		try {
			mBleStatus = status;
		} finally {
			mBleStatusLock.writeLock().unlock();	
		}
			
		updateCurrentConnectionMode();
	}
	
	public void updateCurrentConnectionMode() {
		IGCService.ConnectionMode currentMode;
		if(getBleStatus() == Status.Verified) {
			if(getSocketStatus() == Status.Connected && getWifiStatus() == Status.Connected) {
				currentMode = IGCService.ConnectionMode.Full;
			} else {
				currentMode = IGCService.ConnectionMode.Partial;
			}
		} else {
			currentMode = IGCService.ConnectionMode.Disconnected;
		}
		
		if(getCurrentConnectionMode() != currentMode) {
			setCurrentConnectionMode(currentMode);
			
			Log.i(Common.TAG, "[GCService] updateCurrentConnectionMode, currentMode= "+currentMode);
			IGCService.ConnectionModeListener connectionModeListener = getCurrentConnectionModeListener();
			if(connectionModeListener != null) {
				switch(currentMode) {
				case Disconnected:
					connectionModeListener.onDisconnectedMode();
					break;
				case Partial:
					connectionModeListener.onPartialMode();
					break;
				case Full:
					connectionModeListener.onFullMode();
					break;
				}
			}
		}
		
		boolean isStandalone = getSocketStatus().equals(Status.Disconnected) &&
								getWifiStatus().equals(Status.Disconnected) &&
								getBleStatus().equals(Status.Disconnected);
		
		if(isStandalone() != isStandalone) {
			setStandalone(isStandalone);
			Log.i(Common.TAG, "[GCService] updateCurrentConnectionMode,isStandalone= "+isStandalone);	
			
			if(isStandalone) {
				IGCService.StandaloneStatusListener standaloneStatusListener = getStandaloneStatusListener();
				if(standaloneStatusListener != null) standaloneStatusListener.onStandalone();
			}

		}
	}
	
	android.content.Context getContentContext() {
		return mCtx;
	}
	
	public boolean reconnectWifiApInternal() {
		WifiManager wifiManager = (WifiManager) getContentContext().getSystemService(android.content.Context.WIFI_SERVICE);
		
		if(android.os.Build.VERSION.SDK_INT >= 21 /* LOLLIPOP */) {
			List<WifiConfiguration> wifiList = wifiManager.getConfiguredNetworks();
			if(wifiList == null) {
				Log.i(Common.TAG, "ConfiguredNetworks list is null");
			} else {
				for(WifiConfiguration configuration:wifiList) {
					boolean enableResult = wifiManager.enableNetwork(configuration.networkId, false);
					Log.i(Common.TAG, "[GCService] enable network: "+configuration.SSID+", reuslt= "+enableResult);
				}					
			}
		}
		
		boolean result = wifiManager.reconnect();
		Log.i(Common.TAG, "[GCService] reconnectWifiApInternal, reuslt= "+result);
		return result;
	}

	public boolean disconnectWifiApInternal() {
    	WifiManager wifiManager = (WifiManager) getContentContext().getSystemService(android.content.Context.WIFI_SERVICE);
    	
		if(android.os.Build.VERSION.SDK_INT >= 21 /* LOLLIPOP */) {
			List<WifiConfiguration> wifiList = wifiManager.getConfiguredNetworks();
			if(wifiList == null) {
				Log.i(Common.TAG, "ConfiguredNetworks list is null");
			} else {
				for(WifiConfiguration configuration:wifiList) {
					boolean disableResult = wifiManager.disableNetwork(configuration.networkId);
					Log.i(Common.TAG, "[GCService] disable network: "+configuration.SSID+", reuslt= "+disableResult);
				}
			}
		}
		
    	boolean result = wifiManager.disconnect();
    	Log.i(Common.TAG, "[GCService] disconnectWifiApInternal, result= "+result);
    	return result;
    }
	
	private IGcConnectivityScanner getScanner() {
		IGcConnectivityScanner scanner = mScanner;
		if (scanner == null) {
			synchronized (mAccessLock) {
				scanner = mScanner;
				if (scanner == null) {
					if(DEBUG &&	new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/" + DEBUG_SOFTAP_FLAG_NAME).isDirectory()) {
						mScanner = new GcDummyConnectivityScanner(mCtx, mMessenger);
					} else {
						mScanner = new GcConnectivityScanner(mCtx, mMessenger);
					}
					scanner = mScanner;
				}
			}
		}
		return scanner;
	}
	
	private IGcConnectivityWifiP2PGroupRemover getWifiP2PGroupRemover() {
		IGcConnectivityWifiP2PGroupRemover wifiP2PGroupRemover = mWifiP2PGroupRemover;
		if (wifiP2PGroupRemover == null) {
			synchronized (mAccessLock) {
				wifiP2PGroupRemover = mWifiP2PGroupRemover;
				if (wifiP2PGroupRemover == null) {
					if(DEBUG &&	new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/" + DEBUG_SOFTAP_FLAG_NAME).isDirectory()) {
						mWifiP2PGroupRemover = new GcDummyConnectivityWifiP2PGroupRemover(mCtx, mMessenger);
					} else {
						mWifiP2PGroupRemover = new GcConnectivityWifiP2PGroupRemover(mCtx, mMessenger);
					}
					wifiP2PGroupRemover = mWifiP2PGroupRemover;
				}
			}
		}
		return wifiP2PGroupRemover;
	}
}
