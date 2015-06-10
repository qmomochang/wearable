package com.htc.gc.interfaces;

import android.net.Uri;

import com.htc.gc.interfaces.Common.ErrorCallback;
import com.htc.gc.interfaces.Common.OperationCallback;

public interface ILiveViewer {
	public enum LiveStreamResolution {
		RTSP_STREAM_SIZE_S	((byte)0x1),
		RTSP_STREAM_SIZE_M	((byte)0x2),
		RTSP_STREAM_SIZE_L	((byte)0x3);
		
		private final byte mVal;
		LiveStreamResolution(byte val) { mVal = val; }
		public byte getVal() { return mVal; } 
		public static LiveStreamResolution getKey(byte val) throws Common.NoImpException {
			for(LiveStreamResolution res : LiveStreamResolution.values()) {
				if(res.getVal() == val) {
					return res;
				}
			}
			throw new Common.NoImpException();
		}
	}
	
	public enum LiveStreamFrameRate {
		RTSP_STREAM_FRAMERATE_15	((short)1500),
		RTSP_STREAM_FRAMERATE_24	((short)2400),
		RTSP_STREAM_FRAMERATE_30	((short)3000);
		
		private final short mVal;
		LiveStreamFrameRate(short val) { mVal = val; }
		public short getVal() { return mVal; } 
		public static LiveStreamFrameRate getKey(short val) throws Common.NoImpException {
			for(LiveStreamFrameRate res : LiveStreamFrameRate.values()) {
				if(res.getVal() == val) {
					return res;
				}
			}
			throw new Common.NoImpException();
		}
	}
	
	public enum LiveStreamCompressRate {
		RTSP_STREAM_COMPRESS_RATE_LOW		((byte)0x0),
		RTSP_STREAM_COMPRESS_RATE_MEDIUM	((byte)0x1),
		RTSP_STREAM_COMPRESS_RATE_HIGH		((byte)0x2);
		
		private final byte mVal;
		LiveStreamCompressRate(byte val) { mVal = val; }
		public byte getVal() { return mVal; } 
		public static LiveStreamCompressRate getKey(byte val) throws Common.NoImpException {
			for(LiveStreamCompressRate res : LiveStreamCompressRate.values()) {
				if(res.getVal() == val) {
					return res;
				}
			}
			throw new Common.NoImpException();
		}
	}
	
	public enum LiveViewMode {
		LIVE_VIEW_MODE_STILL		((byte)0x0),
		LIVE_VIEW_MODE_VIDEO		((byte)0x1),
		LIVE_VIEW_MODE_TIMELAPSE	((byte)0x2),
		LIVE_VIEW_MODE_SLOWMOTION	((byte)0x3);
		
		private final byte mVal;
		LiveViewMode(byte val) { mVal = val; }
		public byte getVal() { return mVal; } 
		public static LiveViewMode getKey(byte val) throws Common.NoImpException {
			for(LiveViewMode res : LiveViewMode.values()) {
				if(res.getVal() == val) {
					return res;
				}
			}
			throw new Common.NoImpException();
		}
	}
	
	public interface ILiveViewStreamReadyListener extends ErrorCallback {
		public void onReady(ILiveViewer that);
	}
	
	public interface StartLiveViewCallback extends ErrorCallback {
		public void result(ILiveViewer that, Uri result);
	}
	
	public interface LiveStreamResolutionCallback extends ErrorCallback {
		public void result(ILiveViewer that, LiveStreamResolution resolution);
	}
	
	public interface LiveStreamFrameRateCallback extends ErrorCallback {
		public void result(ILiveViewer that, LiveStreamFrameRate rate);
	}
	
	public interface LiveStreamCompressRateCallback extends ErrorCallback {
		public void result(ILiveViewer that, LiveStreamCompressRate rate);
	}
	
	public interface GetLiveViewModeCallback extends ErrorCallback {
		public void result(ILiveViewer that, LiveViewMode mode);
	}

	public void startLiveView(StartLiveViewCallback callback) throws Exception;
	public void stopLiveView(OperationCallback callback) throws Exception;
	
	public void setLiveStreamResolution(LiveStreamResolution res, OperationCallback callback) throws Exception;
	public void getLiveStreamResolution(LiveStreamResolutionCallback callback) throws Exception;
	public void setLiveStreamFrameRate(LiveStreamFrameRate rate, OperationCallback callback) throws Exception;
	public void getLiveStreamFrameRate(LiveStreamFrameRateCallback callback) throws Exception;
	public void setLiveStreamCompressRate(LiveStreamCompressRate rate, OperationCallback callback) throws Exception;
	public void getLiveStreamCompressRate(LiveStreamCompressRateCallback callback) throws Exception;
	
	public void setLiveViewMode(LiveViewMode mode, OperationCallback callback) throws Exception;
	public void getLiveViewMode(GetLiveViewModeCallback callback) throws Exception;
	
	public void setLiveViewStreamReadyListener(ILiveViewStreamReadyListener l);
}
