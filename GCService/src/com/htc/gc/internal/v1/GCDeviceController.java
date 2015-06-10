package com.htc.gc.internal.v1;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.Calendar;
import java.util.HashMap;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import com.htc.gc.connectivity.interfaces.IGcConnectivityServiceBase.LongTermEvent;
import com.htc.gc.connectivity.interfaces.IGcConnectivityServiceBase.PlugIO;
import com.htc.gc.connectivity.v2.interfaces.IGcConnectivityService;
import com.htc.gc.connectivity.v2.interfaces.IGcConnectivityService.Operation;
import com.htc.gc.interfaces.Common;
import com.htc.gc.interfaces.Common.BleCommandException;
import com.htc.gc.interfaces.Common.DataCallback;
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
import com.htc.gc.internal.v1.IMediator.IBleEventListener;
import com.htc.gc.internal.v1.IMediator.IEventListener;
import com.htc.gc.internal.Protocol;
import com.htc.gc.tasks.CheckSDCardTask;
import com.htc.gc.tasks.DRStatusGetTask;
import com.htc.gc.tasks.FormatSDCardTask;
import com.htc.gc.tasks.GetAutoLevelStatusTask;
import com.htc.gc.tasks.GetBatteryInfoTask;
import com.htc.gc.tasks.GetBtMacAddressTask;
import com.htc.gc.tasks.GetCountryCodeTask;
import com.htc.gc.tasks.GetDebugLogEnableSettingTask;
import com.htc.gc.tasks.GetDoubleClickModeTask;
import com.htc.gc.tasks.GetErrorLogFromGcTask;
import com.htc.gc.tasks.GetFakeShotSettingTask;
import com.htc.gc.tasks.GetFileCountInStorageTask;
import com.htc.gc.tasks.GetFirmwareVersionTask;
import com.htc.gc.tasks.GetGcGpsStatusTask;
import com.htc.gc.tasks.GetGripShotSettingTask;
import com.htc.gc.tasks.GetPowerSaveModeTask;
import com.htc.gc.tasks.GetSerialNumberTask;
import com.htc.gc.tasks.GetSpaceInfoTask;
import com.htc.gc.tasks.GetSpeakerModeTask;
import com.htc.gc.tasks.GetStorageInUseTask;
import com.htc.gc.tasks.GetUpsideDownStatusTask;
import com.htc.gc.tasks.GetUsePhoneGpsSettingTask;
import com.htc.gc.tasks.GetVideoRecordButtonConfigTask;
import com.htc.gc.tasks.ModeGetTask;
import com.htc.gc.tasks.ModeSetTask;
import com.htc.gc.tasks.ModeSetTask.SetControllerModeCallback;
import com.htc.gc.tasks.SetAutoLevelStatusTask;
import com.htc.gc.tasks.SetAutoPowerOffTimeThisBootUpTask;
import com.htc.gc.tasks.SetDebugLogEnableSettingTask;
import com.htc.gc.tasks.SetDoubleClickModeTask;
import com.htc.gc.tasks.SetFakeShotSettingTask;
import com.htc.gc.tasks.SetGcGpsStatusTask;
import com.htc.gc.tasks.SetGcToOobeModeTask;
import com.htc.gc.tasks.SetGripShotSettingTask;
import com.htc.gc.tasks.SetPowerSaveModeTask;
import com.htc.gc.tasks.SetSpeakerModeTask;
import com.htc.gc.tasks.SetUpsideDownStatusTask;
import com.htc.gc.tasks.SetUsePhoneGpsSettingTask;
import com.htc.gc.tasks.SetVideoRecordButtonConfigTask;
import com.htc.gc.tasks.SystemResetTask;
import com.htc.gc.tasks.UpgradeFirmwareTask;
import com.htc.gc.tasks.UploadFragmentTask;

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
	
	GCDeviceController(IMediator service) {
		mService = service;

		mService.addEventListener(Protocol.EVENT_FUNCTION_MODE_CHANGE_DONE, new IEventListener() {

			@Override
			public void event(int eventID, ByteBuffer body) {
				byte mode = body.get();
				Common.Mode result = Common.Mode.None;
				if(mode == Protocol.PROP_FUNCTIONMODE_BROWSE) result = Common.Mode.Browse;
				else if(mode == Protocol.PROP_FUNCTIONMODE_CONTROL) result = Common.Mode.Control;
				else result = Common.Mode.None;

				if(DEBUG) Log.i(Common.TAG, "[GCController] mode changed event, mode: " + result);

				ModeChangeListener l = mModeChangeListener;
				if(l != null) l.onModeChange(GCDeviceController.this, result);
			}
		});

		mService.addEventListener(Protocol.EVENT_BATTERY_LEVEL_CHANGED, new IEventListener() {

			@Override
			public void event(int eventID, ByteBuffer body) {
				int level = body.getInt();
				ChargingType type = null;
				try {
					type = ChargingType.getKey(body.getInt());
					if(DEBUG) Log.i(Common.TAG, "[GCController] battery level changed event, level: " + level + ", type: " + type);
					BatteryLevelChangeListener l = mBatteryLevelChangeListener;
					if(l != null) l.onBatteryLevelChange(GCDeviceController.this, ChargingType.BATTERY_LEVEL_AC_POWER.equals(type), level);
				} catch (NoImpException e) {
					e.printStackTrace();
				}
			}
		});

		mService.addEventListener(Protocol.EVENT_OBJECT_ADDED, new IEventListener() {

			@Override
			public void event(int eventID, ByteBuffer body) {
				if(DEBUG) Log.i(Common.TAG, "[GCController] space change event");
				
				body.getInt(); // file type
				body.getInt(); // object ID
				
				HashMap<IMediaItem.Type, Integer> remainingSpace = new HashMap<IMediaItem.Type, Integer>(); 
				
				for(int i = 0; i < 4 /* JPG, MOV, TIMELAPSE, SLOWMOION */; i++) {
					int type = body.getInt();
					int value = body.getInt();
					switch(type) {
					case Protocol.FILE_TYPE_JPG:
						remainingSpace.put(IMediaItem.Type.Photo, value);
						break;
					case Protocol.FILE_TYPE_MOV:
						remainingSpace.put(IMediaItem.Type.Video, value);
						break;
					case Protocol.FILE_TYPE_TIMELAPSE:
						remainingSpace.put(IMediaItem.Type.TimeLapse, value);
						break;
					case Protocol.FILE_TYPE_SLOWMOTION:
						remainingSpace.put(IMediaItem.Type.SlowMotion, value);
						break;
					default:
						if(DEBUG) Log.i(Common.TAG, "Comsuming unkown file type: "+type+" free space data");
					}
				}
			
				long freeSpaceByte = body.getLong();
				
				SpaceChangeListener l = mSpaceChangeListener;
				if(l != null) l.onSpaceChange(GCDeviceController.this, remainingSpace, freeSpaceByte);
			}
		});


//		mService.addEventListener(Protocol.EVENT_SD_CARD_UNPLUG, new IEventListener() {
//
//			@Override
//			public void event(int eventID, ByteBuffer body) {
//				Log.i(Common.TAG, "[GCController] SD card un-plug event");
//				
//				mService.updateCursorValidityKey();
//				
//				SdCardStatusListener l = mSdCardStatusListener;
//				if(l != null) l.onUnplug(GCDeviceController.this);
//			}
//		});

//		mService.addEventListener(Protocol.EVENT_SD_CARD_ONREADY, new IEventListener() {
//
//			@Override
//			public void event(int eventID, ByteBuffer body) {
//				Log.i(Common.TAG, "[GCController] SD card ready event");
//				// TODO: callback listener
//
//				mService.updateCursorValidityKey();
//			}
//		});
		
		mService.addEventListener(Protocol.EVENT_SD_CARD_WRONG_FORMAT, new IEventListener() {
			
			@Override
			public void event(int eventID, ByteBuffer body) {
				Log.i(Common.TAG, "[GCController] SD card wrong format event");
				
				SdCardStatusListener l = mSdCardStatusListener;
				if(l != null) l.onWrongFormat(GCDeviceController.this);
			}
		});
		
		mService.addEventListener(Protocol.EVENT_SD_CARD_FORMAT_BEGIN, new IEventListener() {
			
			@Override
			public void event(int eventID, ByteBuffer body) {
				Log.i(Common.TAG, "[GCController] SD card format begin event");
				
				SdCardStatusListener l = mSdCardStatusListener;
				if(l != null) l.onFormatBegin(GCDeviceController.this);
			}
		});
		
		mService.addEventListener(Protocol.EVENT_SD_CARD_FORMAT_END, new IEventListener() {
			
			@Override
			public void event(int eventID, ByteBuffer body) {
				Log.i(Common.TAG, "[GCController] SD card format end event");
				
				SdCardStatusListener l = mSdCardStatusListener;
				if(l != null) l.onFormatEnd(GCDeviceController.this);				
			}
		});
		
		mService.addEventListener(Protocol.EVENT_SD_CARD_WRITE_PROTECT, new IEventListener() {
			
			@Override
			public void event(int eventID, ByteBuffer body) {
				Log.i(Common.TAG, "[GCController] SD card write protect event");
				
				SdCardStatusListener l = mSdCardStatusListener;
				if(l != null) l.onWriteProtect(GCDeviceController.this);				
			}
		});
		
		mService.addEventListener(Protocol.EVENT_SD_UNUSABLE, new IEventListener() {
			
			@Override
			public void event(int eventID, ByteBuffer body) {
				Log.i(Common.TAG, "[GCController] SD card unusable event");
				
				SdCardStatusListener l = mSdCardStatusListener;
				if(l != null) l.onUnusableAndShutdownInFiveSeconds(GCDeviceController.this);
			}
		});
		
		mService.addEventListener(Protocol.EVENT_NO_SD_CARD, new IEventListener() {
			
			@Override
			public void event(int eventID, ByteBuffer body) {
				Log.i(Common.TAG, "[GCController] no SD card event");
				
				SdCardStatusListener l = mSdCardStatusListener;
				if(l != null) l.onNoSdCard(GCDeviceController.this);
			}
		});
		
		mService.addEventListener(Protocol.EVENT_HIGH_TEMPERATURE, new IEventListener() {
			
			@Override
			public void event(int eventID, ByteBuffer body) {
				Log.i(Common.TAG, "[GCController] overheat event");
				
				TemperatureStatusListener l = mTemperatureStatusListener;
				if(l != null) l.onOverHeatAndShutdownInOneMin(GCDeviceController.this);
			}
		});
		
		mService.addEventListener(Protocol.EVENT_POCKET_MODE_CHAGNE, new IEventListener() {
			
			@Override
			public void event(int eventID, ByteBuffer body) {			
				int mode = body.getInt();
				boolean isOn = false;
				try {
					isOn = PocketModeSetting.getKey(mode).equals(PocketModeSetting.POCKET_MODE_ON);
				} catch (NoImpException e) {
					e.printStackTrace();
				}
				
				Log.i(Common.TAG, "[GCController] pocket mode change event, isOn= "+isOn);
				
				PocketModeChangeListener l = mPocketModeChangeListener;
				if(l != null) l.onModeChange(GCDeviceController.this, isOn);
			}
		});
		
		mService.addBleEventListener(IGcConnectivityService.CB_LONG_TERM_EVENT_RESULT, new IBleEventListener() {

			@Override
			public void event(int callbackID, Bundle bundle) {
				LongTermEvent event = (LongTermEvent) bundle.getSerializable(IGcConnectivityService.PARAM_LONG_TERM_EVENT);
				if(event.equals(LongTermEvent.LTEVENT_CAMERA_STATUS)) {
					IGcConnectivityService.OperationEvent opEvent = (IGcConnectivityService.OperationEvent) bundle.getSerializable(IGcConnectivityService.PARAM_OPERATION_EVENT);
					switch(opEvent) {
					case OPEVENT_COMPLETE_CAPTURING:
					case OPEVENT_COMPLETE_RECORDING:
						if(!mService.getCurrentConnectionMode().equals(ConnectionMode.Partial)) {
							break;
						}
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
					int batteryLevel = bundle.getInt(IGcConnectivityService.PARAM_BATTERY_LEVEL);
					PlugIO adapterPlugin = (PlugIO) bundle.getSerializable(IGcConnectivityService.PARAM_ADAPTER_PLUGIN);
					PlugIO usbStorage = (PlugIO) bundle.getSerializable(IGcConnectivityService.PARAM_USB_STORAGE);
					
					if(adapterPlugin != null) {
						if(mService.getCurrentConnectionMode().equals(ConnectionMode.Partial)) {
							if(DEBUG) Log.i(Common.TAG, "[GCController] BLE battery level change event, level: "+batteryLevel+" type: "+adapterPlugin.toString());
							if(mBatteryLevelChangeListener != null) mBatteryLevelChangeListener.onBatteryLevelChange(GCDeviceController.this, adapterPlugin.equals(PlugIO.PLUG_IN), batteryLevel);
						}
					}
					
					if(usbStorage != null) {
						if(DEBUG) Log.i(Common.TAG, "[GCController] BLE usb storage change event, type: "+usbStorage);
						if(mUsbStorageStatusListener != null) mUsbStorageStatusListener.onMount(GCDeviceController.this, usbStorage.equals(PlugIO.PLUG_IN));
					}
					
				}

			}
			
		});
	}

	@Override
	public void getSpaceInfo(SpaceInfoCallback callback) throws Exception {
		if(DEBUG) Log.i(Common.TAG, "[GCController] getSpaceInfo");

		if(callback == null) throw new NullPointerException();
		
		switch(mService.getCurrentConnectionMode()) {
		case Full:
			mService.requestCommand(new GetSpaceInfoTask(this, callback));
			break;
			
		case Partial:
			DeviceItem device = (DeviceItem)mService.getTargetDevice();
			if(!mService.getConnectivityService().gcSetOperation(device.getDevice(), Operation.OPERATION_GET_FREE_SPACE)) {
				throw new BleCommandException();
			} else {
				mService.addBleCallback(GCDeviceController.this, IGcConnectivityService.CB_SET_OPERATION_RESULT, callback);
			}
			break;
			
		default:
			throw new StatusException();
		}
	}

	@Override
	public void getBatteryInfo(BatteryInfoCallback callback) throws Exception {
		if(DEBUG) Log.i(Common.TAG, "[GCController] getBatteryInfo");

		if(callback == null) throw new NullPointerException();
		
		if(mService.getCurrentConnectionMode() == ConnectionMode.Full) {
			mService.requestCommand(new GetBatteryInfoTask(this, callback));
		} else if(mService.getCurrentConnectionMode() == ConnectionMode.Partial) {
			final DeviceItem device = (DeviceItem)mService.getTargetDevice();
			if(!mService.getConnectivityService().gcGetHwStatus(device.getDevice())) {
				throw new BleCommandException();
			} else {
				mService.addBleCallback(GCDeviceController.this, IGcConnectivityService.CB_GET_HW_STATUS_RESULT, callback);
			}
		} else {
			throw new StatusException();
		}

	}

	@Override
	public void getMode(final ModeCallback callback) throws Exception {
		if(DEBUG) Log.i(Common.TAG, "[GCController] getMode");

		if(callback == null) throw new NullPointerException();
		if(mService.getCurrentConnectionMode() != ConnectionMode.Full) throw new StatusException();

		mService.requestCommand(new ModeGetTask(this, new ModeCallback() {

			@Override
			public void error(Exception e) {
				callback.error(e);
			}

			@Override
			public void result(IDeviceController that, Common.Mode mode) {
				callback.result(that, mode);

				ModeChangeListener l = mModeChangeListener;
				if(l != null) l.onModeChange(GCDeviceController.this, mode);
			}

		}));
	}

	@Override
	public void setMode(final Common.Mode mode, final OperationCallback callback) throws Exception {
		if(DEBUG) Log.i(Common.TAG, "[GCController] setMode '" + mode.toString() + "'");

		if(callback == null) throw new NullPointerException();
		if(mService.getCurrentConnectionMode() != ConnectionMode.Full) throw new StatusException();

		mService.requestCommand(new ModeSetTask(this, mode, new SetControllerModeCallback() {

			@Override
			public void noChange() {
				ModeChangeListener l = mModeChangeListener;
				if(l != null) l.onModeChange(GCDeviceController.this, mode);
			}

			@Override
			public void error(Exception e) {
				callback.error(e);
			}

			@Override
			public void done(Object that) {
				callback.done(that);
			}

		}));
	}
	
	@Override
	public void getDRStatus(final StatusCallback callback) throws Exception {
		if(DEBUG) Log.i(Common.TAG, "[GCController] getDRStatus");
		
		if(callback == null) throw new NullPointerException();
		
		switch(mService.getCurrentConnectionMode()) {
		case Full:
			mService.requestCommand(new DRStatusGetTask(this, callback));
			break;
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
		
		mService.requestCommand(new FormatSDCardTask(this, type, callback));
	}
	
	@Override
	public void CheckSDCard(SDChangeCallback callback) throws Exception {
		if(DEBUG) Log.i(Common.TAG, "[GCController] CheckSDCard");
		if(callback == null) throw new NullPointerException();
		if(mService.getCurrentConnectionMode() != ConnectionMode.Full) throw new StatusException();
		
		mService.requestCommand(new CheckSDCardTask(this, callback));
	}
	
	@Override
	public void getFileCountInStorage(Filter type, final StorageFileCountCallback callback) throws Exception {
		if(DEBUG) Log.i(Common.TAG, "[GCController] getFileCountInStorage");
		
		if(callback == null) throw new NullPointerException();
		if(mService.getCurrentConnectionMode() != ConnectionMode.Full) throw new StatusException();
		
		mService.requestCommand(new GetFileCountInStorageTask(this, type, callback));
	}
	
	@Override
	public void getSpeakerMode(SpeakerModeCallback callback) throws Exception {
		if(DEBUG) Log.i(Common.TAG, "[GCController] getSpeakerMode");
		
		if(callback == null) throw new NullPointerException();
		if(mService.getCurrentConnectionMode() != ConnectionMode.Full) throw new StatusException();
		
		mService.requestCommand(new GetSpeakerModeTask(this, callback));
	}
	
	@Override
	public void setSpeakerMode(SpeakerMode mode, OperationCallback callback) throws Exception {
		if(DEBUG) Log.i(Common.TAG, "[GCController] setSpeakerMode");
		
		if(callback == null) throw new NullPointerException();
		if(mService.getCurrentConnectionMode() != ConnectionMode.Full) throw new StatusException();
		
		mService.requestCommand(new SetSpeakerModeTask(this, mode, callback));
	}
	
	@Override
	public void getDoubleClickMode(DoubleClickModeCallback callback) throws Exception {
		if(DEBUG) Log.i(Common.TAG, "[GCController] getDoubleClickMode");
		
		if(callback == null) throw new NullPointerException();
		if(mService.getCurrentConnectionMode() != ConnectionMode.Full) throw new StatusException();
		
		mService.requestCommand(new GetDoubleClickModeTask(this, callback));
	}
	
	@Override
	public void setDoubleClickMode(DoubleClickMode mode, OperationCallback callback) throws Exception {
		if(DEBUG) Log.i(Common.TAG, "[GCController] setDoubleClickMode");
		
		if(callback == null) throw new NullPointerException();
		if(mService.getCurrentConnectionMode() != ConnectionMode.Full) throw new StatusException();
		
		mService.requestCommand(new SetDoubleClickModeTask(this, mode, callback));
	}
	
	@Override
	public void getVideoRecordButtonConfig(VideoRecordButtonConfigCallback callback) throws Exception {
		if(DEBUG) Log.i(Common.TAG, "[GCController] getVideoRecordButtonConfig");
		
		if(callback == null) throw new NullPointerException();
		if(mService.getCurrentConnectionMode() != ConnectionMode.Full) throw new StatusException();
		
		mService.requestCommand(new GetVideoRecordButtonConfigTask(this, callback));
	}
	
	@Override
	public void setVideoRecordButtonConfig(VideoRecBtnConfig config, OperationCallback callback) throws Exception {
		if(DEBUG) Log.i(Common.TAG, "[GCController] setVideoRecordButtonConfig");
		
		if(callback == null) throw new NullPointerException();
		if(mService.getCurrentConnectionMode() != ConnectionMode.Full) throw new StatusException();
		
		mService.requestCommand(new SetVideoRecordButtonConfigTask(this, config, callback));
	}
	
	@Override
	public ICancelable uploadFile(URI srcFile, String destPath, UploadCallback callback) throws Exception {
		if(DEBUG) Log.i(Common.TAG, "[GCController] uploadFile");
		
		if(callback == null) throw new NullPointerException();
		if(mService.getCurrentConnectionMode() != ConnectionMode.Full) throw new StatusException();
		
		UploadFragmentTask task;
		mService.requestCommand(task = new UploadFragmentTask(this, srcFile, destPath, callback));
		return task;
	}
	@Override
	public void upgradeFirmware(byte selectFirmwareFlag, int bootCodeVersion, int mainCodeVersion, int mcuVersion, int bleVersion, OperationCallback callback) throws Exception {
		if(DEBUG) Log.i(Common.TAG, "[GCController] upgradeFirmware");
		
		if(callback == null) throw new NullPointerException();
		if(mService.getCurrentConnectionMode() != ConnectionMode.Full) throw new StatusException();
		
		mService.requestCommand(new UpgradeFirmwareTask(this, selectFirmwareFlag, bootCodeVersion, mainCodeVersion, mcuVersion, bleVersion, callback));
	}
	
	@Override
	public void	getFirmwareVersion(FirmwareVersionCallback callback) throws Exception {
		if(DEBUG) Log.i(Common.TAG, "[GCController] getFirmwareVersion");
		
		if(callback == null) throw new NullPointerException();
		if(mService.getCurrentConnectionMode() != ConnectionMode.Full) throw new StatusException();
		
		mService.requestCommand(new GetFirmwareVersionTask(this, callback));
	}
	
	@Override
	public void triggerFirmwareUpdate(boolean updateA12, boolean updateModem, boolean updateMCU, String firmwareVersion, OperationCallback callback) throws Exception {
		if(DEBUG) Log.i(Common.TAG, "[GCController] triggerFirmwareUpdate");
		
		if(callback == null) throw new NullPointerException();
		if(mService.getCurrentConnectionMode() != ConnectionMode.Full) throw new StatusException();
		
		throw new NoImpException();
	}
	
	@Override
	public void getStorageInUse(StorageInUseCallback callback) throws Exception {
		if(DEBUG) Log.i(Common.TAG, "[GCController] getStorageInUse");
		
		if(callback == null) throw new NullPointerException();
		if(mService.getCurrentConnectionMode() != ConnectionMode.Full) throw new StatusException();
		
		mService.requestCommand(new GetStorageInUseTask(this, callback));		
	}
	
	@Override
	public void getAutoLevelStatus(AutoLevelStatusCallback callback) throws Exception {
		if(DEBUG) Log.i(Common.TAG, "[GCController] getAutoLevelStatus");
		
		if(callback == null) throw new NullPointerException();
		if(mService.getCurrentConnectionMode() != ConnectionMode.Full) throw new StatusException();
		
		mService.requestCommand(new GetAutoLevelStatusTask(this, callback));
	}
	
	@Override
	public void setAutoLevelStatus(AutoLevelStatus status, OperationCallback callback) throws Exception {
		if(DEBUG) Log.i(Common.TAG, "[GCController] setAutoLevelStatus");
		
		if(callback == null) throw new NullPointerException();
		if(mService.getCurrentConnectionMode() != ConnectionMode.Full) throw new StatusException();
		
		mService.requestCommand(new SetAutoLevelStatusTask(this, status, callback));	
	}
	
	@Override
	public void getUpsideDownStatus(UpsideDownStatusCallback callback) throws Exception {
		if(DEBUG) Log.i(Common.TAG, "[GCController] getUpsideDownStatus");
		
		if(callback == null) throw new NullPointerException();
		if(mService.getCurrentConnectionMode() != ConnectionMode.Full) throw new StatusException();
		
		mService.requestCommand(new GetUpsideDownStatusTask(this,callback));
	}
	
	@Override
	public void setUpsideDownStatus(UpsideDownStatus status, OperationCallback callback) throws Exception {
		if(DEBUG) Log.i(Common.TAG, "[GCController] setUpsideDownStatus");
		
		if(callback == null) throw new NullPointerException();
		if(mService.getCurrentConnectionMode() != ConnectionMode.Full) throw new StatusException();
		
		mService.requestCommand(new SetUpsideDownStatusTask(this, status, callback));
	}
	
	@Override
	public void getGcGpsStatus(GcGpsStatusCallback callback) throws Exception {
		if(DEBUG) Log.i(Common.TAG, "[GCController] getGcGpsStatus");
		
		if(callback == null) throw new NullPointerException();
		if(mService.getCurrentConnectionMode() != ConnectionMode.Full) throw new StatusException();
		
		mService.requestCommand(new GetGcGpsStatusTask(this, callback));		
	}

	@Override
	public void setGcGpsStatus(GcGpsStatus status, OperationCallback callback)
			throws Exception {
		if(DEBUG) Log.i(Common.TAG, "[GCController] setGcGpsStatus");
		
		if(callback == null) throw new NullPointerException();
		if(mService.getCurrentConnectionMode() != ConnectionMode.Full) throw new StatusException();
		
		mService.requestCommand(new SetGcGpsStatusTask(this, status, callback));
	}

	@Override
	public void getUsePhoneGpsSetting(UsePhoneGpsSettingCallback callback)
			throws Exception {
		if(DEBUG) Log.i(Common.TAG, "[GCController] getUsePhoneGpsSetting");
		
		if(callback == null) throw new NullPointerException();
		if(mService.getCurrentConnectionMode() != ConnectionMode.Full) throw new StatusException();
		
		mService.requestCommand(new GetUsePhoneGpsSettingTask(this, callback));
	}

	@Override
	public void setUsePhoneGpsSetting(UsePhoneGpsSetting setting, OperationCallback callback) throws Exception {
		if(DEBUG) Log.i(Common.TAG, "[GCController] setUsePhoneGpsSetting");
		
		if(callback == null) throw new NullPointerException();
		if(mService.getCurrentConnectionMode() != ConnectionMode.Full) throw new StatusException();
		
		mService.requestCommand(new SetUsePhoneGpsSettingTask(this, setting, callback));
	}
	
	@Override
	public void updateGpsInfo(Calendar calendar, double longitude, double latitude, double altitude, OperationCallback callback) throws Exception {
		if(DEBUG) Log.i(Common.TAG, "[GCController] updateGpsInfo");
		
		if(callback == null) throw new NullPointerException();
		if ((mService.getCurrentConnectionMode() != ConnectionMode.Full) && (mService.getCurrentConnectionMode() != ConnectionMode.Partial)) throw new StatusException();
		
		DeviceItem device = (DeviceItem) mService.getTargetDevice();
		if(!mService.getConnectivityService().gcSetGpsInfo(device.getDevice(), calendar, longitude, latitude, altitude)) {
			throw new BleCommandException();
		} else {
			mService.addBleCallback(GCDeviceController.this, IGcConnectivityService.CB_SET_GPS_INFO_RESULT, callback);
		}
	}
	
	@Override
	public void setDeviceName(final IDeviceItem device, String name, final OperationCallback callback) throws Exception {
		if(DEBUG) Log.i(Common.TAG, "[GCController] setDeviceName");
		if(callback == null) throw new NullPointerException();
		if(mService.getCurrentConnectionMode() != ConnectionMode.Full && mService.getCurrentConnectionMode() != ConnectionMode.Partial) throw new StatusException();
		if(name.length() > IDeviceController.MAX_DEVICE_NAME_LEN) throw new InvalidArgumentsException("Device name length should <= "+IDeviceController.MAX_DEVICE_NAME_LEN);
		
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
	}
	
	@Override
	public void resetSystem(final OperationCallback callback) throws Exception {
		if(DEBUG) Log.i(Common.TAG, "[GCController] resetSystem");
		
		if(callback == null) throw new NullPointerException();
		if(mService.getCurrentConnectionMode() != ConnectionMode.Full) throw new StatusException();
		
		mService.requestCommand(new SystemResetTask(this, new OperationCallback() {
			
			@Override
			public void error(Exception e) {
				callback.error(e);
			}
			
			@Override
			public void done(Object that) {
				IDeviceItem item = mService.getTargetDevice();
				if(item != null) {
					final DeviceItem deviceItem = (DeviceItem) item;
					// if has target device then reset to default password
					deviceItem.setPassword(Common.GC_DEFAULT_PASSWORD);
					callback.done(that);
				} else {
					callback.done(that);
				}
			}
		}));
	}
	
	@Override
	public void getErrorLogFromGC(DebugLogType type, boolean autoDelete, final GetErrorLogFromGcCallback callback) throws Exception {
		if(DEBUG) Log.i(Common.TAG, "[GCController] getErrorLogFromGC");
		
		if(callback == null) throw new NullPointerException();
		if(mService.getCurrentConnectionMode() != ConnectionMode.Full) throw new StatusException();
		
		final StringBuilder log = new StringBuilder();
		mService.requestCommand(new GetErrorLogFromGcTask(type, autoDelete, new DataCallback() {
			
			@Override
			public void error(Exception e) {
				callback.error(e);
			}
			
			@Override
			public void end() {
				callback.result(GCDeviceController.this, log.toString());
			}
			
			@Override
			public void data(ByteBuffer buffer) {
				try {
					String dataString = new String(buffer.array(), buffer.arrayOffset() + buffer.position(), buffer.remaining(), "UTF-8");
					log.append(dataString);
				} catch (UnsupportedEncodingException e) {
					e.printStackTrace();
					callback.error(e);
				}
			}
			
			@Override
			public void cancel() {
				callback.error(new Common.CancelException());
			}
		}));
	}
	
	@Deprecated
	@Override
	public void getBtMacAddress(GetBtMacAddressCallback callback) throws Exception {
		if(DEBUG) Log.i(Common.TAG, "[GCController] getBtMacAddress");
		
		if(callback == null) throw new NullPointerException();
		if(mService.getCurrentConnectionMode() != ConnectionMode.Full) throw new StatusException();
		
		mService.requestCommand(new GetBtMacAddressTask(this, callback));
	}
	
	@Override
	public void getSerialNumber(GetSerialNumberCallback callback) throws Exception {
		if(DEBUG) Log.i(Common.TAG, "[GCController] getSerialNumber");
		
		if(callback == null) throw new NullPointerException();
		if(mService.getCurrentConnectionMode() != ConnectionMode.Full) throw new StatusException();
		
		mService.requestCommand(new GetSerialNumberTask(this, callback));
	}
	
	@Override
	public void getCountryCode(GetCountryCodeCallback callback) throws Exception {
		if(DEBUG) Log.i(Common.TAG, "[GCController] getCountryCode");
		
		if(callback == null) throw new NullPointerException();
		if(mService.getCurrentConnectionMode() != ConnectionMode.Full) throw new StatusException();
		
		GCServiceWorker service = (GCServiceWorker) mService;
		if(service.getFWVersion() < 7250) throw new NoImpException();
		
		mService.requestCommand(new GetCountryCodeTask(this, callback));
	}
	
	@Override
	public void setAutoPowerOffTimeThisBootUp(short seconds, OperationCallback callback) throws Exception {
		if(DEBUG) Log.i(Common.TAG, "[GCController] setAutoPowerOffTimeThisBootUp");
		
		if(callback == null) throw new NullPointerException();
		if(mService.getCurrentConnectionMode() != ConnectionMode.Full) throw new StatusException();
		
		mService.requestCommand(new SetAutoPowerOffTimeThisBootUpTask(this, seconds, callback));
	}
	
	@Override
	public void getPowerSavingMode(GetPowerSaveModeCallback callback) throws Exception {
		if(DEBUG) Log.i(Common.TAG, "[GCController] getPowerSaveMode");
		
		if(callback == null) throw new NullPointerException();
		if(mService.getCurrentConnectionMode() != ConnectionMode.Full) throw new StatusException();
		
		mService.requestCommand(new GetPowerSaveModeTask(this, callback));
	}

	@Override
	public void setPowerSavingMode(PowerSaveMode mode, OperationCallback callback) throws Exception {
		if(DEBUG) Log.i(Common.TAG, "[GCController] setPowerSaveMode");
		
		if(callback == null) throw new NullPointerException();
		if(mService.getCurrentConnectionMode() != ConnectionMode.Full) throw new StatusException();
		
		mService.requestCommand(new SetPowerSaveModeTask(this, mode, callback));		
	}
	
	@Override
	public void getDebugLogEnableSetting(GetDebugLogSettingCallback callback) throws Exception {
		if(DEBUG) Log.i(Common.TAG, "[GCController] getDebugLogEnableSetting");
		
		if(callback == null) throw new NullPointerException();
		if(mService.getCurrentConnectionMode() != ConnectionMode.Full) throw new StatusException();
		
		mService.requestCommand(new GetDebugLogEnableSettingTask(this, callback));
	}

	@Override
	public void setDebugLogEnableSetting(DebugLogSetting setting, OperationCallback callback) throws Exception {
		if(DEBUG) Log.i(Common.TAG, "[GCController] setDebugLogEnableSetting");
		
		if(callback == null) throw new NullPointerException();
		if(mService.getCurrentConnectionMode() != ConnectionMode.Full) throw new StatusException();
		
		mService.requestCommand(new SetDebugLogEnableSettingTask(this, setting, callback));
	}
	
	@Override
	public void getFakeShotSetting(GetFakeShotSettingCallback callback) throws Exception {
		if(DEBUG) Log.i(Common.TAG, "[GCController] getFakeShotSetting");
		
		if(callback == null) throw new NullPointerException();
		if(mService.getCurrentConnectionMode() != ConnectionMode.Full) throw new StatusException();
		
		mService.requestCommand(new GetFakeShotSettingTask(this, callback));
	}

	@Override
	public void setFakeShotSetting(FakeShotSetting setting, OperationCallback callback) throws Exception {
		if(DEBUG) Log.i(Common.TAG, "[GCController] setFakeShotSetting");
		
		if(callback == null) throw new NullPointerException();
		if(mService.getCurrentConnectionMode() != ConnectionMode.Full) throw new StatusException();
		
		mService.requestCommand(new SetFakeShotSettingTask(this, setting, callback));
	}

	@Override
	public void getGripShotSetting(GetGripShotSettingCallback callback) throws Exception {
		if(DEBUG) Log.i(Common.TAG, "[GCController] getGripShotSetting");
		
		if(callback == null) throw new NullPointerException();
		if(mService.getCurrentConnectionMode() != ConnectionMode.Full) throw new StatusException();
		GCServiceWorker service = (GCServiceWorker) mService;
		if(service.getFWVersion() < 7030) throw new NoImpException();
			
		mService.requestCommand(new GetGripShotSettingTask(this, callback));
	}

	@Override
	public void setGripShotSetting(GripShotSetting setting, OperationCallback callback) throws Exception {
		if(DEBUG) Log.i(Common.TAG, "[GCController] setGripShotSetting");
		
		if(callback == null) throw new NullPointerException();
		if(mService.getCurrentConnectionMode() != ConnectionMode.Full) throw new StatusException();
		GCServiceWorker service = (GCServiceWorker) mService;
		if(service.getFWVersion() < 7030) throw new NoImpException();
		
		mService.requestCommand(new SetGripShotSettingTask(this, setting, callback));
	}
	
	@Override
	public void setGcToOobeMode(final RequestCallback callback) throws Exception {
		if(DEBUG) Log.i(Common.TAG, "[GCController] setGcToOobeMode");
		
		if(callback == null) throw new NullPointerException();
		if(mService.getCurrentConnectionMode() != ConnectionMode.Full) throw new StatusException();
		
		GCServiceWorker service = (GCServiceWorker) mService;
		boolean isEnableRequestCallbackResponse = service.getFWVersion() >= Common.ENABLE_REQUESTCALLBACK_RESPONSE_VERSION;
		if(DEBUG) Log.i(Common.TAG, "[GCController] setGcToOobeMode, isEnableRequestCallbackResponse= "+isEnableRequestCallbackResponse);
		
		mService.requestCommand(new SetGcToOobeModeTask(this, new RequestCallback() {
			
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
				mService.forceResetAllConnections();
				callback.done(that);
			}
		},isEnableRequestCallbackResponse));
	}
	
	@Override
	public void setDeviceTime(Calendar time, OperationCallback callback) throws Exception {
		if(DEBUG) Log.i(Common.TAG, "[GCController] setDeviceTime");
		
		if(callback == null) throw new NullPointerException();
		if(mService.getCurrentConnectionMode() == ConnectionMode.Partial || mService.getCurrentConnectionMode() == ConnectionMode.Full) {
			DeviceItem device = (DeviceItem)mService.getTargetDevice();
			if(!mService.getConnectivityService().gcSetDateTime(device.getDevice(), time)) {
				throw new BleCommandException();
			} else {
				mService.addBleCallback(GCDeviceController.this, IGcConnectivityService.CB_SET_DATE_TIME_RESULT, callback);	
			}
		} else {
			throw new StatusException();
		}
	}
	
	@Override
	public void getSimHwStatus(GetSimHwStatusCallback callback) throws Exception {
		if(DEBUG) Log.i(Common.TAG, "[GCController] getSimHwStatus");
		
		if(callback == null) throw new NullPointerException();
		
		throw new NoImpException();
	}
	
	@Override
	public void setCameraMode(CameraMode mode, OperationCallback callback) throws Exception {
		if(DEBUG) Log.i(Common.TAG, "[GCController] setCameraMode");
		
		if(callback == null) throw new NullPointerException();
		
		throw new NoImpException();
	}
	
	@Override
	public void getCameraMode(GetCameraModeCallback callback) throws Exception {
		if(DEBUG) Log.i(Common.TAG, "[GCController] getCameraMode");
		
		if(callback == null) throw new NullPointerException();
		
		throw new NoImpException();
	}
	
	@Override
	public void getLTECampingStatus(GetLTECampingStatusCallback callback) throws Exception {
		if(DEBUG) Log.i(Common.TAG, "[GCController] getLTECampingStatus");
		
		if(callback == null) throw new NullPointerException();
		
		throw new NoImpException();
	}
	
	@Override
	public void getSimInfo(GetSimInfoCallback callback) throws Exception {
		if(DEBUG) Log.i(Common.TAG, "[GCController] getSimInfo");
		
		if(callback == null) throw new NullPointerException();
		
		throw new NoImpException();
	}
	
	@Override
	public void unlockSimPin(String pinCode, UnlockSimPinCallback callback) throws Exception {
		if(DEBUG) Log.i(Common.TAG, "[GCController] unlockSimPin");
		
		if (TextUtils.isEmpty(pinCode) || pinCode.length() > 8) throw new InvalidArgumentsException("invalid pin code");
		
		if (callback == null) throw new NullPointerException();
		
		throw new NoImpException();
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
	
}
