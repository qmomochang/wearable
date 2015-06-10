package com.htc.gc.internal.v1;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.TimeUnit;
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

import com.htc.dummy.connectivity.v2.GcDummyConnectivityService;
import com.htc.gc.GCMediaItem;
import com.htc.gc.GCService;
import com.htc.gc.GCService.Status;
import com.htc.gc.connectivity.interfaces.IGcConnectivityServiceBase.LongTermEvent;
import com.htc.gc.connectivity.interfaces.IGcConnectivityServiceBase.PlugIO;
import com.htc.gc.connectivity.interfaces.IGcConnectivityServiceBase.Result;
import com.htc.gc.connectivity.interfaces.IGcConnectivityServiceBase.SwitchOnOff;
import com.htc.gc.connectivity.interfaces.IGcConnectivityServiceBase.VerifyPasswordStatus;
import com.htc.gc.connectivity.v2.GcConnectivityService;
import com.htc.gc.connectivity.v2.interfaces.IGcConnectivityService;
import com.htc.gc.interfaces.Common;
import com.htc.gc.interfaces.Common.BleCommandException;
import com.htc.gc.interfaces.Common.CancelException;
import com.htc.gc.interfaces.Common.CommonException;
import com.htc.gc.interfaces.Common.ConnectionErrorCode;
import com.htc.gc.interfaces.Common.Context;
import com.htc.gc.interfaces.Common.ErrorCallback;
import com.htc.gc.interfaces.Common.NoImpException;
import com.htc.gc.interfaces.Common.OperationCallback;
import com.htc.gc.interfaces.Common.StatusException;
import com.htc.gc.interfaces.IAuthManager;
import com.htc.gc.interfaces.IAutoBackuper;
import com.htc.gc.interfaces.IAutoBackuper.AutoBackupGetHttpProxyCallback;
import com.htc.gc.interfaces.IDeviceController;
import com.htc.gc.interfaces.IDeviceController.BatteryInfoCallback;
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
import com.htc.gc.internal.CancelTask;
import com.htc.gc.internal.DeviceItem;
import com.htc.gc.internal.GCTask;
import com.htc.gc.internal.IBackgroundTask;
import com.htc.gc.internal.IGCServiceWorker;
import com.htc.gc.internal.NetworkHelper;
import com.htc.gc.internal.Protocol;
import com.htc.gc.tasks.BackgroundDownloadFragmentTask;
import com.htc.gc.tasks.CheckValidationTask;
import com.htc.gc.tasks.CheckValidationTask.ValidationCallback;
import com.htc.gc.tasks.DeleteItemsTask;
import com.htc.gc.tasks.GetErrorLogFromGcTask;
import com.htc.gc.tasks.UpgradeFirmwareTask;


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
	
	private static final int PROTOCOL_VERSION = 1;

	private static final int TX_PORT = 9000;
    private static final int RX_PORT = 9001;
    private static final int EVENT_PORT = 9002;
    private static final int FILE_RX_PORT = 9003;
    private static final int THUMBNAIL_RX_PORT = 9004;
    

    private static final int CONNECT_SOCKET_TIMEOUT = 10000; //ms
    private static final int CONNECT_SOCKET_RETRY_TIMES = 2;
    private static final int HEART_BEAT_TIMEOUT = 5500; //ms
    private static final int HEART_BEAT_DEAD_TOLERANCE_THRESHOLD = 3;
    private static final int COMMAND_NORMAL_RESPONSE_TIMEOUT = 30000; //ms
    private static final int COMMAND_UPGRADE_RESPONSE_TIMEOUT = 900000; //ms
    private static final int COMMAND_GET_LOG_RESPONSE_TIMEOUT = 900000; //ms
    private static final int COMMAND_DELETE_RESPONSE_TIMEOUT = 900000; //ms
    
    private static final int CONNECT_SILENT_RECONNECT_BLE_TIMEOUT = 25000; //ms
    private static final int CONNECT_SILENT_RECONNECT_BLE_MAX_RETRY_TIMES = 15;
    
    private final android.content.Context mCtx;
    protected final IGCService mGCService;
	private final AtomicInteger mSequenceIDGenerator = new AtomicInteger();
	
	protected final byte[] mAppGuid;
	protected final IGcConnectivityService mConn;
	
	private Socket mCommandTXSocket;
	private OutputStream mCommandChannel;
	private Socket mCommandRXSocket;
	private InputStream mResponseChannel;
	private Socket mEventSocket;
	private InputStream mEventChannel;
	private Socket mFileRXSocket;
	private InputStream mFileChannel;
	private Socket mThumbnailRXSocket;
	private InputStream mThumbnailChannel;

	private Thread mCommandThread;
	protected final PriorityBlockingQueue<GCTask> mCommandQueue = new PriorityBlockingQueue<GCTask>();

	private Thread mEventThread;
	protected final SparseArray<ArrayList<IEventListener>> mEventHandlers = new SparseArray<ArrayList<IEventListener>> ();

	private Thread mFileDownloadThread;
	private final SparseArray<IBackgroundTask> mFileDownloadTasks = new SparseArray<IBackgroundTask>();
	
	private Thread mThumbnailDownloadThread;
	private final SparseArray<IBackgroundTask> mThumbnailDownloadTasks = new SparseArray<IBackgroundTask>();
	
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

	protected long mConnectBegin;
	protected long mHeartBeatTime;
	protected boolean mHeartBeatLive;
	private final AtomicInteger mHeartBeatDeadCounter = new AtomicInteger();
	protected String mAddress;
	protected int mFWVersion;
	protected int mBootVersion;
	protected byte mMcuVersion;
	protected String mBleVersion;

	protected volatile Context mContext = Context.None;
	
//	protected volatile WifiConnectMode mSuggestWifiConnectionMode = WifiConnectMode.None;
	
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
				Log.i(Common.TAG, "[GCServiceWorker] Create Socket Connection Fail. Error= "+e.toString());
				e.printStackTrace();
				
				final int retry = mOpenSocketRetryCounter.getAndAdd(1);
				
				if(retry < CONNECT_SOCKET_RETRY_TIMES) {
					Log.i(Common.TAG, "[GCServiceWorker] Create Socket Connection Fail, retry connect "+retry+"th time(s).");
					// TODO Move this into decideNextConnectionMove
					mRunOnUiHandler.post(new Runnable() {

						@Override
						public void run() {
							closeSocket(e);
						}
						
					});
					decideNextConnectionMove();
				} else {
					Log.i(Common.TAG, "[GCServiceWorker] Create Socket Connection Fail, retry connect "+retry+"th time(s), stop retry. Disconnect wifi");
					mOpenSocketRetryCounter.set(0);
					setLastConnectionError("[GCServiceWorker] Connect Socket Fail twice time, wifi might disconnect", NetworkHelper.getWifiErrorRecoveryAction(ConnectionErrorCode.SOCKET_CONNECT_FAIL.getVal()), ConnectionErrorCode.SOCKET_CONNECT_FAIL.getVal());
					setWifiStatus(Status.Error);
					decideNextConnectionMove();
				}
			}

			@Override
			public void done(Object that) {
				Log.i(Common.TAG, "[GCServiceWorker] Create Socket Connection Success");
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
			Log.i(Common.TAG, "[GCServiceWorker] setBleStartConnectTimestampAndIncreaseCounter timestamp= "+timestamp+" counter= "+count);
		} else {
			Log.i(Common.TAG, "[GCServiceWorker] setBleStartConnectTimestamp last timestamp is not 0, do nothing. counter= "+count);
		}
	}
	
	private void resetBleStartConnectTimestampAndCounter() {
		Log.i(Common.TAG, "[GCServiceWorker] resetBleStartConnectTimestampAndCounter");
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
					Log.i(Common.TAG, "[GCServiceWorker] Error occurs. BleStatus= "+getBleStatus()+", Reset to "+Status.Disconnected);
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
					Log.i(Common.TAG, "[GCServiceWorker] Error occurs. WifiStatus= "+getWifiStatus()+", Reset to "+Status.Disconnected);
					// clean up process
					closeSocket(new CancelException());
					checkIfNeedToRemoveWifiP2pGroupWhenWifiDisconnected();
					setWifiStatus(Status.Disconnected);
					errorOccurs = true;
				}
				if(getSocketStatus() == Status.Error) {
					Log.i(Common.TAG, "[GCServiceWorker] Error occurs. SocketStatus= "+getSocketStatus()+", Reset to "+Status.Disconnected);
					// clean up process
					closeSocket(new CancelException());
					errorOccurs = true;
				}

				if(errorOccurs == false) {
					IGCService.ConnectionMode expectedConnectionMode = mGCService.getExpectedConnectionMode();
					Status bleStatus = getBleStatus();
					Status wifiStatus = getWifiStatus();
					Status socketStatus = getSocketStatus();
					Log.i(Common.TAG, "[GCServiceWorker] decideNextConnectionMove, bleStauts: "+bleStatus+", wifiStatus: "+wifiStatus+", socketStatus: "+socketStatus + ", expected connection mode: " + expectedConnectionMode);
					
					switch(expectedConnectionMode) {
					case Disconnected:
						if(socketStatus == Status.Connected || socketStatus == Status.Connecting) {
							Log.i(Common.TAG, "[GCServiceWorker] Disconnect Socket");
							closeSocket(new CancelException());
							decideNextConnectionMove();
							break;
						}
						if(wifiStatus == Status.Connected) {
							if(mConn.gcWifiDisconnect(((DeviceItem)getTargetDevice()).getDevice())) {
								Log.i(Common.TAG, "[GCServiceWorker] Disconnect Wifi");
								setWifiStatus(Status.Disconnecting);
							} else {
								Log.e(Common.TAG, "[GCServiceWorker] Disconnect Wifi Fail");
								checkIfNeedToRemoveWifiP2pGroupWhenWifiDisconnected();
							}
							break;
						}
						if(wifiStatus == Status.Disconnected && (bleStatus == Status.Connected || bleStatus == Status.Verified)) {
							if(mConn.gcBleDisconnect(((DeviceItem)getTargetDevice()).getDevice())) {
								Log.i(Common.TAG, "[GCServiceWorker] Disconnect BLE");
								setBleStatus(Status.Disconnecting);	
							} else {
								Log.e(Common.TAG, "[GCServiceWorker] Disconnect BLE Fail");
							}
							break;
						}
						break;
					case Partial:
						if(bleStatus == Status.Disconnected) {
							if(mConn.gcBleConnect(((DeviceItem)getTargetDevice()).getDevice())) {
								Log.i(Common.TAG, "[GCServiceWorker] Connect BLE");
								setBleStatus(Status.Connecting);
								setBleStartConnectTimestampAndIncreaseCounter(System.currentTimeMillis());
							} else {
								Log.e(Common.TAG, "[GCServiceWorker] Connect BLE Fail");
							}
							break;
						}
						if(bleStatus == Status.Connected) {
							DeviceItem device = (DeviceItem)getTargetDevice();
							if(mConn.gcVerifyPassword(device.getDevice(), device.getPassword())) {
								Log.i(Common.TAG, "[GCServiceWorker] VerifyPassword");								
								setBleStatus(Status.Verifying);
							} else {
								Log.e(Common.TAG, "[GCServiceWorker] VerifyPassword Fail");
							}
							break;
						}
						if(socketStatus == Status.Connected || socketStatus == Status.Connecting) {
							Log.i(Common.TAG, "[GCServiceWorker] Disconnect Socket");
							closeSocket(new CancelException());
							// closeSocket is a sync function, no need to break, do next thing
						}
						if(bleStatus == Status.Verified && wifiStatus == Status.Connected) {
							if(mConn.gcWifiDisconnect(((DeviceItem)getTargetDevice()).getDevice())) {
								Log.i(Common.TAG, "[GCServiceWorker] Disconnect WIFI");
								setWifiStatus(Status.Disconnecting);
							} else {
								Log.e(Common.TAG, "[GCServiceWorker] Disconnect WIFI Fail");
								
								checkIfNeedToRemoveWifiP2pGroupWhenWifiDisconnected();
							}
							break;
						}
						break;
					case Full:
						if(bleStatus == Status.Disconnected) {
							if(mConn.gcBleConnect(((DeviceItem)getTargetDevice()).getDevice())) {
								Log.i(Common.TAG, "[GCServiceWorker] Connect BLE");
								setBleStatus(Status.Connecting);
								setBleStartConnectTimestampAndIncreaseCounter(System.currentTimeMillis());
							} else {
								Log.e(Common.TAG, "[GCServiceWorker] Connect BLE Fail");
							}
							break;
						} 
						if(bleStatus == Status.Connected) {
							DeviceItem device = (DeviceItem)getTargetDevice();
							if(mConn.gcVerifyPassword(device.getDevice(), device.getPassword())) {
								Log.i(Common.TAG, "[GCServiceWorker] VerifyPassword");								
								setBleStatus(Status.Verifying);
							} else {
								Log.e(Common.TAG, "[GCServiceWorker] VerifyPassword Fail");
							}
							break;
						}
						if(bleStatus == Status.Verified && wifiStatus == Status.Disconnected) {
							if(mGCService.isSoftApEnable()) {
								DeviceItem deviceItem = (DeviceItem) getTargetDevice();
								if(mConn.gcSoftAPConnect(deviceItem.getDevice(), deviceItem.getPassword())) {
									Log.i(Common.TAG, "[GCServiceWorker] Connect Soft AP WIFI");
									setWifiStatus(Status.Connecting);	
								} else {
									Log.e(Common.TAG, "[GCServiceWorker] Connect Soft Ap WIFI Fail");
								}								
							} else {
								((GCService)mGCService).disconnectWifiApInternal();
								
								if(mConn.gcWifiConnect(((DeviceItem)getTargetDevice()).getDevice())) {
									Log.i(Common.TAG, "[GCServiceWorker] Connect WIFI");
									setWifiStatus(Status.Connecting);
								} else {
									Log.e(Common.TAG, "[GCServiceWorker] Connect WIFI Fail");
								}								
							}
							break;
						}
						if(bleStatus == Status.Verified &&
							wifiStatus == Status.Connected &&
							socketStatus == Status.Disconnected) 
						{
							Log.i(Common.TAG, "[GCServiceWorker] Connect Socket");
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
								Log.i(Common.TAG, "[GCServiceWorker] slient reconnectBle flag= true");
								isSilentReconnectBle = true;
							} else {
								Log.i(Common.TAG, "[GCServiceWorker] slient reconnectBle flag= false");
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
					Log.i(Common.TAG, "[GCServiceWorker] BLE Connect Result= "+result);
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
					Log.i(Common.TAG, "[GCServiceWorker] BLE Disconnect Result= "+result);
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
					Log.i(Common.TAG, "[GCServiceWorker] BLE Force Disconnect Result= "+result);
					
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
							Log.e(Common.TAG, "[GCServiceWorker] Ble Force Disconnect Callback ID doesn't match: "+bleCallback.getID());
						}						
					} else {
						Log.e(Common.TAG, "[GCServiceWorker] Ble Callback List is empty");
					}
					break;

				case IGcConnectivityService.CB_WIFI_CONNECT_RESULT:
					result = (Result) bundle.getSerializable(IGcConnectivityService.PARAM_RESULT);
					
					boolean isSoftAp = false;
					Boolean softApFlag = (Boolean) bundle.getSerializable(IGcConnectivityService.PARAM_RESULT_SOFTAP);
					if(softApFlag != null && softApFlag.booleanValue() == true) {
						isSoftAp = true;
					}
					
					Log.i(Common.TAG, "[GCServiceWorker] WIFI Connect Result= "+result+", isSoftAp= "+isSoftAp);
					if (result == Result.RESULT_SUCCESS) {
						String ip = bundle.getString(IGcConnectivityService.PARAM_DEVICE_IP_ADDRESS);
						IDeviceItem deviceItem = getTargetDevice();
						deviceItem.setIP(ip);
						Log.i(Common.TAG, "[GCServiceWorker] Receive Device IP= "+ip);
						setWifiStatus(Status.Connected);
					} else {
						int errorCode = bundle.getInt(IGcConnectivityService.PARAM_WIFI_ERROR_CODE);
						Log.i(Common.TAG, "[GCServiceWorker] WIFI Connect Result= "+result+", Error code= 0x"+Integer.toHexString(errorCode));
						setWifiStatus(Status.Error);
						setLastConnectionError("Wifi Connect Fail.", NetworkHelper.getWifiErrorRecoveryAction(errorCode), errorCode);
						
						if(errorCode == 0x1a) {
							Log.i(Common.TAG,"[GCServiceWorker] Wifi connect fail 0x1a, remove wifi p2p group");
							try {
								removeWifiP2pGroup(new OperationCallback(){

									@Override
									public void error(Exception e) {
										Log.e(Common.TAG, "[GCServiceWorker] removeWifiP2pGroupWhenWifiConnectFail0x1a error= "+e.toString());
									}

									@Override
									public void done(Object that) {
										Log.e(Common.TAG, "[GCServiceWorker] removeWifiP2pGroupWhenWifiConnectFail0x1a done");
									}
									
								});
							} catch(Exception e) {
								Log.e(Common.TAG,"[GCServiceWorker] Wifi connect fail 0x1a, remove wifi p2p group but fail");				
							}
						}
					}
					decideNextConnectionMove();
					break;

				case IGcConnectivityService.CB_WIFI_DISCONNECT_RESULT:
					result = (Result) bundle.getSerializable(IGcConnectivityService.PARAM_RESULT);
					if (result == Result.RESULT_SUCCESS) {
						Log.i(Common.TAG, "[GCServiceWorker] WIFI Disconnect Result= "+result);
						setWifiStatus(Status.Disconnected);
					} else {
						int errorCode = bundle.getInt(IGcConnectivityService.PARAM_WIFI_ERROR_CODE);
						Log.i(Common.TAG, "[GCServiceWorker] WIFI Disconnect Result= "+result+", Error code= 0x"+Integer.toHexString(errorCode));
						setWifiStatus(Status.Error);
						setLastConnectionError("Wifi Disconnect Fail.", NetworkHelper.getWifiErrorRecoveryAction(errorCode), errorCode);
					}
					
					checkIfNeedToRemoveWifiP2pGroupWhenWifiDisconnected();
					
					decideNextConnectionMove();
					break;
					
				case IGcConnectivityService.CB_CREATE_WIFI_P2P_GROUP_RESULT:
					result = (Result) bundle.getSerializable(IGcConnectivityService.PARAM_RESULT);
					Log.i(Common.TAG, "[GCServiceWorker] BLE Create Wifi P2P Group Result= "+result);
					break;
				
				case IGcConnectivityService.CB_REMOVE_WIFI_P2P_GROUP_RESULT:
					result = (Result) bundle.getSerializable(IGcConnectivityService.PARAM_RESULT);
					Log.i(Common.TAG, "[GCServiceWorker] BLE Remove Wifi P2P Group Result= "+result);
					
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
							Log.e(Common.TAG, "[GCServiceWorker] Ble Remove Wifi P2p Group Callback ID doesn't match: "+bleCallback.getID());
						}						
					} else {
						Log.e(Common.TAG, "[GCServiceWorker] Ble Callback List is empty");
					}
					break;
					
				case IGcConnectivityService.CB_GET_BLE_FW_VERSION_RESULT:
					result = (Result) bundle.getSerializable(IGcConnectivityService.PARAM_RESULT);
					if(result.equals(Result.RESULT_SUCCESS)) {
						String version = bundle.getString(IGcConnectivityService.PARAM_BLE_FW_VERSION);
						Log.i(Common.TAG, "[GCServiceWorker] BLE Get Firmware Version Result= "+result+", Version="+version);						
						mBleVersion = version;						
					} else {
						Log.i(Common.TAG, "[GCServiceWorker] BLE Get Firmware Version Result= "+result);
					}
					break;
					
				case IGcConnectivityService.CB_GET_ALL_FW_VERSION_RESULT:
					result = (Result) bundle.getSerializable(IGcConnectivityService.PARAM_RESULT);
					Log.i(Common.TAG, "[GCServiceWorker] BLE Get Firmware Version Result= "+result);
					if(result.equals(Result.RESULT_SUCCESS)) {
						mFWVersion = bundle.getInt(IGcConnectivityService.PARAM_MAIN_FW_VERSION, 0);
						Log.i(Common.TAG, "[GCServiceWorker] BLE Get Main Code Version = "+mFWVersion);		
						mBootVersion = bundle.getInt(IGcConnectivityService.PARAM_BOOT_FW_VERSION, 0);
						Log.i(Common.TAG, "[GCServiceWorker] BLE Get Boot Code Version = "+mBootVersion);
						mMcuVersion = (byte)bundle.getInt(IGcConnectivityService.PARAM_MCU_FW_VERSION, 0);
						Log.i(Common.TAG, "[GCServiceWorker] BLE Get MCU Version = "+mMcuVersion);
					}
					break;
				case IGcConnectivityService.CB_VERIFY_PASSWORD_RESULT:
					result = (Result) bundle.getSerializable(IGcConnectivityService.PARAM_RESULT);
					if(result.equals(Result.RESULT_SUCCESS)) {
						VerifyPasswordStatus status = (VerifyPasswordStatus) bundle.getSerializable(IGcConnectivityService.PARAM_VERIFY_PASSWORD_STATUS);
						Log.i(Common.TAG, "[GCServiceWorker] BLE Verify Password Result= "+result+", Status="+status);
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
						Log.i(Common.TAG, "[GCServiceWorker] BLE Verify Password Result= "+result);
					}
					break;
				case IGcConnectivityService.CB_LONG_TERM_EVENT_RESULT:
					LongTermEvent event = (LongTermEvent) bundle.getSerializable(IGcConnectivityService.PARAM_LONG_TERM_EVENT);
					BluetoothDevice device = (BluetoothDevice) bundle.getParcelable(IGcConnectivityService.PARAM_BLUETOOTH_DEVICE);
					Log.i(Common.TAG, "[GCServiceWorker] BLE Long Term Event Event="+event+", Device="+device);
					switch(event) {
					case LTEVENT_DISCONNECTED_FROM_GATT_SERVER:
						Status bleStatus = getBleStatus();
						if(bleStatus.equals(Status.Connected) || bleStatus.equals(Status.Verifying) || bleStatus.equals(Status.Verified)) {
							Log.i(Common.TAG, "[GCServiceWorker] LTEVENT_DISCONNECTED_FROM_GATT_SERVER");
							setBleStatus(Status.Error);
							setLastConnectionError("LTEVENT_DISCONNECTED_FROM_GATT_SERVER", BLE_ERROR_RECOVERY_ACTION, ConnectionErrorCode.BLE_DISCONNECT_FROM_GATT_SERVER.getVal());
							decideNextConnectionMove();							
						} else {
							Log.w(Common.TAG, "[GCServiceWorker] ignore LTEVENT_DISCONNECTED_FROM_GATT_SERVER, because current BLE status= "+bleStatus);
						}
						break;
					case LTEVENT_WIFI_DISCONNECTED:
						Status wifiStatus = getWifiStatus();
						if(wifiStatus.equals(Status.Connected)) {
							// only handle this event if wifi is connected
							// if error occurs in connecting or disconnecting should handle by command fail response
							setWifiStatus(Status.Error);
							setLastConnectionError("[GCServiceWorker] LTEVENT_WIFI_DISCONNECTED", NetworkHelper.getWifiErrorRecoveryAction(ConnectionErrorCode.WIFI_UNEXPECTED_DISCONNECT.getVal()), ConnectionErrorCode.WIFI_UNEXPECTED_DISCONNECT.getVal());
							decideNextConnectionMove();							
						} else {
							Log.w(Common.TAG, "[GCServiceWorker] ignore LTEVENT_WIFI_DISCONNECTED, because current WIFI status= "+wifiStatus);
						}
						break;
					default: // default is acceptable
						break;
					}
					break;
				case IGcConnectivityService.CB_PERFORMANCE_RESULT:
					Log.i(Common.TAG, "[GCServiceWorker] BLE Task "+bundle.getString(IGcConnectivityService.PARAM_TASK_NAME)+" cost "+bundle.getLong(IGcConnectivityService.PARAM_TIME_COST_MS)+" ms");
					break;
					
				case IGcConnectivityService.CB_CHANGE_PASSWORD_RESULT:
					result = (Result) bundle.getSerializable(IGcConnectivityService.PARAM_RESULT);
					Log.i(Common.TAG, "[GCServiceWorker] Change Password Result= "+result);
					
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
							Log.e(Common.TAG, "[GCServiceWorker] Ble Change Password Callback ID doesn't match: "+bleCallback.getID());
						}						
					} else {
						Log.e(Common.TAG, "[GCServiceWorker] Ble Callback List is empty");
					}
					
					if(result.equals(Result.RESULT_SUCCESS)) {
						if(getBleStatus().equals(Status.Connected)) {
							Log.i(Common.TAG, "[GCServiceWorker] Change Password and Verify again");
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
							Log.e(Common.TAG, "[GCServiceWorker] Ble Set Date Time Callback ID doesn't match: "+bleCallback.getID());
						}						
					} else {
						Log.e(Common.TAG, "[GCServiceWorker] Ble Callback List is empty");
					}
					break;
					
				case IGcConnectivityService.CB_SET_OPERATION_RESULT:
					bleCallback = popBleCallbackFromQueue();
					if(bleCallback != null) {
						if(bleCallback.getID() == IGcConnectivityService.CB_SET_OPERATION_RESULT) {
							ErrorCallback callback = bleCallback.getCallback();
							result = (Result) bundle.getSerializable(IGcConnectivityService.PARAM_RESULT);
							Log.i(Common.TAG, "[GCServiceWorker] Operation Ble Callback: "+mMsg.what+", Result: "+result);
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
								Log.e(Common.TAG, "[GCServiceWorker] Ble Operation Callback ID doesn't match: "+mMsg.what);
							}
							
						}
					} else {
						Log.e(Common.TAG, "[GCServiceWorker] Ble Callback List is empty");
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
							Log.e(Common.TAG, "[GCServiceWorker] Ble Set Gps Info Callback ID doesn't match: "+bleCallback.getID());
						}						
					} else {
						Log.e(Common.TAG, "[GCServiceWorker] Ble Callback List is empty");
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
							Log.e(Common.TAG, "[GCServiceWorker] Ble Set Name Callback ID doesn't match: "+bleCallback.getID());
						}						
					} else {
						Log.e(Common.TAG, "[GCServiceWorker] Ble Callback List is empty");
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
							Log.e(Common.TAG, "[GCServiceWorker] Ble Get Name Callback ID doesn't match: "+bleCallback.getID());
						}						
					} else {
						Log.e(Common.TAG, "[GCServiceWorker] Ble Callback List is empty");
					}
				break;
				case IGcConnectivityService.CB_SET_AUTO_BACKUP_AP_RESULT:
				case IGcConnectivityService.CB_CLR_AUTO_BACKUP_AP_RESULT:
				case IGcConnectivityService.CB_SET_AUTO_BACKUP_AP_SCAN_RESULT:
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
										error = NetworkHelper.connectionCodeErrorCode2ErrorCode(ConnectionErrorCode.getKey(errorCode));
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
							Log.e(Common.TAG, "[GCServiceWorker] Ble Auto Backup Callback ID doesn't match: "+bleCallback.getID()+", Expected: "+mMsg.what);
						}
					} else {
						Log.e(Common.TAG, "[GCServiceWorker] Ble Callback List is empty");
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
							Log.e(Common.TAG, "[GCServiceWorker] Ble Auto Backup Callback ID doesn't match: "+bleCallback.getID()+", Expected: "+mMsg.what);
						}
					} else {
						Log.e(Common.TAG, "[GCServiceWorker] Ble Callback List is empty");
					}
				break;
				case IGcConnectivityService.CB_GET_HW_STATUS_RESULT:
					bleCallback = popBleCallbackFromQueue();
					if(bleCallback != null) {
						if(bleCallback.getID() == mMsg.what) {
							result = (Result) bundle.getSerializable(IGcConnectivityService.PARAM_RESULT);
							BatteryInfoCallback batteryInfoCallback = (BatteryInfoCallback) bleCallback.getCallback();
							if(result.equals(Result.RESULT_SUCCESS)) {
								int batteryLevel = bundle.getInt(IGcConnectivityService.PARAM_BATTERY_LEVEL);
								PlugIO adapterPlugin = (PlugIO) bundle.getSerializable(IGcConnectivityService.PARAM_ADAPTER_PLUGIN);
								if(adapterPlugin != null) {
									batteryInfoCallback.result((IDeviceController)bleCallback.getThat(), adapterPlugin.equals(PlugIO.PLUG_IN), batteryLevel);	
								} else {
									batteryInfoCallback.error(new NullPointerException("adapterPlugin is null"));
								}
								
							} else {
								Log.w(Common.TAG, "[" + mMsg.what + "] Operation fail error");
								batteryInfoCallback.error(new BleCommandException());
							}								
							
						} else {
							Log.e(Common.TAG, "[GCServiceWorker] Ble Get Hardware Status Callback ID doesn't match: "+bleCallback.getID()+", Expected: "+mMsg.what);
						}
					} else {
						Log.e(Common.TAG, "[GCServiceWorker] Ble Callback List is empty");
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
				result = (Result) bundle.getSerializable(IGcConnectivityService.PARAM_RESULT);
				Log.i(Common.TAG, "[GCServiceWorker] BLE Get/Set LTEvent "+mMsg.what+", Result= "+result);
				break;
				
			default:
				Log.w(Common.TAG, "[GCServiceWorker] Unknown BLE Callback: "+mMsg.what);
			}
			
			synchronized(mBleEventHandler) {
				ArrayList<IBleEventListener> listeners = mBleEventHandler.get(mMsg.what);
				if(listeners != null) {
					Log.i(Common.TAG, "[GCServiceWorker] Ble event " + mMsg.what);

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
		Log.i(Common.TAG, "[GCServiceWorker] Set LTNotify");
		mConn.gcSetLTNotify(device.getDevice());
		
		/// We have to get BLE FW version first because we will reference it for GC bootup workaround.
		Log.i(Common.TAG, "[GCServiceWorker] Get Ble Version");
		mConn.gcGetBleFWVersion(device.getDevice());
		
		final Calendar calendar = Calendar.getInstance();
		try {
			getController().setDeviceTime(calendar, new OperationCallback() {

				@Override
				public void error(Exception e) {
					Log.e(Common.TAG, "[GCServiceWorker] BLE Set Date Time fail, error= "+e.toString());		
				}

				@Override
				public void done(Object that) {
					Log.i(Common.TAG, "[GCServiceWorker] BLE Set Date Time= "+calendar.getTime().toString());			
				}
				
			});
		} catch (Exception e) {
			e.printStackTrace();
			Log.e(Common.TAG, "[GCServiceWorker] BLE Set Date Time fail");
		}
		
		Log.i(Common.TAG, "[GCServiceWorker] Get All Version");
		mConn.gcGetAllFwVersion(device.getDevice());
		Log.i(Common.TAG, "[GCServiceWorker] Set Operation LTEvent");
		mConn.gcSetOperationLTEvent(device.getDevice());
		Log.i(Common.TAG, "[GCServiceWorker] Set GpsInfo LTEvent");
		mConn.gcSetGpsInfoLTEvent(device.getDevice());
		Log.i(Common.TAG, "[GCServiceWorker] Set Metadata LTEvent");
		mConn.gcSetMetadataLTEvent(device.getDevice());
		Log.i(Common.TAG, "[GCServiceWorker] Set CameraError LTEvent");
		mConn.gcSetCameraErrorLTEvent(device.getDevice());
		Log.i(Common.TAG, "[GCServiceWorker] Set AutoBackup LTEvent");
		mConn.gcSetAutoBackupLTEvent(device.getDevice());
		Log.i(Common.TAG, "[GCServiceWorker] Set Hardware status LTEvent");
		mConn.gcSetHwStatusLTEvent(device.getDevice());
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
		addEventListener(Protocol.EVENT_WIFI_STATUS_SYNC, new IEventListener() {

			@Override
			public void event(int eventID, ByteBuffer body) {
				Log.i(Common.TAG, "[GCServiceWorker] status sync event");

				int tmp = body.getInt();
				Common.Mode mode = Common.Mode.None;
				if(tmp == Protocol.PROP_FUNCTIONMODE_BROWSE) mode = Common.Mode.Browse;
				else if(tmp == Protocol.PROP_FUNCTIONMODE_CONTROL) mode = Common.Mode.Control;
				else if(tmp == Protocol.PROP_FUNCTIONMODE_STANDARD) mode = Common.Mode.None;
				else if(tmp == Protocol.PROP_FUNCTIONMODE_STANDBY) mode = Common.Mode.None;
				else Log.e(Common.TAG, "[GCServiceWorker] status sync event: unknown mode");

				int status = body.getInt();
				if(status == Protocol.STATUS_TIME_LAPSE) setContext(Common.Context.TimeLapse);
				else if(status == Protocol.STATUS_VIDEO_RECORDING) setContext(Common.Context.Recording);
				else if(status == Protocol.STATUS_IMAGE_PROCESSING) setContext(Common.Context.Capturing);
				else if(status == Protocol.STATUS_TIME_LAPSE_PAUSED) setContext(Common.Context.TimeLapse);
				else if(status == Protocol.STATUS_NONE) setContext(Common.Context.None);
				else Log.e(Common.TAG, "[GCServiceWorker] status sync event: unknown status");
				int timecode = body.getInt();

				int ready = body.getInt();
				setReady(ready);

				int handle = body.getInt();
				
				int timeLapseTotalFrameCount = body.getInt();

				int supportedFuncs = 0; 
				int telecomCode = 0;
				if(body.remaining() >= (Integer.SIZE / 8)) {
					int funcSupportListAndTelecomCode = body.getInt(); 
					Log.i(Common.TAG, "[GCServiceWorker] get firmware supported function list and telecom code, data= 0x"+Integer.toHexString(funcSupportListAndTelecomCode));
					supportedFuncs = (funcSupportListAndTelecomCode >> 16 /* get first two bytes */);
					Log.i(Common.TAG, "[GCServiceWorker] get firmware supported function list= 0x"+Integer.toHexString(supportedFuncs));
					telecomCode = (funcSupportListAndTelecomCode & 0x0000FFFF /* get first two bytes */);
					Log.i(Common.TAG, "[GCServiceWorker] get telecom code= 0x"+Integer.toHexString(telecomCode));
				} else {
					Log.i(Common.TAG, "[GCServiceWorker] get firmware supported function list and telecom code fail");
					Log.i(Common.TAG, "[GCServiceWorker] reset firmware supported function list to 0x"+Integer.toHexString(supportedFuncs));
					Log.i(Common.TAG, "[GCServiceWorker] reset telecom code to 0x"+Integer.toHexString(telecomCode));
				}

				IGCService.SyncDataListener syncDataListener = ((GCService)mGCService).getSyncInitDataListener();
				if(syncDataListener != null) syncDataListener.onSync(mGCService, mode, getContext(), timecode, timeLapseTotalFrameCount, getReady(), mLastItem = handle != 0? new GCMediaItem(0, handle) : null);
				
				IGCService.ReadyStatusListener readyStatusListener = ((GCService)mGCService).getReadyStatusListener();
				if(readyStatusListener != null) readyStatusListener.onChange(getReady());
				
				IGCService.FwSupportedFunctionListener fwSupportedFunctionListener = ((GCService)mGCService).getFwSupportedFunctionListener();
				if(fwSupportedFunctionListener != null) fwSupportedFunctionListener.onChange(supportedFuncs);
				Log.i(Common.TAG, "[GCServiceWorker] status sync event mode: " + mode.toString() + ", context: " + getContext().toString() + ", timecode: " + timecode + ", framecount: " + timeLapseTotalFrameCount +", ready: 0x" + Integer.toHexString(getReady()) + " last handle: 0x" + Integer.toHexString(handle));
			}

		});

		addEventListener(Protocol.EVENT_WIFI_HEART_BEAT, new IEventListener() {

			@Override
			public void event(int eventID, ByteBuffer body) {
				Log.i(Common.TAG, "[GCServiceWorker] heart beat event, Maincode: "+getFWVersion()+", MCU: "+getMCUVersion()+", BootCode: "+getBootVersion()+", BLE: "+getBleVersion());

				long now = System.currentTimeMillis();
				if(mHeartBeatLive == false) {
					Log.i(Common.TAG, "[GCServiceWorker] heart beat live event");
					mHeartBeatLive = true;
					mHeartBeatDeadCounter.set(0);

					IGCService.HeartBeatListener l = ((GCService)mGCService).getHeartBeatListener();
					if(l != null) l.onHeartBeat(mGCService, mHeartBeatLive);
				}

				mHeartBeatTime = now;
			}
		});

		addEventListener(Protocol.EVENT_ID_POWEROFF, new IEventListener() {

			@Override
			public void event(int eventID, ByteBuffer body) {
				Log.i(Common.TAG, "(decrepated) [GCServiceWorker] GC power off event");
				// Use BLE notify
				//onUnexpectedGcShutdown();
			}

		});

		addEventListener(Protocol.EVENT_ID_CLOSE_CONNECTION_SOCKET, new IEventListener() {

			@Override
			public void event(int eventID, ByteBuffer body) {
				Log.i(Common.TAG, "[GCServiceWorker] GC Close connection event");
				onSocketDisconnectByGc();
			}

		});

		addEventListener(Protocol.EVENT_ID_AUTOPOWER_OFF_EVENT, new IEventListener() {

			@Override
			public void event(int eventID, ByteBuffer body) {
				Log.i(Common.TAG, "(decrepated) [GCServiceWorker] GC auto power off event");
				// Use BLE notify
				//onUnexpectedGcShutdown();
			}
		});
		
		addEventListener(Protocol.EVENT_START_CAPTURING, new IEventListener() {

			@Override
			public void event(int eventID, ByteBuffer body) {
				if(DEBUG) Log.i(Common.TAG, "[GCServiceWorker] start capturing event, ready: 0x"+Integer.toHexString(Common.READY_NONE));

				IGCService.ReadyStatusListener l = ((GCService)mGCService).getReadyStatusListener();
				if(l != null) l.onChange(Common.READY_NONE);
			}
		});

		addEventListener(Protocol.EVENT_COMPLETE_CAPTURING, new IEventListener() {

			@Override
			public void event(int eventID, ByteBuffer body) {
				int type = body.getInt();
				int ready = body.getInt();
				
				if(DEBUG) Log.i(Common.TAG, "[GCServiceWorker] complete capturing event, GC ready for capture type: " + type + ", ready: 0x" + Integer.toHexString(ready));

				IGCService.ReadyStatusListener l = ((GCService)mGCService).getReadyStatusListener();
				if(l != null) l.onChange(ready);
			}
		});

		addEventListener(Protocol.EVENT_START_RECORDING, new IEventListener() {

			@Override
			public void event(int eventID, ByteBuffer body) {
				if(DEBUG) Log.i(Common.TAG, "[GCServiceWorker] start recording event, GC start recording, ready: 0x"+Integer.toHexString(Common.READY_NONE));

				IGCService.ReadyStatusListener l = ((GCService)mGCService).getReadyStatusListener();
				if(l != null) l.onChange(Common.READY_NONE);
			}
		});

		addEventListener(Protocol.EVENT_COMPLETE_RECORDING, new IEventListener() {

			@Override
			public void event(int eventID, ByteBuffer body) {
				int ready = body.getInt();

				if(DEBUG) Log.i(Common.TAG, "[GCServiceWorker] complete recording event, GC ready for record, ready:" + Integer.toHexString(ready));
				
				IGCService.ReadyStatusListener l = ((GCService)mGCService).getReadyStatusListener();
				if(l != null) l.onChange(ready);
			}
		});
		
		addBleEventListener(IGcConnectivityService.CB_LONG_TERM_EVENT_RESULT, new IBleEventListener() {

			@Override
			public void event(int callbackID, Bundle bundle) {
				LongTermEvent event = (LongTermEvent) bundle.getSerializable(IGcConnectivityService.PARAM_LONG_TERM_EVENT);
				if(event.equals(LongTermEvent.LTEVENT_CAMERA_STATUS)) {
					IGcConnectivityService.OperationEvent opEvent = (IGcConnectivityService.OperationEvent) bundle.getSerializable(IGcConnectivityService.PARAM_OPERATION_EVENT);
					switch(opEvent) {
					case OPEVENT_START_CAPTURING:
						if(!mGCService.getCurrentConnectionMode().equals(IGCService.ConnectionMode.Partial)) {
							break;
						}
						
						if(DEBUG) Log.i(Common.TAG, "[GCServiceWorker] BLE onCapture event, GC start capturing, ready: 0x"+Integer.toHexString(Common.READY_NONE));

						{
							IGCService.ReadyStatusListener l = ((GCService)mGCService).getReadyStatusListener();
							if(l != null) l.onChange(Common.READY_NONE);
						}
						break;
					
					case OPEVENT_COMPLETE_CAPTURING:
						if(!mGCService.getCurrentConnectionMode().equals(IGCService.ConnectionMode.Partial)) {
							break;
						}
						
						int ready = bundle.getInt(IGcConnectivityService.PARAM_READY_BIT);

						if(DEBUG) Log.i(Common.TAG, "[GCServiceWorker] BLE onReady event, GC ready for capture ready: 0x" + Integer.toHexString(ready));
						{
							IGCService.ReadyStatusListener l = ((GCService)mGCService).getReadyStatusListener();
							if(l != null) l.onChange(ready);
						}
						break;
						
					default: // other case is acceptable
					}
				}
			}
		});
		
		addBleEventListener(IGcConnectivityService.CB_LONG_TERM_EVENT_RESULT, new IBleEventListener() {

			@Override
			public void event(int callbackID, Bundle bundle) {
				LongTermEvent event = (LongTermEvent) bundle.getSerializable(IGcConnectivityService.PARAM_LONG_TERM_EVENT);
				if(event.equals(LongTermEvent.LTEVENT_CAMERA_STATUS)) {
					IGcConnectivityService.OperationEvent opEvent = (IGcConnectivityService.OperationEvent) bundle.getSerializable(IGcConnectivityService.PARAM_OPERATION_EVENT);
					switch(opEvent) {
					case OPEVENT_START_RECORDING:
						if(!mGCService.getCurrentConnectionMode().equals(IGCService.ConnectionMode.Partial)) {
							break;
						}
						
						if(DEBUG) Log.i(Common.TAG, "[GCServiceWorker] BLE onRecord event, GC start recording, ready: 0x"+Integer.toHexString(Common.READY_NONE));

						{
							IGCService.ReadyStatusListener l = ((GCService)mGCService).getReadyStatusListener();
							if(l != null) l.onChange(Common.READY_NONE);
						}
						break;
					
					case OPEVENT_COMPLETE_RECORDING:
						if(!mGCService.getCurrentConnectionMode().equals(IGCService.ConnectionMode.Partial)) {
							break;
						}
						
						int ready = bundle.getInt(IGcConnectivityService.PARAM_READY_BIT);

						if(DEBUG) Log.i(Common.TAG, "[GCServiceWorker] BLE onReady event, GC ready for record type: "+bundle.getInt(IGcConnectivityService.PARAM_FILE_TYPE)+", ready:" + Integer.toHexString(ready));

						{
							IGCService.ReadyStatusListener l = ((GCService)mGCService).getReadyStatusListener();
							if(l != null) l.onChange(ready);
						}
						break;
						
					default: // other case is acceptable
					}
				} else if(event.equals(LongTermEvent.LTEVENT_HW_STATUS)) {
					SwitchOnOff powerStatus = (SwitchOnOff) bundle.getSerializable(IGcConnectivityService.PARAM_GC_POWER);
					if(powerStatus != null) {
						if(DEBUG) Log.i(Common.TAG, "[GCServiceWorker] BLE Power status event, status= "+powerStatus);
						
						if(powerStatus.equals(SwitchOnOff.SWITCH_OFF)) {
							onUnexpectedGcShutdown();
						}
						
						IGCService.PowerStateListener powerStateListener = ((GCService)mGCService).getPowerStateChangeListener();
						if(powerStateListener != null) powerStateListener.onPowerOn(powerStatus.equals(SwitchOnOff.SWITCH_ON));
 					}
					
					PlugIO usbStorage = (PlugIO) bundle.getSerializable(IGcConnectivityService.PARAM_USB_STORAGE);
					if(usbStorage != null) {
						if(DEBUG) Log.i(Common.TAG, "[GCServiceWorker] BLE usb storage change event, type: "+usbStorage);
						
						if(usbStorage.equals(PlugIO.PLUG_IN)) {
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
			Log.i(Common.TAG, "[GCServiceWorker] onSocketDisconnectByGc");
			setWifiStatus(Status.Error);
			setLastConnectionError("onSocketDisconnectByGc", NetworkHelper.getWifiErrorRecoveryAction(ConnectionErrorCode.WIFI_DISCONNECT_BY_GC_DISCONNECT_SOCKET.getVal()), ConnectionErrorCode.WIFI_DISCONNECT_BY_GC_DISCONNECT_SOCKET.getVal());
			decideNextConnectionMove();
		} else {
			Log.i(Common.TAG, "[GCServiceWorker] onSocketDisconnectByGc, but wifi status= "+wifiStatus+" so do nothing");
		}
	}
	
	protected void onUnexpectedGcShutdown() {
		final Status wifiStatus = getWifiStatus(); 
		if(wifiStatus.equals(Status.Connected)) {
			Log.i(Common.TAG, "[GCServiceWorker] onUnexpectedGcShutdown");
			setWifiStatus(Status.Error);
			setLastConnectionError("onUnexpectedGcShutdown", NetworkHelper.getWifiErrorRecoveryAction(ConnectionErrorCode.WIFI_DISCONNECT_BY_GC_POWER_OFF.getVal()), ConnectionErrorCode.WIFI_DISCONNECT_BY_GC_POWER_OFF.getVal());
			decideNextConnectionMove();
		} else {
			Log.i(Common.TAG, "[GCServiceWorker] onUnexpectedGcShutdown, but wifi status= "+wifiStatus+" so do nothing");
		}
	}
	
	private void onError(Exception e) {
		Log.e(Common.TAG, "[GCServiceWorker] heart beat wait time: " + (System.currentTimeMillis() - mHeartBeatTime) + "ms");

		if(e != null) {
			Log.e(Common.TAG, "[GCServiceWorker] Error !! " + e.toString());
			Log.e(Common.TAG, Log.getStackTraceString(e));
		}
		else {
			Log.e(Common.TAG, "[GCServiceWorker] Error !! without exception");
			StackTraceElement[] elements = Thread.currentThread().getStackTrace();

			for(StackTraceElement ste : elements)
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
		Log.i(Common.TAG, "[GCServiceWorker] openSocket");
		mConnectBegin = System.currentTimeMillis();
		try {
			if(mGCService.getCurrentConnectionMode() != IGCService.ConnectionMode.Partial) throw new StatusException();

			setSocketStatus(Status.Connecting);

			new Thread(new Runnable() {
				@Override
				public void run() {
					try {
						if(mCommandTXSocket == null) mCommandTXSocket = new Socket();
						
						try {
							mCommandTXSocket.setTcpNoDelay(true);	
						} catch(Exception e) {
							Log.e(Common.TAG, "[GCServiceWorker] Device didn't support TcpNoDelay");
						}
						
						Log.i(Common.TAG, "[GCServiceWorker] mCommandTXSocket sendBufferSize= "+mCommandTXSocket.getSendBufferSize()+"bytes");
						mCommandTXSocket.connect(new InetSocketAddress(addr, TX_PORT), CONNECT_SOCKET_TIMEOUT);
						mCommandChannel = mCommandTXSocket.getOutputStream();

						if(mCommandRXSocket == null) mCommandRXSocket = new Socket();
						mCommandRXSocket.setSoTimeout(COMMAND_NORMAL_RESPONSE_TIMEOUT);
						Log.i(Common.TAG, "[GCServiceWorker] mCommandTXSocket receiveBufferSize= "+mCommandRXSocket.getReceiveBufferSize()+"bytes");
						mCommandRXSocket.connect(new InetSocketAddress(addr, RX_PORT), CONNECT_SOCKET_TIMEOUT);
						mResponseChannel = mCommandRXSocket.getInputStream();

						if(mEventSocket == null) mEventSocket = new Socket();
						mEventSocket.setSoTimeout(HEART_BEAT_TIMEOUT);
						Log.i(Common.TAG, "[GCServiceWorker] mEventSocket receiveBufferSize= "+mEventSocket.getReceiveBufferSize()+"bytes");
						mEventSocket.connect(new InetSocketAddress(addr, EVENT_PORT), CONNECT_SOCKET_TIMEOUT);
						mEventChannel = mEventSocket.getInputStream();

						if(mFileRXSocket == null) mFileRXSocket = new Socket();
						Log.i(Common.TAG, "[GCServiceWorker] mFileRXSocket receiveBufferSize= "+mFileRXSocket.getReceiveBufferSize()+"bytes");
						mFileRXSocket.connect(new InetSocketAddress(addr, FILE_RX_PORT), CONNECT_SOCKET_TIMEOUT);
						mFileChannel = mFileRXSocket.getInputStream();

						if(mThumbnailRXSocket == null) mThumbnailRXSocket = new Socket();
						Log.i(Common.TAG, "[GCServiceWorker] mThumbnailRXSocket receiveBufferSize= "+mThumbnailRXSocket.getReceiveBufferSize()+"bytes");
						mThumbnailRXSocket.connect(new InetSocketAddress(addr, THUMBNAIL_RX_PORT), CONNECT_SOCKET_TIMEOUT);
						mThumbnailChannel = mThumbnailRXSocket.getInputStream();
						
						mHeartBeatTime = System.currentTimeMillis();
						
						mFileDownloadThread = new Thread(mFileDownloadHandler, "FileDownloadThread");

						mThumbnailDownloadThread = new Thread(mThumbnailDownloadHandler, "ThumbnailDownloadThread");

						mCommandThread = new Thread(mCommandHandler, "CommandThread");
						mCommandThread.setPriority(Thread.NORM_PRIORITY+1);
						mCommandThread.start();

						mEventThread = new Thread(mEventHandler, "EventThread");
						mEventThread.setPriority(Thread.NORM_PRIORITY+2);
						
						mCommandQueue.add(new CheckValidationTask(appGuid, PROTOCOL_VERSION, new ValidationCallback() {

							@Override
							public void error(Exception e) {
								callback.error(e);
							}

							@Override
							public void result(byte[] dcGUID, int protocolVersion, final int fwVersion, int bootVersion, byte mcuVersion) {
								if(Arrays.equals(dcGUID, DC_GUID) != true) {
									Log.i(Common.TAG, "[GCServiceWorker] GC connect fail, GUID not match, spend: " + (System.currentTimeMillis() - mConnectBegin) + "ms");

									Exception e = new CommonException("GC GUID not match", Common.ErrorCode.ERR_SYSTEM_ERROR);
									callback.error(e);
								}

								if(protocolVersion != PROTOCOL_VERSION) {
									Log.i(Common.TAG, "[GCServiceWorker] GC connect fail, protocol not match, spend: " + (System.currentTimeMillis() - mConnectBegin) + "ms");

									Exception e = new CommonException("GC Version not match", Common.ErrorCode.ERR_SYSTEM_ERROR);
									callback.error(e);
								}

								synchronized(GCServiceWorkerImp.this) {
									mAddress = addr;
									
									mEventThread.start();
									mFileDownloadThread.start();
									mThumbnailDownloadThread.start();
								}
								
								setSocketStatus(Status.Connected);

								Log.i(Common.TAG, "[GCServiceWorker] GC connected, FW version: "+ fwVersion + " Bootcode version: "+bootVersion+" Mcu version: "+mcuVersion+" spend: " + (System.currentTimeMillis() - mConnectBegin) + "ms");
								callback.done(GCServiceWorkerImp.this);
							}
						}));
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
		Log.i(Common.TAG, "[GCServiceWorker] closeSocket");
		final long begin = System.currentTimeMillis();

		if(getSocketStatus() == Status.Disconnected || getSocketStatus() == Status.Disconnecting) {
			return;
		}
		
		setSocketStatus(Status.Disconnecting);
		
		synchronized(this) {
			mHeartBeatLive = false;
			mHeartBeatDeadCounter.set(0);
			mAddress = null;
			mIsInUpgradeProcess.set(false);
		}

		try {
			if(mCommandTXSocket != null) mCommandTXSocket.close();
		} catch (IOException exception) {
		} finally {
			mCommandTXSocket = null;			
		}

		try {
			if(mCommandRXSocket != null) mCommandRXSocket.close();
		} catch (IOException exception) {
		} finally {
			mCommandRXSocket = null;
		}

		try {
			if(mEventSocket != null) mEventSocket.close();
		} catch (IOException exception) {
		} finally {
			mEventSocket = null;
		}
		
		try {
			if(mFileRXSocket != null) mFileRXSocket.close();
		} catch (IOException exception) {
		} finally {
			mFileRXSocket = null;
		}
		
		try {
			if(mThumbnailRXSocket != null) mThumbnailRXSocket.close();
		} catch (IOException exception) {
		} finally {
			mThumbnailRXSocket = null;
		}
		
		try {
			if(mEventThread != null) {
				mEventThread.interrupt();
				mEventThread.join();
			}
		} catch (Exception exception) {
		}

		try {
			if(mCommandThread != null) {
				mCommandThread.interrupt();
				mCommandThread.join();
			}
		} catch (Exception exception) {
		} finally {
			mCommandThread = null;
		}
		
		try {
			if(mThumbnailDownloadThread != null) {
				mThumbnailDownloadThread.interrupt();
				mThumbnailDownloadThread.join();				
			}
		} catch (Exception exception) {
		} finally {
			mThumbnailDownloadThread = null;
		}
		
		try {
			if(mFileDownloadThread != null) {
				mFileDownloadThread.interrupt();
				mFileDownloadThread.join();
			}
		} catch (Exception exception) {
		} finally {
			mFileDownloadThread = null;
		}
		
		GCTask task;
		while((task = mCommandQueue.poll()) != null) {
			try {
				task.error(e);
			} catch (Exception exception) {
			}
		}
		
		int key = 0;
		BackgroundDownloadFragmentTask backgroundDownloadTask;
		for(int i = 0; i < mFileDownloadTasks.size(); i++) {
			key = mFileDownloadTasks.keyAt(i);
			backgroundDownloadTask = (BackgroundDownloadFragmentTask) mFileDownloadTasks.get(key);
			try {
				Log.i(Common.TAG, "[GCServiceWorker] File download task sequence id= "+backgroundDownloadTask.getSequenceID()+" still in array but socket is disconnected, so callback error");
				backgroundDownloadTask.error(e);
			} catch (Exception exception) {
			}
		}
		mFileDownloadTasks.clear();
		
		for(int i = 0; i < mThumbnailDownloadTasks.size(); i++) {
			key = mThumbnailDownloadTasks.keyAt(i);
			backgroundDownloadTask = (BackgroundDownloadFragmentTask) mThumbnailDownloadTasks.get(key);
			try {
				Log.i(Common.TAG, "[GCServiceWorker] Thumbnail download task sequence id= "+backgroundDownloadTask.getSequenceID()+" still in array but socket is disconnected, so callback error");
				backgroundDownloadTask.error(e);	
			} catch (Exception exception) {
			}
		}
		mThumbnailDownloadTasks.clear();
		
		synchronized(this) {
			mHeartBeatLive = false;
			mHeartBeatDeadCounter.set(0);
			mAddress = null;
			mIsInUpgradeProcess.set(false);
		}

		setSocketStatus(Status.Disconnected);
		Log.i(Common.TAG, "[GCServiceWorker] closed, spend: " + (System.currentTimeMillis() - begin) + "ms");
	}
	
	private final Runnable mCommandHandler = new Runnable() {

		@Override
		public void run() {
			Log.i(Common.TAG, "[GCServiceWorker] command thread begin");

			try {
				IGCService.ICommandCancel cancel = new IGCService.ICommandCancel() {
					@Override
					public boolean isCancel() {
						return mCommandThread.isInterrupted();
					}

					@Override
					public void requestCancel(CancelTask task) {
						try {
							task.setSequenceID(mSequenceIDGenerator.getAndIncrement());
							task.request(mCommandChannel);
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				};

				while(mCommandThread.isInterrupted() == false) {
					GCTask task = mCommandQueue.poll(Long.MAX_VALUE, TimeUnit.SECONDS);

					task.setSequenceID(mSequenceIDGenerator.getAndIncrement());
					
					if(task instanceof UpgradeFirmwareTask) {
						mIsInUpgradeProcess.set(true);
						if(mCommandRXSocket != null) {
							mCommandRXSocket.setSoTimeout(COMMAND_UPGRADE_RESPONSE_TIMEOUT);
						} else {
							Log.w(Common.TAG, "mCommandRXSocket is null");
						}
					} else if (task instanceof GetErrorLogFromGcTask) {
						if(mCommandRXSocket != null) {
							mCommandRXSocket.setSoTimeout(COMMAND_GET_LOG_RESPONSE_TIMEOUT);
						} else {
							Log.w(Common.TAG, "mCommandRXSocket is null");
						}
					} else if (task instanceof DeleteItemsTask) {
						if(mCommandRXSocket != null) {
							mCommandRXSocket.setSoTimeout(COMMAND_DELETE_RESPONSE_TIMEOUT);
						} else {
							Log.w(Common.TAG, "mCommandRXSocket is null");
						}
					}
					
					long requestStartTimestamp = System.currentTimeMillis();
					
					if(task instanceof IBackgroundTask) {
						IBackgroundTask backgroundTask = (IBackgroundTask) task;
						switch(backgroundTask.getChannelType()) {
						case FILE_CHANNEL:
							if(DEBUG) Log.i(Common.TAG, "[GCServiceWorker] insert FileDownloadTask Sequence= "+task.getSequenceID()+" to array");
							mFileDownloadTasks.put(task.getSequenceID(), backgroundTask);
							break;
						case THUMBNAIL_CHANNEL:
							if(DEBUG) Log.i(Common.TAG, "[GCServiceWorker] insert ThumbnailDownloadTask Sequence= "+task.getSequenceID()+" to array");
							mThumbnailDownloadTasks.put(task.getSequenceID(), backgroundTask);
							break;
						default:
							throw new Common.NoImpException();
						}
					}
					
					task.request(mCommandChannel);
					mCommandChannel.flush();
					if(DEBUG) Log.d(Common.TAG,"["+task.getClass().toString()+"] request spends: "+(System.currentTimeMillis() - requestStartTimestamp)+"ms");
					
					if((task instanceof IBackgroundTask) == false) {
						try {
							long responseStartTimestamp = System.currentTimeMillis();
							task.response(mResponseChannel, cancel);
							if(DEBUG) Log.d(Common.TAG,"["+task.getClass().toString()+"] response spends: "+(System.currentTimeMillis() - responseStartTimestamp)+"ms");
							
							if(task instanceof UpgradeFirmwareTask) {
								if(mCommandRXSocket != null) {
									mCommandRXSocket.setSoTimeout(COMMAND_NORMAL_RESPONSE_TIMEOUT);
								} else {
									Log.w(Common.TAG, "mCommandRXSocket is null");
								}
								mIsInUpgradeProcess.set(false);
							} else if(task instanceof GetErrorLogFromGcTask) {
								if(mCommandRXSocket != null) {
									mCommandRXSocket.setSoTimeout(COMMAND_NORMAL_RESPONSE_TIMEOUT);
								} else {
									Log.w(Common.TAG, "mCommandRXSocket is null");
								}
							} else if(task instanceof DeleteItemsTask) {
								if(mCommandRXSocket != null) {
									mCommandRXSocket.setSoTimeout(COMMAND_NORMAL_RESPONSE_TIMEOUT);
								} else {
									Log.w(Common.TAG, "mCommandRXSocket is null");
								}
							}
 						} catch (SocketTimeoutException e) {
							Log.e(Common.TAG, "[GCServiceWorker] mCommandRXSocket Timeout!");
							onError(e);
						}
						
					}
				}

			} catch (InterruptedException e) {
			} catch (Exception e) {
				onError(e);
			}

			Log.i(Common.TAG, "[GCServiceWorker] command thread end");
		}
	};

	private final Runnable mFileDownloadHandler = new Runnable() {
		@Override
		public void run() {
			Log.i(Common.TAG, "[GCServiceWorker] file download thread begin");
			
			try {
				IGCService.ICommandCancel cancel = new IGCService.ICommandCancel() {
					@Override
					public boolean isCancel() {
						return mFileDownloadThread.isInterrupted();
					}

					@Override
					public void requestCancel(CancelTask task) {
						mCommandQueue.add(task);
					}
				};
					
				while(mFileDownloadThread.isInterrupted() == false) {
					ByteBuffer headerBuffer = ByteBuffer.allocate(Protocol.ResponseHeader.MIN_RESPONSE_LENGTH);
					headerBuffer.order(ByteOrder.LITTLE_ENDIAN);

					Protocol.ResponseHeader header = new Protocol.ResponseHeader();
					headerBuffer.position(0);
					NetworkHelper.receive(mFileChannel, headerBuffer, cancel);
					
					if(NetworkHelper.DUMP_STREAM) {
						Log.d(Common.TAG, "  Dump response stream, header " + headerBuffer.remaining() + " bytes in file channel");
						NetworkHelper.dumpBuffer(headerBuffer);
					}
					
					header.mResponseID	= headerBuffer.getInt();
					header.mLength		= headerBuffer.getInt();
					header.mSequenceID	= headerBuffer.getInt();
					header.mFlag		= headerBuffer.getInt();
					
					IBackgroundTask backgroundTask = mFileDownloadTasks.get(header.mSequenceID);
					if(backgroundTask == null) throw new NullPointerException("File download task id does not match");
					backgroundTask.response(header, mFileChannel, cancel);
					mFileDownloadTasks.remove(header.mSequenceID);
					if(DEBUG) Log.i(Common.TAG, "[GCServiceWorker] remove FileDownloadTask Sequence= "+header.mSequenceID+" from array");
				}			
			} catch (InterruptedException e) {
			} catch (Exception e) {
				onError(e);
			}
			
			Log.i(Common.TAG, "[GCServiceWorker] file download thread end");
		}
	};
	
	private final Runnable mThumbnailDownloadHandler = new Runnable() {
		@Override
		public void run() {
			Log.i(Common.TAG, "[GCServiceWorker] thumbnail download thread begin");
			try {
				IGCService.ICommandCancel cancel = new IGCService.ICommandCancel() {
					@Override
					public boolean isCancel() {
						return mThumbnailDownloadThread.isInterrupted();
					}

					@Override
					public void requestCancel(CancelTask task) {
						mCommandQueue.add(task);
					}
				};
					
				while(mThumbnailDownloadThread.isInterrupted() == false) {
					ByteBuffer headerBuffer = ByteBuffer.allocate(Protocol.ResponseHeader.MIN_RESPONSE_LENGTH);
					headerBuffer.order(ByteOrder.LITTLE_ENDIAN);

					Protocol.ResponseHeader header = new Protocol.ResponseHeader();
					headerBuffer.position(0);
					NetworkHelper.receive(mThumbnailChannel, headerBuffer, cancel);
					
					if(NetworkHelper.DUMP_STREAM) {
						Log.d(Common.TAG, "  Dump response stream, header " + headerBuffer.remaining() + " bytes in thumbnail channel");
						NetworkHelper.dumpBuffer(headerBuffer);
					}
					
					header.mResponseID	= headerBuffer.getInt();
					header.mLength		= headerBuffer.getInt();
					header.mSequenceID	= headerBuffer.getInt();
					header.mFlag		= headerBuffer.getInt();
					
					IBackgroundTask backgroundTask = mThumbnailDownloadTasks.get(header.mSequenceID);
					if(backgroundTask == null) throw new NullPointerException("Thumbnail download task id does not match");
					backgroundTask.response(header, mThumbnailChannel, cancel);
					mThumbnailDownloadTasks.remove(header.mSequenceID);
					if(DEBUG) Log.i(Common.TAG, "[GCServiceWorker] remove Thumbnail download task Sequence= "+header.mSequenceID+" from array");
				}			
			} catch (InterruptedException e) {
			} catch (Exception e) {
				onError(e);
			}
			Log.i(Common.TAG, "[GCServiceWorker] thumbnail download thread end");			
		}
	};

	private final Runnable mEventHandler = new Runnable() {
		@Override
		public void run() {
			Log.i(Common.TAG, "[GCServiceWorker] event thread begin");

			try {
				ByteBuffer headerBuffer = ByteBuffer.allocate(Protocol.EventHeader.MIN_EVENT_LENGTH);
				headerBuffer.order(ByteOrder.LITTLE_ENDIAN);

				Protocol.EventHeader header = new Protocol.EventHeader();

				IGCService.ICancel cancel = new IGCService.ICancel() {
					@Override
					public boolean isCancel() {
						return mEventThread.isInterrupted();
					}
				};

				while(mEventThread.isInterrupted() == false) {
					headerBuffer.position(0);

					ByteBuffer bodyBuffer = null;
					try {
						NetworkHelper.receive(mEventChannel, headerBuffer, cancel);
						header.mEventID = headerBuffer.getInt();
						header.mLength = headerBuffer.getInt();
						header.mSequenceID = headerBuffer.getInt();

						int bodySize = header.mLength - Protocol.EventHeader.MIN_EVENT_LENGTH;
						if(bodySize > Protocol.EventHeader.MAX_EVENT_LENGTH || bodySize < 0)
							throw new CommonException("Length of event is not correct", Common.ErrorCode.ERR_SYSTEM_ERROR);

						if(bodySize != 0) {
							bodyBuffer = ByteBuffer.allocate(bodySize);
							bodyBuffer.order(ByteOrder.LITTLE_ENDIAN);
							NetworkHelper.receive(mEventChannel, bodyBuffer, cancel);
						}
					} catch (SocketTimeoutException e) {
						Log.i(Common.TAG, "[GCServiceWorker] heart beat dead event");
						mHeartBeatLive = false;
						if(mHeartBeatDeadCounter.incrementAndGet() >= HEART_BEAT_DEAD_TOLERANCE_THRESHOLD) {
							Log.e(Common.TAG, "[GCServiceWorker] heart beat dead over "+HEART_BEAT_DEAD_TOLERANCE_THRESHOLD+" times");
							onError(e);
						}
						
						IGCService.HeartBeatListener l = ((GCService)mGCService).getHeartBeatListener();
						if(l != null) l.onHeartBeat(mGCService, mHeartBeatLive);

						continue;
					}

					long eventBroadcastStartTimestamp = System.currentTimeMillis();
					synchronized(mEventHandlers) {

						ArrayList<IEventListener> listeners = mEventHandlers.get(header.mEventID);
						if(listeners != null) {
							Log.i(Common.TAG, "[GCServiceWorker] event 0x" + Integer.toHexString(header.mEventID) + " (" + header.mSequenceID + ")");

							int position = (bodyBuffer != null)? bodyBuffer.position(): -1;
							for(IEventListener listener : listeners) {
								if(bodyBuffer != null) bodyBuffer.position(position);
								listener.event(header.mEventID, bodyBuffer);
							}
						}
						else Log.w(Common.TAG, "[GCServiceWorker] event 0x" + Integer.toHexString(header.mEventID) + " (" + header.mSequenceID + ") no listener");
					}
					if(DEBUG) Log.i(Common.TAG, "[GCServiceWorker] event 0x"+Integer.toHexString(header.mEventID)+" broadcast spent "+(System.currentTimeMillis()-eventBroadcastStartTimestamp)+"ms");
				}

			} catch (InterruptedException e) {
			} catch (Exception e) {
				onError(e);
			}

			Log.i(Common.TAG, "[GCServiceWorker] event thread end");
		}
	};
}
