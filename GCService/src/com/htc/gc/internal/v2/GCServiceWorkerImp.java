package com.htc.gc.internal.v2;

import java.io.File;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import android.bluetooth.BluetoothDevice;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.util.Log;
import android.util.SparseArray;

import com.htc.dummy.connectivity.v3.GcDummyConnectivityService;
import com.htc.gc.GCService;
import com.htc.gc.GCService.Status;
import com.htc.gc.connectivity.interfaces.IGcConnectivityServiceBase.LongTermEvent;
import com.htc.gc.connectivity.interfaces.IGcConnectivityServiceBase.PlugIO;
import com.htc.gc.connectivity.interfaces.IGcConnectivityServiceBase.Result;
import com.htc.gc.connectivity.interfaces.IGcConnectivityServiceBase.SwitchOnOff;
import com.htc.gc.connectivity.interfaces.IGcConnectivityServiceBase.VerifyPasswordStatus;
import com.htc.gc.connectivity.v3.GcConnectivityService;
import com.htc.gc.connectivity.v3.interfaces.IGcConnectivityService;
import com.htc.gc.connectivity.v3.interfaces.IGcConnectivityService.TriggerFWUpdateResult;
import com.htc.gc.interfaces.Common;
import com.htc.gc.interfaces.Common.BleCommandException;
import com.htc.gc.interfaces.Common.CancelException;
import com.htc.gc.interfaces.Common.CommonException;
import com.htc.gc.interfaces.Common.ConnectionErrorCode;
import com.htc.gc.interfaces.Common.GC2WifiMgrErrorCode;
import com.htc.gc.interfaces.Common.Context;
import com.htc.gc.interfaces.Common.ErrorCallback;
import com.htc.gc.interfaces.Common.NoImpException;
import com.htc.gc.interfaces.Common.OperationCallback;
import com.htc.gc.interfaces.Common.StatusException;
import com.htc.gc.interfaces.IAuthManager;
import com.htc.gc.interfaces.IAutoBackuper;
import com.htc.gc.interfaces.IAutoBackuper.AutoBackupGetAccountCallback;
import com.htc.gc.interfaces.IAutoBackuper.AutoBackupGetHttpProxyCallback;
import com.htc.gc.interfaces.IAutoBackuper.AutoBackupPreferenceCallback;
import com.htc.gc.interfaces.IAutoBackuper.AutoBackupStatusCallback;
import com.htc.gc.interfaces.IAutoBackuper.OptionCheck;
import com.htc.gc.interfaces.IAutoBackuper.ProviderType;
import com.htc.gc.interfaces.IDeviceController;
import com.htc.gc.interfaces.IDeviceController.BatteryInfoCallback;
import com.htc.gc.interfaces.IDeviceController.GetCameraModeCallback;
import com.htc.gc.interfaces.IDeviceController.GetLTECampingStatusCallback;
import com.htc.gc.interfaces.IDeviceController.GetSimHwStatusCallback;
import com.htc.gc.interfaces.IDeviceController.GetSimInfoCallback;
import com.htc.gc.interfaces.IDeviceController.UnlockSimPinCallback;
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
import com.htc.gc.interfaces.IVideoRecorder.BroadcastEnableSettingCallback;
import com.htc.gc.interfaces.IVideoRecorder.BroadcastErrorListCallback;
import com.htc.gc.interfaces.IVideoRecorder.BroadcastInvitationListCallback;
import com.htc.gc.interfaces.IVideoRecorder.BroadcastPlatformCallback;
import com.htc.gc.interfaces.IVideoRecorder.BroadcastPrivacyCallback;
import com.htc.gc.interfaces.IVideoRecorder.BroadcastSMSContentCallback;
import com.htc.gc.interfaces.IVideoRecorder.BroadcastStatusCallback;
import com.htc.gc.interfaces.IVideoRecorder.BroadcastUserNameCallback;
import com.htc.gc.interfaces.IVideoRecorder.BroadcastVideoUrlCallback;
import com.htc.gc.internal.DeviceItem;
import com.htc.gc.internal.IGCServiceWorker;
import com.htc.gc.internal.NetworkHelper;

import de.tavendo.autobahn.WebSocket;
import de.tavendo.autobahn.WebSocketConnection;
import de.tavendo.autobahn.WebSocketConnectionHandler;

abstract class GCServiceWorkerImp implements IGCServiceWorker, IMediator {
	protected class BleErrorCallback {
		private final Object mThat;
		private final int mCallbackID;
		private final ErrorCallback mCallback;
		
		BleErrorCallback(Object that, int callbackID, ErrorCallback callback) {
			mThat = that;
			mCallbackID = callbackID;
			mCallback = callback;
		}
		
		public Object getThat() {
			return mThat;
		}
		
		public int getID() {
			return mCallbackID;
		}
		
		public ErrorCallback getCallback() {
			return mCallback;
		}
	}
	
	protected static final boolean DEBUG = Common.DEBUG;
	protected static final String DEBUG_SOFTAP_FLAG_NAME = "SOFTAP";
	
	public static final byte[] DC_GUID =
		 {(byte)0x22,(byte)0x2D,(byte)0x35,(byte)0x26,
		  (byte)0x2C,(byte)0x25,(byte)0x34,(byte)0x24,
		  (byte)0x28,(byte)0x36,(byte)0x2A,(byte)0x25,
		  (byte)0xF3,(byte)0xF2,(byte)0xF1,(byte)0xF0};


	private static final int CONNECT_SOCKET_RETRY_TIMES = 2;
	private static final int HEART_BEAT_DEAD_TOLERANCE_THRESHOLD = 3;

	private static final int CONNECT_SILENT_RECONNECT_BLE_TIMEOUT = 25000; // ms
	private static final int CONNECT_SILENT_RECONNECT_BLE_MAX_RETRY_TIMES = 15;

	private final android.content.Context mCtx;
	protected final IGCService mGCService;

	protected final byte[] mAppGuid;
	protected final IGcConnectivityService mConn;

	protected final LinkedList<BleErrorCallback> mBleCallbacks = new LinkedList<BleErrorCallback>();
	protected final SparseArray<ArrayList<IBleEventListener>> mBleEventHandler = new SparseArray<ArrayList<IBleEventListener>>();

	protected final IItemQuerier mItemQuerier = new GCItemQuerier(this);
	protected final IDeviceController mController = new GCDeviceController(this);
	protected final IStillCapturer mStillCapturer = new GCStillCapturer(this);
	protected final IVideoRecorder mVideoRecorder = new GCVideoRecorder(this);
	protected final ILiveViewer mLiveViewer = new GCLiveViewer(this);
	protected final IItemDownloader mItemDownloader = new GCItemDownloader(this);
	protected final IItemOperator mItemOperator = new GCItemOperator(this);
	protected final IItemPlayer mItemPlayer = new GCItemPlayer(this);
	protected final IAuthManager mAuthManager = new GCAuthManager(this);
	protected final IAutoBackuper mAutoBackuper = new GCAutoBackuper(this);

	private WebSocketConnection mWebSocketConnection;
	
	protected final AtomicBoolean mHeartBeatLive = new AtomicBoolean(false);
	private final AtomicInteger mHeartBeatDeadCounter = new AtomicInteger();
	private final AtomicBoolean mHeartBeatGotNotify = new AtomicBoolean(false);
	private Thread mHeartBeatSendPingThread;
	private Thread mHeartBeatMonitorThread;

	protected String mAddress;
	protected String mBleVersion;
	protected String mA12Version;
	protected String mModemVersion;
	protected String mMcuVersion2;

	protected volatile Context mContext = Context.None;

	// protected volatile WifiConnectMode mSuggestWifiConnectionMode =
	// WifiConnectMode.None;

	private Handler mRunOnUiHandler = new Handler(Looper.getMainLooper());
	
	protected int mReady = 0;
	protected IMediaItem mLastItem = null;

	protected UUID mCursorUniqueKey = UUID.randomUUID();
	
	private static String NO_CONNECTION_ERROR = "No connection error";
	private static String NONE = "None";
	private static String BLE_ERROR_RECOVERY_ACTION = "Toggle BT enable/disable, if no use then reset GC";
	private static String SOCKET_ERROR_RECOVERY_ACTION = "Try to connect again";
	
	private String mLastConnectionErrorString = NO_CONNECTION_ERROR; 
	private String mLastConnectionErrorRecoveryActionString = NONE;
	private final AtomicInteger mLastConnectionErrorCode = new AtomicInteger();
	
	private final AtomicLong mBleStartConnectTimestamp = new AtomicLong(0);
	private final AtomicInteger mBleSilentConnectRetryTimes = new AtomicInteger(0);
	
	protected final ConcurrentLinkedQueue<Object> mInternetOperationCounterQueue = new ConcurrentLinkedQueue<Object>();
	
	private void setLastConnectionError(String err, String recoveryAction, int code) {
		mLastConnectionErrorString = err;
		mLastConnectionErrorRecoveryActionString = recoveryAction;
		mLastConnectionErrorCode.set(code);
	}
	
	protected final AtomicBoolean mIsInUpgradeProcess = new AtomicBoolean();

	protected OperationCallback mOpenSocketCallback = createSocketCallback();
	private final AtomicInteger mOpenSocketRetryCounter = new AtomicInteger();
	
	GCServiceWorkerImp(android.content.Context ctx, byte[] appGuid, IGCService gcService) {
		mCtx = ctx;
		mGCService = gcService;
		
		if(DEBUG &&	new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/" + DEBUG_SOFTAP_FLAG_NAME).isDirectory()) {
			mConn = new GcDummyConnectivityService(mCtx, new Messenger(createConnectionHandler()));
		} else {
			mConn = new GcConnectivityService(mCtx, new Messenger(createConnectionHandler()));
		}
		
		mAppGuid = appGuid;
			
		initEventListener();
	}
	
	protected android.content.Context getContentContext() {
		return mCtx;
	}
	
	private OperationCallback createSocketCallback() {
		return new OperationCallback() {
			@Override
			public void error(final Exception e) {
				Log.i(Common.TAG, "[GCServiceWorker2] Create Socket Connection Fail. Error= "+e.toString());
				e.printStackTrace();
				
				final int retry = mOpenSocketRetryCounter.getAndAdd(1);
				
				if(retry < CONNECT_SOCKET_RETRY_TIMES) {
					Log.i(Common.TAG, "[GCServiceWorker2] Create Socket Connection Fail, retry connect "+retry+"th time(s).");
					// TODO Move this into decideNextConnectionMove
					mRunOnUiHandler.post(new Runnable() {

						@Override
						public void run() {
							closeSocket(e);
						}
						
					});
					decideNextConnectionMove();
				} else {
					Log.i(Common.TAG, "[GCServiceWorker2] Create Socket Connection Fail, retry connect "+retry+"th time(s), stop retry. Disconnect wifi");
					mOpenSocketRetryCounter.set(0);
					setLastConnectionError("[GCServiceWorker2] Connect Socket Fail twice time, wifi might disconnect", NetworkHelper.getWifiErrorRecoveryAction(ConnectionErrorCode.SOCKET_CONNECT_FAIL.getVal()), ConnectionErrorCode.SOCKET_CONNECT_FAIL.getVal());
					setWifiStatus(Status.Error);
					decideNextConnectionMove();
				}
			}

			@Override
			public void done(Object that) {
				Log.i(Common.TAG, "[GCServiceWorker2] Create Socket Connection Success");
				mOpenSocketRetryCounter.set(0);
				
				decideNextConnectionMove();
			}
		};
	}
	
	private void resetExpectedConnectionMode(IGCService.ConnectionMode mode) {
		((GCService)mGCService).resetExpectedConnectionMode(mode);
	}
	
	protected void forceResetWifiStatus() {
		setWifiStatus(Status.Disconnected);
		checkIfNeedToRemoveWifiP2pGroupWhenWifiDisconnected();
	}
	
	protected void forceResetBleStatus() {
		setBleStatus(Status.Disconnected);
	}
	
	private void setBleStartConnectTimestampAndIncreaseCounter(long timestamp) {
		int count = mBleSilentConnectRetryTimes.incrementAndGet();
		if(mBleStartConnectTimestamp.compareAndSet(0, timestamp)) {
			Log.i(Common.TAG, "[GCServiceWorker2] setBleStartConnectTimestampAndIncreaseCounter timestamp= "+timestamp+" counter= "+count);
		} else {
			Log.i(Common.TAG, "[GCServiceWorker2] setBleStartConnectTimestamp last timestamp is not 0, do nothing. counter= "+count);
		}
	}
	
	private void resetBleStartConnectTimestampAndCounter() {
		Log.i(Common.TAG, "[GCServiceWorker2] resetBleStartConnectTimestampAndCounter");
		mBleStartConnectTimestamp.set(0);
		mBleSilentConnectRetryTimes.set(0);
	}
	
	@Override
	public void decideNextConnectionMove() {
		mRunOnUiHandler.post(new Runnable() {

			@Override
			public void run() {
				boolean errorOccurs = false;
				if(getBleStatus() == Status.Error) {
					Log.i(Common.TAG, "[GCServiceWorker2] Error occurs. BleStatus= "+getBleStatus()+", Reset to "+Status.Disconnected);
					// clean up process
					if(!mIsInUpgradeProcess.get()) {
						// Don't disconnect socket & wifi when ble error if in upgrade process
						closeSocket(new CancelException());
						checkIfNeedToRemoveWifiP2pGroupWhenWifiDisconnected();
						setWifiStatus(Status.Disconnected);						
					}
					setBleStatus(Status.Disconnected);
					errorOccurs = true;
				}
				if(getWifiStatus() == Status.Error) {
					Log.i(Common.TAG, "[GCServiceWorker2] Error occurs. WifiStatus= "+getWifiStatus()+", Reset to "+Status.Disconnected);
					// clean up process
					closeSocket(new CancelException());
					checkIfNeedToRemoveWifiP2pGroupWhenWifiDisconnected();
					setWifiStatus(Status.Disconnected);
					errorOccurs = true;
				}
				if(getSocketStatus() == Status.Error) {
					Log.i(Common.TAG, "[GCServiceWorker2] Error occurs. SocketStatus= "+getSocketStatus()+", Reset to "+Status.Disconnected);
					// clean up process
					closeSocket(new CancelException());
					errorOccurs = true;
				}

				if(errorOccurs == false) {
					IGCService.ConnectionMode expectedConnectionMode = mGCService.getExpectedConnectionMode();
					Status bleStatus = getBleStatus();
					Status wifiStatus = getWifiStatus();
					Status socketStatus = getSocketStatus();
					Log.i(Common.TAG, "[GCServiceWorker2] decideNextConnectionMove, bleStauts: "+bleStatus+", wifiStatus: "+wifiStatus+", socketStatus: "+socketStatus + ", expected connection mode: " + expectedConnectionMode);
					
					switch(expectedConnectionMode) {
					case Disconnected:
						if(socketStatus == Status.Connected || socketStatus == Status.Connecting) {
							Log.i(Common.TAG, "[GCServiceWorker2] Disconnect Socket");
							closeSocket(new CancelException());
							decideNextConnectionMove();
							break;
						}
						if(wifiStatus == Status.Connected) {
							if(mConn.gcWifiDisconnect(((DeviceItem)getTargetDevice()).getDevice())) {
								Log.i(Common.TAG, "[GCServiceWorker2] Disconnect Wifi");
								setWifiStatus(Status.Disconnecting);
							} else {
								Log.e(Common.TAG, "[GCServiceWorker2] Disconnect Wifi Fail");
								checkIfNeedToRemoveWifiP2pGroupWhenWifiDisconnected();
							}
							break;
						}
						if(wifiStatus == Status.Disconnected && (bleStatus == Status.Connected || bleStatus == Status.Verified)) {
							if(mConn.gcBleDisconnect(((DeviceItem)getTargetDevice()).getDevice())) {
								Log.i(Common.TAG, "[GCServiceWorker2] Disconnect BLE");
								setBleStatus(Status.Disconnecting);	
							} else {
								Log.e(Common.TAG, "[GCServiceWorker2] Disconnect BLE Fail");
							}
							break;
						}
						break;
					case Partial:
						if(bleStatus == Status.Disconnected) {
							if(mConn.gcBleConnect(((DeviceItem)getTargetDevice()).getDevice())) {
								Log.i(Common.TAG, "[GCServiceWorker2] Connect BLE");
								setBleStatus(Status.Connecting);
								setBleStartConnectTimestampAndIncreaseCounter(System.currentTimeMillis());
							} else {
								Log.e(Common.TAG, "[GCServiceWorker2] Connect BLE Fail");
							}
							break;
						}
						if(bleStatus == Status.Connected) {
							DeviceItem device = (DeviceItem)getTargetDevice();
							if(mConn.gcVerifyPassword(device.getDevice(), device.getPassword())) {
								Log.i(Common.TAG, "[GCServiceWorker2] VerifyPassword");								
								setBleStatus(Status.Verifying);
							} else {
								Log.e(Common.TAG, "[GCServiceWorker2] VerifyPassword Fail");
							}
							break;
						}
						if(socketStatus == Status.Connected || socketStatus == Status.Connecting) {
							Log.i(Common.TAG, "[GCServiceWorker2] Disconnect Socket");
							closeSocket(new CancelException());
							// closeSocket is a sync function, no need to break, do next thing
						}
						if(bleStatus == Status.Verified && wifiStatus == Status.Connected) {
							if(mConn.gcWifiDisconnect(((DeviceItem)getTargetDevice()).getDevice())) {
								Log.i(Common.TAG, "[GCServiceWorker2] Disconnect WIFI");
								setWifiStatus(Status.Disconnecting);
							} else {
								Log.e(Common.TAG, "[GCServiceWorker2] Disconnect WIFI Fail");
								
								checkIfNeedToRemoveWifiP2pGroupWhenWifiDisconnected();
							}
							break;
						}
						break;
					case Full:
						if(bleStatus == Status.Disconnected) {
							if(mConn.gcBleConnect(((DeviceItem)getTargetDevice()).getDevice())) {
								Log.i(Common.TAG, "[GCServiceWorker2] Connect BLE");
								setBleStatus(Status.Connecting);
								setBleStartConnectTimestampAndIncreaseCounter(System.currentTimeMillis());
							} else {
								Log.e(Common.TAG, "[GCServiceWorker2] Connect BLE Fail");
							}
							break;
						} 
						if(bleStatus == Status.Connected) {
							DeviceItem device = (DeviceItem)getTargetDevice();
							if(mConn.gcVerifyPassword(device.getDevice(), device.getPassword())) {
								Log.i(Common.TAG, "[GCServiceWorker2] VerifyPassword");								
								setBleStatus(Status.Verifying);
							} else {
								Log.e(Common.TAG, "[GCServiceWorker2] VerifyPassword Fail");
							}
							break;
						}
						if(bleStatus == Status.Verified && wifiStatus == Status.Disconnected) {
							if(mGCService.isSoftApEnable()) {
								DeviceItem deviceItem = (DeviceItem) getTargetDevice();
								if(mConn.gcSoftAPConnect(deviceItem.getDevice(), deviceItem.getPassword())) {
									Log.i(Common.TAG, "[GCServiceWorker2] Connect Soft AP WIFI");
									setWifiStatus(Status.Connecting);	
								} else {
									Log.e(Common.TAG, "[GCServiceWorker2] Connect Soft Ap WIFI Fail");
								}								
							} else {
								((GCService)mGCService).disconnectWifiApInternal();
								
								if(mConn.gcWifiConnect(((DeviceItem)getTargetDevice()).getDevice())) {
									Log.i(Common.TAG, "[GCServiceWorker2] Connect WIFI");
									setWifiStatus(Status.Connecting);
								} else {
									Log.e(Common.TAG, "[GCServiceWorker2] Connect WIFI Fail");
								}								
							}
							break;
						}
						if(bleStatus == Status.Verified &&
							wifiStatus == Status.Connected &&
							socketStatus == Status.Disconnected) 
						{
							Log.i(Common.TAG, "[GCServiceWorker2] Connect Socket");
							openSocket(mAppGuid, getTargetDevice().getIP(), mOpenSocketCallback);
							break;
						}
						break;
					}
				} else {
					final IGCService.ConnectionMode connectionMode = mGCService.getCurrentConnectionMode();
					resetExpectedConnectionMode(connectionMode);
					final int errorCode = mLastConnectionErrorCode.get();
					IGCService.ConnectionModeListener connectionModeListener = ((GCService)mGCService).getCurrentConnectionModeListener();
					if(connectionModeListener != null) {
						String description = mLastConnectionErrorString+" \n"
											+"Recovery action: "+mLastConnectionErrorRecoveryActionString+"\n"
											+"Error code: 0x"+Integer.toHexString(errorCode);
						
						boolean isSilentReconnectSocket = false;
						// TODO
						
						boolean isSilentReconnectBle = false;
						if(errorCode == ConnectionErrorCode.BLE_DISCONNECT_FROM_GATT_SERVER.getVal() ||
							errorCode == ConnectionErrorCode.BLE_CONNECT_FAIL.getVal()) {
							long currentTimestamp = System.currentTimeMillis();
							if(currentTimestamp - mBleStartConnectTimestamp.get() <= CONNECT_SILENT_RECONNECT_BLE_TIMEOUT &&
								mBleSilentConnectRetryTimes.get() <= CONNECT_SILENT_RECONNECT_BLE_MAX_RETRY_TIMES) {
								Log.i(Common.TAG, "[GCServiceWorker2] slient reconnectBle flag= true");
								isSilentReconnectBle = true;
							} else {
								Log.i(Common.TAG, "[GCServiceWorker2] slient reconnectBle flag= false");
								resetBleStartConnectTimestampAndCounter();
							}
						}
						
						connectionModeListener.onError(mGCService, 
								new Common.ConnectionException(connectionMode, mLastConnectionErrorCode.get(), description, isSilentReconnectSocket, isSilentReconnectBle));	
					}
				}				
			}
			
		});
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
				case IGcConnectivityService.CB_BLE_CONNECT_RESULT:
					result = (Result) bundle.getSerializable(IGcConnectivityService.PARAM_RESULT);
					Log.i(Common.TAG, "[GCServiceWorker2] BLE Connect Result= "+result);
					if(result.equals(Result.RESULT_SUCCESS)) {
						setBleStatus(Status.Connected);
					} else {
						setBleStatus(Status.Error);
						setLastConnectionError("BLE Connect Fail!", BLE_ERROR_RECOVERY_ACTION, ConnectionErrorCode.BLE_CONNECT_FAIL.getVal());
					}
					decideNextConnectionMove();
					break;
				case IGcConnectivityService.CB_BLE_DISCONNECT_RESULT:
					result = (Result) bundle.getSerializable(IGcConnectivityService.PARAM_RESULT);
					Log.i(Common.TAG, "[GCServiceWorker2] BLE Disconnect Result= "+result);
					if(result.equals(Result.RESULT_SUCCESS)) {
						setBleStatus(Status.Disconnected);
					} else {
						setBleStatus(Status.Error);
						setLastConnectionError("BLE Disconnect Fail!", BLE_ERROR_RECOVERY_ACTION, ConnectionErrorCode.BLE_DISCONNECT_FAIL.getVal());
					}
					decideNextConnectionMove();
					break;
					
				case IGcConnectivityService.CB_BLE_DISCONNECT_FORCE_RESULT:
					result = (Result) bundle.getSerializable(IGcConnectivityService.PARAM_RESULT);
					Log.i(Common.TAG, "[GCServiceWorker2] BLE Force Disconnect Result= "+result);
					
					BleErrorCallback bleCallback = popBleCallbackFromQueue();
					if(bleCallback != null) {
						if(bleCallback.getID() == IGcConnectivityService.CB_BLE_DISCONNECT_FORCE_RESULT) {
							OperationCallback operationCallback = (OperationCallback) bleCallback.getCallback();
							if(result.equals(Result.RESULT_SUCCESS)) {
								operationCallback.done(bleCallback.getThat());
							} else {
								operationCallback.error(new BleCommandException());
							}
						} else {
							Log.e(Common.TAG, "[GCServiceWorker2] Ble Force Disconnect Callback ID doesn't match: "+bleCallback.getID());
						}						
					} else {
						Log.e(Common.TAG, "[GCServiceWorker2] Ble Callback List is empty");
					}
					break;

				case IGcConnectivityService.CB_WIFI_CONNECT_RESULT:
					result = (Result) bundle.getSerializable(IGcConnectivityService.PARAM_RESULT);
					
					boolean isSoftAp = false;
					Boolean softApFlag = (Boolean) bundle.getSerializable(IGcConnectivityService.PARAM_RESULT_SOFTAP);
					if(softApFlag != null && softApFlag.booleanValue() == true) {
						isSoftAp = true;
					}
					
					Log.i(Common.TAG, "[GCServiceWorker2] WIFI Connect Result= "+result+", isSoftAp= "+isSoftAp);
					if (result == Result.RESULT_SUCCESS) {
						String ip = bundle.getString(IGcConnectivityService.PARAM_DEVICE_IP_ADDRESS);
						IDeviceItem deviceItem = getTargetDevice();
						deviceItem.setIP(ip);
						Log.i(Common.TAG, "[GCServiceWorker2] Receive Device IP= "+ip);
						setWifiStatus(Status.Connected);
						
						BleInitProcessAfterWifiIsConnected();
					} else {
						int errorCode = bundle.getInt(IGcConnectivityService.PARAM_WIFI_ERROR_CODE);
						Log.i(Common.TAG, "[GCServiceWorker2] WIFI Connect Result= "+result+", Error code= 0x"+Integer.toHexString(errorCode));
						setWifiStatus(Status.Error);
						setLastConnectionError("Wifi Connect Fail.", NetworkHelper.getWifiErrorRecoveryAction(errorCode), errorCode);
						
						if(errorCode == 0x1a) {
							Log.i(Common.TAG,"[GCServiceWorker2] Wifi connect fail 0x1a, remove wifi p2p group");
							try {
								removeWifiP2pGroup(new OperationCallback(){

									@Override
									public void error(Exception e) {
										Log.e(Common.TAG, "[GCServiceWorker2] removeWifiP2pGroupWhenWifiConnectFail0x1a error= "+e.toString());
									}

									@Override
									public void done(Object that) {
										Log.e(Common.TAG, "[GCServiceWorker2] removeWifiP2pGroupWhenWifiConnectFail0x1a done");
									}
									
								});
							} catch(Exception e) {
								Log.e(Common.TAG,"[GCServiceWorker2] Wifi connect fail 0x1a, remove wifi p2p group but fail");				
							}
						}
					}
					decideNextConnectionMove();
					break;

				case IGcConnectivityService.CB_WIFI_DISCONNECT_RESULT:
					result = (Result) bundle.getSerializable(IGcConnectivityService.PARAM_RESULT);
					if (result == Result.RESULT_SUCCESS) {
						Log.i(Common.TAG, "[GCServiceWorker2] WIFI Disconnect Result= "+result);
						setWifiStatus(Status.Disconnected);
					} else {
						int errorCode = bundle.getInt(IGcConnectivityService.PARAM_WIFI_ERROR_CODE);
						Log.i(Common.TAG, "[GCServiceWorker2] WIFI Disconnect Result= "+result+", Error code= 0x"+Integer.toHexString(errorCode));
						setWifiStatus(Status.Error);
						setLastConnectionError("Wifi Disconnect Fail.", NetworkHelper.getWifiErrorRecoveryAction(errorCode), errorCode);
					}
					
					checkIfNeedToRemoveWifiP2pGroupWhenWifiDisconnected();
					
					decideNextConnectionMove();
					break;
					
				case IGcConnectivityService.CB_CREATE_WIFI_P2P_GROUP_RESULT:
					result = (Result) bundle.getSerializable(IGcConnectivityService.PARAM_RESULT);
					Log.i(Common.TAG, "[GCServiceWorker2] BLE Create Wifi P2P Group Result= "+result);
					break;
				
				case IGcConnectivityService.CB_REMOVE_WIFI_P2P_GROUP_RESULT:
					result = (Result) bundle.getSerializable(IGcConnectivityService.PARAM_RESULT);
					Log.i(Common.TAG, "[GCServiceWorker2] BLE Remove Wifi P2P Group Result= "+result);
					
					// for M8 and EYE both only can scan at background after GO is removed, so reconnect at here
					mGCService.reconnectWifiAp();
					
					bleCallback = popBleCallbackFromQueue();
					if(bleCallback != null) {
						if(bleCallback.getID() == IGcConnectivityService.CB_REMOVE_WIFI_P2P_GROUP_RESULT) {
							OperationCallback operationCallback = (OperationCallback) bleCallback.getCallback();
							if(result.equals(Result.RESULT_SUCCESS)) {
								operationCallback.done(bleCallback.getThat());
							} else {
								operationCallback.error(new BleCommandException());
							}
						} else {
							Log.e(Common.TAG, "[GCServiceWorker2] Ble Remove Wifi P2p Group Callback ID doesn't match: "+bleCallback.getID());
						}						
					} else {
						Log.e(Common.TAG, "[GCServiceWorker2] Ble Callback List is empty");
					}
					break;
					
				case IGcConnectivityService.CB_GET_BLE_FW_VERSION_RESULT:
					result = (Result) bundle.getSerializable(IGcConnectivityService.PARAM_RESULT);
					if(result.equals(Result.RESULT_SUCCESS)) {
						String version = bundle.getString(IGcConnectivityService.PARAM_BLE_FW_VERSION);
						Log.i(Common.TAG, "[GCServiceWorker2] BLE Get Firmware Version Result= "+result+", Version="+version);						
						mBleVersion = version;						
					} else {
						Log.i(Common.TAG, "[GCServiceWorker2] BLE Get Firmware Version Result= "+result);
					}
					break;
					
				case IGcConnectivityService.CB_GET_ALL_FW_VERSION_RESULT:
					result = (Result) bundle.getSerializable(IGcConnectivityService.PARAM_RESULT);
					Log.i(Common.TAG, "[GCServiceWorker2] BLE Get All Firmware Version Result= "+result);
					if(result.equals(Result.RESULT_SUCCESS)) {
						mA12Version = bundle.getString(IGcConnectivityService.PARAM_A12_FW_VERSION);
						Log.i(Common.TAG, "[GCServiceWorker2] BLE Get A12 Code Version = " + mA12Version);		
						mModemVersion = bundle.getString(IGcConnectivityService.PARAM_MOD_FW_VERSION);
						Log.i(Common.TAG, "[GCServiceWorker2] BLE Get Modem Code Version = " + mModemVersion);
						mMcuVersion2 = bundle.getString(IGcConnectivityService.PARAM_MCU_FW_VERSION);
						Log.i(Common.TAG, "[GCServiceWorker2] BLE Get MCU Version = " + mMcuVersion2);
					}
					break;
				case IGcConnectivityService.CB_VERIFY_PASSWORD_RESULT:
					result = (Result) bundle.getSerializable(IGcConnectivityService.PARAM_RESULT);
					if(result.equals(Result.RESULT_SUCCESS)) {
						VerifyPasswordStatus status = (VerifyPasswordStatus) bundle.getSerializable(IGcConnectivityService.PARAM_VERIFY_PASSWORD_STATUS);
						Log.i(Common.TAG, "[GCServiceWorker2] BLE Verify Password Result= "+result+", Status="+status);
						resetBleStartConnectTimestampAndCounter();
						
						switch(status) {
						case VPSTATUS_NOT_CHANGED_AND_CORRECT:
							setBleStatus(Status.Connected);
							break;
						case VPSTATUS_NOT_CHANGED_AND_INCORRECT:
							setBleStatus(Status.Connected);
							break;
						case VPSTATUS_CHANGED_AND_CORRECT:
							setBleStatus(Status.Verified);
							BleInitProcess();
							decideNextConnectionMove();
							break;
						case VPSTATUS_CHANGED_AND_INCORRECT:
							setBleStatus(Status.Connected);
							break;
						}						
					} else {
						Log.i(Common.TAG, "[GCServiceWorker2] BLE Verify Password Result= "+result);
					}
					break;
				case IGcConnectivityService.CB_LONG_TERM_EVENT_RESULT:
					LongTermEvent event = (LongTermEvent) bundle.getSerializable(IGcConnectivityService.PARAM_LONG_TERM_EVENT);
					BluetoothDevice device = (BluetoothDevice) bundle.getParcelable(IGcConnectivityService.PARAM_BLUETOOTH_DEVICE);
					Log.i(Common.TAG, "[GCServiceWorker2] BLE Long Term Event Event="+event+", Device="+device);
					switch(event) {
					case LTEVENT_DISCONNECTED_FROM_GATT_SERVER:
						Status bleStatus = getBleStatus();
						if(bleStatus.equals(Status.Connected) || bleStatus.equals(Status.Verifying) || bleStatus.equals(Status.Verified)) {
							Log.i(Common.TAG, "[GCServiceWorker2] LTEVENT_DISCONNECTED_FROM_GATT_SERVER");
							setBleStatus(Status.Error);
							setLastConnectionError("LTEVENT_DISCONNECTED_FROM_GATT_SERVER", BLE_ERROR_RECOVERY_ACTION, ConnectionErrorCode.BLE_DISCONNECT_FROM_GATT_SERVER.getVal());
							decideNextConnectionMove();							
						} else {
							Log.w(Common.TAG, "[GCServiceWorker2] ignore LTEVENT_DISCONNECTED_FROM_GATT_SERVER, because current BLE status= "+bleStatus);
						}
						break;
					case LTEVENT_WIFI_DISCONNECTED:
						Status wifiStatus = getWifiStatus();
						if(wifiStatus.equals(Status.Connected)) {
							// only handle this event if wifi is connected
							// if error occurs in connecting or disconnecting should handle by command fail response
							setWifiStatus(Status.Error);
							setLastConnectionError("[GCServiceWorker2] LTEVENT_WIFI_DISCONNECTED", NetworkHelper.getWifiErrorRecoveryAction(ConnectionErrorCode.WIFI_UNEXPECTED_DISCONNECT.getVal()), ConnectionErrorCode.WIFI_UNEXPECTED_DISCONNECT.getVal());
							decideNextConnectionMove();							
						} else {
							Log.w(Common.TAG, "[GCServiceWorker2] ignore LTEVENT_WIFI_DISCONNECTED, because current WIFI status= "+wifiStatus);
						}
						break;
					default: // default is acceptable
						break;
					}
					break;
				case IGcConnectivityService.CB_PERFORMANCE_RESULT:
					Log.i(Common.TAG, "[GCServiceWorker2] BLE Task "+bundle.getString(IGcConnectivityService.PARAM_TASK_NAME)+" cost "+bundle.getLong(IGcConnectivityService.PARAM_TIME_COST_MS)+" ms");
					break;
					
				case IGcConnectivityService.CB_CHANGE_PASSWORD_RESULT:
					result = (Result) bundle.getSerializable(IGcConnectivityService.PARAM_RESULT);
					Log.i(Common.TAG, "[GCServiceWorker2] Change Password Result= "+result);
					
					bleCallback = popBleCallbackFromQueue();
					if(bleCallback != null) {
						if(bleCallback.getID() == IGcConnectivityService.CB_CHANGE_PASSWORD_RESULT) {
							OperationCallback operationCallback = (OperationCallback) bleCallback.getCallback();
							if(result.equals(Result.RESULT_SUCCESS)) {
								operationCallback.done(bleCallback.getThat());
							} else {
								operationCallback.error(new BleCommandException());
							}
						} else {
							Log.e(Common.TAG, "[GCServiceWorker2] Ble Change Password Callback ID doesn't match: "+bleCallback.getID());
						}						
					} else {
						Log.e(Common.TAG, "[GCServiceWorker2] Ble Callback List is empty");
					}
					
					if(result.equals(Result.RESULT_SUCCESS)) {
						if(getBleStatus().equals(Status.Connected)) {
							Log.i(Common.TAG, "[GCServiceWorker2] Change Password and Verify again");
							decideNextConnectionMove();
						}
					}
					break;
				
				case IGcConnectivityService.CB_SET_DATE_TIME_RESULT:
					bleCallback = popBleCallbackFromQueue();
					if(bleCallback != null) {
						if(bleCallback.getID() == IGcConnectivityService.CB_SET_DATE_TIME_RESULT) {
							OperationCallback operationCallback = (OperationCallback) bleCallback.getCallback();
							result = (Result) bundle.getSerializable(IGcConnectivityService.PARAM_RESULT);
							if(result.equals(Result.RESULT_SUCCESS)) {
								operationCallback.done(bleCallback.getThat());
							} else {
								operationCallback.error(new BleCommandException());
							}
						} else {
							Log.e(Common.TAG, "[GCServiceWorker2] Ble Set Date Time Callback ID doesn't match: "+bleCallback.getID());
						}						
					} else {
						Log.e(Common.TAG, "[GCServiceWorker2] Ble Callback List is empty");
					}
					break;
					
				case IGcConnectivityService.CB_GET_SIM_HW_STATUS_RESULT:
					bleCallback = popBleCallbackFromQueue();
					if(bleCallback != null) {
						if (bleCallback.getID() == IGcConnectivityService.CB_GET_SIM_HW_STATUS_RESULT) {
							GetSimHwStatusCallback getSimHwStatusCallback = (GetSimHwStatusCallback) bleCallback.getCallback();
							result = (Result) bundle.getSerializable(IGcConnectivityService.PARAM_RESULT);
							if (result.equals(Result.RESULT_SUCCESS)) {
								try {
									IGcConnectivityService.SimHwStatus sourceSimHwStatus = (IGcConnectivityService.SimHwStatus) bundle.getSerializable(IGcConnectivityService.PARAM_SIM_HW_STATUS);
									IDeviceController.SimHwStatus simHwStatus = IDeviceController.SimHwStatus.getKey(sourceSimHwStatus.getStatus());
									getSimHwStatusCallback.result((IDeviceController)bleCallback.getThat(), simHwStatus);
								} catch (NoImpException e) {
									Log.i(Common.TAG, "[GCServiceWorker2] Ble Get Sim Hw Status cannot get sim hw status");
									getSimHwStatusCallback.error(e);
								}
							} else {
								getSimHwStatusCallback.error(new BleCommandException());
							}
						} else {
							Log.e(Common.TAG, "[GCServiceWorker2] Ble Get Sim Hw Status Callback ID doesn't match: "+bleCallback.getID());
						}
					} else {
						Log.e(Common.TAG, "[GCServiceWorker2] Ble Callback List is empty");
					}
					break;
					
				case IGcConnectivityService.CB_SET_CAMERA_MODE_RESULT:
					bleCallback = popBleCallbackFromQueue();
					if(bleCallback != null) {
						if (bleCallback.getID() == IGcConnectivityService.CB_SET_CAMERA_MODE_RESULT) {
							OperationCallback operationCallback = (OperationCallback) bleCallback.getCallback();
							result = (Result) bundle.getSerializable(IGcConnectivityService.PARAM_RESULT);
							if (result.equals(Result.RESULT_SUCCESS)) {
								operationCallback.done((IDeviceController)bleCallback.getThat());
							} else {
								operationCallback.error(new BleCommandException());
							}
						} else {
							Log.e(Common.TAG, "[GCServiceWorker2] Ble Set Camera Mode Callback ID doesn't match: "+bleCallback.getID());
						}
					} else {
						Log.e(Common.TAG, "[GCServiceWorker2] Ble Callback List is empty");
					}
					break;
					
				case IGcConnectivityService.CB_GET_CAMERA_MODE_RESULT:
					bleCallback = popBleCallbackFromQueue();
					if (bleCallback != null) {
						if (bleCallback.getID() == IGcConnectivityService.CB_GET_CAMERA_MODE_RESULT) {
							GetCameraModeCallback getCameraModeCallback = (GetCameraModeCallback) bleCallback.getCallback();
							result = (Result) bundle.getSerializable(IGcConnectivityService.PARAM_RESULT);
							if (result.equals(Result.RESULT_SUCCESS)) {
								try {
									IGcConnectivityService.CameraMode sourceCameraMode = (IGcConnectivityService.CameraMode) bundle.getSerializable(IGcConnectivityService.PARAM_CAMERA_MODE);
									IDeviceController.CameraMode cameraMode = IDeviceController.CameraMode.getKey(sourceCameraMode.getMode());
									getCameraModeCallback.result((IDeviceController)bleCallback.getThat(), cameraMode);
								} catch (NoImpException e) {
									Log.i(Common.TAG, "[GCServiceWorker2] Ble Get Camera Mode cannot get camera mode");
									getCameraModeCallback.error(e);
								}
							} else {
								getCameraModeCallback.error(new BleCommandException());
							}
						} else {
							Log.e(Common.TAG, "[GCServiceWorker2] Ble Get Camera Mode Callback ID doesn't match: "+bleCallback.getID());
						}
					} else {
						Log.e(Common.TAG, "[GCServiceWorker2] Ble Callback List is empty");
					}
					break;
					
				case IGcConnectivityService.CB_GET_LTE_CAMPING_STATUS_RESULT:
					bleCallback = popBleCallbackFromQueue();
					if (bleCallback != null) {
						if (bleCallback.getID() == IGcConnectivityService.CB_GET_LTE_CAMPING_STATUS_RESULT) {
							GetLTECampingStatusCallback getLTECampingStatusCallback = (GetLTECampingStatusCallback) bleCallback.getCallback();
							result = (Result) bundle.getSerializable(IGcConnectivityService.PARAM_RESULT);
							if (result.equals(Result.RESULT_SUCCESS)) {
								try {
									IGcConnectivityService.LTECampingStatus sourceLTECampingStatus = (IGcConnectivityService.LTECampingStatus) bundle.getSerializable(IGcConnectivityService.PARAM_LTE_CAMPING_STATUS);
									IDeviceController.LTECampingStatus lteCampingStatus = IDeviceController.LTECampingStatus.getKey(sourceLTECampingStatus.getStatus());
									getLTECampingStatusCallback.result((IDeviceController)bleCallback.getThat(), lteCampingStatus);
								} catch (NoImpException e) {
									Log.i(Common.TAG, "[GCServiceWorker2] Ble Get LTE Camping Status cannot get lte camping status");
									getLTECampingStatusCallback.error(e);
								}
							} else {
								getLTECampingStatusCallback.error(new BleCommandException());
							}
						} else {
							Log.e(Common.TAG, "[GCServiceWorker2] Ble Get LTE Camping Status Callback ID doesn't match: "+bleCallback.getID());
						}
					} else {
						Log.e(Common.TAG, "[GCServiceWorker2] Ble Callback List is empty");
					}
					break;
					
				case IGcConnectivityService.CB_GET_MODEM_STATUS_RESULT:
					bleCallback = popBleCallbackFromQueue();
					if (bleCallback != null) {
						if (bleCallback.getID() == IGcConnectivityService.CB_GET_MODEM_STATUS_RESULT) {
							GetSimInfoCallback getSimInfoCallback = (GetSimInfoCallback) bleCallback.getCallback();
							result = (Result) bundle.getSerializable(IGcConnectivityService.PARAM_RESULT);
							if (result.equals(Result.RESULT_SUCCESS)) {
								try {
									IGcConnectivityService.SimLockType sourceSimLockType = (IGcConnectivityService.SimLockType) bundle.getSerializable(IGcConnectivityService.PARAM_SIM_LOCK_TYPE);
									int pinRetryCount = bundle.getInt(IGcConnectivityService.PARAM_SIM_PIN_RETRY_COUNT);
									int pukRetryCount = bundle.getInt(IGcConnectivityService.PARAM_SIM_PUK_RETRY_COUNT);
									IDeviceController.SimLockType simLockType = IDeviceController.SimLockType.getKey(sourceSimLockType.getType());
									getSimInfoCallback.result((IDeviceController)bleCallback.getThat(), simLockType, pinRetryCount, pukRetryCount);
								} catch (NoImpException e) {
									Log.i(Common.TAG, "[GCServiceWorker] Ble Get Modem Status cannot get sim lock type");
									getSimInfoCallback.error(e);
								}
							} else {
								getSimInfoCallback.error(new BleCommandException());
							}
						} else {
							Log.e(Common.TAG, "[GCServiceWorker2] Ble Get Modem Status Callback ID doesn't match: "+bleCallback.getID());
						}
					} else {
						Log.e(Common.TAG, "[GCServiceWorker2] Ble Callback List is empty");
					}
					break;
					
				case IGcConnectivityService.CB_UNLOCK_SIM_PIN_RESULT:
					bleCallback = popBleCallbackFromQueue();
					if (bleCallback != null) {
						if (bleCallback.getID() == IGcConnectivityService.CB_UNLOCK_SIM_PIN_RESULT) {
							UnlockSimPinCallback unlockSimPinCallback = (UnlockSimPinCallback) bleCallback.getCallback();
							result = (Result) bundle.getSerializable(IGcConnectivityService.PARAM_RESULT);
							if (result.equals(Result.RESULT_SUCCESS)) {
								boolean unlockResult = bundle.getBoolean(IGcConnectivityService.PARAM_SIM_UNLOCK_PIN_RESULT);
								int pinRetryCount = bundle.getInt(IGcConnectivityService.PARAM_SIM_PIN_RETRY_COUNT);
								unlockSimPinCallback.result((IDeviceController)bleCallback.getThat(), unlockResult, pinRetryCount);
							} else {
								unlockSimPinCallback.error(new BleCommandException());
							}
						} else {
							Log.e(Common.TAG, "[GCServiceWorker2] Ble Disable Sim Pin Callback ID doesn't match: "+bleCallback.getID());
						}
					} else {
						Log.e(Common.TAG, "[GCServiceWorker2] Ble Callback List is empty");
					}
					break;
					
				case IGcConnectivityService.CB_SET_OPERATION_RESULT:
					bleCallback = popBleCallbackFromQueue();
					if(bleCallback != null) {
						if(bleCallback.getID() == IGcConnectivityService.CB_SET_OPERATION_RESULT) {
							ErrorCallback callback = bleCallback.getCallback();
							result = (Result) bundle.getSerializable(IGcConnectivityService.PARAM_RESULT);
							Log.i(Common.TAG, "[GCServiceWorker2] Operation Ble Callback: "+mMsg.what+", Result: "+result);
							int errorCode = bundle.getInt(IGcConnectivityService.PARAM_OPERATION_ERROR_CODE);
							if(callback instanceof OperationCallback) {
								OperationCallback operationCallback = (OperationCallback) callback;
								if(result.equals(Result.RESULT_SUCCESS)) {
									if(errorCode == Common.ErrorCode.ERR_SUCCESS.getVal()) {
										operationCallback.done(bleCallback.getThat());	
									} else {
										Log.w(Common.TAG, "[" + mMsg.what + "] Operation fail error");
										operationCallback.error(new CommonException("Operation fail", Common.ErrorCode.getKey(errorCode)));											
									}
									
								} else {
									Log.w(Common.TAG, "[" + mMsg.what + "] Operation fail error");
									operationCallback.error(new BleCommandException());
								}										
							} else if(callback instanceof IDeviceController.StatusCallback) {
								IDeviceController.StatusCallback statusCallback = (IDeviceController.StatusCallback) callback;
								if(result.equals(Result.RESULT_SUCCESS)) {
									if(errorCode == Common.ErrorCode.ERR_SUCCESS.getVal()) {
										try {
											statusCallback.result((IDeviceController)bleCallback.getThat(), IDeviceController.DRStatus.getKey((byte)bundle.getInt(IGcConnectivityService.PARAM_DR_STATUS)), 0);
										} catch (NoImpException e) {
											e.printStackTrace();
											statusCallback.error(e);
										}											
									} else {
										Log.w(Common.TAG, "[" + mMsg.what + "] Operation fail error");
										statusCallback.error(new CommonException("Operation fail", Common.ErrorCode.getKey(errorCode)));											
									}
								} else {
									Log.w(Common.TAG, "[" + mMsg.what + "] Operation fail error");
									statusCallback.error(new BleCommandException());
								}
							} else if(callback instanceof IDeviceController.SpaceInfoCallback) {
								IDeviceController.SpaceInfoCallback spaceInfoCallback = (IDeviceController.SpaceInfoCallback) callback;
								if(result.equals(Result.RESULT_SUCCESS)) {
									if(errorCode == Common.ErrorCode.ERR_SUCCESS.getVal()) {
										spaceInfoCallback.result((IDeviceController)bleCallback.getThat(), new HashMap<IMediaItem.Type, Integer>(), bundle.getLong(IGcConnectivityService.PARAM_FREE_SPACE), bundle.getLong(IGcConnectivityService.PARAM_TOTAL_SPACE));
									} else {
										Log.w(Common.TAG, "[" + mMsg.what + "] Operation fail error");
										spaceInfoCallback.error(new CommonException("Operation fail", Common.ErrorCode.getKey(errorCode)));											
									}
								} else {
									Log.w(Common.TAG, "[" + mMsg.what + "] Operation fail error");
									spaceInfoCallback.error(new BleCommandException());	
								}
							} else {
								Log.e(Common.TAG, "[GCServiceWorker2] Ble Operation Callback ID doesn't match: "+mMsg.what);
							}
							
						}
					} else {
						Log.e(Common.TAG, "[GCServiceWorker2] Ble Callback List is empty");
					}
				break;
				case IGcConnectivityService.CB_GET_BROADCAST_SETTING_RESULT:
					bleCallback = popBleCallbackFromQueue();
					if (bleCallback != null) {
						if (bleCallback.getID() == mMsg.what) {
							BroadcastEnableSettingCallback broadcastEnableSettingCallback = (BroadcastEnableSettingCallback) bleCallback.getCallback();
							result = (Result) bundle.getSerializable(IGcConnectivityService.PARAM_RESULT);
							if (result.equals(Result.RESULT_SUCCESS)) {
								try {
									IGcConnectivityService.BroadcastSetting sourceBroadcastSetting = (IGcConnectivityService.BroadcastSetting) bundle.getSerializable(IGcConnectivityService.PARAM_BROADCAST_SETTING);
									IVideoRecorder.BroadcastEnableSetting broadcaseEnableSetting = IVideoRecorder.BroadcastEnableSetting.getKey(sourceBroadcastSetting.getSetting());
									broadcastEnableSettingCallback.result((IVideoRecorder) bleCallback.getThat(), broadcaseEnableSetting);
								} catch (NoImpException e) {
									Log.i(Common.TAG, "[GCServiceWorker2] Get Broadcast Setting cannot get broadcast enable setting");
									broadcastEnableSettingCallback.error(e);
								}
							} else {
								int errorCode = bundle.getInt(IGcConnectivityService.PARAM_BROADCAST_ERROR_CODE);
								Log.i(Common.TAG, "[GCServiceWorker2] Get Broadcast Setting Result= "+result+", Error code= 0x"+Integer.toHexString(errorCode));
								broadcastEnableSettingCallback.error(new BleCommandException());
							}
						} else {
							Log.e(Common.TAG, "[GCServiceWorker2] Get Broadcast Setting Callback ID doesn't match: "+mMsg.what);
						}
					} else {
						Log.e(Common.TAG, "[GCServiceWorker2] Ble Callback List is empty");
					}
					break;
				case IGcConnectivityService.CB_SET_BROADCAST_SETTING_RESULT:
					bleCallback = popBleCallbackFromQueue();
					if (bleCallback != null) {
						if (bleCallback.getID() == mMsg.what) {
							OperationCallback operationCallback = (OperationCallback) bleCallback.getCallback();
							result = (Result) bundle.getSerializable(IGcConnectivityService.PARAM_RESULT);
							if(result.equals(Result.RESULT_SUCCESS)) {
								operationCallback.done(bleCallback.getThat());
							} else {
								int errorCode = bundle.getInt(IGcConnectivityService.PARAM_BROADCAST_ERROR_CODE);
								Log.i(Common.TAG, "[GCServiceWorker2] Set Broadcast Setting Result= "+result+", Error code= 0x"+Integer.toHexString(errorCode));
								operationCallback.error(new BleCommandException());
							}
						} else {
							Log.e(Common.TAG, "[GCServiceWorker2] Set Broadcast Setting Callback ID doesn't match: "+mMsg.what);
						}
					} else {
						Log.e(Common.TAG, "[GCServiceWorker2] Ble Callback List is empty");
					}
					break;
				case IGcConnectivityService.CB_SET_BROADCAST_PLATFORM_RESULT:
					bleCallback = popBleCallbackFromQueue();
					if (bleCallback != null) {
						if (bleCallback.getID() == mMsg.what) {
							OperationCallback operationCallback = (OperationCallback) bleCallback.getCallback();
							result = (Result) bundle.getSerializable(IGcConnectivityService.PARAM_RESULT);
							if(result.equals(Result.RESULT_SUCCESS)) {
								operationCallback.done(bleCallback.getThat());
							} else {
								int errorCode = bundle.getInt(IGcConnectivityService.PARAM_BROADCAST_ERROR_CODE);
								Log.i(Common.TAG, "[GCServiceWorker2] Set Broadcast Platform Result= "+result+", Error code= 0x"+Integer.toHexString(errorCode));
								operationCallback.error(new BleCommandException());
							}
						} else {
							Log.e(Common.TAG, "[GCServiceWorker2] Set Broadcast Platform Callback ID doesn't match: "+mMsg.what);
						}
					} else {
						Log.e(Common.TAG, "[GCServiceWorker2] Ble Callback List is empty");
					}
					break;
				case IGcConnectivityService.CB_SET_BROADCAST_INVITATION_LIST_RESULT:
					bleCallback = popBleCallbackFromQueue();
					if (bleCallback != null) {
						if (bleCallback.getID() == mMsg.what) {
							OperationCallback operationCallback = (OperationCallback) bleCallback.getCallback();
							result = (Result) bundle.getSerializable(IGcConnectivityService.PARAM_RESULT);
							if(result.equals(Result.RESULT_SUCCESS)) {
								operationCallback.done(bleCallback.getThat());
							} else {
								int errorCode = bundle.getInt(IGcConnectivityService.PARAM_BROADCAST_ERROR_CODE);
								Log.i(Common.TAG, "[GCServiceWorker2] Set Broadcast Invitation List Result= "+result+", Error code= 0x"+Integer.toHexString(errorCode));
								operationCallback.error(new BleCommandException());
							}
						} else {
							Log.e(Common.TAG, "[GCServiceWorker2] Set Broadcast Invitation List Callback ID doesn't match: "+mMsg.what);
						}
					} else {
						Log.e(Common.TAG, "[GCServiceWorker2] Ble Callback List is empty");
					}
					break;
				case IGcConnectivityService.CB_SET_BROADCAST_PRIVACY_RESULT:
					bleCallback = popBleCallbackFromQueue();
					if (bleCallback != null) {
						if (bleCallback.getID() == mMsg.what) {
							OperationCallback operationCallback = (OperationCallback) bleCallback.getCallback();
							result = (Result) bundle.getSerializable(IGcConnectivityService.PARAM_RESULT);
							if(result.equals(Result.RESULT_SUCCESS)) {
								operationCallback.done(bleCallback.getThat());
							} else {
								int errorCode = bundle.getInt(IGcConnectivityService.PARAM_BROADCAST_ERROR_CODE);
								Log.i(Common.TAG, "[GCServiceWorker2] Set Broadcast Privacy Result= "+result+", Error code= 0x"+Integer.toHexString(errorCode));
								operationCallback.error(new BleCommandException());
							}
						} else {
							Log.e(Common.TAG, "[GCServiceWorker2] Set Broadcast Privacy Callback ID doesn't match: "+mMsg.what);
						}
					} else {
						Log.e(Common.TAG, "[GCServiceWorker2] Ble Callback List is empty");
					}
					break;
				case IGcConnectivityService.CB_GET_BROADCAST_STATUS_RESULT:
					bleCallback = popBleCallbackFromQueue();
					if (bleCallback != null) {
						if (bleCallback.getID() == mMsg.what) {
							BroadcastStatusCallback broadcastStatusCallback = (BroadcastStatusCallback) bleCallback.getCallback();
							result = (Result) bundle.getSerializable(IGcConnectivityService.PARAM_RESULT);
							if(result.equals(Result.RESULT_SUCCESS)) {
								try {
									IGcConnectivityService.BroadcastStatus sourceBroadcastStatus = (IGcConnectivityService.BroadcastStatus) bundle.getSerializable(IGcConnectivityService.PARAM_BROADCAST_STATUS);
									IVideoRecorder.BroadcastStatus broadcastStatus = IVideoRecorder.BroadcastStatus.getKey(sourceBroadcastStatus.getStatus());
									broadcastStatusCallback.result((IVideoRecorder) bleCallback.getThat(), broadcastStatus);
								} catch (NoImpException e) {
									Log.i(Common.TAG, "[GCServiceWorker2] Get Broadcast Status cannot get broadcast status");
									broadcastStatusCallback.error(e);
								}
							} else {
								int errorCode = bundle.getInt(IGcConnectivityService.PARAM_BROADCAST_ERROR_CODE);
								Log.i(Common.TAG, "[GCServiceWorker2] Get Broadcast Status Result= "+result+", Error code= 0x"+Integer.toHexString(errorCode));
								broadcastStatusCallback.error(new BleCommandException());
							}
						} else {
							Log.e(Common.TAG, "[GCServiceWorker2] Get Broadcast Status Callback ID doesn't match: "+mMsg.what);
						}
					} else {
						Log.e(Common.TAG, "[GCServiceWorker2] Ble Callback List is empty");
					}
					break;
				case IGcConnectivityService.CB_GET_BROADCAST_INVITATION_LIST_RESULT:
					bleCallback = popBleCallbackFromQueue();
					if (bleCallback != null) {
						if (bleCallback.getID() == mMsg.what) {
							BroadcastInvitationListCallback broadcastInvitationListCallback = (BroadcastInvitationListCallback) bleCallback.getCallback();
							result = (Result) bundle.getSerializable(IGcConnectivityService.PARAM_RESULT);
							if(result.equals(Result.RESULT_SUCCESS)) {
								ArrayList<String> invitationList = bundle.getStringArrayList(IGcConnectivityService.PARAM_BROADCAST_INVITATION_LIST);
								broadcastInvitationListCallback.result((IVideoRecorder) bleCallback.getThat(), invitationList);
							} else {
								int errorCode = bundle.getInt(IGcConnectivityService.PARAM_BROADCAST_ERROR_CODE);
								Log.i(Common.TAG, "[GCServiceWorker2] Get Broadcast Invitation List Result= "+result+", Error code= 0x"+Integer.toHexString(errorCode));
								broadcastInvitationListCallback.error(new BleCommandException());
							}
						} else {
							Log.e(Common.TAG, "[GCServiceWorker2] Get Broadcast Invitation List Callback ID doesn't match: "+mMsg.what);
						}
					} else {
						Log.e(Common.TAG, "[GCServiceWorker2] Ble Callback List is empty");
					}
					break;
				case IGcConnectivityService.CB_GET_BROADCAST_PRIVACY_RESULT:
					bleCallback = popBleCallbackFromQueue();
					if (bleCallback != null) {
						if (bleCallback.getID() == mMsg.what) {
							BroadcastPrivacyCallback broadcastInvitationListCallback = (BroadcastPrivacyCallback) bleCallback.getCallback();
							result = (Result) bundle.getSerializable(IGcConnectivityService.PARAM_RESULT);
							if(result.equals(Result.RESULT_SUCCESS)) {
								try {
									IGcConnectivityService.BroadcastPrivacy sourceBroadcastPrivacy = (IGcConnectivityService.BroadcastPrivacy) bundle.getSerializable(IGcConnectivityService.PARAM_BROADCAST_PRIVACY);
									IVideoRecorder.BroadcastPrivacy broadcastPrivacy = IVideoRecorder.BroadcastPrivacy.getKey(sourceBroadcastPrivacy.getPrivacy());
									broadcastInvitationListCallback.result((IVideoRecorder) bleCallback.getThat(), broadcastPrivacy);
								} catch (NoImpException e) {
									Log.i(Common.TAG, "[GCServiceWorker2] Get Broadcast Privacy cannot get broadcast privacy");
									broadcastInvitationListCallback.error(e);
								}
							} else {
								int errorCode = bundle.getInt(IGcConnectivityService.PARAM_BROADCAST_ERROR_CODE);
								Log.i(Common.TAG, "[GCServiceWorker2] Get Broadcast Privacy Result= "+result+", Error code= 0x"+Integer.toHexString(errorCode));
								broadcastInvitationListCallback.error(new BleCommandException());
							}
						} else {
							Log.e(Common.TAG, "[GCServiceWorker2] Get Broadcast Privacy Callback ID doesn't match: "+mMsg.what);
						}
					} else {
						Log.e(Common.TAG, "[GCServiceWorker2] Ble Callback List is empty");
					}
					break;
				case IGcConnectivityService.CB_GET_BROADCAST_PLATFORM_RESULT:
					bleCallback = popBleCallbackFromQueue();
					if (bleCallback != null) {
						if (bleCallback.getID() == mMsg.what) {
							BroadcastPlatformCallback broadcastPlatformCallback = (BroadcastPlatformCallback) bleCallback.getCallback();
							result = (Result) bundle.getSerializable(IGcConnectivityService.PARAM_RESULT);
							if(result.equals(Result.RESULT_SUCCESS)) {
								try {
									IGcConnectivityService.BroadcastPlatform sourceBroadcastPlatform = (IGcConnectivityService.BroadcastPlatform) bundle.getSerializable(IGcConnectivityService.PARAM_BROADCAST_PLATFORM);
									IVideoRecorder.BroadcastPlatform broadcastPlatform = IVideoRecorder.BroadcastPlatform.getKey(sourceBroadcastPlatform.getPlatform());
									broadcastPlatformCallback.result((IVideoRecorder) bleCallback.getThat(), broadcastPlatform);
								} catch (NoImpException e) {
									Log.i(Common.TAG, "[GCServiceWorker2] Get Broadcast Platform cannot get broadcast platform");
									broadcastPlatformCallback.error(e);
								}
							} else {
								int errorCode = bundle.getInt(IGcConnectivityService.PARAM_BROADCAST_ERROR_CODE);
								Log.i(Common.TAG, "[GCServiceWorker2] Get Broadcast Platform Result= "+result+", Error code= 0x"+Integer.toHexString(errorCode));
								broadcastPlatformCallback.error(new BleCommandException());
							}
						} else {
							Log.e(Common.TAG, "[GCServiceWorker2] Get Broadcast Platform Callback ID doesn't match: "+mMsg.what);
						}
					} else {
						Log.e(Common.TAG, "[GCServiceWorker2] Ble Callback List is empty");
					}
					break;
				case IGcConnectivityService.CB_GET_BROADCAST_VIDEO_URL_RESULT:
					bleCallback = popBleCallbackFromQueue();
					if (bleCallback != null) {
						if (bleCallback.getID() == mMsg.what) {
							BroadcastVideoUrlCallback broadcastVideoUrlCallback = (BroadcastVideoUrlCallback) bleCallback.getCallback();
							result = (Result) bundle.getSerializable(IGcConnectivityService.PARAM_RESULT);
							if(result.equals(Result.RESULT_SUCCESS)) {
								String videoUrl = bundle.getString(IGcConnectivityService.PARAM_BROADCAST_VIDEO_URL);
								broadcastVideoUrlCallback.result((IVideoRecorder) bleCallback.getThat(), videoUrl);
							} else {
								int errorCode = bundle.getInt(IGcConnectivityService.PARAM_BROADCAST_ERROR_CODE);
								Log.i(Common.TAG, "[GCServiceWorker2] Get Broadcast Video Url Result= "+result+", Error code= 0x"+Integer.toHexString(errorCode));
								broadcastVideoUrlCallback.error(new BleCommandException());
							}
						} else {
							Log.e(Common.TAG, "[GCServiceWorker2] Get Broadcast Video Url Callback ID doesn't match: "+mMsg.what);
						}
					} else {
						Log.e(Common.TAG, "[GCServiceWorker2] Ble Callback List is empty");
					}
					break;
				case IGcConnectivityService.CB_GET_BROADCAST_ERROR_LIST_RESULT:
					bleCallback = popBleCallbackFromQueue();
					if (bleCallback != null) {
						if (bleCallback.getID() == mMsg.what) {
							BroadcastErrorListCallback broadcastErrorListCallback = (BroadcastErrorListCallback) bleCallback.getCallback();
							result = (Result) bundle.getSerializable(IGcConnectivityService.PARAM_RESULT);
							if (result.equals(Result.RESULT_SUCCESS)) {
								byte[] sourceErrorList = bundle.getByteArray(IGcConnectivityService.PARAM_BROADCAST_ERROR_LIST);
								if (sourceErrorList != null) {
									List<IVideoRecorder.BroadcastError> errorList = new ArrayList<IVideoRecorder.BroadcastError>();
									for (byte sourceError : sourceErrorList) {
										try {
											errorList.add(IVideoRecorder.BroadcastError.getKey(sourceError));
										} catch (NoImpException e) {
											Log.i(Common.TAG, "[GCServiceWorker2] Get Broadcast Error List cannot get broadcast error from " + sourceError);
											broadcastErrorListCallback.error(e);
										}
									}
									broadcastErrorListCallback.result((IVideoRecorder) bleCallback.getThat(), errorList);
								} else {
									Log.i(Common.TAG, "[GCServiceWorker2] Get Broadcast Error List no source error list");
									broadcastErrorListCallback.error(new BleCommandException());
								}
							} else {
								int errorCode = bundle.getInt(IGcConnectivityService.PARAM_BROADCAST_ERROR_CODE);
								Log.i(Common.TAG, "[GCServiceWorker2] Get Broadcast Error List Result= "+result+", Error code= 0x"+Integer.toHexString(errorCode));
								broadcastErrorListCallback.error(new BleCommandException());
							}
						} else {
							Log.e(Common.TAG, "[GCServiceWorker2] Get Broadcast Error List Callback ID doesn't match: "+mMsg.what);
						}
					} else {
						Log.e(Common.TAG, "[GCServiceWorker2] Ble Callback List is empty");
					}
					break;
				case IGcConnectivityService.CB_SET_BROADCAST_USER_NAME_RESULT:
					bleCallback = popBleCallbackFromQueue();
					if (bleCallback != null) {
						if (bleCallback.getID() == mMsg.what) {
							OperationCallback operationCallback = (OperationCallback) bleCallback.getCallback();
							result = (Result) bundle.getSerializable(IGcConnectivityService.PARAM_RESULT);
							if (result.equals(Result.RESULT_SUCCESS)) {
								operationCallback.done((IVideoRecorder) bleCallback.getThat());
							} else {
								int errorCode = bundle.getInt(IGcConnectivityService.PARAM_BROADCAST_ERROR_CODE);
								Log.i(Common.TAG, "[GCServiceWorker2] Set Broadcast User Name Result= "+result+", Error code= 0x"+Integer.toHexString(errorCode));
								operationCallback.error(new BleCommandException());
							}
						} else {
							Log.e(Common.TAG, "[GCServiceWorker2] Set Broadcast User Name Callback ID doesn't match: "+mMsg.what);
						}
					} else {
						Log.e(Common.TAG, "[GCServiceWorker2] Ble Callback List is empty");
					}
					break;
				case IGcConnectivityService.CB_SET_BROADCAST_SMS_CONTENT_RESULT:
					bleCallback = popBleCallbackFromQueue();
					if (bleCallback != null) {
						if (bleCallback.getID() == mMsg.what) {
							OperationCallback operationCallback = (OperationCallback) bleCallback.getCallback();
							result = (Result) bundle.getSerializable(IGcConnectivityService.PARAM_RESULT);
							if (result.equals(Result.RESULT_SUCCESS)) {
								operationCallback.done((IVideoRecorder) bleCallback.getThat());
							} else {
								int errorCode = bundle.getInt(IGcConnectivityService.PARAM_BROADCAST_ERROR_CODE);
								Log.i(Common.TAG, "[GCServiceWorker2] Set Broadcast SMS Content Result= "+result+", Error code= 0x"+Integer.toHexString(errorCode));
								operationCallback.error(new BleCommandException());
							}
						} else {
							Log.e(Common.TAG, "[GCServiceWorker2] Set Broadcast SMS Content Callback ID doesn't match: "+mMsg.what);
						}
					} else {
						Log.e(Common.TAG, "[GCServiceWorker2] Ble Callback List is empty");
					}
					break;
				case IGcConnectivityService.CB_GET_BROADCAST_USER_NAME_RESULT:
					bleCallback = popBleCallbackFromQueue();
					if (bleCallback != null) {
						if (bleCallback.getID() == mMsg.what) {
							BroadcastUserNameCallback broadcastUserNameCallback = (BroadcastUserNameCallback) bleCallback.getCallback();
							result = (Result) bundle.getSerializable(IGcConnectivityService.PARAM_RESULT);
							if (result.equals(Result.RESULT_SUCCESS)) {
								String userName = bundle.getString(IGcConnectivityService.PARAM_BROADCAST_USER_NAME);
								broadcastUserNameCallback.result((IVideoRecorder) bleCallback.getThat(), userName);
							} else {
								int errorCode = bundle.getInt(IGcConnectivityService.PARAM_BROADCAST_ERROR_CODE);
								Log.i(Common.TAG, "[GCServiceWorker2] Get Broadcast User Name Result= "+result+", Error code= 0x"+Integer.toHexString(errorCode));
								broadcastUserNameCallback.error(new BleCommandException());
							}
						} else {
							Log.e(Common.TAG, "[GCServiceWorker2] Get Broadcast User Name Callback ID doesn't match: "+mMsg.what);
						}
					} else {
						Log.e(Common.TAG, "[GCServiceWorker2] Ble Callback List is empty");
					}
					break;
				case IGcConnectivityService.CB_GET_BROADCAST_SMS_CONTENT_RESULT:
					bleCallback = popBleCallbackFromQueue();
					if (bleCallback != null) {
						if (bleCallback.getID() == mMsg.what) {
							BroadcastSMSContentCallback broadcastSMSContentCallback = (BroadcastSMSContentCallback) bleCallback.getCallback();
							result = (Result) bundle.getSerializable(IGcConnectivityService.PARAM_RESULT);
							if (result.equals(Result.RESULT_SUCCESS)) {
								String smsContent = bundle.getString(IGcConnectivityService.PARAM_BROADCAST_SMS_CONTENT);
								broadcastSMSContentCallback.result((IVideoRecorder) bleCallback.getThat(), smsContent);
							} else {
								int errorCode = bundle.getInt(IGcConnectivityService.PARAM_BROADCAST_ERROR_CODE);
								Log.i(Common.TAG, "[GCServiceWorker2] Get Broadcast SMS Content Result= "+result+", Error code= 0x"+Integer.toHexString(errorCode));
								broadcastSMSContentCallback.error(new BleCommandException());
							}
						} else {
							Log.e(Common.TAG, "[GCServiceWorker2] Get Broadcast SMS Content Callback ID doesn't match: "+mMsg.what);
						}
					} else {
						Log.e(Common.TAG, "[GCServiceWorker2] Ble Callback List is empty");
					}
					break;
				case IGcConnectivityService.CB_SET_GPS_INFO_RESULT:
					bleCallback = popBleCallbackFromQueue();
					if(bleCallback != null) {
						if(bleCallback.getID() == IGcConnectivityService.CB_SET_GPS_INFO_RESULT) {
							OperationCallback operationCallback = (OperationCallback) bleCallback.getCallback();
							result = (Result) bundle.getSerializable(IGcConnectivityService.PARAM_RESULT);
							if(result.equals(Result.RESULT_SUCCESS)) {
								operationCallback.done(bleCallback.getThat());
							} else {
								operationCallback.error(new BleCommandException());
							}
						} else {
							Log.e(Common.TAG, "[GCServiceWorker2] Ble Set Gps Info Callback ID doesn't match: "+bleCallback.getID());
						}						
					} else {
						Log.e(Common.TAG, "[GCServiceWorker2] Ble Callback List is empty");
					}
				break;
				case IGcConnectivityService.CB_SET_NAME_RESULT:
					bleCallback = popBleCallbackFromQueue();
					if(bleCallback != null) {
						if(bleCallback.getID() == IGcConnectivityService.CB_SET_NAME_RESULT) {
							OperationCallback operationCallback = (OperationCallback) bleCallback.getCallback();
							result = (Result) bundle.getSerializable(IGcConnectivityService.PARAM_RESULT);
							if(result.equals(Result.RESULT_SUCCESS)) {
								operationCallback.done(bleCallback.getThat());
							} else {
								operationCallback.error(new BleCommandException());
							}
						} else {
							Log.e(Common.TAG, "[GCServiceWorker2] Ble Set Name Callback ID doesn't match: "+bleCallback.getID());
						}						
					} else {
						Log.e(Common.TAG, "[GCServiceWorker2] Ble Callback List is empty");
					}
				break;
				case IGcConnectivityService.CB_GET_NAME_RESULT:
					bleCallback = popBleCallbackFromQueue();
					if(bleCallback != null) {
						if(bleCallback.getID() == IGcConnectivityService.CB_GET_NAME_RESULT) {
							IDeviceController.CameraNameCallback cameraNameCallback = (IDeviceController.CameraNameCallback) bleCallback.getCallback();
							result = (Result) bundle.getSerializable(IGcConnectivityService.PARAM_RESULT);
							if(result.equals(Result.RESULT_SUCCESS)) {
								String name = bundle.getString(IGcConnectivityService.PARAM_GC_NAME);
								if(name != null) {
									cameraNameCallback.result((IDeviceController)bleCallback.getThat(), name);	
								} else {
									cameraNameCallback.error(new NullPointerException("name is null"));
								}
								
							} else {
								Log.w(Common.TAG, "[" + mMsg.what + "] Operation fail error");
								cameraNameCallback.error(new BleCommandException());	
							}
						} else {
							Log.e(Common.TAG, "[GCServiceWorker2] Ble Get Name Callback ID doesn't match: "+bleCallback.getID());
						}						
					} else {
						Log.e(Common.TAG, "[GCServiceWorker2] Ble Callback List is empty");
					}
				break;
				case IGcConnectivityService.CB_SET_AUTO_BACKUP_AP_RESULT:
				case IGcConnectivityService.CB_CLR_AUTO_BACKUP_AP_RESULT:
				case IGcConnectivityService.CB_SET_AUTO_BACKUP_AP_SCAN_START_RESULT:
				case IGcConnectivityService.CB_SET_AUTO_BACKUP_AP_SCAN_STOP_RESULT:
				case IGcConnectivityService.CB_SET_AUTO_BACKUP_PROXY_RESULT:
					bleCallback = popBleCallbackFromQueue();
					if(bleCallback != null) {
						if(bleCallback.getID() == mMsg.what) {
							result = (Result) bundle.getSerializable(IGcConnectivityService.PARAM_RESULT);
							int errorCode = bundle.getInt(IGcConnectivityService.PARAM_AUTO_BACKUP_ERROR_CODE);
								OperationCallback operationCallback = (OperationCallback) bleCallback.getCallback();
								if(result.equals(Result.RESULT_SUCCESS)) {
									Common.ErrorCode error;
									if(mMsg.what == IGcConnectivityService.CB_SET_AUTO_BACKUP_AP_RESULT) {
										error = NetworkHelper.GC2WifiMgrErrorCode2ErrorCode(GC2WifiMgrErrorCode.getKey(errorCode));
									} else {
										error = Common.ErrorCode.getKey(errorCode);
									}
									
									if(error.equals(Common.ErrorCode.ERR_SUCCESS)) {
										operationCallback.done(bleCallback.getThat());	
									} else {
										Log.w(Common.TAG, "[" + mMsg.what + "] Operation fail error");
										operationCallback.error(new CommonException("Operation fail", error));											
									}
									
								} else {
									Log.w(Common.TAG, "[" + mMsg.what + "] Operation fail error");
									operationCallback.error(new BleCommandException());
								}								
							
						} else {
							Log.e(Common.TAG, "[GCServiceWorker2] Ble Auto Backup Callback ID doesn't match: "+bleCallback.getID()+", Expected: "+mMsg.what);
						}
					} else {
						Log.e(Common.TAG, "[GCServiceWorker2] Ble Callback List is empty");
					}
				break;
				case IGcConnectivityService.CB_GET_AUTO_BACKUP_PROXY_RESULT:
					bleCallback = popBleCallbackFromQueue();
					if(bleCallback != null) {
						if(bleCallback.getID() == mMsg.what) {
							result = (Result) bundle.getSerializable(IGcConnectivityService.PARAM_RESULT);
							int errorCode = bundle.getInt(IGcConnectivityService.PARAM_AUTO_BACKUP_ERROR_CODE);
							AutoBackupGetHttpProxyCallback autoBackupGetHttpProxyCallback = (AutoBackupGetHttpProxyCallback) bleCallback.getCallback();
								if(result.equals(Result.RESULT_SUCCESS)) {
									if(errorCode == Common.ErrorCode.ERR_SUCCESS.getVal()) {
										String proxy = bundle.getString(IGcConnectivityService.PARAM_AP_PROXY);
										int port = bundle.getInt(IGcConnectivityService.PARAM_AP_PORT);
										
										if(proxy != null) {
											autoBackupGetHttpProxyCallback.result((IAutoBackuper)bleCallback.getThat(), proxy, port);	
										} else {
											autoBackupGetHttpProxyCallback.error(new NullPointerException("proxy is null"));
										}
									} else {
										Log.w(Common.TAG, "[" + mMsg.what + "] Operation fail error");
										autoBackupGetHttpProxyCallback.error(new CommonException("Operation fail", Common.ErrorCode.getKey(errorCode)));											
									}
									
								} else {
									Log.w(Common.TAG, "[" + mMsg.what + "] Operation fail error");
									autoBackupGetHttpProxyCallback.error(new BleCommandException());
								}								
							
						} else {
							Log.e(Common.TAG, "[GCServiceWorker2] Ble Auto Backup Callback ID doesn't match: "+bleCallback.getID()+", Expected: "+mMsg.what);
						}
					} else {
						Log.e(Common.TAG, "[GCServiceWorker2] Ble Callback List is empty");
					}
				break;
				case IGcConnectivityService.CB_SET_AUTO_BACKUP_TOKEN_RESULT:
					bleCallback = popBleCallbackFromQueue();
					if(bleCallback != null) {
						if (bleCallback.getID() == mMsg.what) {
							result = (Result) bundle.getSerializable(IGcConnectivityService.PARAM_RESULT);
							OperationCallback operationCallback = (OperationCallback) bleCallback.getCallback();
							if (result.equals(Result.RESULT_SUCCESS)) {
								operationCallback.done(bleCallback.getThat());
							} else {
								int errorCode = bundle.getInt(IGcConnectivityService.PARAM_AUTO_BACKUP_ERROR_CODE);
								Log.i(Common.TAG, "[GCServiceWorker2] Set Token Result= "+result+", Error code= 0x"+Integer.toHexString(errorCode));
								operationCallback.error(new BleCommandException());
							}
						} else {
							Log.e(Common.TAG, "[GCServiceWorker2] Ble Set Token Callback ID doesn't match: "+bleCallback.getID()+", Expected: "+mMsg.what);
						}
					} else {
						Log.e(Common.TAG, "[GCServiceWorker2] Ble Callback List is empty");
					}
				break;
				case IGcConnectivityService.CB_SET_AUTO_BACKUP_PREFERENCE_RESULT:
					bleCallback = popBleCallbackFromQueue();
					if (bleCallback != null) {
						if (bleCallback.getID() == mMsg.what) {
							result = (Result) bundle.getSerializable(IGcConnectivityService.PARAM_RESULT);
							OperationCallback operationCallback = (OperationCallback) bleCallback.getCallback();
							if (result.equals(Result.RESULT_SUCCESS)) {
								operationCallback.done(bleCallback.getThat());
							} else {
								int errorCode = bundle.getInt(IGcConnectivityService.PARAM_AUTO_BACKUP_ERROR_CODE);
								Log.i(Common.TAG, "[GCServiceWorker2] Set Autobackup Preference Result= "+result+", Error code= 0x"+Integer.toHexString(errorCode));
								operationCallback.error(new BleCommandException());
							}
						} else {
							Log.e(Common.TAG, "[GCServiceWorker2] Ble Set Autobackup Preference Callback ID doesn't match: "+bleCallback.getID()+", Expected: "+mMsg.what);
						}
					} else {
						Log.e(Common.TAG, "[GCServiceWorker2] Ble Callback List is empty");
					}
				break;
				case IGcConnectivityService.CB_GET_AUTO_BACKUP_PREFERENCE_RESULT:
					bleCallback = popBleCallbackFromQueue();
					if (bleCallback != null) {
						if (bleCallback.getID() == mMsg.what) {
							result = (Result) bundle.getSerializable(IGcConnectivityService.PARAM_RESULT);
							AutoBackupPreferenceCallback autoBackupPreferenceCallback = (AutoBackupPreferenceCallback) bleCallback.getCallback();
							if (result.equals(Result.RESULT_SUCCESS)) {
								boolean isEnableAutoBackup = bundle.getBoolean(IGcConnectivityService.PARAM_AUTO_BACKUP_IS_ENABLE_BACKUP);
								boolean isDeleteAfterBackup = bundle.getBoolean(IGcConnectivityService.PARAM_AUTO_BACKUP_IS_DELETE_AFTER_BACKUP);
								boolean isBackupWithoutAC = bundle.getBoolean(IGcConnectivityService.PARAM_AUTO_BACKUP_IS_BACKUP_WITHOUT_AC);
								autoBackupPreferenceCallback.result(
										(IAutoBackuper) bleCallback.getThat(), 
										isEnableAutoBackup ? OptionCheck.CHECK_ON : OptionCheck.CHECK_OFF,
										isBackupWithoutAC ? OptionCheck.CHECK_OFF : OptionCheck.CHECK_ON,
										isDeleteAfterBackup ? OptionCheck.CHECK_ON : OptionCheck.CHECK_OFF);
							} else {
								int errorCode = bundle.getInt(IGcConnectivityService.PARAM_AUTO_BACKUP_ERROR_CODE);
								Log.i(Common.TAG, "[GCServiceWorker2] Get Autobackup Preference Result= "+result+", Error code= 0x"+Integer.toHexString(errorCode));
								autoBackupPreferenceCallback.error(new BleCommandException());
							}
						} else {
							Log.e(Common.TAG, "[GCServiceWorker2] Ble Get Autobackup Preference Callback ID doesn't match: "+bleCallback.getID()+", Expected: "+mMsg.what);
						}
					} else {
						Log.e(Common.TAG, "[GCServiceWorker2] Ble Callback List is empty");
					}
				break;
				case IGcConnectivityService.CB_GET_AUTO_BACKUP_IS_AVAILABLE_RESULT:
					bleCallback = popBleCallbackFromQueue();
					if (bleCallback != null) {
						if (bleCallback.getID() == mMsg.what) {
							result = (Result) bundle.getSerializable(IGcConnectivityService.PARAM_RESULT);
							OperationCallback operationCallback = (OperationCallback)bleCallback.getCallback();
							if (result.equals(Result.RESULT_SUCCESS)) {
								boolean isAvailable = bundle.getBoolean(IGcConnectivityService.PARAM_AUTO_BACKUP_IS_AVAILABLE);
								if (isAvailable) {
									operationCallback.done((IAutoBackuper)bleCallback.getThat());
								} else {
									operationCallback.error(new NoImpException());
								}
							} else {
								int errorCode = bundle.getInt(IGcConnectivityService.PARAM_AUTO_BACKUP_ERROR_CODE);
								Log.i(Common.TAG, "[GCServiceWorker2] Get Autobackup Is Available Result= "+result+", Error code= 0x"+Integer.toHexString(errorCode));
								operationCallback.error(new BleCommandException());
							}
						} else {
							Log.e(Common.TAG, "[GCServiceWorker2] Ble Get Autobackup Is Available Callback ID doesn't match: "+bleCallback.getID()+", Expected: "+mMsg.what);
						}
					} else {
						Log.e(Common.TAG, "[GCServiceWorker2] Ble Callback List is empty");
					}
				break;
				case IGcConnectivityService.CB_SET_AUTO_BACKUP_ACCOUNT_RESULT:
					bleCallback = popBleCallbackFromQueue();
					if (bleCallback != null) {
						if (bleCallback.getID() == mMsg.what) {
							result = (Result) bundle.getSerializable(IGcConnectivityService.PARAM_RESULT);
							OperationCallback operationCallback = (OperationCallback)bleCallback.getCallback();
							if (result.equals(Result.RESULT_SUCCESS)) {
								operationCallback.done(bleCallback.getThat());
							} else {
								int errorCode = bundle.getInt(IGcConnectivityService.PARAM_AUTO_BACKUP_ERROR_CODE);
								Log.i(Common.TAG, "[GCServiceWorker2] Set Autobackup Account Result= "+result+", Error code= 0x"+Integer.toHexString(errorCode));
								operationCallback.error(new BleCommandException());
							}
						} else {
							Log.e(Common.TAG, "[GCServiceWorker2] Ble Set Autobackup Account Callback ID doesn't match: "+bleCallback.getID()+", Expected: "+mMsg.what);
						}
					} else {
						Log.e(Common.TAG, "[GCServiceWorker2] ble Callback List is empty");
					}
				break;
				case IGcConnectivityService.CB_GET_AUTO_BACKUP_ACCOUNT_RESULT:
					bleCallback = popBleCallbackFromQueue();
					if (bleCallback != null) {
						if (bleCallback.getID() == mMsg.what) {
							result = (Result) bundle.getSerializable(IGcConnectivityService.PARAM_RESULT);
							AutoBackupGetAccountCallback autoBackupGetAccountCallback = (AutoBackupGetAccountCallback)bleCallback.getCallback();
							if (result.equals(Result.RESULT_SUCCESS)) {
								String account = bundle.getString(IGcConnectivityService.PARAM_AUTO_BACKUP_ACCOUNT);
								autoBackupGetAccountCallback.result((IAutoBackuper)bleCallback.getThat(), account);
							} else {
								int errorCode = bundle.getInt(IGcConnectivityService.PARAM_AUTO_BACKUP_ERROR_CODE);
								Log.i(Common.TAG, "[GCServiceWorker2] Get Autobackup Account Result= "+result+", Error code= 0x"+Integer.toHexString(errorCode));
								autoBackupGetAccountCallback.error(new BleCommandException());
							}
						} else {
							Log.e(Common.TAG, "[GCServiceWorker2] Ble Get Autobackup Account Callback ID doesn't match: "+bleCallback.getID()+", Expected: "+mMsg.what);
						}
					} else {
						Log.e(Common.TAG, "[GCServiceWorker2] ble Callback List is empty");
					}
				break;
				case IGcConnectivityService.CB_GET_HW_STATUS_RESULT:
					bleCallback = popBleCallbackFromQueue();
					if(bleCallback != null) {
						if(bleCallback.getID() == mMsg.what) {
							result = (Result) bundle.getSerializable(IGcConnectivityService.PARAM_RESULT);
							BatteryInfoCallback batteryInfoCallback = (BatteryInfoCallback) bleCallback.getCallback();
							if(result.equals(Result.RESULT_SUCCESS)) {
								IGcConnectivityService.MCUBatteryLevel batteryLevel = (IGcConnectivityService.MCUBatteryLevel) bundle.getSerializable(IGcConnectivityService.PARAM_BATTERY_LEVEL);
								PlugIO adapterPlugin = (PlugIO) bundle.getSerializable(IGcConnectivityService.PARAM_ADAPTER_PLUGIN);
								GCDeviceInfoManager.setCurrentBatteryLevel(batteryLevel);
								if(adapterPlugin != null && batteryLevel != null) {
									batteryInfoCallback.result((IDeviceController)bleCallback.getThat(), adapterPlugin.equals(PlugIO.PLUG_IN), GCDeviceController.getMappedBatteryLevel(batteryLevel));
								} else {
									batteryInfoCallback.error(new NullPointerException("adapterPlugin is null || batteryLevel is null"));
								}
								
							} else {
								Log.w(Common.TAG, "[" + mMsg.what + "] Operation fail error");
								batteryInfoCallback.error(new BleCommandException());
							}								
							
						} else {
							Log.e(Common.TAG, "[GCServiceWorker2] Ble Get Hardware Status Callback ID doesn't match: "+bleCallback.getID()+", Expected: "+mMsg.what);
						}
					} else {
						Log.e(Common.TAG, "[GCServiceWorker2] Ble Callback List is empty");
					}
				break;
				
			case IGcConnectivityService.CB_GET_AUTO_BACKUP_STATUS_RESULT:
				bleCallback = popBleCallbackFromQueue();
				if (bleCallback != null) {
					if(bleCallback.getID() == mMsg.what) {
						result = (Result) bundle.getSerializable(IGcConnectivityService.PARAM_RESULT);
						AutoBackupStatusCallback autoBackupStatusCallback = (AutoBackupStatusCallback)bleCallback.getCallback();
						if (result == Result.RESULT_SUCCESS) {
							int errorCode = bundle.getInt(IGcConnectivityService.PARAM_AUTO_BACKUP_ERROR_CODE);
							Log.i(Common.TAG, "[GCServiceWorker2] BLE Get Auto Backup Status Result=" + result + ", error code= 0x"+Integer.toHexString(errorCode));
							
							IGcConnectivityService.BackupProcessStatus processStatus = (IGcConnectivityService.BackupProcessStatus) bundle.getSerializable(IGcConnectivityService.PARAM_AUTO_BACKUP_PROCESS_STATUS);
							IGcConnectivityService.BackupProviderIdIndex providerIndex = (IGcConnectivityService.BackupProviderIdIndex) bundle.getSerializable(IGcConnectivityService.PARAM_AUTO_BACKUP_PROVIDER_INDEX);
							int unbackupItemCount = bundle.getInt(IGcConnectivityService.PARAM_AUTO_BACKUP_UNBACKUP_ITEM_NUMBER);
							int totalItemCount = bundle.getInt(IGcConnectivityService.PARAM_AUTO_BACKUP_TOTAL_ITEM_NUMBER);
							Calendar lastBackupDateTime = (Calendar) bundle.getSerializable(IGcConnectivityService.PARAM_AUTO_BACKUP_LAST_BACKUP_DATE_TIME);
							
							Log.i(Common.TAG,
									"[GCServiceWorker2] process status=" + processStatus
										+ ", providerIndex=" + providerIndex
										+ ", unbackup item count=" + unbackupItemCount
										+ ", total item count=" + totalItemCount
										+ ", last backup time=" + lastBackupDateTime);
							try {
								ProviderType providerType = ProviderType.getKey(providerIndex.getID());
								autoBackupStatusCallback.result(
										(IAutoBackuper)bleCallback.getThat(), 
										providerType, 
										unbackupItemCount, 
										lastBackupDateTime);
							} catch (NoImpException e) {
								Log.w(Common.TAG, "[GCServiceWorker2] unmatched provider, " + providerIndex);
								autoBackupStatusCallback.error(e);
							}
						} else {
							int errorCode = bundle.getInt(IGcConnectivityService.PARAM_AUTO_BACKUP_ERROR_CODE);
							Log.i(Common.TAG, "[GCServiceWorker2] BLE Get Auto Backup Status Result=" + result + ", error code= 0x"+Integer.toHexString(errorCode));
							autoBackupStatusCallback.error(new BleCommandException());
						}
					} else {
						Log.e(Common.TAG, "[GCServiceWorker2] Ble Get Auto Backup Status Callback ID doesn't match: "+bleCallback.getID()+", Expected: "+mMsg.what);
					}
				} else {
					Log.e(Common.TAG, "[GCServiceWorker2] Ble Callback List is empty");
				}
				break;
				
			case IGcConnectivityService.CB_SET_HW_STATUS_LTEVENT_RESULT:
			case IGcConnectivityService.CB_CLR_HW_STATUS_LTEVENT_RESULT:
			case IGcConnectivityService.CB_SET_OPERATION_LTEVENT_RESULT:
			case IGcConnectivityService.CB_CLR_OPERATION_LTEVENT_RESULT:
			case IGcConnectivityService.CB_SET_GPS_INFO_LTEVENT_RESULT:
			case IGcConnectivityService.CB_CLR_GPS_INFO_LTEVENT_RESULT:
			case IGcConnectivityService.CB_SET_METADATA_LTEVENT_RESULT:
			case IGcConnectivityService.CB_CLR_METADATA_LTEVENT_RESULT:
			case IGcConnectivityService.CB_SET_CAMERA_ERROR_LTEVENT_RESULT:
			case IGcConnectivityService.CB_CLR_CAMERA_ERROR_LTEVENT_RESULT:
			case IGcConnectivityService.CB_SET_AUTO_BACKUP_LTEVENT_RESULT:
			case IGcConnectivityService.CB_CLR_AUTO_BACKUP_LTEVENT_RESULT:
			case IGcConnectivityService.CB_SET_LTNOTIFY_RESULT:
			case IGcConnectivityService.CB_CLR_LTNOTIFY_RESULT:
			case IGcConnectivityService.CB_SET_GENERAL_PURPOSE_COMMAND_LTNOTIFY_RESULT:
			case IGcConnectivityService.CB_CLR_GENERAL_PURPOSE_COMMAND_LTNOTIFY_RESULT:
			case IGcConnectivityService.CB_SET_CAMERA_MODE_LTEVENT_RESULT:
			case IGcConnectivityService.CB_CLR_CAMERA_MODE_LTEVENT_RESULT:
			case IGcConnectivityService.CB_SET_LTE_CAMPING_STATUS_LTEVENT_RESULT:
			case IGcConnectivityService.CB_CLR_LTE_CAMPING_STATUS_LTEVENT_RESULT:
				result = (Result) bundle.getSerializable(IGcConnectivityService.PARAM_RESULT);
				Log.i(Common.TAG, "[GCServiceWorker2] BLE Get/Set LTEvent "+mMsg.what+", Result= "+result);
				break;
				
			case IGcConnectivityService.CB_TRIGGER_FWUPDATE_RESULT:
				bleCallback = popBleCallbackFromQueue();
				if (bleCallback != null) {
					if(bleCallback.getID() == mMsg.what) {
						result = (Result) bundle.getSerializable(IGcConnectivityService.PARAM_RESULT);
						TriggerFWUpdateResult triggerFwUpdateResult = (TriggerFWUpdateResult) bundle.getSerializable(IGcConnectivityService.PARAM_TRIGGER_FWUPDATE_RESULT);
						OperationCallback operationCallback = (OperationCallback) bleCallback.getCallback();
						Log.i(Common.TAG, "[GCServiceWorker2] Trigger FW Update " + result + ", result=" + triggerFwUpdateResult);
						if(result.equals(Result.RESULT_SUCCESS)) {
							operationCallback.done(bleCallback.getThat());
						} else {
							operationCallback.error(new BleCommandException());
						}
					} else {
						Log.e(Common.TAG, "[GCServiceWorker2] Ble Trigger FW Update Result Callback ID doesn't match: "+bleCallback.getID()+", Expected: "+mMsg.what);
					}
				} else {
					Log.e(Common.TAG, "[GCServiceWorker2] Ble Callback List is empty");
				}
				break;
				
			case IGcConnectivityService.CB_FWUPDATE_RESULT:
				result = (Result) bundle.getSerializable(IGcConnectivityService.PARAM_RESULT);
				Log.i(Common.TAG, "[GCServiceWorker2] BLE FW Update Result= "+result);
				break;
				
			default:
				Log.w(Common.TAG, "[GCServiceWorker2] Unknown BLE Callback: "+mMsg.what);
			}
			
			synchronized(mBleEventHandler) {
				ArrayList<IBleEventListener> listeners = mBleEventHandler.get(mMsg.what);
				if(listeners != null) {
					Log.i(Common.TAG, "[GCServiceWorker2] Ble event " + mMsg.what);

					for(IBleEventListener listener : listeners) {
						listener.event(mMsg.what, bundle);
					}
				}
			}
		}
	}
	
	private BleErrorCallback popBleCallbackFromQueue() {
		synchronized(mBleCallbacks) {
			if(!mBleCallbacks.isEmpty()) {
				return mBleCallbacks.pop();
			} else {
				return null;
			}
		}
	}

	private void BleInitProcess() {

		DeviceItem device = (DeviceItem) getTargetDevice();		
		Log.i(Common.TAG, "[GCServiceWorker2] Set LTNotify");
		mConn.gcSetLTNotify(device.getDevice());

		// test suggested by Marvin
		/*
		 * /// We have to get BLE FW version first because we will reference it
		 * for GC bootup workaround. Log.i(Common.TAG,
		 * "[GCServiceWorker2] Get Ble Version");
		 * mConn.gcGetBleFWVersion(device.getDevice());
		 */

		// moved to BleInitProcessAfterWifiIsConnected() per the request of Brian Liao 
		/*
		final Calendar calendar = Calendar.getInstance();
		try {
			getController().setDeviceTime(calendar, new OperationCallback() {

				@Override
				public void error(Exception e) {
					Log.e(Common.TAG, "[GCServiceWorker2] BLE Set Date Time fail, error= "+e.toString());		
				}

				@Override
				public void done(Object that) {
					Log.i(Common.TAG, "[GCServiceWorker2] BLE Set Date Time= "+calendar.getTime().toString());			
				}
				
			});
		} catch (Exception e) {
			e.printStackTrace();
			Log.e(Common.TAG, "[GCServiceWorker2] BLE Set Date Time fail");
		}

		Log.i(Common.TAG, "[GCServiceWorker2] Get All Version");
		mConn.gcGetAllFwVersion(device.getDevice());
		*/

		Log.i(Common.TAG, "[GCServiceWorker2] Set Operation LTEvent");
		mConn.gcSetOperationLTEvent(device.getDevice());
		Log.i(Common.TAG, "[GCServiceWorker2] Set GpsInfo LTEvent");
		mConn.gcSetGpsInfoLTEvent(device.getDevice());
		Log.i(Common.TAG, "[GCServiceWorker2] Set Metadata LTEvent");
		mConn.gcSetMetadataLTEvent(device.getDevice());
		Log.i(Common.TAG, "[GCServiceWorker2] Set CameraError LTEvent");
		mConn.gcSetCameraErrorLTEvent(device.getDevice());
		Log.i(Common.TAG, "[GCServiceWorker2] Set AutoBackup LTEvent");
		mConn.gcSetAutoBackupLTEvent(device.getDevice());
		Log.i(Common.TAG, "[GCServiceWorker2] Set Hardware status LTEvent");
		mConn.gcSetHwStatusLTEvent(device.getDevice());
		Log.i(Common.TAG, "[GCServiceWorker2] Set General purpose command LTEvent");
		mConn.gcSetGeneralPurposeCommandLTNotify(device.getDevice());
		Log.i(Common.TAG, "[GCServiceWorker2] Set Camera mode LTEvent");
		mConn.gcSetCameraModeLTEvent(device.getDevice());
		Log.i(Common.TAG, "[GCServiceWorker2] Set LTE Camping Status LTEvent");
		mConn.gcSetLTECampingStatusLTEvent(device.getDevice());
	}
	
	private void BleInitProcessAfterWifiIsConnected() {
		DeviceItem device = (DeviceItem) getTargetDevice();
		
		final Calendar calendar = Calendar.getInstance();
		try {
			getController().setDeviceTime(calendar, new OperationCallback() {

				@Override
				public void error(Exception e) {
					Log.e(Common.TAG, "[GCServiceWorker2] BLE Set Date Time fail, error= "+e.toString());		
				}

				@Override
				public void done(Object that) {
					Log.i(Common.TAG, "[GCServiceWorker2] BLE Set Date Time= "+calendar.getTime().toString());			
				}
				
			});
		} catch (Exception e) {
			e.printStackTrace();
			Log.e(Common.TAG, "[GCServiceWorker2] BLE Set Date Time fail");
		}

		Log.i(Common.TAG, "[GCServiceWorker2] Get All Version");
		mConn.gcGetAllFwVersion(device.getDevice());
	}
	
	private Handler createConnectionHandler() {
		return new Handler(Looper.getMainLooper()) {
			@Override
			public void handleMessage(Message msg) {
				mRunOnUiHandler.post(new ConnectionCallbackRunnable(msg));
			}
		};
	}
	
	private void checkIfNeedToRemoveWifiP2pGroupWhenWifiDisconnected() {
		Log.i(Common.TAG,"checkIfNeedToRemoveWifiP2pGroupWhenWifiDisconnected");
		if(!mGCService.isSoftApEnable()) {
			try {
				removeWifiP2pGroup(new OperationCallback(){

					@Override
					public void error(Exception e) {
						Log.e(Common.TAG, "removeWifiP2pGroupWhenWifiDisconnected error= "+e.toString());
					}

					@Override
					public void done(Object that) {
						Log.i(Common.TAG, "removeWifiP2pGroupWhenWifiDisconnected done");
					}

				});
			} catch(Exception e) {
				Log.e(Common.TAG,"checkIfNeedToRemoveWifiP2pGroupWhenWifiDisconnected, fail");				
			}			
		}
	}

	private void initEventListener() {
		addBleEventListener(IGcConnectivityService.CB_LONG_TERM_EVENT_RESULT,
				new IBleEventListener() {

					@Override
					public void event(int callbackID, Bundle bundle) {
						LongTermEvent event = (LongTermEvent) bundle
								.getSerializable(IGcConnectivityService.PARAM_LONG_TERM_EVENT);
						if (event.equals(LongTermEvent.LTEVENT_CAMERA_STATUS)) {
							IGcConnectivityService.OperationEvent opEvent = (IGcConnectivityService.OperationEvent) bundle
									.getSerializable(IGcConnectivityService.PARAM_OPERATION_EVENT);
							switch (opEvent) {
							case OPEVENT_START_CAPTURING:
								if (!mGCService
										.getCurrentConnectionMode()
										.equals(IGCService.ConnectionMode.Partial)) {
									break;
								}

								if (DEBUG)
									Log.i(Common.TAG,
											"[GCServiceWorker2] BLE onCapture event, GC start capturing, ready: 0x"
													+ Integer
															.toHexString(Common.READY_NONE));

								{
									IGCService.ReadyStatusListener l = ((GCService) mGCService)
											.getReadyStatusListener();
									if (l != null)
										l.onChange(Common.READY_NONE);
								}
								break;

							case OPEVENT_COMPLETE_CAPTURING:
								if (!mGCService
										.getCurrentConnectionMode()
										.equals(IGCService.ConnectionMode.Partial)) {
									break;
								}

								int ready = bundle
										.getInt(IGcConnectivityService.PARAM_READY_BIT);

								if (DEBUG)
									Log.i(Common.TAG,
											"[GCServiceWorker2] BLE onReady event, GC ready for capture ready: 0x"
													+ Integer
															.toHexString(ready));
								{
									IGCService.ReadyStatusListener l = ((GCService) mGCService)
											.getReadyStatusListener();
									if (l != null)
										l.onChange(ready);
								}
								break;

							default: // other case is acceptable
							}
						}
					}
				});

		addBleEventListener(IGcConnectivityService.CB_LONG_TERM_EVENT_RESULT,
				new IBleEventListener() {

					@Override
					public void event(int callbackID, Bundle bundle) {
						LongTermEvent event = (LongTermEvent) bundle
								.getSerializable(IGcConnectivityService.PARAM_LONG_TERM_EVENT);
						if (event.equals(LongTermEvent.LTEVENT_CAMERA_STATUS)) {
							IGcConnectivityService.OperationEvent opEvent = (IGcConnectivityService.OperationEvent) bundle
									.getSerializable(IGcConnectivityService.PARAM_OPERATION_EVENT);
							switch (opEvent) {
							case OPEVENT_START_RECORDING:
								if (!mGCService
										.getCurrentConnectionMode()
										.equals(IGCService.ConnectionMode.Partial)) {
									break;
								}

								if (DEBUG)
									Log.i(Common.TAG,
											"[GCServiceWorker2] BLE onRecord event, GC start recording, ready: 0x"
													+ Integer
															.toHexString(Common.READY_NONE));

								{
									IGCService.ReadyStatusListener l = ((GCService) mGCService)
											.getReadyStatusListener();
									if (l != null)
										l.onChange(Common.READY_NONE);
								}
								break;

							case OPEVENT_COMPLETE_RECORDING:
								if (!mGCService
										.getCurrentConnectionMode()
										.equals(IGCService.ConnectionMode.Partial)) {
									break;
								}

								int ready = bundle
										.getInt(IGcConnectivityService.PARAM_READY_BIT);

								if (DEBUG)
									Log.i(Common.TAG,
											"[GCServiceWorker2] BLE onReady event, GC ready for record type: "
													+ bundle.getInt(IGcConnectivityService.PARAM_FILE_TYPE)
													+ ", ready:"
													+ Integer
															.toHexString(ready));

								{
									IGCService.ReadyStatusListener l = ((GCService) mGCService)
											.getReadyStatusListener();
									if (l != null)
										l.onChange(ready);
								}
								break;

							default: // other case is acceptable
							}
						} else if (event
								.equals(LongTermEvent.LTEVENT_HW_STATUS)) {
							SwitchOnOff powerStatus = (SwitchOnOff) bundle
									.getSerializable(IGcConnectivityService.PARAM_GC_POWER);
							if (powerStatus != null) {
								if (DEBUG)
									Log.i(Common.TAG,
											"[GCServiceWorker2] BLE Power status event, status= "
													+ powerStatus);

								if (powerStatus.equals(SwitchOnOff.SWITCH_OFF)) {
									onUnexpectedGcShutdown();
								}

								IGCService.PowerStateListener powerStateListener = ((GCService) mGCService)
										.getPowerStateChangeListener();
								if (powerStateListener != null)
									powerStateListener.onPowerOn(powerStatus
											.equals(SwitchOnOff.SWITCH_ON));
							}

							PlugIO usbStorage = (PlugIO) bundle
									.getSerializable(IGcConnectivityService.PARAM_USB_STORAGE);
							if (usbStorage != null) {
								if (DEBUG)
									Log.i(Common.TAG,
											"[GCServiceWorker2] BLE usb storage change event, type: "
													+ usbStorage);

								if (usbStorage.equals(PlugIO.PLUG_IN)) {
									onUnexpectedGcShutdown();
								}
							}

						}
					}
				});
	}
	
	protected void onSocketDisconnectByGc() {
		final Status wifiStatus = getWifiStatus(); 
		if(wifiStatus.equals(Status.Connected)) {
			Log.i(Common.TAG, "[GCServiceWorker2] onSocketDisconnectByGc");
			setWifiStatus(Status.Error);
			setLastConnectionError("onSocketDisconnectByGc", NetworkHelper.getWifiErrorRecoveryAction(ConnectionErrorCode.WIFI_DISCONNECT_BY_GC_DISCONNECT_SOCKET.getVal()), ConnectionErrorCode.WIFI_DISCONNECT_BY_GC_DISCONNECT_SOCKET.getVal());
			decideNextConnectionMove();
		} else {
			Log.i(Common.TAG, "[GCServiceWorker2] onSocketDisconnectByGc, but wifi status= "+wifiStatus+" so do nothing");
		}
	}
	
	protected void onUnexpectedGcShutdown() {
		final Status wifiStatus = getWifiStatus(); 
		if(wifiStatus.equals(Status.Connected)) {
			Log.i(Common.TAG, "[GCServiceWorker2] onUnexpectedGcShutdown");
			setWifiStatus(Status.Error);
			setLastConnectionError("onUnexpectedGcShutdown", NetworkHelper.getWifiErrorRecoveryAction(ConnectionErrorCode.WIFI_DISCONNECT_BY_GC_POWER_OFF.getVal()), ConnectionErrorCode.WIFI_DISCONNECT_BY_GC_POWER_OFF.getVal());
			decideNextConnectionMove();
		} else {
			Log.i(Common.TAG, "[GCServiceWorker2] onUnexpectedGcShutdown, but wifi status= "+wifiStatus+" so do nothing");
		}
	}
	
	private void onGCWifiUnreachable() {
		final Status wifiStatus = getWifiStatus(); 
		if(wifiStatus.equals(Status.Connected)) {
			Log.i(Common.TAG, "[GCServiceWorker2] onGCWifiUnreachable");
			setWifiStatus(Status.Error);
			setLastConnectionError("onGCWifiUnreachable", NetworkHelper.getWifiErrorRecoveryAction(ConnectionErrorCode.WIFI_DISCONNECT_BY_GC_WIFI_UNREACHABLE.getVal()), ConnectionErrorCode.WIFI_DISCONNECT_BY_GC_WIFI_UNREACHABLE.getVal());
			decideNextConnectionMove();
		} else {
			Log.i(Common.TAG, "[GCServiceWorker2] onGCWifiUnreachable, but wifi status= "+wifiStatus+" so do nothing");
		}
	}

	private void onError(Exception e) {
		if (e != null) {
			Log.e(Common.TAG, "[GCServiceWorker2] Error !! " + e.toString());
			Log.e(Common.TAG, Log.getStackTraceString(e));
		} else {
			Log.e(Common.TAG, "[GCServiceWorker2] Error !! without exception");
			StackTraceElement[] elements = Thread.currentThread()
					.getStackTrace();

			for (StackTraceElement ste : elements)
				Log.e(Common.TAG, "  " + ste.toString());
		}

		if(!getSocketStatus().equals(Status.Error)
			&& !getSocketStatus().equals(Status.Disconnected)
			&& !getSocketStatus().equals(Status.Disconnecting)) {
			
			setSocketStatus(Status.Error);
			
			IGCService.ErrorListener l = ((GCService)mGCService).getErrorListener();
			if(l != null) l.onError(mGCService, e);
			
			if(e != null && e instanceof SocketException) { // check connection exception first
				setLastConnectionError("Socket error, ", SOCKET_ERROR_RECOVERY_ACTION, ConnectionErrorCode.SOCKET_EXCEPTION.getVal());
			} else if(!isConnectionLive()) { // check heart beat
				setLastConnectionError("Socket error, heart beat dead.", SOCKET_ERROR_RECOVERY_ACTION, ConnectionErrorCode.SOCKET_HEARTBEAT_DEAD.getVal());
			} else if(e != null && e instanceof SocketTimeoutException) { // check socket timeout exception
				setLastConnectionError("Socket error, "+e.toString(), SOCKET_ERROR_RECOVERY_ACTION, ConnectionErrorCode.SOCKET_COMMAND_NO_RESPONSE.getVal());
			} else {
				setLastConnectionError("Socket error, ", SOCKET_ERROR_RECOVERY_ACTION, ConnectionErrorCode.SOCKET_COMMON_ERROR.getVal());
			}
			
			decideNextConnectionMove();			
		}
	}
	
	protected Status getSocketStatus() {
		return ((GCService)mGCService).getSocketStatus();
	}
	
	private void setSocketStatus(Status status) {
		((GCService)mGCService).setSocketStatus(status);
	}
	
	protected Status getWifiStatus() {
		return ((GCService)mGCService).getWifiStatus();
	}
	
	private void setWifiStatus(Status status) {
		((GCService)mGCService).setWifiStatus(status);
	}
	
	protected Status getBleStatus() {
		return ((GCService)mGCService).getBleStatus();
	}
	
	private void setBleStatus(Status status) {
		((GCService)mGCService).setBleStatus(status);
	}
	
	private void openSocket(final byte[] appGuid, final String addr, final OperationCallback callback) {
		Log.i(Common.TAG, "[GCServiceWorker2] openSocket");
		try {
			if(mGCService.getCurrentConnectionMode() != IGCService.ConnectionMode.Partial) throw new StatusException();

			setSocketStatus(Status.Connecting);

			new Thread(new Runnable() {
				@Override
				public void run() {
					try {
						mWebSocketConnection = new WebSocketConnection();
						mWebSocketConnection.connect(getWebSocketUri(addr),
								new WebSocketConnectionHandler() {

									@Override
									public void onOpen() {
										Log.d(Common.TAG,
												"[GCServiceWorker2] Web Socket opened");

										synchronized (GCServiceWorkerImp.this) {
											mAddress = addr;
											
											mHeartBeatLive.set(true);
											mHeartBeatDeadCounter.set(0);
											mHeartBeatGotNotify.set(true);
											mHeartBeatSendPingThread = new Thread(new Runnable() {

												@Override
												public void run() {
													while (!mHeartBeatSendPingThread.isInterrupted()) {
														try {
															// put current time stamp to ping payload
															ByteBuffer buffer = ByteBuffer.allocate(Long.SIZE);
															Long pingTimeStamp = System.currentTimeMillis(); 
															buffer.putLong(pingTimeStamp);
															byte[] pingPayload = buffer.array();
															mWebSocketConnection.sendPingMessage(pingPayload);
															Log.i(Common.TAG, "[GCServiceWorker2] ping is sent, " + pingTimeStamp + ", waiting for pong...");
															
															Thread.sleep(3000);
															
														} catch (InterruptedException e) {
															Log.d(Common.TAG, "[GCServiceWorker2] heart beat send ping thread interrupted");
															break;
														} catch (Exception e) {
															Log.d(Common.TAG, "[GCServiceWorker2] heart beat send ping thread exception raised", e);
														}
													}
												}
											});
											mHeartBeatMonitorThread = new Thread(new Runnable() {

												@Override
												public void run() {
													while (!mHeartBeatMonitorThread.isInterrupted()) {
														try {
															if (mHeartBeatGotNotify.getAndSet(false)) {
																if (!mHeartBeatLive.get()) {
																	Log.i(Common.TAG, "[GCServiceWorker2] heart beat live");
																	
																	mHeartBeatLive.set(true);
																	mHeartBeatDeadCounter.set(0);
																	
																	IGCService.HeartBeatListener l = ((GCService)mGCService).getHeartBeatListener();
																	if(l != null) l.onHeartBeat(mGCService, mHeartBeatLive.get());
																}
															} else {
																Log.d(Common.TAG, "[GCServiceWorker2] heart beat dead");
																
																mHeartBeatLive.set(false);
																if (mHeartBeatDeadCounter.incrementAndGet() >= HEART_BEAT_DEAD_TOLERANCE_THRESHOLD) {
																	boolean isGCWifiReachable = false;
																	try {
																		isGCWifiReachable = InetAddress.getByName(addr).isReachable(1000);
																	} catch (Exception e) {
																		Log.w(Common.TAG, "[GCServiceWorker2] fail to check if gc is wifi reachable", e);
																	}
																	
																	if (isGCWifiReachable) {
																		Log.d(Common.TAG, "[GCServiceWorker2] gc is wifi reachable");
																		onError(null);
																	} else {
																		Log.d(Common.TAG, "[GCServiceWorker2] gc is not wifi reachable");
																		onGCWifiUnreachable();
																	}
																}
																
																IGCService.HeartBeatListener l = ((GCService)mGCService).getHeartBeatListener();
																if(l != null) l.onHeartBeat(mGCService, mHeartBeatLive.get());
															}
															
															Thread.sleep(3000);
															
														} catch (InterruptedException e) {
															Log.d(Common.TAG, "[GCServiceWorker2] heart beat monitor thread interrupted");
															break;
														} catch (Exception e) {
															Log.d(Common.TAG, "[GCServiceWorker2] heart beat monitor thread exception raised", e);
														}
													}
												}
											});
										}

										setSocketStatus(Status.Connected);
										callback.done(GCServiceWorkerImp.this);
										mHeartBeatSendPingThread.start();
										mHeartBeatMonitorThread.start();
									}

									@Override
									public void onClose(int code, String reason) {
										Log.d(Common.TAG,
												"[GCServiceWorker2] Web Socket closed, code:" + code + 
												", reason:" + reason + 
												", socket status:" + getSocketStatus());
										if (code == WebSocket.ConnectionHandler.CLOSE_NORMAL) {
											// no need to handle onClose when it is closed normally, 
											// which should be triggered by ourselves in closeSocket()
										} else {
											if (getSocketStatus() == Status.Connected) {
												onError(null);
											} else if (getSocketStatus() == Status.Connecting) {
												Exception e = new CommonException(
														"Web Socket closed",
														Common.ErrorCode.ERR_FAIL);
												callback.error(e);
											}
										}
									}
									
									@Override
									public void onPongMessage(byte[] payload) {
										if (payload != null && payload.length == Long.SIZE) {
											ByteBuffer buffer = ByteBuffer.allocate(Long.SIZE);
											buffer.put(payload);
											buffer.flip();
											Long timeStamp = buffer.getLong();
											mHeartBeatGotNotify.set(true);
											
											Log.d(Common.TAG, "[GCServiceWorker2] Web Socket got pong message, " + timeStamp + ", " + new SimpleDateFormat("HH:mm:ss.SSS").format(new Date(timeStamp)));
										} else {
											Log.d(Common.TAG, "[GCServiceWorker2] Web Socket got invalid pong message:" + payload);
										}
									}
								});

					} catch (Exception e) {
						callback.error(e);
					}
				}
			}).start();
		} catch (Exception e) {
			callback.error(e);
		}
	}

	protected void closeSocket(Exception e) {
		Log.i(Common.TAG, "[GCServiceWorker2] closeSocket");
		final long begin = System.currentTimeMillis();

		if(getSocketStatus() == Status.Disconnected || getSocketStatus() == Status.Disconnecting) {
			return;
		}
		
		setSocketStatus(Status.Disconnecting);

		if (mWebSocketConnection != null) {
			mWebSocketConnection.disconnect();
			mWebSocketConnection = null;
		}
		
		if (mHeartBeatSendPingThread != null) {
			try {
				mHeartBeatSendPingThread.interrupt();
				mHeartBeatSendPingThread.join();
			} catch (Exception exception) {
			}
			mHeartBeatSendPingThread = null;
		}
		
		if (mHeartBeatMonitorThread != null) {
			try {
				mHeartBeatMonitorThread.interrupt();
				mHeartBeatMonitorThread.join();
			} catch (Exception exception) {
			}
			mHeartBeatMonitorThread = null;
		}

		synchronized (this) {
			mHeartBeatLive.set(false);
			mHeartBeatDeadCounter.set(0);
			mAddress = null;
			mIsInUpgradeProcess.set(false);
		}

		setSocketStatus(Status.Disconnected);
		Log.i(Common.TAG, "[GCServiceWorker2] closed, spend: " + (System.currentTimeMillis() - begin) + "ms");
	}

	private String getWebSocketUri(String address) {
		StringBuilder result = new StringBuilder("ws://");
		result.append(address);
		result.append(":3000/sock");
		return result.toString();
	}

}
