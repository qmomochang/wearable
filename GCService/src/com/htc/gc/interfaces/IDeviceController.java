package com.htc.gc.interfaces;

import java.net.URI;
import java.util.Calendar;
import java.util.HashMap;

import com.htc.gc.interfaces.Common.ErrorCallback;
import com.htc.gc.interfaces.Common.Filter;
import com.htc.gc.interfaces.Common.ICancelable;
import com.htc.gc.interfaces.Common.OperationCallback;
import com.htc.gc.interfaces.Common.RequestCallback;
import com.htc.gc.interfaces.Common.UploadCallback;
import com.htc.gc.interfaces.IMediaItem.Type;

public interface IDeviceController {
	public static final int MAX_DEVICE_NAME_LEN = 15;
	public static final int SERIAL_NUMBER_LEN = 33;
	
	public static final byte UPGRADE_BOOTCODE 	= 0x01;
	public static final byte UPGRADE_MAINCODE 	= 0x02;
	public static final byte UPGRADE_MCU		= 0x04;
	public static final byte UPGRADE_BLE		= 0x08;
    
	public enum SDChangeStatus {
		CHANGE		((byte)0xF),
		NO_CHANGE	((byte)0x0);
		
		private final byte mVal;
		SDChangeStatus(byte val) { mVal = val; }
		public byte getVal() { return mVal; } 
		public static SDChangeStatus getKey(byte val) throws Common.NoImpException {
			for(SDChangeStatus res : SDChangeStatus.values()) {
				if(res.getVal() == val) {
					return res;
				}
			}
			throw new Common.NoImpException();
		}
	}
	
	public enum StorageFileCountType {
		COUNT_ALL			((byte)0x0),
		COUNT_STILL_MODE	((byte)0x1),
		COUNT_VIDEO_MODE	((byte)0x2);
		
		private final byte mVal;
		StorageFileCountType(byte val) { mVal = val; }
		public byte getVal() { return mVal; } 
		public static StorageFileCountType getKey(byte val) throws Common.NoImpException {
			for(StorageFileCountType res : StorageFileCountType.values()) {
				if(res.getVal() == val) {
					return res;
				}
			}
			throw new Common.NoImpException();
		}
	}
	
	public enum DoubleClickMode {
		CAPTURE_MODE_TIMELAPSE				((byte)0x1),
		CAPTURE_MODE_NIGHT_LONG_EXPOSURE	((byte)0x2),
		CAPTURE_MODEL_WATERFALL				((byte)0x3),
		VIDEO_MODE							((byte)0x10);
		
		private final byte mVal;
		DoubleClickMode(byte val) { mVal = val; }
		public byte getVal() { return mVal; } 
		public static DoubleClickMode getKey(byte val) throws Common.NoImpException {
			for(DoubleClickMode res : DoubleClickMode.values()) {
				if(res.getVal() == val) {
					return res;
				}
			}
			throw new Common.NoImpException();
		}
	}
	
	public enum SpeakerMode {
		AUDIO_VOL_HIGH		((byte)0x3),
		AUDIO_VOL_NORMAL	((byte)0x2),
		AUDIO_VOL_LOW		((byte)0x1),
		AUDIO_VOL_MUTE		((byte)0x0);
		
		private final byte mVal;
		SpeakerMode(byte val) { mVal = val; }
		public byte getVal() { return mVal; } 
		public static SpeakerMode getKey(byte val) throws Common.NoImpException {
			for(SpeakerMode res : SpeakerMode.values()) {
				if(res.getVal() == val) {
					return res;
				}
			}
			throw new Common.NoImpException();
		}
	}
	
	public enum StorageInUseType {
		STORAGE_ID_CARD	((byte)0x10),
		STORAGE_ID_NAND	((byte)0x20);
		
		private final byte mVal;
		StorageInUseType(byte val) { mVal = val; }
		public byte getVal() { return mVal; } 
		public static StorageInUseType getKey(byte val) throws Common.NoImpException {
			for(StorageInUseType res : StorageInUseType.values()) {
				if(res.getVal() == val) {
					return res;
				}
			}
			throw new Common.NoImpException();
		}
	}
	
	public enum VideoRecBtnConfig {
		VIDEO_REC_BTN_HOLD		((byte)0x0),
		VIDEO_REC_BTN_SWITCH	((byte)0x1),
		VIDEO_REC_BTN_CONFIG3	((byte)0x2),
		VIDEO_REC_BTN_CONFIG4	((byte)0x3),
		VIDEO_REC_BTN_CONFIG5	((byte)0x4);
		
		private final byte mVal;
		VideoRecBtnConfig(byte val) { mVal = val; }
		public byte getVal() { return mVal; } 
		public static VideoRecBtnConfig getKey(byte val) throws Common.NoImpException {
			for(VideoRecBtnConfig res : VideoRecBtnConfig.values()) {
				if(res.getVal() == val) {
					return res;
				}
			}
			throw new Common.NoImpException();
		}
	}
	
	public enum ChargingType {
		BATTERY_LEVEL_NO_CHARGE	(0x00),
		BATTERY_LEVEL_AC_POWER	(0xFF);
		
		private final int mVal;
		ChargingType(int val) { mVal = val; }
		public int getVal() { return mVal; } 
		public static ChargingType getKey(int val) throws Common.NoImpException {
			for(ChargingType res : ChargingType.values()) {
				if(res.getVal() == val) {
					return res;
				}
			}
			throw new Common.NoImpException();
		}
	}
	
	public enum AutoLevelStatus {
		AUTOLEVEL_OFF	((byte) 0x00),
		AUTOLEVEL_ON	((byte) 0x01);
		
		private final byte mVal;
		AutoLevelStatus(byte val) { mVal = val; }
		public byte getVal() { return mVal; } 
		public static AutoLevelStatus getKey(byte val) throws Common.NoImpException {
			for(AutoLevelStatus res : AutoLevelStatus.values()) {
				if(res.getVal() == val) {
					return res;
				}
			}
			throw new Common.NoImpException();
		}
	}
	
	public enum UpsideDownStatus {
		UPSIDEDOWN_OFF	((byte) 0x00),
		UPSIDEDOWN_ON	((byte) 0x01);
		
		private final byte mVal;
		UpsideDownStatus(byte val) { mVal = val; }
		public byte getVal() { return mVal; } 
		public static UpsideDownStatus getKey(byte val) throws Common.NoImpException {
			for(UpsideDownStatus res : UpsideDownStatus.values()) {
				if(res.getVal() == val) {
					return res;
				}
			}
			throw new Common.NoImpException();
		}
	}
	
	public enum GcGpsStatus {
		GC_GPS_OFF	((byte) 0x00),
		GC_GPS_ON	((byte) 0x01);
		
		private final byte mVal;
		GcGpsStatus(byte val) { mVal = val; }
		public byte getVal() { return mVal; } 
		public static GcGpsStatus getKey(byte val) throws Common.NoImpException {
			for(GcGpsStatus res : GcGpsStatus.values()) {
				if(res.getVal() == val) {
					return res;
				}
			}
			throw new Common.NoImpException();
		}
	}
	
	public enum UsePhoneGpsSetting {
		DISABLE	((byte) 0x00),
		ENABLE	((byte) 0x01);
		
		private final byte mVal;
		UsePhoneGpsSetting(byte val) { mVal = val; }
		public byte getVal() { return mVal; } 
		public static UsePhoneGpsSetting getKey(byte val) throws Common.NoImpException {
			for(UsePhoneGpsSetting res : UsePhoneGpsSetting.values()) {
				if(res.getVal() == val) {
					return res;
				}
			}
			throw new Common.NoImpException();
		}
	}
	
	public enum DRStatus {
		STATUS_NONE 				((byte) 0x00),
		STATUS_TIME_LAPSE 			((byte) 0x01),
		STATUS_VIDEO_RECORDING 		((byte) 0x02),
		STATUS_VIDEO_PLAYING 		((byte) 0x03),
		STATUS_IMAGE_PROCESSING		((byte) 0x04),
		STATUS_TIME_LAPSE_PAUSED	((byte) 0x05);
		
		private final byte mVal;
		DRStatus(byte val) { mVal = val; }
		public byte getVal() { return mVal; } 
		public static DRStatus getKey(byte val) throws Common.NoImpException {
			for(DRStatus res : DRStatus.values()) {
				if(res.getVal() == val) {
					return res;
				}
			}
			throw new Common.NoImpException();
		}
	}
	
	public enum DebugLogSetting {
		DEBUGLOG_OFF	((byte) 0x00),
		DEBUGLOG_ON		((byte) 0x01);
		
		private final byte mVal;
		DebugLogSetting(byte val) { mVal = val; }
		public byte getVal() { return mVal; } 
		public static DebugLogSetting getKey(byte val) throws Common.NoImpException {
			for(DebugLogSetting res : DebugLogSetting.values()) {
				if(res.getVal() == val) {
					return res;
				}
			}
			throw new Common.NoImpException();
		}
	}
	
	public enum DebugLogType {
		DEBUGLOG_ERROR			((byte) 0x00),
		DEBUGLOG_WIFIINTERR		((byte) 0x01),
		DEBUGLOG_WIFICONFIGERR	((byte) 0x02),
		DEBUGLOG_COMMONLOG		((byte) 0x03);
		
		
		private final byte mVal;
		DebugLogType(byte val) { mVal = val; }
		public byte getVal() { return mVal; } 
		public static DebugLogType getKey(byte val) throws Common.NoImpException {
			for(DebugLogType res : DebugLogType.values()) {
				if(res.getVal() == val) {
					return res;
				}
			}
			throw new Common.NoImpException();
		}
	}
	
	public enum DebugLogDeleteSetting {
		DEBUGLOG_NOTDELETELOG	((byte) 0x00),
		DEBUGLOG_DELETELOG		((byte) 0x01);
		
		
		private final byte mVal;
		DebugLogDeleteSetting(byte val) { mVal = val; }
		public byte getVal() { return mVal; } 
		public static DebugLogDeleteSetting getKey(byte val) throws Common.NoImpException {
			for(DebugLogDeleteSetting res : DebugLogDeleteSetting.values()) {
				if(res.getVal() == val) {
					return res;
				}
			}
			throw new Common.NoImpException();
		}
	}
	
	public enum PowerSaveMode {
		POWERSAV_MODE_OFF	((byte) 0x00),
		POWERSAV_MODE_ON	((byte) 0x01);
		
		
		private final byte mVal;
		PowerSaveMode(byte val) { mVal = val; }
		public byte getVal() { return mVal; } 
		public static PowerSaveMode getKey(byte val) throws Common.NoImpException {
			for(PowerSaveMode res : PowerSaveMode.values()) {
				if(res.getVal() == val) {
					return res;
				}
			}
			throw new Common.NoImpException();
		}
	}
	
	public enum GripShotSetting {
		GRIP_SHOT_OFF		((byte) 0x00),
		GRIP_SHOT_ON 		((byte) 0x01);
		
		
		private final byte mVal;
		GripShotSetting(byte val) { mVal = val; }
		public byte getVal() { return mVal; } 
		public static GripShotSetting getKey(byte val) throws Common.NoImpException {
			for(GripShotSetting res : GripShotSetting.values()) {
				if(res.getVal() == val) {
					return res;
				}
			}
			throw new Common.NoImpException();
		}
	}
	
	public enum FakeShotSetting {
		FAKE_SHOT_OFF		((byte) 0x00),
		FAKE_SHOT_ON		((byte) 0x01);
		
		
		private final byte mVal;
		FakeShotSetting(byte val) { mVal = val; }
		public byte getVal() { return mVal; } 
		public static FakeShotSetting getKey(byte val) throws Common.NoImpException {
			for(FakeShotSetting res : FakeShotSetting.values()) {
				if(res.getVal() == val) {
					return res;
				}
			}
			throw new Common.NoImpException();
		}
	}
	
	public enum PocketModeSetting {
		POCKET_MODE_OFF		((int) 0x0),
		POCKET_MODE_ON		((int) 0x1);
		
		private final int mVal;
		PocketModeSetting(int val) { mVal = val; }
		public int getVal() { return mVal; } 
		public static PocketModeSetting getKey(int val) throws Common.NoImpException {
			for(PocketModeSetting res : PocketModeSetting.values()) {
				if(res.getVal() == val) {
					return res;
				}
			}
			throw new Common.NoImpException();
		}
	}
	
	public enum SDCardFormatType {
		FILESYS_FORMAT_QUICK,
		FILESYS_FORMAT_FULL
	}
	
	public enum SimHwStatus {
		SIM_HW_STATUS_PLUG_OUT			((byte)0x0),
		SIM_HW_STATUS_PLUG_IN			((byte)0x1),
		SIM_HW_STATUS_NOT_IN_LTE_MODE	((byte)0x2);
		
		private final byte mVal;
		SimHwStatus(byte val) { mVal = val; }
		public byte getVal() { return mVal; }
		public static SimHwStatus getKey(byte val) throws Common.NoImpException {
			for (SimHwStatus simHwStatus : SimHwStatus.values()) {
				if (simHwStatus.getVal() == val) {
					return simHwStatus;
				}
			}
			throw new Common.NoImpException();
		}
	}
	
	public enum CameraMode {
		CAMERA_MODE_NORMAL		((byte)0x1),
		CAMERA_MODE_SLOW_MOTION	((byte)0x2),
		CAMERA_MODE_LTE_MODE	((byte)0x3);
		
		private final byte mVal;
		CameraMode(byte val) { mVal = val; }
		public byte getVal() { return mVal; }
		public static CameraMode getKey(byte val) throws Common.NoImpException {
			for (CameraMode cameraMode : CameraMode.values()) {
				if (cameraMode.getVal() == val) {
					return cameraMode;
				}
			}
			throw new Common.NoImpException();
		}
	}
	
	public enum LTECampingStatus {
		LTE_CAMPING_STATUS_SUCCESS		((byte)0x00),
		LTE_CAMPING_STATUS_WRONG_APN	((byte)0x01),
		LTE_CAMPING_STATUS_CONNECT_FAIL	((byte)0x02),
		LTE_CAMPING_STATUS_UNKNOWN_ERROR((byte)0x03);
		
		private final byte mVal;
		LTECampingStatus(byte val) { mVal = val; }
		public byte getVal() { return mVal; }
		public static LTECampingStatus getKey(byte val) throws Common.NoImpException {
			for (LTECampingStatus lteCampingStatus : LTECampingStatus.values()) {
				if (lteCampingStatus.getVal() == val) {
					return lteCampingStatus;
				}
			}
			throw new Common.NoImpException();
		}
	}
	
	public enum SimLockType {
		SIM_LOCK_TYPE_UNKNOWN	((byte)0x00),
		SIM_LOCK_TYPE_NONE		((byte)0x01),
		SIM_LOCK_TYPE_PIN		((byte)0x02),
		SIM_LOCK_TYPE_PUK		((byte)0x03);
		
		private final byte mVal;
		SimLockType(byte val) { mVal = val; }
		public byte getVal() { return mVal; }
		public static SimLockType getKey(byte val) throws Common.NoImpException {
			for (SimLockType simLockType : SimLockType.values()) {
				if (simLockType.getVal() == val) {
					return simLockType;
				}
			}
			throw new Common.NoImpException();
		}
	}
	
	public interface SDChangeCallback extends ErrorCallback {
		void result(IDeviceController that, SDChangeStatus status);
	}
	
	public interface SpaceInfoCallback extends ErrorCallback {
		void result(IDeviceController that, HashMap<IMediaItem.Type, Integer> freeSpaceUnit, long freeSpaceByte, long totalByte);
	}

	public interface BatteryInfoCallback extends ErrorCallback {
		void result(IDeviceController that, boolean acPower, int percentage);
	}

	public interface ModeCallback extends ErrorCallback {
		void result(IDeviceController that, Common.Mode mode);
	}
	
	public interface StatusCallback extends ErrorCallback {
		void result(IDeviceController that, DRStatus status, int count);
	}
	
	public interface StorageFileCountCallback extends ErrorCallback {
		public void result(IDeviceController that, Filter type, int count);
	}
	
	public interface DoubleClickModeCallback extends ErrorCallback {
		public void result(IDeviceController that, DoubleClickMode mode);
	}
	
	public interface SpeakerModeCallback extends ErrorCallback {
		public void result(IDeviceController that, SpeakerMode mode);
	}
	
	public interface FirmwareVersionCallback extends ErrorCallback {
		public void result(IDeviceController that, int ver);
	}

	public interface CameraNameCallback extends ErrorCallback {
		public void result(IDeviceController that, String name);
	}
	
	public interface StorageInUseCallback extends ErrorCallback {
		public void result(IDeviceController that, StorageInUseType type);
	}
	
	public interface VideoRecordButtonConfigCallback extends ErrorCallback {
		public void result(IDeviceController that, VideoRecBtnConfig config);
	}
	
	public interface AutoLevelStatusCallback extends ErrorCallback {
		public void result(IDeviceController that, AutoLevelStatus status);
	}
	
	public interface UpsideDownStatusCallback extends ErrorCallback {
		public void result(IDeviceController that, UpsideDownStatus status);
	}
	
	public interface GcGpsStatusCallback extends ErrorCallback {
		public void result(IDeviceController that, GcGpsStatus status);
	}
	
	public interface UsePhoneGpsSettingCallback extends ErrorCallback {
		public void result(IDeviceController that, UsePhoneGpsSetting status);
	}
	
	public interface GetErrorLogFromGcCallback extends ErrorCallback {
		public void result(IDeviceController that, String log);
	}
	
	public interface GetBtMacAddressCallback extends ErrorCallback {
		public void result(IDeviceController that, String address);
	}
	
	public interface GetSerialNumberCallback extends ErrorCallback {
		public void result(IDeviceController that, String modelName, String serialNumber);
	}
	
	public interface GetDebugLogSettingCallback extends ErrorCallback {
		public void result(IDeviceController that, DebugLogSetting setting);
	}
	
	public interface GetPowerSaveModeCallback extends ErrorCallback {
		public void result(IDeviceController that, PowerSaveMode mode);
	}
	
	public interface GetFakeShotSettingCallback extends ErrorCallback {
		public void result(IDeviceController that, FakeShotSetting setting);
	}
	
	public interface GetGripShotSettingCallback extends ErrorCallback {
		public void result(IDeviceController that, GripShotSetting setting);
	}
	
	public interface GetCountryCodeCallback extends ErrorCallback {
		public void result(IDeviceController that, byte countryCode);
	}
	
	public interface GetSimHwStatusCallback extends ErrorCallback {
		public void result(IDeviceController that, SimHwStatus status);
	}
	
	public interface GetCameraModeCallback extends ErrorCallback {
		public void result(IDeviceController that, CameraMode mode);
	}
	
	public interface GetLTECampingStatusCallback extends ErrorCallback {
		public void result(IDeviceController that, LTECampingStatus status);
	}
	
	public interface GetSimInfoCallback extends ErrorCallback {
		public void result(IDeviceController that, SimLockType type, int pinRetryCount, int pukRetryCount);
	}
	
	public interface UnlockSimPinCallback extends ErrorCallback {
		public void result(IDeviceController that, boolean unlockResult, int pinRetryCount);
	}
	
	public interface ModeChangeListener {
		public void onModeChange(IDeviceController that, Common.Mode mode);
	}
	
	public interface BatteryLevelChangeListener {
		public void onBatteryLevelChange(IDeviceController that, boolean acPower, int percentage);
	}

	public interface SpaceChangeListener {
		public void onSpaceChange(IDeviceController that, HashMap<Type, Integer> freeSpaceUnit, long freeSpaceByte);
	}
	
	public interface RequestGpsInfoChangeListener {
		public void onRequestGpsInfoChange(IDeviceController that, boolean on);
	}
	
	public interface SdCardStatusListener {
		public void onFormatBegin(IDeviceController that);
		public void onFormatEnd(IDeviceController that);
		public void onWriteProtect(IDeviceController that);
		public void onWrongFormat(IDeviceController that);
		public void onUnusableAndShutdownInFiveSeconds(IDeviceController that);
		public void onNoSdCard(IDeviceController that);
	}
	
	public interface UsbStorageStatusListener {
		public void onMount(IDeviceController that, boolean mount);
	}
	
	public interface TemperatureStatusListener {
		public void onOverHeatAndShutdownInOneMin(IDeviceController that);
	}
	
	public interface PocketModeChangeListener{
		public void onModeChange(IDeviceController that, boolean on);
	}
	
	public interface CameraModeChangeListener {
		public void onModeChange(IDeviceController that, CameraMode mode);
	}
	
	public interface SMSListener {
		public void onReceived(String dateTime, String phoneNumber, String messageContent);
	}
	
	public interface AllFwVersionListener {
		public void onGetVersion(String a12Version, String modemVersion, String mcuVersion);
	}
	
	public interface FwUpdateResultListener {
		public void onResult(boolean isSuccess);
	}
	
	public interface LTECampingStatusListener {
		public void onChange(LTECampingStatus status);
	}
	
	public void getSpaceInfo(SpaceInfoCallback callback) throws Exception;
	public void getBatteryInfo(BatteryInfoCallback callback) throws Exception;

	public void getMode(ModeCallback callback) throws Exception;
	public void setMode(Common.Mode mode, OperationCallback callback) throws Exception;
	public void getDRStatus(StatusCallback callback) throws Exception;
	public void formatSDCard(SDCardFormatType type, OperationCallback callback) throws Exception;
	public void CheckSDCard(SDChangeCallback callback) throws Exception;
	public void getFileCountInStorage(Filter type, StorageFileCountCallback callback) throws Exception;
	
	public void getSpeakerMode(SpeakerModeCallback callback) throws Exception;
	public void setSpeakerMode(SpeakerMode mode, OperationCallback callback) throws Exception;
	
	public void getDoubleClickMode(DoubleClickModeCallback callback) throws Exception;
	public void setDoubleClickMode(DoubleClickMode mode, OperationCallback callback) throws Exception;
	
	public void getVideoRecordButtonConfig(VideoRecordButtonConfigCallback callback) throws Exception;
	public void setVideoRecordButtonConfig(VideoRecBtnConfig config, OperationCallback callback) throws Exception;
	
	public ICancelable uploadFile(URI srcFile, String destPath, UploadCallback callback) throws Exception;
	public void upgradeFirmware(byte selectFirmwareFlag, int bootCodeVersion, int mainCodeVersion, int mcuVersion, int bleVersion, OperationCallback callback) throws Exception;
	
	public void	getFirmwareVersion(FirmwareVersionCallback callback) throws Exception;
	
	public void triggerFirmwareUpdate(boolean updateA12, boolean updateModem, boolean updateMCU, String firmwareVersion, OperationCallback callback) throws Exception;
	
	public void getStorageInUse(StorageInUseCallback callback) throws Exception;
	
	public void getAutoLevelStatus(AutoLevelStatusCallback callback) throws Exception;
	public void setAutoLevelStatus(AutoLevelStatus status, OperationCallback callback) throws Exception;
	
	public void getUpsideDownStatus(UpsideDownStatusCallback callback) throws Exception;
	public void setUpsideDownStatus(UpsideDownStatus status, OperationCallback callback) throws Exception;
	
	public void getGcGpsStatus(GcGpsStatusCallback callback) throws Exception;
	public void setGcGpsStatus(GcGpsStatus status, OperationCallback callback) throws Exception;
	
	public void getUsePhoneGpsSetting(UsePhoneGpsSettingCallback callback) throws Exception;
	public void setUsePhoneGpsSetting(UsePhoneGpsSetting setting, OperationCallback callback) throws Exception;
	
	public void updateGpsInfo(Calendar calendar, double longitude, double latitude, double altitude, OperationCallback callback) throws Exception;
	
	public void setDeviceName(IDeviceItem device, String name, OperationCallback callback) throws Exception;
	
	public void resetSystem(final OperationCallback callback) throws Exception;
	
	public void getErrorLogFromGC(DebugLogType type, boolean autoDelete, GetErrorLogFromGcCallback callback) throws Exception;
	
	public void getSerialNumber(GetSerialNumberCallback callback) throws Exception;
	public void getCountryCode(GetCountryCodeCallback callback) throws Exception;
	
	public void setAutoPowerOffTimeThisBootUp(short seconds, OperationCallback callback) throws Exception;
	
	public void getPowerSavingMode(GetPowerSaveModeCallback callback) throws Exception;
	public void setPowerSavingMode(PowerSaveMode mode, OperationCallback callback) throws Exception;
	
	@Deprecated
	public void getBtMacAddress(GetBtMacAddressCallback callback) throws Exception; // iOS only
	
	public void getDebugLogEnableSetting(GetDebugLogSettingCallback callback) throws Exception;
	public void setDebugLogEnableSetting(DebugLogSetting setting, OperationCallback callback) throws Exception;
	
	public void getFakeShotSetting(GetFakeShotSettingCallback callback) throws Exception;
	public void setFakeShotSetting(FakeShotSetting setting, OperationCallback callback) throws Exception;
	
	public void getGripShotSetting(GetGripShotSettingCallback callback) throws Exception;
	public void setGripShotSetting(GripShotSetting setting, OperationCallback callback) throws Exception;
	
	public void setGcToOobeMode(RequestCallback callback) throws Exception;
	
	public void setDeviceTime(Calendar time, OperationCallback callback) throws Exception;
	
	public void getSimHwStatus(GetSimHwStatusCallback callback) throws Exception;
	
	public void setCameraMode(CameraMode mode, OperationCallback callback) throws Exception;
	public void getCameraMode(GetCameraModeCallback callback) throws Exception;
	
	public void getLTECampingStatus(GetLTECampingStatusCallback callback) throws Exception;
	
	public void getSimInfo(GetSimInfoCallback callback) throws Exception;
	public void unlockSimPin(String pinCode, UnlockSimPinCallback callback) throws Exception;

	public void setModeChangeListener(ModeChangeListener l);
	public void setBatteryLevelChangeListener(BatteryLevelChangeListener l);
	public void setSpaceChangeListener(SpaceChangeListener l);
	public void setRequestGpsInfoChangeListener(RequestGpsInfoChangeListener l);
	public void setSdCardStatusListener(SdCardStatusListener l);
	public void setTemperatureListener(TemperatureStatusListener l);
	public void setUsbStorageListener(UsbStorageStatusListener l);
	public void setPocketModeChangeListener(PocketModeChangeListener l);
	public void setCameraModeChangeListener(CameraModeChangeListener l);
	public void setSMSListener(SMSListener l);
	public void setAllFwVersionListener(AllFwVersionListener l);
	public void setFwUpdateResultListener(FwUpdateResultListener l);
	public void setLTECampingStatusListener(LTECampingStatusListener l);
}
