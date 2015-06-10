package com.htc.gc.interfaces;

import com.htc.gc.interfaces.Common.DataCallback;
import com.htc.gc.interfaces.Common.ErrorCallback;
import com.htc.gc.interfaces.Common.ICancelable;
import com.htc.gc.interfaces.Common.OperationCallback;

public interface IStillCapturer {
	
	public enum ImageRatio {
		IMG_RATIO_16_9	((byte)0x2),
		IMG_RATIO_4_3	((byte)0x0);
		
		private final byte mVal;
		ImageRatio(byte val) { mVal = val; }
		public byte getVal() { return mVal; } 
		public static ImageRatio getKey(byte val) throws Common.NoImpException {
			for(ImageRatio res : ImageRatio.values()) {
				if(res.getVal() == val) {
					return res;
				}
			}
			throw new Common.NoImpException();
		}
	}
	
	public enum PreviewSetting {
		PREVIEW_ON	((byte)0x1),
		PREVIEW_OFF	((byte)0x2);
		
		private final byte mVal;
		PreviewSetting(byte val) { mVal = val; }
		public byte getVal() { return mVal; } 
		public static PreviewSetting getKey(byte val) throws Common.NoImpException {
			for(PreviewSetting res : PreviewSetting.values()) {
				if(res.getVal() == val) {
					return res;
				}
			}
			throw new Common.NoImpException();
		}
	}
	
	
	public enum IsoStatus {
		ISO_AUTO	((byte)0x00),
		ISO_100		((byte)0x04),
		ISO_200		((byte)0x09),
		ISO_400		((byte)0x0D),
		ISO_800		((byte)0x10),
		ISO_1600	((byte)0x13);
		
		private final byte mVal;
		IsoStatus(byte val) { mVal = val; }
		public byte getVal() { return mVal; } 
		public static IsoStatus getKey(byte val) throws Common.NoImpException {
			for(IsoStatus res : IsoStatus.values()) {
				if(res.getVal() == val) {
					return res;
				}
			}
			throw new Common.NoImpException();
		}
	}
	
	public enum WbStatus {
		AWB_AUTO			((byte)0x0),
		AWB_DAYLIGHT		((byte)0x1),
		AWB_CLOUDY			((byte)0xA),
		AWB_FLUORESCENT		((byte)0x2),
		AWB_TUNGSTEN		((byte)0x3);
		//AWB_UNDERWATER		((byte)0x13);
		
		private final byte mVal;
		WbStatus(byte val) { mVal = val; }
		public byte getVal() { return mVal; } 
		public static WbStatus getKey(byte val) throws Common.NoImpException {
			for(WbStatus res : WbStatus.values()) {
				if(res.getVal() == val) {
					return res;
				}
			}
			throw new Common.NoImpException();
		}
	}
	
	public enum WideAngleMode {
		WIDE_ANGLE_OFF	((byte)0x0),
		WIDE_ANGLE_ON	((byte)0x1);

		private final byte mVal;
		WideAngleMode(byte val) { mVal = val; }
		public byte getVal() { return mVal; } 
		public static WideAngleMode getKey(byte val) throws Common.NoImpException {
			for(WideAngleMode res : WideAngleMode.values()) {
				if(res.getVal() == val) {
					return res;
				}
			}
			throw new Common.NoImpException();
		}
	}
	
	public enum LedSwitch {
		LED_ON	((byte)0x1),
		LED_OFF	((byte)0x0);

		private final byte mVal;
		LedSwitch(byte val) { mVal = val; }
		public byte getVal() { return mVal; } 
		public static LedSwitch getKey(byte val) throws Common.NoImpException {
			for(LedSwitch res : LedSwitch.values()) {
				if(res.getVal() == val) {
					return res;
				}
			}
			throw new Common.NoImpException();
		}
	}
	
	public enum ImageResolution {
		IMAGERESOLUTION_SMALL	((byte)0x0),
		IMAGERESOLUTION_MEDIUM	((byte)0x1),
		IMAGERESOLUTION_LARGE	((byte)0x2);

		private final byte mVal;
		ImageResolution(byte val) { mVal = val; }
		public byte getVal() { return mVal; } 
		public static ImageResolution getKey(byte val) throws Common.NoImpException {
			for(ImageResolution res : ImageResolution.values()) {
				if(res.getVal() == val) {
					return res;
				}
			}
			throw new Common.NoImpException();
		}
	}
	
	public enum TimeLapseAutoStop {
		TIMELAPSE_AUTOSTOP_OFF	((byte)0x0),
		TIMELAPSE_AUTOSTOP_ON	((byte)0x1);
		
		private final byte mVal;
		TimeLapseAutoStop(byte val) { mVal = val; }
		public byte getVal() { return mVal; } 
		public static TimeLapseAutoStop getKey(byte val) throws Common.NoImpException {
			for(TimeLapseAutoStop res : TimeLapseAutoStop.values()) {
				if(res.getVal() == val) {
					return res;
				}
			}
			throw new Common.NoImpException();
		}
	}
	
	public enum FaceCountTimes {
		FACE_COUNT_TIMES_ONE ((byte)0x1),
		FACE_COUNT_TIMES_TWO ((byte)0x2);
		
		private final byte mVal;
		FaceCountTimes(byte val) { mVal = val; }
		public byte getVal() { return mVal; } 
		public static FaceCountTimes getKey(byte val) throws Common.NoImpException {
			for(FaceCountTimes res : FaceCountTimes.values()) {
				if(res.getVal() == val) {
					return res;
				}
			}
			throw new Common.NoImpException();
		}
	}
	
	public interface CaptureListener {
		public void onCapture(IStillCapturer that);
		public void onCaptureStop(IStillCapturer that);
		public void onCaptureQVComplete(IStillCapturer that, IMediaItem item);
		public void onCaptureComplete(IStillCapturer that, int fileType, int ready);
		public void onError(IStillCapturer that, Exception e);
	}
	
	public interface TimeLapseListener {
		public void onCaptureTimeLapseOne(IStillCapturer that, int currentShotIdx, int freeRemainCount, int totalFrameCount);
	}
	
	public interface ImgRatioCallback extends ErrorCallback {
		void result(IStillCapturer that, ImageRatio ratio);
	}
	
	public interface TimeLapseDurationCallback extends ErrorCallback {
		void result(IStillCapturer that, int min);
	}
	
	public interface TimeLapseRateCallback extends ErrorCallback {
		void result(IStillCapturer that, int sec);
	}
	
	public interface PreviewSettingCallback extends ErrorCallback {
		void result(IStillCapturer that, PreviewSetting setting);
	}
	
	public interface IsoStatusCallback extends ErrorCallback {
		void result(IStillCapturer that, IsoStatus status);
	}
	
	public interface WbStatusCallback extends ErrorCallback {
		void result(IStillCapturer that, WbStatus status);
	}
	
	public interface WideAngleModeCallback extends ErrorCallback {
		void result(IStillCapturer that, WideAngleMode mode);
	}
	
	public interface TimeLapseFrameRateCallback extends ErrorCallback {
		void result(IStillCapturer that, byte rate);
	}
	
	public interface TimeLapseLedSettingCallback extends ErrorCallback {
		void result(IStillCapturer that, LedSwitch onOff);
	}
	
	public interface ImgResolutionCallback extends ErrorCallback {
		void result(IStillCapturer that, ImageResolution resolution);
	}
	
	public interface TimeLapseAutoStopCallback extends ErrorCallback {
		void result(IStillCapturer that, TimeLapseAutoStop autoStop);
	}
	
	public interface FaceCountTimesCallback extends ErrorCallback {
		void result(IStillCapturer that, FaceCountTimes count);
	}
	
	public void getImgRatio(ImgRatioCallback callback) throws Exception;
	public void setImgRatio(ImageRatio ratio, OperationCallback callback) throws Exception;
	
	public void getImgResolution(ImgResolutionCallback callback) throws Exception;
	public void setImgResolution(ImageResolution resolution, OperationCallback callback) throws Exception;
	
	public void getIsoStatus(IsoStatusCallback callback) throws Exception;
	public void setIsoStatus(IsoStatus status, OperationCallback callback) throws Exception;
	
	@Deprecated
	public void getWbStatus(WbStatusCallback callback) throws Exception;
	@Deprecated
	public void setWbStatus(WbStatus status, OperationCallback callback) throws Exception;
	
	public void getWideAngleMode(WideAngleModeCallback callback) throws Exception;
	public void setWideAngleMode(WideAngleMode mode, OperationCallback callback) throws Exception;
	
	public void captureStill(OperationCallback callback) throws Exception;
	
	public void getTimeLapseRate(TimeLapseRateCallback callback) throws Exception;
	public void setTimeLapseRate(int sec, OperationCallback callback) throws Exception;
	public void getTimeLapseDuration(TimeLapseDurationCallback callback) throws Exception;
	public void setTimeLapseDuration(int min, OperationCallback callback) throws Exception;
	public void getTimeLapseFrameRate(TimeLapseFrameRateCallback callback) throws Exception;
	public void setTimeLapseFrameRate(byte rate, OperationCallback callback) throws Exception;
	public void getTimeLapseLedSetting(TimeLapseLedSettingCallback callback) throws Exception;
	public void setTimeLapseLedSetting(LedSwitch onOff, OperationCallback callback) throws Exception;
	public void getTimeLapseAutoStopSetting(TimeLapseAutoStopCallback callback) throws Exception;
	public void setTimeLapseAutoStopSetting(TimeLapseAutoStop autoStop, OperationCallback callback) throws Exception;
	
	public void setFaceCountTimesSetting(FaceCountTimes times, OperationCallback callback) throws Exception;
	public void getFaceCountTimesSetting(FaceCountTimesCallback callback) throws Exception;

	public void captureTimeLapseStart(OperationCallback callback) throws Exception;
	public void captureTimeLapseStop(OperationCallback callback) throws Exception;
	public void captureTimeLapsePause(OperationCallback callback) throws Exception;
	public void captureTimeLapseResume(OperationCallback callback) throws Exception;
	

	public ICancelable getCaptureQVImage(DataCallback callback) throws Exception;
	
	public void setCaptureListener(CaptureListener l);
	public void setTimeLapseListener(TimeLapseListener l);
}
