package com.htc.gc.internal;

import com.htc.gc.interfaces.IStillCapturer;
import com.htc.gc.interfaces.Common.DataCallback;
import com.htc.gc.interfaces.Common.ICancelable;
import com.htc.gc.interfaces.Common.OperationCallback;

public class GCStillCapturerProxy implements IStillCapturer {
	
	private IStillCapturer mStillCapturer = new NullGCStillCapturer();
	
	private CaptureListener mCaptureListener;
	private TimeLapseListener mTimeLapseListener;
	
	public void setStillCapturer(IStillCapturer stillCapturer) {
		mStillCapturer = stillCapturer;
		
		mStillCapturer.setCaptureListener(mCaptureListener);
		mStillCapturer.setTimeLapseListener(mTimeLapseListener);
	}

	@Override
	public void getImgRatio(ImgRatioCallback callback) throws Exception {
		mStillCapturer.getImgRatio(callback);
	}

	@Override
	public void setImgRatio(ImageRatio ratio, OperationCallback callback)
			throws Exception {
		mStillCapturer.setImgRatio(ratio, callback);
	}

	@Override
	public void getImgResolution(ImgResolutionCallback callback)
			throws Exception {
		mStillCapturer.getImgResolution(callback);
	}

	@Override
	public void setImgResolution(ImageResolution resolution,
			OperationCallback callback) throws Exception {
		mStillCapturer.setImgResolution(resolution, callback);
	}

	@Override
	public void getIsoStatus(IsoStatusCallback callback) throws Exception {
		mStillCapturer.getIsoStatus(callback);
	}

	@Override
	public void setIsoStatus(IsoStatus status, OperationCallback callback)
			throws Exception {
		mStillCapturer.setIsoStatus(status, callback);
	}

	@Deprecated
	@Override
	public void getWbStatus(WbStatusCallback callback) throws Exception {
		mStillCapturer.getWbStatus(callback);
	}

	@Deprecated
	@Override
	public void setWbStatus(WbStatus status, OperationCallback callback)
			throws Exception {
		mStillCapturer.setWbStatus(status, callback);
	}

	@Override
	public void getWideAngleMode(WideAngleModeCallback callback)
			throws Exception {
		mStillCapturer.getWideAngleMode(callback);
	}

	@Override
	public void setWideAngleMode(WideAngleMode mode, OperationCallback callback)
			throws Exception {
		mStillCapturer.setWideAngleMode(mode, callback);
	}

	@Override
	public void captureStill(OperationCallback callback) throws Exception {
		mStillCapturer.captureStill(callback);
	}

	@Override
	public void getTimeLapseRate(TimeLapseRateCallback callback)
			throws Exception {
		mStillCapturer.getTimeLapseRate(callback);
	}

	@Override
	public void setTimeLapseRate(int sec, OperationCallback callback)
			throws Exception {
		mStillCapturer.setTimeLapseRate(sec, callback);
	}

	@Override
	public void getTimeLapseDuration(TimeLapseDurationCallback callback)
			throws Exception {
		mStillCapturer.getTimeLapseDuration(callback);
	}

	@Override
	public void setTimeLapseDuration(int min, OperationCallback callback)
			throws Exception {
		mStillCapturer.setTimeLapseDuration(min, callback);
	}

	@Override
	public void getTimeLapseFrameRate(TimeLapseFrameRateCallback callback)
			throws Exception {
		mStillCapturer.getTimeLapseFrameRate(callback);
	}

	@Override
	public void setTimeLapseFrameRate(byte rate, OperationCallback callback)
			throws Exception {
		mStillCapturer.setTimeLapseFrameRate(rate, callback);
	}

	@Override
	public void getTimeLapseLedSetting(TimeLapseLedSettingCallback callback)
			throws Exception {
		mStillCapturer.getTimeLapseLedSetting(callback);
	}

	@Override
	public void setTimeLapseLedSetting(LedSwitch onOff,
			OperationCallback callback) throws Exception {
		mStillCapturer.setTimeLapseLedSetting(onOff, callback);
	}

	@Override
	public void getTimeLapseAutoStopSetting(TimeLapseAutoStopCallback callback)
			throws Exception {
		mStillCapturer.getTimeLapseAutoStopSetting(callback);
	}

	@Override
	public void setTimeLapseAutoStopSetting(TimeLapseAutoStop autoStop,
			OperationCallback callback) throws Exception {
		mStillCapturer.setTimeLapseAutoStopSetting(autoStop, callback);
	}

	@Override
	public void setFaceCountTimesSetting(FaceCountTimes times,
			OperationCallback callback) throws Exception {
		mStillCapturer.setFaceCountTimesSetting(times, callback);
	}

	@Override
	public void getFaceCountTimesSetting(FaceCountTimesCallback callback)
			throws Exception {
		mStillCapturer.getFaceCountTimesSetting(callback);
	}

	@Override
	public void captureTimeLapseStart(OperationCallback callback)
			throws Exception {
		mStillCapturer.captureTimeLapseStart(callback);
	}

	@Override
	public void captureTimeLapseStop(OperationCallback callback)
			throws Exception {
		mStillCapturer.captureTimeLapseStop(callback);
	}

	@Override
	public void captureTimeLapsePause(OperationCallback callback)
			throws Exception {
		mStillCapturer.captureTimeLapsePause(callback);
	}

	@Override
	public void captureTimeLapseResume(OperationCallback callback)
			throws Exception {
		mStillCapturer.captureTimeLapseResume(callback);
	}

	@Override
	public ICancelable getCaptureQVImage(DataCallback callback)
			throws Exception {
		return mStillCapturer.getCaptureQVImage(callback);
	}

	@Override
	public void setCaptureListener(CaptureListener l) {
		mCaptureListener = l;
		mStillCapturer.setCaptureListener(l);
	}

	@Override
	public void setTimeLapseListener(TimeLapseListener l) {
		mTimeLapseListener = l;
		mStillCapturer.setTimeLapseListener(l);
	}

}
