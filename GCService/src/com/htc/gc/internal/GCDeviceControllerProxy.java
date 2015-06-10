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

public class GCDeviceControllerProxy implements IDeviceController {

	private IDeviceController mDeviceController = new NullGCDeviceController();

	public ModeChangeListener mModeChangeListener;
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

	public void setDeviceController(IDeviceController deviceController) {
		mDeviceController = deviceController;

		mDeviceController.setModeChangeListener(mModeChangeListener);
		mDeviceController.setBatteryLevelChangeListener(mBatteryLevelChangeListener);
		mDeviceController.setSpaceChangeListener(mSpaceChangeListener);
		mDeviceController.setRequestGpsInfoChangeListener(mRequestGpsInfoChangeListener);
		mDeviceController.setSdCardStatusListener(mSdCardStatusListener);
		mDeviceController.setTemperatureListener(mTemperatureStatusListener);
		mDeviceController.setUsbStorageListener(mUsbStorageStatusListener);
		mDeviceController.setPocketModeChangeListener(mPocketModeChangeListener);
		mDeviceController.setCameraModeChangeListener(mCameraModeChangeListener);
		mDeviceController.setSMSListener(mSMSListener);
		mDeviceController.setAllFwVersionListener(mAllFwVersionListener);
		mDeviceController.setFwUpdateResultListener(mFwUpdateResultListener);
		mDeviceController.setLTECampingStatusListener(mLTECampingStatusListener);
	}

	@Override
	public void getSpaceInfo(SpaceInfoCallback callback) throws Exception {
		mDeviceController.getSpaceInfo(callback);
	}

	@Override
	public void getBatteryInfo(BatteryInfoCallback callback) throws Exception {
		mDeviceController.getBatteryInfo(callback);
	}

	@Override
	public void getMode(ModeCallback callback) throws Exception {
		mDeviceController.getMode(callback);
	}

	@Override
	public void setMode(Mode mode, OperationCallback callback) throws Exception {
		mDeviceController.setMode(mode, callback);
	}

	@Override
	public void getDRStatus(StatusCallback callback) throws Exception {
		mDeviceController.getDRStatus(callback);
	}

	@Override
	public void formatSDCard(SDCardFormatType type, OperationCallback callback)
			throws Exception {
		mDeviceController.formatSDCard(type, callback);
	}

	@Override
	public void CheckSDCard(SDChangeCallback callback) throws Exception {
		mDeviceController.CheckSDCard(callback);
	}

	@Override
	public void getFileCountInStorage(Filter type,
			StorageFileCountCallback callback) throws Exception {
		mDeviceController.getFileCountInStorage(type, callback);
	}

	@Override
	public void getSpeakerMode(SpeakerModeCallback callback) throws Exception {
		mDeviceController.getSpeakerMode(callback);
	}

	@Override
	public void setSpeakerMode(SpeakerMode mode, OperationCallback callback)
			throws Exception {
		mDeviceController.setSpeakerMode(mode, callback);
	}

	@Override
	public void getDoubleClickMode(DoubleClickModeCallback callback)
			throws Exception {
		mDeviceController.getDoubleClickMode(callback);
	}

	@Override
	public void setDoubleClickMode(DoubleClickMode mode,
			OperationCallback callback) throws Exception {
		mDeviceController.setDoubleClickMode(mode, callback);
	}

	@Override
	public void getVideoRecordButtonConfig(
			VideoRecordButtonConfigCallback callback) throws Exception {
		mDeviceController.getVideoRecordButtonConfig(callback);
	}

	@Override
	public void setVideoRecordButtonConfig(VideoRecBtnConfig config,
			OperationCallback callback) throws Exception {
		mDeviceController.setVideoRecordButtonConfig(config, callback);
	}

	@Override
	public ICancelable uploadFile(URI srcFile, String destPath,
			UploadCallback callback) throws Exception {
		return mDeviceController.uploadFile(srcFile, destPath, callback);
	}

	@Override
	public void upgradeFirmware(byte selectFirmwareFlag, int bootCodeVersion,
			int mainCodeVersion, int mcuVersion, int bleVersion,
			OperationCallback callback) throws Exception {
		mDeviceController.upgradeFirmware(selectFirmwareFlag, bootCodeVersion,
				mainCodeVersion, mcuVersion, bleVersion, callback);
	}

	@Override
	public void getFirmwareVersion(FirmwareVersionCallback callback)
			throws Exception {
		mDeviceController.getFirmwareVersion(callback);
	}

	@Override
	public void triggerFirmwareUpdate(boolean updateA12, boolean updateModem,
			boolean updateMCU, String firmwareVersion,
			OperationCallback callback) throws Exception {
		mDeviceController.triggerFirmwareUpdate(updateA12, updateModem,
				updateMCU, firmwareVersion, callback);
	}

	@Override
	public void getStorageInUse(StorageInUseCallback callback) throws Exception {
		mDeviceController.getStorageInUse(callback);
	}

	@Override
	public void getAutoLevelStatus(AutoLevelStatusCallback callback)
			throws Exception {
		mDeviceController.getAutoLevelStatus(callback);
	}

	@Override
	public void setAutoLevelStatus(AutoLevelStatus status,
			OperationCallback callback) throws Exception {
		mDeviceController.setAutoLevelStatus(status, callback);
	}

	@Override
	public void getUpsideDownStatus(UpsideDownStatusCallback callback)
			throws Exception {
		mDeviceController.getUpsideDownStatus(callback);
	}

	@Override
	public void setUpsideDownStatus(UpsideDownStatus status,
			OperationCallback callback) throws Exception {
		mDeviceController.setUpsideDownStatus(status, callback);
	}

	@Override
	public void getGcGpsStatus(GcGpsStatusCallback callback) throws Exception {
		mDeviceController.getGcGpsStatus(callback);
	}

	@Override
	public void setGcGpsStatus(GcGpsStatus status, OperationCallback callback)
			throws Exception {
		mDeviceController.setGcGpsStatus(status, callback);
	}

	@Override
	public void getUsePhoneGpsSetting(UsePhoneGpsSettingCallback callback)
			throws Exception {
		mDeviceController.getUsePhoneGpsSetting(callback);
	}

	@Override
	public void setUsePhoneGpsSetting(UsePhoneGpsSetting setting,
			OperationCallback callback) throws Exception {
		mDeviceController.setUsePhoneGpsSetting(setting, callback);
	}

	@Override
	public void updateGpsInfo(Calendar calendar, double longitude,
			double latitude, double altitude, OperationCallback callback)
			throws Exception {
		mDeviceController.updateGpsInfo(calendar, longitude, latitude,
				altitude, callback);
	}

	@Override
	public void setDeviceName(IDeviceItem device, String name,
			OperationCallback callback) throws Exception {
		mDeviceController.setDeviceName(device, name, callback);
	}

	@Override
	public void resetSystem(OperationCallback callback) throws Exception {
		mDeviceController.resetSystem(callback);
	}

	@Override
	public void getErrorLogFromGC(DebugLogType type, boolean autoDelete,
			GetErrorLogFromGcCallback callback) throws Exception {
		mDeviceController.getErrorLogFromGC(type, autoDelete, callback);
	}

	@Override
	public void getSerialNumber(GetSerialNumberCallback callback)
			throws Exception {
		mDeviceController.getSerialNumber(callback);
	}

	@Override
	public void getCountryCode(GetCountryCodeCallback callback)
			throws Exception {
		mDeviceController.getCountryCode(callback);
	}

	@Override
	public void setAutoPowerOffTimeThisBootUp(short seconds,
			OperationCallback callback) throws Exception {
		mDeviceController.setAutoPowerOffTimeThisBootUp(seconds, callback);
	}

	@Override
	public void getPowerSavingMode(GetPowerSaveModeCallback callback)
			throws Exception {
		mDeviceController.getPowerSavingMode(callback);
	}

	@Override
	public void setPowerSavingMode(PowerSaveMode mode,
			OperationCallback callback) throws Exception {
		mDeviceController.setPowerSavingMode(mode, callback);
	}

	@Deprecated
	@Override
	public void getBtMacAddress(GetBtMacAddressCallback callback)
			throws Exception {
		mDeviceController.getBtMacAddress(callback);
	}

	@Override
	public void getDebugLogEnableSetting(GetDebugLogSettingCallback callback)
			throws Exception {
		mDeviceController.getDebugLogEnableSetting(callback);
	}

	@Override
	public void setDebugLogEnableSetting(DebugLogSetting setting,
			OperationCallback callback) throws Exception {
		mDeviceController.setDebugLogEnableSetting(setting, callback);
	}

	@Override
	public void getFakeShotSetting(GetFakeShotSettingCallback callback)
			throws Exception {
		mDeviceController.getFakeShotSetting(callback);
	}

	@Override
	public void setFakeShotSetting(FakeShotSetting setting,
			OperationCallback callback) throws Exception {
		mDeviceController.setFakeShotSetting(setting, callback);
	}

	@Override
	public void getGripShotSetting(GetGripShotSettingCallback callback)
			throws Exception {
		mDeviceController.getGripShotSetting(callback);
	}

	@Override
	public void setGripShotSetting(GripShotSetting setting,
			OperationCallback callback) throws Exception {
		mDeviceController.setGripShotSetting(setting, callback);
	}

	@Override
	public void setGcToOobeMode(RequestCallback callback) throws Exception {
		mDeviceController.setGcToOobeMode(callback);
	}

	@Override
	public void setDeviceTime(Calendar time, OperationCallback callback)
			throws Exception {
		mDeviceController.setDeviceTime(time, callback);
	}

	@Override
	public void getSimHwStatus(GetSimHwStatusCallback callback)
			throws Exception {
		mDeviceController.getSimHwStatus(callback);
	}

	@Override
	public void setCameraMode(CameraMode mode, OperationCallback callback)
			throws Exception {
		mDeviceController.setCameraMode(mode, callback);
	}

	@Override
	public void getCameraMode(GetCameraModeCallback callback) throws Exception {
		mDeviceController.getCameraMode(callback);
	}
	
	@Override
	public void getLTECampingStatus(GetLTECampingStatusCallback callback) throws Exception {
		mDeviceController.getLTECampingStatus(callback);
	}
	
	@Override
	public void getSimInfo(GetSimInfoCallback callback) throws Exception {
		mDeviceController.getSimInfo(callback);
	}
	
	@Override
	public void unlockSimPin(String pinCode, UnlockSimPinCallback callback) throws Exception {
		mDeviceController.unlockSimPin(pinCode, callback);
	}

	@Override
	public void setModeChangeListener(ModeChangeListener l) {
		mModeChangeListener = l;
		mDeviceController.setModeChangeListener(l);
	}

	@Override
	public void setBatteryLevelChangeListener(BatteryLevelChangeListener l) {
		mBatteryLevelChangeListener = l;
		mDeviceController.setBatteryLevelChangeListener(l);
	}

	@Override
	public void setSpaceChangeListener(SpaceChangeListener l) {
		mSpaceChangeListener = l;
		mDeviceController.setSpaceChangeListener(l);
	}

	@Override
	public void setRequestGpsInfoChangeListener(RequestGpsInfoChangeListener l) {
		mRequestGpsInfoChangeListener = l;
		mDeviceController.setRequestGpsInfoChangeListener(l);
	}

	@Override
	public void setSdCardStatusListener(SdCardStatusListener l) {
		mSdCardStatusListener = l;
		mDeviceController.setSdCardStatusListener(l);
	}

	@Override
	public void setTemperatureListener(TemperatureStatusListener l) {
		mTemperatureStatusListener = l;
		mDeviceController.setTemperatureListener(l);
	}

	@Override
	public void setUsbStorageListener(UsbStorageStatusListener l) {
		mUsbStorageStatusListener = l;
		mDeviceController.setUsbStorageListener(l);
	}

	@Override
	public void setPocketModeChangeListener(PocketModeChangeListener l) {
		mPocketModeChangeListener = l;
		mDeviceController.setPocketModeChangeListener(l);
	}
	
	@Override
	public void setCameraModeChangeListener(CameraModeChangeListener l) {
		mCameraModeChangeListener = l;
		mDeviceController.setCameraModeChangeListener(l);
	}

	@Override
	public void setSMSListener(SMSListener l) {
		mSMSListener = l;
		mDeviceController.setSMSListener(l);
	}
	
	@Override
	public void setAllFwVersionListener(AllFwVersionListener l) {
		mAllFwVersionListener = l;
		mDeviceController.setAllFwVersionListener(l);
	}
	
	@Override
	public void setFwUpdateResultListener(FwUpdateResultListener l) {
		mFwUpdateResultListener = l;
		mDeviceController.setFwUpdateResultListener(l);
	}
	
	@Override
	public void setLTECampingStatusListener(LTECampingStatusListener l) {
		mLTECampingStatusListener = l;
		mDeviceController.setLTECampingStatusListener(l);
	}

}
