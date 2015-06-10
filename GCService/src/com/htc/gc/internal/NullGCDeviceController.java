package com.htc.gc.internal;

import java.net.URI;
import java.util.Calendar;

import com.htc.gc.interfaces.Common.Filter;
import com.htc.gc.interfaces.Common.ICancelable;
import com.htc.gc.interfaces.Common.Mode;
import com.htc.gc.interfaces.Common.OperationCallback;
import com.htc.gc.interfaces.Common.RequestCallback;
import com.htc.gc.interfaces.Common.UploadCallback;
import com.htc.gc.interfaces.IDeviceController;
import com.htc.gc.interfaces.IDeviceItem;

class NullGCDeviceController implements IDeviceController {

	@Override
	public void getSpaceInfo(SpaceInfoCallback callback) throws Exception {
	}

	@Override
	public void getBatteryInfo(BatteryInfoCallback callback) throws Exception {
	}

	@Override
	public void getMode(ModeCallback callback) throws Exception {
	}

	@Override
	public void setMode(Mode mode, OperationCallback callback) throws Exception {
	}

	@Override
	public void getDRStatus(StatusCallback callback) throws Exception {
	}

	@Override
	public void formatSDCard(SDCardFormatType type, OperationCallback callback)
			throws Exception {
	}

	@Override
	public void CheckSDCard(SDChangeCallback callback) throws Exception {
	}

	@Override
	public void getFileCountInStorage(Filter type,
			StorageFileCountCallback callback) throws Exception {
	}

	@Override
	public void getSpeakerMode(SpeakerModeCallback callback) throws Exception {
	}

	@Override
	public void setSpeakerMode(SpeakerMode mode, OperationCallback callback)
			throws Exception {
	}

	@Override
	public void getDoubleClickMode(DoubleClickModeCallback callback)
			throws Exception {
	}

	@Override
	public void setDoubleClickMode(DoubleClickMode mode,
			OperationCallback callback) throws Exception {
	}

	@Override
	public void getVideoRecordButtonConfig(
			VideoRecordButtonConfigCallback callback) throws Exception {
	}

	@Override
	public void setVideoRecordButtonConfig(VideoRecBtnConfig config,
			OperationCallback callback) throws Exception {
	}

	@Override
	public ICancelable uploadFile(URI srcFile, String destPath,
			UploadCallback callback) throws Exception {
		return null;
	}

	@Override
	public void upgradeFirmware(byte selectFirmwareFlag, int bootCodeVersion,
			int mainCodeVersion, int mcuVersion, int bleVersion,
			OperationCallback callback) throws Exception {
	}

	@Override
	public void getFirmwareVersion(FirmwareVersionCallback callback)
			throws Exception {
	}

	@Override
	public void triggerFirmwareUpdate(boolean updateA12, boolean updateModem,
			boolean updateMCU, String firmwareVersion,
			OperationCallback callback) throws Exception {
	}

	@Override
	public void getStorageInUse(StorageInUseCallback callback) throws Exception {
	}

	@Override
	public void getAutoLevelStatus(AutoLevelStatusCallback callback)
			throws Exception {
	}

	@Override
	public void setAutoLevelStatus(AutoLevelStatus status,
			OperationCallback callback) throws Exception {
	}

	@Override
	public void getUpsideDownStatus(UpsideDownStatusCallback callback)
			throws Exception {
	}

	@Override
	public void setUpsideDownStatus(UpsideDownStatus status,
			OperationCallback callback) throws Exception {
	}

	@Override
	public void getGcGpsStatus(GcGpsStatusCallback callback) throws Exception {
	}

	@Override
	public void setGcGpsStatus(GcGpsStatus status, OperationCallback callback)
			throws Exception {
	}

	@Override
	public void getUsePhoneGpsSetting(UsePhoneGpsSettingCallback callback)
			throws Exception {
	}

	@Override
	public void setUsePhoneGpsSetting(UsePhoneGpsSetting setting,
			OperationCallback callback) throws Exception {
	}

	@Override
	public void updateGpsInfo(Calendar calendar, double longitude,
			double latitude, double altitude, OperationCallback callback)
			throws Exception {
	}

	@Override
	public void setDeviceName(IDeviceItem device, String name,
			OperationCallback callback) throws Exception {
	}

	@Override
	public void resetSystem(OperationCallback callback) throws Exception {
	}

	@Override
	public void getErrorLogFromGC(DebugLogType type, boolean autoDelete,
			GetErrorLogFromGcCallback callback) throws Exception {
	}

	@Override
	public void getSerialNumber(GetSerialNumberCallback callback)
			throws Exception {
	}

	@Override
	public void getCountryCode(GetCountryCodeCallback callback)
			throws Exception {
	}

	@Override
	public void setAutoPowerOffTimeThisBootUp(short seconds,
			OperationCallback callback) throws Exception {
	}

	@Override
	public void getPowerSavingMode(GetPowerSaveModeCallback callback)
			throws Exception {
	}

	@Override
	public void setPowerSavingMode(PowerSaveMode mode,
			OperationCallback callback) throws Exception {
	}

	@Deprecated
	@Override
	public void getBtMacAddress(GetBtMacAddressCallback callback)
			throws Exception {
	}

	@Override
	public void getDebugLogEnableSetting(GetDebugLogSettingCallback callback)
			throws Exception {
	}

	@Override
	public void setDebugLogEnableSetting(DebugLogSetting setting,
			OperationCallback callback) throws Exception {
	}

	@Override
	public void getFakeShotSetting(GetFakeShotSettingCallback callback)
			throws Exception {
	}

	@Override
	public void setFakeShotSetting(FakeShotSetting setting,
			OperationCallback callback) throws Exception {
	}

	@Override
	public void getGripShotSetting(GetGripShotSettingCallback callback)
			throws Exception {
	}

	@Override
	public void setGripShotSetting(GripShotSetting setting,
			OperationCallback callback) throws Exception {
	}

	@Override
	public void setGcToOobeMode(RequestCallback callback) throws Exception {
	}

	@Override
	public void setDeviceTime(Calendar time, OperationCallback callback)
			throws Exception {
	}

	@Override
	public void getSimHwStatus(GetSimHwStatusCallback callback)
			throws Exception {
	}

	@Override
	public void setCameraMode(CameraMode mode, OperationCallback callback)
			throws Exception {
	}

	@Override
	public void getCameraMode(GetCameraModeCallback callback) throws Exception {
	}

	@Override
	public void setModeChangeListener(ModeChangeListener l) {
	}

	@Override
	public void setBatteryLevelChangeListener(BatteryLevelChangeListener l) {
	}

	@Override
	public void setSpaceChangeListener(SpaceChangeListener l) {
	}

	@Override
	public void setRequestGpsInfoChangeListener(RequestGpsInfoChangeListener l) {
	}

	@Override
	public void setSdCardStatusListener(SdCardStatusListener l) {
	}

	@Override
	public void setTemperatureListener(TemperatureStatusListener l) {
	}

	@Override
	public void setUsbStorageListener(UsbStorageStatusListener l) {
	}

	@Override
	public void setPocketModeChangeListener(PocketModeChangeListener l) {
	}
	
	@Override
	public void setCameraModeChangeListener(CameraModeChangeListener l) {
	}
	
	@Override
	public void getLTECampingStatus(GetLTECampingStatusCallback callback) throws Exception {
	}
	
	@Override
	public void getSimInfo(GetSimInfoCallback callback) throws Exception {
	}
	
	@Override
	public void unlockSimPin(String pinCode, UnlockSimPinCallback callback) throws Exception {
	}

	@Override
	public void setSMSListener(SMSListener l) {
	}

	@Override
	public void setAllFwVersionListener(AllFwVersionListener l) {
	}
	
	@Override
	public void setFwUpdateResultListener(FwUpdateResultListener l) {
	}
	
	@Override
	public void setLTECampingStatusListener(LTECampingStatusListener l) {
	}
	
}
