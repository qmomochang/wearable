package com.htc.gc.internal.v2;

import java.net.URI;
import java.util.Calendar;
import java.util.HashMap;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;

import com.htc.gc.connectivity.interfaces.IGcConnectivityServiceBase.LongTermEvent;
import com.htc.gc.connectivity.interfaces.IGcConnectivityServiceBase.PlugIO;
import com.htc.gc.connectivity.interfaces.IGcConnectivityServiceBase.Result;
import com.htc.gc.connectivity.v3.interfaces.IGcConnectivityService;
import com.htc.gc.connectivity.v3.interfaces.IGcConnectivityService.Operation;
import com.htc.gc.interfaces.Common;
import com.htc.gc.interfaces.Common.BleCommandException;
import com.htc.gc.interfaces.Common.Filter;
import com.htc.gc.interfaces.Common.ICancelable;
import com.htc.gc.interfaces.Common.InvalidArgumentsException;
import com.htc.gc.interfaces.Common.NoImpException;
import com.htc.gc.interfaces.Common.OperationCallback;
import com.htc.gc.interfaces.Common.RequestCallback;
import com.htc.gc.interfaces.Common.StatusException;
import com.htc.gc.interfaces.Common.UploadCallback;
import com.htc.gc.interfaces.IDeviceController;
import com.htc.gc.interfaces.IDeviceItem;
import com.htc.gc.interfaces.IGCService.ConnectionMode;
import com.htc.gc.interfaces.IMediaItem;
import com.htc.gc.internal.DeviceItem;
import com.htc.gc.internal.v2.IMediator.IBleEventListener;

class GCDeviceController implements IDeviceController {
	protected static final boolean DEBUG = Common.DEBUG;

	private final IMediator mService;
	
	private ModeChangeListener mModeChangeListener;
	private BatteryLevelChangeListener mBatteryLevelChangeListener;
	private SpaceChangeListener mSpaceChangeListener;
	private RequestGpsInfoChangeListener mRequestGpsInfoChangeListener;
	private SdCardStatusListener mSdCardStatusListener;
	private TemperatureStatusListener mTemperatureStatusListener;
	private UsbStorageStatusListener mUsbStorageStatusListener;
	private PocketModeChangeListener mPocketModeChangeListener;
	private CameraModeChangeListener mCameraModeChangeListener;
	private SMSListener mSMSListener;
	private AllFwVersionListener mAllFwVersionListener;
	private FwUpdateResultListener mFwUpdateResultListener;
	private LTECampingStatusListener mLTECampingStatusListener;
	
	private Handler mHandler;
	
	GCDeviceController(IMediator service) {
		mService = service;

		mService.addBleEventListener(IGcConnectivityService.CB_LONG_TERM_EVENT_RESULT, new IBleEventListener() {

			@Override
			public void event(int callbackID, Bundle bundle) {
				LongTermEvent event = (LongTermEvent) bundle.getSerializable(IGcConnectivityService.PARAM_LONG_TERM_EVENT);
				if(event.equals(LongTermEvent.LTEVENT_CAMERA_STATUS)) {
					IGcConnectivityService.OperationEvent opEvent = (IGcConnectivityService.OperationEvent) bundle.getSerializable(IGcConnectivityService.PARAM_OPERATION_EVENT);
					switch(opEvent) {
					case OPEVENT_COMPLETE_CAPTURING:
					case OPEVENT_COMPLETE_RECORDING:
						if(DEBUG) Log.i(Common.TAG, "[GCController] BLE space change event");
						HashMap<IMediaItem.Type, Integer> remainingSpace = new HashMap<IMediaItem.Type, Integer>(); 
						remainingSpace.put(IMediaItem.Type.Photo, bundle.getInt(IGcConnectivityService.PARAM_IMAGE_REMAIN_COUNT));
						remainingSpace.put(IMediaItem.Type.Video, bundle.getInt(IGcConnectivityService.PARAM_VIDEO_REMAIN_SECOND));
						remainingSpace.put(IMediaItem.Type.TimeLapse, bundle.getInt(IGcConnectivityService.PARAM_TIME_LAPSE_REMAIN_COUNT));
						remainingSpace.put(IMediaItem.Type.SlowMotion, bundle.getInt(IGcConnectivityService.PARAM_SLOW_MOTION_REMAIN_SECOND));
						if(mSpaceChangeListener != null) mSpaceChangeListener.onSpaceChange(GCDeviceController.this, remainingSpace, 0);						
						break;
					default: // other case is acceptable
					}
				} else if(event.equals(LongTermEvent.LTEVENT_CAMERA_ERROR)) {
					int index = bundle.getInt(IGcConnectivityService.PARAM_CAMERA_ERROR_INDEX);
					int code = bundle.getInt(IGcConnectivityService.PARAM_CAMERA_ERROR_CODE);
					
					switch(index) {
					case Common.ERR_MODULE_BATTERY:
						if(!mService.getCurrentConnectionMode().equals(ConnectionMode.Partial)) {
							break;
						}
						if(DEBUG) Log.i(Common.TAG, "[GCController] BLE overheat event");
						if(mTemperatureStatusListener != null) mTemperatureStatusListener.onOverHeatAndShutdownInOneMin(GCDeviceController.this);
						break;
													
					case Common.ERR_MODULE_CMOS:
						if(DEBUG) Log.i(Common.TAG, "[GCController] BLE overheat event");
						if(mTemperatureStatusListener != null) mTemperatureStatusListener.onOverHeatAndShutdownInOneMin(GCDeviceController.this);
						break;
						
					case Common.ERR_MODULE_CARD:
						if(code == Common.ErrorCode.ERR_SD_CAPACITY_UNKNOWN.getVal() || code == Common.ErrorCode.ERR_SYSTEM_ERROR.getVal()) {
							if(mService.getCurrentConnectionMode().equals(ConnectionMode.Full)) { // don't send when full connected
								break;
							}
							if(DEBUG) Log.i(Common.TAG, "[GCController] BLE SD card wrong format event");
							if(mSdCardStatusListener != null) mSdCardStatusListener.onWrongFormat(GCDeviceController.this);
							break;
						}
						break;
					case Common.ERR_MODULE_NOCARD:
						if(code == Common.ErrorCode.ERR_NO_SD_CARD.getVal()) {
							if(mService.getCurrentConnectionMode().equals(ConnectionMode.Full)) { // don't send when full connected
								break;
							}
							if(DEBUG) Log.i(Common.TAG, "[GCController] BLE no SD card event");
							if(mSdCardStatusListener != null) mSdCardStatusListener.onNoSdCard(GCDeviceController.this);
							break;
						}
						break;
					}
				} else if(event.equals(LongTermEvent.LTEVENT_REQUEST_GPS_INFO)) {
					IGcConnectivityService.SwitchOnOff onOff = (IGcConnectivityService.SwitchOnOff) bundle.getSerializable(IGcConnectivityService.PARAM_REQUEST_GPS_INFO_SWITCH);
					if(DEBUG) Log.i(Common.TAG, "[GCController] BLE request gps info change event");
					switch(onOff) {
					case SWITCH_OFF:
						if(mRequestGpsInfoChangeListener != null) mRequestGpsInfoChangeListener.onRequestGpsInfoChange(GCDeviceController.this, false);
						break;
					case SWITCH_ON:
						if(mRequestGpsInfoChangeListener != null) mRequestGpsInfoChangeListener.onRequestGpsInfoChange(GCDeviceController.this, true);
						break;
					default:
						Log.e(Common.TAG, "[GCController] Invalid BLE request gps info change event setting");
					}
				} else if(event.equals(LongTermEvent.LTEVENT_HW_STATUS)) {
					IGcConnectivityService.MCUBatteryLevel batteryLevel = (IGcConnectivityService.MCUBatteryLevel) bundle.getSerializable(IGcConnectivityService.PARAM_BATTERY_LEVEL);
					PlugIO adapterPlugin = (PlugIO) bundle.getSerializable(IGcConnectivityService.PARAM_ADAPTER_PLUGIN);
					PlugIO usbStorage = (PlugIO) bundle.getSerializable(IGcConnectivityService.PARAM_USB_STORAGE);
					
					if (batteryLevel == null) {
						batteryLevel = GCDeviceInfoManager.getCurrentBatteryLevel();
					} else {
						GCDeviceInfoManager.setCurrentBatteryLevel(batteryLevel);
					}
					
					if(adapterPlugin != null && batteryLevel != null) {
						if(DEBUG) Log.i(Common.TAG, "[GCController] BLE battery level change event, level: "+batteryLevel+" type: "+adapterPlugin.toString());
						if(mBatteryLevelChangeListener != null) mBatteryLevelChangeListener.onBatteryLevelChange(GCDeviceController.this, adapterPlugin.equals(PlugIO.PLUG_IN), getMappedBatteryLevel(batteryLevel));
					} else {
						if(DEBUG) Log.i(Common.TAG, "[GCController] BLE battery level change event, cannot callback, adapterPlugin=" + adapterPlugin + ", batteryLevel=" + batteryLevel);
					}
					
					if(usbStorage != null) {
						if(DEBUG) Log.i(Common.TAG, "[GCController] BLE usb storage change event, type: "+usbStorage);
						if(mUsbStorageStatusListener != null) mUsbStorageStatusListener.onMount(GCDeviceController.this, usbStorage.equals(PlugIO.PLUG_IN));
					}
				} else if (event.equals(LongTermEvent.LTEVENT_CAMERA_MODE)) {
					IGcConnectivityService.CameraMode sourceCameraMode = (IGcConnectivityService.CameraMode) bundle.getSerializable(IGcConnectivityService.PARAM_CAMERA_MODE);
					if (sourceCameraMode != null) {
						try {
							CameraMode cameraMode = CameraMode.getKey(sourceCameraMode.getMode());
							
							if(DEBUG) Log.i(Common.TAG, "[GCController] BLE camera mode change event, mode: " + cameraMode);
							
							if (mCameraModeChangeListener != null) mCameraModeChangeListener.onModeChange(GCDeviceController.this, cameraMode);
						} catch (NoImpException e) {
							if(DEBUG) Log.w(Common.TAG, "[GCController] BLE camera mode change event, cannot get camera mode");
							e.printStackTrace();
						}
					} else {
						if(DEBUG) Log.w(Common.TAG, "[GCController] BLE camera mode change event, got invalid camera mode");
					}
				} else if (event.equals(LongTermEvent.LTEVENT_SMS_RECEIVED)) {
					String dateTime = bundle.getString(IGcConnectivityService.PARAM_SMS_DATE_TIME);
					String phoneNumber = bundle.getString(IGcConnectivityService.PARAM_SMS_PHONE_NUMBER);
					String messageContent = bundle.getString(IGcConnectivityService.PARAM_SMS_MESSAGE_CONTENT);
					
					if(DEBUG) Log.i(Common.TAG, "[GCController] received sms, date time:" + dateTime + ", phoneNumber:" + phoneNumber + ", messageContent:" + messageContent);
					if (mSMSListener != null) mSMSListener.onReceived(dateTime, phoneNumber, messageContent);
				} else if (event.equals(LongTermEvent.LTEVENT_LTE_CAMPING_STATUS)) {
					IGcConnectivityService.LTECampingStatus sourceLTECampingStatus = (IGcConnectivityService.LTECampingStatus) bundle.getSerializable(IGcConnectivityService.PARAM_LTE_CAMPING_STATUS);
					if (sourceLTECampingStatus != null) {
						try {
							LTECampingStatus lteCampingStatus = LTECampingStatus.getKey(sourceLTECampingStatus.getStatus());
							
							if(DEBUG) Log.i(Common.TAG, "[GCController] BLE lte camping status change event, status: " + lteCampingStatus);
							
							if (mLTECampingStatusListener != null) mLTECampingStatusListener.onChange(lteCampingStatus);
						} catch (NoImpException e) {
							if(DEBUG) Log.w(Common.TAG, "[GCController] BLE lte camping status change event, cannot get status");
							e.printStackTrace();
						}
					} else {
						if(DEBUG) Log.w(Common.TAG, "[GCController] BLE lte camping status change event, got invalid status");
					}
				}

			}
			
		});
		
		mService.addBleEventListener(IGcConnectivityService.CB_GET_ALL_FW_VERSION_RESULT, new IBleEventListener() {

			@Override
			public void event(int callbackID, Bundle bundle) {
				Result result = (Result) bundle.getSerializable(IGcConnectivityService.PARAM_RESULT);
				if (result.equals(Result.RESULT_SUCCESS)) {
					String a12Version = bundle.getString(IGcConnectivityService.PARAM_A12_FW_VERSION);		
					String modemVersion = bundle.getString(IGcConnectivityService.PARAM_MOD_FW_VERSION);
					String mcuVersion = bundle.getString(IGcConnectivityService.PARAM_MCU_FW_VERSION);
					
					if (mAllFwVersionListener != null) mAllFwVersionListener.onGetVersion(a12Version, modemVersion, mcuVersion);
				}
				
			}
			
		});
		
		mService.addBleEventListener(IGcConnectivityService.CB_FWUPDATE_RESULT, new IBleEventListener() {
			
			@Override
			public void event(int callbackID, Bundle bundle) {
				Result result = (Result) bundle.getSerializable(IGcConnectivityService.PARAM_RESULT);
				if (mFwUpdateResultListener != null) mFwUpdateResultListener.onResult(result == Result.RESULT_SUCCESS);
			}
		});
	
		mHandler = new Handler(Looper.getMainLooper());
	}

	@Override
	public void getSpaceInfo(final SpaceInfoCallback callback) throws Exception {
		if(DEBUG) Log.i(Common.TAG, "[GCController] getSpaceInfo");

		if(callback == null) throw new NullPointerException();
		
		switch(mService.getCurrentConnectionMode()) {
		case Full:
		case Partial:
			mHandler.post(new Runnable(){

				@Override
				public void run() {
					DeviceItem device = (DeviceItem)mService.getTargetDevice();
					if(!mService.getConnectivityService().gcSetOperation(device.getDevice(), Operation.OPERATION_GET_FREE_SPACE)) {
						callback.error(new BleCommandException());
					} else {
						mService.addBleCallback(GCDeviceController.this, IGcConnectivityService.CB_SET_OPERATION_RESULT, callback);
					}
				}
				
			});
			break;
			
		default:
			throw new StatusException();
		}
	}

	@Override
	public void getBatteryInfo(final BatteryInfoCallback callback) throws Exception {
		if(DEBUG) Log.i(Common.TAG, "[GCController] getBatteryInfo");

		if(callback == null) throw new NullPointerException();
		
		switch(mService.getCurrentConnectionMode()) {
		case Full:
		case Partial:
			mHandler.post(new Runnable(){

				@Override
				public void run() {
					final DeviceItem device = (DeviceItem)mService.getTargetDevice();
					if(!mService.getConnectivityService().gcGetHwStatus(device.getDevice())) {
						callback.error(new BleCommandException());
					} else {
						mService.addBleCallback(GCDeviceController.this, IGcConnectivityService.CB_GET_HW_STATUS_RESULT, callback);
					}
				}
				
			});
			break;
			
		default:
			throw new StatusException();
		}
	}

	@Override
	public void getMode(final ModeCallback callback) throws Exception {
		if(DEBUG) Log.i(Common.TAG, "[GCController] getMode");

		if(callback == null) throw new NullPointerException();
		if(mService.getCurrentConnectionMode() != ConnectionMode.Full) throw new StatusException();

		throw new NoImpException();
	}

	@Override
	public void setMode(final Common.Mode mode, final OperationCallback callback) throws Exception {
		if(DEBUG) Log.i(Common.TAG, "[GCController] setMode '" + mode.toString() + "'");

		if(callback == null) throw new NullPointerException();
		if(mService.getCurrentConnectionMode() != ConnectionMode.Full) throw new StatusException();

		throw new NoImpException();
	}
	
	@Override
	public void getDRStatus(final StatusCallback callback) throws Exception {
		if(DEBUG) Log.i(Common.TAG, "[GCController] getDRStatus");
		
		if(callback == null) throw new NullPointerException();
		
		switch(mService.getCurrentConnectionMode()) {
		case Full:
		case Partial:
			DeviceItem device = (DeviceItem)mService.getTargetDevice();
			if(!mService.getConnectivityService().gcSetOperation(device.getDevice(), Operation.OPERATION_GET_DR_STATUS)) {
				throw new BleCommandException();
			} else {
				mService.addBleCallback(GCDeviceController.this, IGcConnectivityService.CB_SET_OPERATION_RESULT, callback);
			}
			break;
		default:
			throw new Common.StatusException();
		}
		
	}
	
	@Override
	public void formatSDCard(final SDCardFormatType type, final OperationCallback callback) throws Exception {
		if(DEBUG) Log.i(Common.TAG, "[GCController] formatSDCard!!!!!!!!!!!!");
		
		if(callback == null) throw new NullPointerException();
		if(mService.getCurrentConnectionMode() != ConnectionMode.Full) throw new StatusException();
		
		throw new NoImpException();
	}
	
	@Override
	public void CheckSDCard(SDChangeCallback callback) throws Exception {
		if(DEBUG) Log.i(Common.TAG, "[GCController] CheckSDCard");
		if(callback == null) throw new NullPointerException();
		if(mService.getCurrentConnectionMode() != ConnectionMode.Full) throw new StatusException();
		
		throw new NoImpException();
	}
	
	@Override
	public void getFileCountInStorage(Filter type, final StorageFileCountCallback callback) throws Exception {
		if(DEBUG) Log.i(Common.TAG, "[GCController] getFileCountInStorage");
		
		if(callback == null) throw new NullPointerException();
		if(mService.getCurrentConnectionMode() != ConnectionMode.Full) throw new StatusException();
		
		throw new NoImpException();
	}
	
	@Override
	public void getSpeakerMode(SpeakerModeCallback callback) throws Exception {
		if(DEBUG) Log.i(Common.TAG, "[GCController] getSpeakerMode");
		
		if(callback == null) throw new NullPointerException();
		if(mService.getCurrentConnectionMode() != ConnectionMode.Full) throw new StatusException();
		
		throw new NoImpException();
	}
	
	@Override
	public void setSpeakerMode(SpeakerMode mode, OperationCallback callback) throws Exception {
		if(DEBUG) Log.i(Common.TAG, "[GCController] setSpeakerMode");
		
		if(callback == null) throw new NullPointerException();
		if(mService.getCurrentConnectionMode() != ConnectionMode.Full) throw new StatusException();
		
		throw new NoImpException();
	}
	
	@Override
	public void getDoubleClickMode(DoubleClickModeCallback callback) throws Exception {
		if(DEBUG) Log.i(Common.TAG, "[GCController] getDoubleClickMode");
		
		if(callback == null) throw new NullPointerException();
		if(mService.getCurrentConnectionMode() != ConnectionMode.Full) throw new StatusException();
		
		throw new NoImpException();
	}
	
	@Override
	public void setDoubleClickMode(DoubleClickMode mode, OperationCallback callback) throws Exception {
		if(DEBUG) Log.i(Common.TAG, "[GCController] setDoubleClickMode");
		
		if(callback == null) throw new NullPointerException();
		if(mService.getCurrentConnectionMode() != ConnectionMode.Full) throw new StatusException();
		
		throw new NoImpException();
	}
	
	@Override
	public void getVideoRecordButtonConfig(VideoRecordButtonConfigCallback callback) throws Exception {
		if(DEBUG) Log.i(Common.TAG, "[GCController] getVideoRecordButtonConfig");
		
		if(callback == null) throw new NullPointerException();
		if(mService.getCurrentConnectionMode() != ConnectionMode.Full) throw new StatusException();
		
		throw new NoImpException();
	}
	
	@Override
	public void setVideoRecordButtonConfig(VideoRecBtnConfig config, OperationCallback callback) throws Exception {
		if(DEBUG) Log.i(Common.TAG, "[GCController] setVideoRecordButtonConfig");
		
		if(callback == null) throw new NullPointerException();
		if(mService.getCurrentConnectionMode() != ConnectionMode.Full) throw new StatusException();
		
		throw new NoImpException();
	}
	
	@Override
	public ICancelable uploadFile(URI srcFile, String destPath, UploadCallback callback) throws Exception {
		if(DEBUG) Log.i(Common.TAG, "[GCController] uploadFile");
		
		if(callback == null) throw new NullPointerException();
		if(mService.getCurrentConnectionMode() != ConnectionMode.Full) throw new StatusException();
		
		throw new NoImpException();
	}
	@Override
	public void upgradeFirmware(byte selectFirmwareFlag, int bootCodeVersion, int mainCodeVersion, int mcuVersion, int bleVersion, OperationCallback callback) throws Exception {
		if(DEBUG) Log.i(Common.TAG, "[GCController] upgradeFirmware");
		
		if(callback == null) throw new NullPointerException();
		if(mService.getCurrentConnectionMode() != ConnectionMode.Full) throw new StatusException();
		
		throw new NoImpException();
	}
	
	@Override
	public void	getFirmwareVersion(FirmwareVersionCallback callback) throws Exception {
		if(DEBUG) Log.i(Common.TAG, "[GCController] getFirmwareVersion");
		
		if(callback == null) throw new NullPointerException();
		if(mService.getCurrentConnectionMode() != ConnectionMode.Full) throw new StatusException();
		
		throw new NoImpException();
	}
	
	@Override
	public void triggerFirmwareUpdate(boolean updateA12, boolean updateModem, boolean updateMCU, String firmwareVersion, OperationCallback callback) throws Exception {
		if(DEBUG) Log.i(Common.TAG, "[GCController] triggerFirmwareUpdate");
		
		if(callback == null) throw new NullPointerException();
		if(mService.getCurrentConnectionMode() != ConnectionMode.Full) throw new StatusException();
		
		IGcConnectivityService conn = mService.getConnectivityService();
		DeviceItem device = (DeviceItem)mService.getTargetDevice();
		if (!conn.gcTriggerFWUpdate(device.getDevice(), updateA12, updateModem, updateMCU, firmwareVersion)) {
			throw new BleCommandException();
		} else {
			mService.addBleCallback(this, IGcConnectivityService.CB_TRIGGER_FWUPDATE_RESULT, callback);
		}
	}
	
	@Override
	public void getStorageInUse(StorageInUseCallback callback) throws Exception {
		if(DEBUG) Log.i(Common.TAG, "[GCController] getStorageInUse");
		
		if(callback == null) throw new NullPointerException();
		if(mService.getCurrentConnectionMode() != ConnectionMode.Full) throw new StatusException();
		
		throw new NoImpException();
	}
	
	@Override
	public void getAutoLevelStatus(AutoLevelStatusCallback callback) throws Exception {
		if(DEBUG) Log.i(Common.TAG, "[GCController] getAutoLevelStatus");
		
		if(callback == null) throw new NullPointerException();
		if(mService.getCurrentConnectionMode() != ConnectionMode.Full) throw new StatusException();
		
		throw new NoImpException();
	}
	
	@Override
	public void setAutoLevelStatus(AutoLevelStatus status, OperationCallback callback) throws Exception {
		if(DEBUG) Log.i(Common.TAG, "[GCController] setAutoLevelStatus");
		
		if(callback == null) throw new NullPointerException();
		if(mService.getCurrentConnectionMode() != ConnectionMode.Full) throw new StatusException();
		
		throw new NoImpException();	
	}
	
	@Override
	public void getUpsideDownStatus(UpsideDownStatusCallback callback) throws Exception {
		if(DEBUG) Log.i(Common.TAG, "[GCController] getUpsideDownStatus");
		
		if(callback == null) throw new NullPointerException();
		if(mService.getCurrentConnectionMode() != ConnectionMode.Full) throw new StatusException();
		
		throw new NoImpException();
	}
	
	@Override
	public void setUpsideDownStatus(UpsideDownStatus status, OperationCallback callback) throws Exception {
		if(DEBUG) Log.i(Common.TAG, "[GCController] setUpsideDownStatus");
		
		if(callback == null) throw new NullPointerException();
		if(mService.getCurrentConnectionMode() != ConnectionMode.Full) throw new StatusException();
		
		throw new NoImpException();
	}
	
	@Override
	public void getGcGpsStatus(GcGpsStatusCallback callback) throws Exception {
		if(DEBUG) Log.i(Common.TAG, "[GCController] getGcGpsStatus");
		
		if(callback == null) throw new NullPointerException();
		if(mService.getCurrentConnectionMode() != ConnectionMode.Full) throw new StatusException();
		
		throw new NoImpException();		
	}

	@Override
	public void setGcGpsStatus(GcGpsStatus status, OperationCallback callback)
			throws Exception {
		if(DEBUG) Log.i(Common.TAG, "[GCController] setGcGpsStatus");
		
		if(callback == null) throw new NullPointerException();
		if(mService.getCurrentConnectionMode() != ConnectionMode.Full) throw new StatusException();
		
		throw new NoImpException();
	}

	@Override
	public void getUsePhoneGpsSetting(UsePhoneGpsSettingCallback callback)
			throws Exception {
		if(DEBUG) Log.i(Common.TAG, "[GCController] getUsePhoneGpsSetting");
		
		if(callback == null) throw new NullPointerException();
		if(mService.getCurrentConnectionMode() != ConnectionMode.Full) throw new StatusException();
		
		throw new NoImpException();
	}

	@Override
	public void setUsePhoneGpsSetting(UsePhoneGpsSetting setting, OperationCallback callback) throws Exception {
		if(DEBUG) Log.i(Common.TAG, "[GCController] setUsePhoneGpsSetting");
		
		if(callback == null) throw new NullPointerException();
		if(mService.getCurrentConnectionMode() != ConnectionMode.Full) throw new StatusException();
		
		throw new NoImpException();
	}
	
	@Override
	public void updateGpsInfo(Calendar calendar, double longitude, double latitude, double altitude, OperationCallback callback) throws Exception {
		if(DEBUG) Log.i(Common.TAG, "[GCController] updateGpsInfo");
		
		if(callback == null) throw new NullPointerException();
		
		switch(mService.getCurrentConnectionMode()) {
		case Full:
		case Partial:
			DeviceItem device = (DeviceItem) mService.getTargetDevice();
			if(!mService.getConnectivityService().gcSetGpsInfo(device.getDevice(), calendar, longitude, latitude, altitude)) {
				throw new BleCommandException();
			} else {
				mService.addBleCallback(GCDeviceController.this, IGcConnectivityService.CB_SET_GPS_INFO_RESULT, callback);
			}
			break;
		default:
			throw new Common.StatusException();
		}
	}
	
	@Override
	public void setDeviceName(final IDeviceItem device, String name, final OperationCallback callback) throws Exception {
		if(DEBUG) Log.i(Common.TAG, "[GCController] setDeviceName");
		if(callback == null) throw new NullPointerException();
		if(name.length() > IDeviceController.MAX_DEVICE_NAME_LEN) throw new InvalidArgumentsException("Device name length should <= "+IDeviceController.MAX_DEVICE_NAME_LEN);
		
		switch(mService.getCurrentConnectionMode()) {
		case Full:
		case Partial:
			final DeviceItem deviceItem = (DeviceItem) device;
			if(!mService.getConnectivityService().gcSetName(deviceItem.getDevice(), name)) {
				throw new BleCommandException();
			} else {
				mService.addBleCallback(GCDeviceController.this, IGcConnectivityService.CB_SET_NAME_RESULT, new OperationCallback() {

					@Override
					public void error(Exception e) {
						callback.error(e);
					}

					@Override
					public void done(Object that) {
						if(!mService.getConnectivityService().gcGetName(deviceItem.getDevice())) {
							callback.error(new BleCommandException());
						} else {
							mService.addBleCallback(GCDeviceController.this, IGcConnectivityService.CB_GET_NAME_RESULT, new CameraNameCallback() {
								
								@Override
								public void error(Exception e) {
									callback.error(e);
								}
								
								@Override
								public void result(IDeviceController that, String name) {
									deviceItem.setDeviceName(name);
									callback.done(that);
								}
							});
						}
					}
				});
			}
			break;
		default:
			throw new Common.StatusException();
		}
	}
	
	@Override
	public void resetSystem(final OperationCallback callback) throws Exception {
		if(DEBUG) Log.i(Common.TAG, "[GCController] resetSystem");
		
		if(callback == null) throw new NullPointerException();
		if(mService.getCurrentConnectionMode() != ConnectionMode.Full) throw new StatusException();
		
		throw new NoImpException();
	}
	
	@Override
	public void getErrorLogFromGC(DebugLogType type, boolean autoDelete, final GetErrorLogFromGcCallback callback) throws Exception {
		if(DEBUG) Log.i(Common.TAG, "[GCController] getErrorLogFromGC");
		
		if(callback == null) throw new NullPointerException();
		if(mService.getCurrentConnectionMode() != ConnectionMode.Full) throw new StatusException();
		
		throw new NoImpException();
	}
	
	@Deprecated
	@Override
	public void getBtMacAddress(GetBtMacAddressCallback callback) throws Exception {
		if(DEBUG) Log.i(Common.TAG, "[GCController] getBtMacAddress");
		
		if(callback == null) throw new NullPointerException();
		if(mService.getCurrentConnectionMode() != ConnectionMode.Full) throw new StatusException();
		
		throw new NoImpException();
	}
	
	@Override
	public void getSerialNumber(GetSerialNumberCallback callback) throws Exception {
		if(DEBUG) Log.i(Common.TAG, "[GCController] getSerialNumber");
		
		if(callback == null) throw new NullPointerException();
		if(mService.getCurrentConnectionMode() != ConnectionMode.Full) throw new StatusException();
		
		throw new NoImpException();
	}
	
	@Override
	public void getCountryCode(GetCountryCodeCallback callback) throws Exception {
		if(DEBUG) Log.i(Common.TAG, "[GCController] getCountryCode");
		
		if(callback == null) throw new NullPointerException();
		if(mService.getCurrentConnectionMode() != ConnectionMode.Full) throw new StatusException();
		
		throw new NoImpException();
	}
	
	@Override
	public void setAutoPowerOffTimeThisBootUp(short seconds, OperationCallback callback) throws Exception {
		if(DEBUG) Log.i(Common.TAG, "[GCController] setAutoPowerOffTimeThisBootUp");
		
		if(callback == null) throw new NullPointerException();
		if(mService.getCurrentConnectionMode() != ConnectionMode.Full) throw new StatusException();
		
		throw new NoImpException();
	}
	
	@Override
	public void getPowerSavingMode(GetPowerSaveModeCallback callback) throws Exception {
		if(DEBUG) Log.i(Common.TAG, "[GCController] getPowerSaveMode");
		
		if(callback == null) throw new NullPointerException();
		if(mService.getCurrentConnectionMode() != ConnectionMode.Full) throw new StatusException();
		
		throw new NoImpException();
	}

	@Override
	public void setPowerSavingMode(PowerSaveMode mode, OperationCallback callback) throws Exception {
		if(DEBUG) Log.i(Common.TAG, "[GCController] setPowerSaveMode");
		
		if(callback == null) throw new NullPointerException();
		if(mService.getCurrentConnectionMode() != ConnectionMode.Full) throw new StatusException();
		
		throw new NoImpException();
	}
	
	@Override
	public void getDebugLogEnableSetting(GetDebugLogSettingCallback callback) throws Exception {
		if(DEBUG) Log.i(Common.TAG, "[GCController] getDebugLogEnableSetting");
		
		if(callback == null) throw new NullPointerException();
		if(mService.getCurrentConnectionMode() != ConnectionMode.Full) throw new StatusException();
		
		throw new NoImpException();
	}

	@Override
	public void setDebugLogEnableSetting(DebugLogSetting setting, OperationCallback callback) throws Exception {
		if(DEBUG) Log.i(Common.TAG, "[GCController] setDebugLogEnableSetting");
		
		if(callback == null) throw new NullPointerException();
		if(mService.getCurrentConnectionMode() != ConnectionMode.Full) throw new StatusException();
		
		throw new NoImpException();
	}
	
	@Override
	public void getFakeShotSetting(GetFakeShotSettingCallback callback) throws Exception {
		if(DEBUG) Log.i(Common.TAG, "[GCController] getFakeShotSetting");
		
		if(callback == null) throw new NullPointerException();
		if(mService.getCurrentConnectionMode() != ConnectionMode.Full) throw new StatusException();
		
		throw new NoImpException();
	}

	@Override
	public void setFakeShotSetting(FakeShotSetting setting, OperationCallback callback) throws Exception {
		if(DEBUG) Log.i(Common.TAG, "[GCController] setFakeShotSetting");
		
		if(callback == null) throw new NullPointerException();
		if(mService.getCurrentConnectionMode() != ConnectionMode.Full) throw new StatusException();
		
		throw new NoImpException();
	}

	@Override
	public void getGripShotSetting(GetGripShotSettingCallback callback) throws Exception {
		if(DEBUG) Log.i(Common.TAG, "[GCController] getGripShotSetting");
		
		if(callback == null) throw new NullPointerException();
		if(mService.getCurrentConnectionMode() != ConnectionMode.Full) throw new StatusException();
		
		throw new NoImpException();
	}

	@Override
	public void setGripShotSetting(GripShotSetting setting, OperationCallback callback) throws Exception {
		if(DEBUG) Log.i(Common.TAG, "[GCController] setGripShotSetting");
		
		if(callback == null) throw new NullPointerException();
		if(mService.getCurrentConnectionMode() != ConnectionMode.Full) throw new StatusException();
		
		throw new NoImpException();
	}
	
	@Override
	public void setGcToOobeMode(final RequestCallback callback) throws Exception {
		if(DEBUG) Log.i(Common.TAG, "[GCController] setGcToOobeMode");
		
		if(callback == null) throw new NullPointerException();
		if(mService.getCurrentConnectionMode() != ConnectionMode.Full) throw new StatusException();
		
		throw new NoImpException();
	}
	
	@Override
	public void setDeviceTime(Calendar time, OperationCallback callback) throws Exception {
		if(DEBUG) Log.i(Common.TAG, "[GCController] setDeviceTime");
		
		if(callback == null) throw new NullPointerException();
		
		switch(mService.getCurrentConnectionMode()) {
		case Full:
		case Partial:
			DeviceItem device = (DeviceItem)mService.getTargetDevice();
			if(!mService.getConnectivityService().gcSetDateTime(device.getDevice(), time)) {
				throw new BleCommandException();
			} else {
				mService.addBleCallback(GCDeviceController.this, IGcConnectivityService.CB_SET_DATE_TIME_RESULT, callback);
			}
			break;
		default:
			throw new Common.StatusException();
		}
	}
	
	@Override
	public void getSimHwStatus(GetSimHwStatusCallback callback) throws Exception {
		if(DEBUG) Log.i(Common.TAG, "[GCController] getSimHwStatus");
		
		if(callback == null) throw new NullPointerException();
		
		switch(mService.getCurrentConnectionMode()) {
		case Full:
		case Partial:
			DeviceItem device = (DeviceItem)mService.getTargetDevice();
			if(!mService.getConnectivityService().gcGetSimHwStatus(device.getDevice())) {
				throw new BleCommandException();
			} else {
				mService.addBleCallback(GCDeviceController.this, IGcConnectivityService.CB_GET_SIM_HW_STATUS_RESULT, callback);
			}
			break;
		default:
			throw new Common.StatusException();
		}
	}
	
	@Override
	public void setCameraMode(CameraMode mode, OperationCallback callback) throws Exception {
		if(DEBUG) Log.i(Common.TAG, "[GCController] setCameraMode");
		
		if(callback == null) throw new NullPointerException();
		
		switch(mService.getCurrentConnectionMode()) {
		case Full:
		case Partial:
			IGcConnectivityService.CameraMode targetCameraMode = IGcConnectivityService.CameraMode.findMode(mode.getVal());
			if (targetCameraMode == null) {
				throw new Common.NoImpException();
			} else {
				DeviceItem device = (DeviceItem)mService.getTargetDevice();
				if(!mService.getConnectivityService().gcSetCameraMode(device.getDevice(), targetCameraMode)) {
					throw new BleCommandException();
				} else {
					mService.addBleCallback(GCDeviceController.this, IGcConnectivityService.CB_SET_CAMERA_MODE_RESULT, callback);
				}
			}
			break;
		default:
			throw new Common.StatusException();
		}
	}
	
	@Override
	public void getCameraMode(GetCameraModeCallback callback) throws Exception {
		if(DEBUG) Log.i(Common.TAG, "[GCController] getCameraMode");
		
		if(callback == null) throw new NullPointerException();
		
		switch(mService.getCurrentConnectionMode()) {
		case Full:
		case Partial:
			DeviceItem device = (DeviceItem)mService.getTargetDevice();
			if(!mService.getConnectivityService().gcGetCameraMode(device.getDevice())) {
				throw new BleCommandException();
			} else {
				mService.addBleCallback(GCDeviceController.this, IGcConnectivityService.CB_GET_CAMERA_MODE_RESULT, callback);
			}
			break;
		default:
			throw new Common.StatusException();
		}
	}
	
	@Override
	public void getLTECampingStatus(GetLTECampingStatusCallback callback) throws Exception {
		if(DEBUG) Log.i(Common.TAG, "[GCController] getLTECampingStatus");
		
		if(callback == null) throw new NullPointerException();
		
		switch(mService.getCurrentConnectionMode()) {
		case Full:
		case Partial:
			DeviceItem device = (DeviceItem)mService.getTargetDevice();
			if(!mService.getConnectivityService().gcGetLTECampingStatus(device.getDevice())) {
				throw new BleCommandException();
			} else {
				mService.addBleCallback(GCDeviceController.this, IGcConnectivityService.CB_GET_LTE_CAMPING_STATUS_RESULT, callback);
			}
			break;
		default:
			throw new Common.StatusException();
		}
	}
	
	@Override
	public void getSimInfo(GetSimInfoCallback callback) throws Exception {
		if(DEBUG) Log.i(Common.TAG, "[GCController] getSimInfo");
		
		if(callback == null) throw new NullPointerException();
		
		switch(mService.getCurrentConnectionMode()) {
		case Full:
		case Partial:
			DeviceItem device = (DeviceItem)mService.getTargetDevice();
			if(!mService.getConnectivityService().gcGetModemStatus(device.getDevice())) {
				throw new BleCommandException();
			} else {
				mService.addBleCallback(GCDeviceController.this, IGcConnectivityService.CB_GET_MODEM_STATUS_RESULT, callback);
			}
			break;
		default:
			throw new Common.StatusException();
		}
	}
	
	@Override
	public void unlockSimPin(String pinCode, UnlockSimPinCallback callback) throws Exception {
		if(DEBUG) Log.i(Common.TAG, "[GCController] unlockSimPin");
		
		if (TextUtils.isEmpty(pinCode) || pinCode.length() > 8) throw new InvalidArgumentsException("invalid pin code");
		
		if (callback == null) throw new NullPointerException();
		
		switch(mService.getCurrentConnectionMode()) {
		case Full:
		case Partial:
			DeviceItem device = (DeviceItem)mService.getTargetDevice();
			if(!mService.getConnectivityService().gcUnlockSimPin(device.getDevice(), pinCode)) {
				throw new BleCommandException();
			} else {
				mService.addBleCallback(GCDeviceController.this, IGcConnectivityService.CB_UNLOCK_SIM_PIN_RESULT, callback);
			}
			break;
		default:
			throw new Common.StatusException();
		}
	}
	
	@Override
	public void setModeChangeListener(ModeChangeListener l) {
		mModeChangeListener = l;
	}

	@Override
	public void setBatteryLevelChangeListener(BatteryLevelChangeListener l) {
		mBatteryLevelChangeListener = l;
	}

	@Override
	public void setSpaceChangeListener(SpaceChangeListener l) {
		mSpaceChangeListener = l;
	}
	
	@Override
	public void setRequestGpsInfoChangeListener(RequestGpsInfoChangeListener l) {
		mRequestGpsInfoChangeListener = l;
	}
	
	@Override
	public void setSdCardStatusListener(SdCardStatusListener l) {
		mSdCardStatusListener = l;
	}
	
	@Override
	public void setTemperatureListener(TemperatureStatusListener l) {
		mTemperatureStatusListener = l;
	}
	
	@Override
	public void setUsbStorageListener(UsbStorageStatusListener l) {
		mUsbStorageStatusListener = l;
	}
	
	@Override
	public void setPocketModeChangeListener(PocketModeChangeListener l) {
		mPocketModeChangeListener = l;
	}
	
	@Override
	public void setCameraModeChangeListener(CameraModeChangeListener l) {
		mCameraModeChangeListener = l;
	}
	
	@Override
	public void setSMSListener(SMSListener l) {
		mSMSListener = l;
	}

	@Override
	public void setAllFwVersionListener(AllFwVersionListener l) {
		mAllFwVersionListener = l;
	}
	
	@Override
	public void setFwUpdateResultListener(FwUpdateResultListener l) {
		mFwUpdateResultListener = l;
	}
	
	@Override
	public void setLTECampingStatusListener(LTECampingStatusListener l) {
		mLTECampingStatusListener = l;
	}

	static int getMappedBatteryLevel(IGcConnectivityService.MCUBatteryLevel batteryLevel) {
		switch (batteryLevel) {
		case MCU_BATTERY_INSUFFICIENT:
			return 0;
		case MCU_BATTERY_NEAR_INSUFFICIENT:
			return 8;
		case MCU_BATTERY_LOW:
			return 25;
		case MCU_BATTERY_HALF:
			return 50;
		case MCU_BATTERY_NEAR_FULL:
			return 75;
		case MCU_BATTERY_FULL:
			return 100;
		default:
			return 0;
		}
	}
}
