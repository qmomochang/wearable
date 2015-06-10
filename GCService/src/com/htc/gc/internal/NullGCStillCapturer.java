package com.htc.gc.internal;

import com.htc.gc.interfaces.IStillCapturer;
import com.htc.gc.interfaces.Common.DataCallback;
import com.htc.gc.interfaces.Common.ICancelable;
import com.htc.gc.interfaces.Common.OperationCallback;

class NullGCStillCapturer implements IStillCapturer {

	@Override
	public void getImgRatio(ImgRatioCallback callback) throws Exception {
	}

	@Override
	public void setImgRatio(ImageRatio ratio, OperationCallback callback)
			throws Exception {
	}

	@Override
	public void getImgResolution(ImgResolutionCallback callback)
			throws Exception {
	}

	@Override
	public void setImgResolution(ImageResolution resolution,
			OperationCallback callback) throws Exception {
	}

	@Override
	public void getIsoStatus(IsoStatusCallback callback) throws Exception {
	}

	@Override
	public void setIsoStatus(IsoStatus status, OperationCallback callback)
			throws Exception {
	}

	@Override
	public void getWbStatus(WbStatusCallback callback) throws Exception {
	}

	@Override
	public void setWbStatus(WbStatus status, OperationCallback callback)
			throws Exception {
	}

	@Override
	public void getWideAngleMode(WideAngleModeCallback callback)
			throws Exception {
	}

	@Override
	public void setWideAngleMode(WideAngleMode mode, OperationCallback callback)
			throws Exception {
	}

	@Override
	public void captureStill(OperationCallback callback) throws Exception {
	}

	@Override
	public void getTimeLapseRate(TimeLapseRateCallback callback)
			throws Exception {
	}

	@Override
	public void setTimeLapseRate(int sec, OperationCallback callback)
			throws Exception {
	}

	@Override
	public void getTimeLapseDuration(TimeLapseDurationCallback callback)
			throws Exception {
	}

	@Override
	public void setTimeLapseDuration(int min, OperationCallback callback)
			throws Exception {
	}

	@Override
	public void getTimeLapseFrameRate(TimeLapseFrameRateCallback callback)
			throws Exception {
	}

	@Override
	public void setTimeLapseFrameRate(byte rate, OperationCallback callback)
			throws Exception {
	}

	@Override
	public void getTimeLapseLedSetting(TimeLapseLedSettingCallback callback)
			throws Exception {
	}

	@Override
	public void setTimeLapseLedSetting(LedSwitch onOff,
			OperationCallback callback) throws Exception {
	}

	@Override
	public void getTimeLapseAutoStopSetting(TimeLapseAutoStopCallback callback)
			throws Exception {
	}

	@Override
	public void setTimeLapseAutoStopSetting(TimeLapseAutoStop autoStop,
			OperationCallback callback) throws Exception {
	}

	@Override
	public void setFaceCountTimesSetting(FaceCountTimes times,
			OperationCallback callback) throws Exception {
	}

	@Override
	public void getFaceCountTimesSetting(FaceCountTimesCallback callback)
			throws Exception {
	}

	@Override
	public void captureTimeLapseStart(OperationCallback callback)
			throws Exception {
	}

	@Override
	public void captureTimeLapseStop(OperationCallback callback)
			throws Exception {
	}

	@Override
	public void captureTimeLapsePause(OperationCallback callback)
			throws Exception {
	}

	@Override
	public void captureTimeLapseResume(OperationCallback callback)
			throws Exception {
	}

	@Override
	public ICancelable getCaptureQVImage(DataCallback callback)
			throws Exception {
		return null;
	}

	@Override
	public void setCaptureListener(CaptureListener l) {
	}

	@Override
	public void setTimeLapseListener(TimeLapseListener l) {
	}

}
