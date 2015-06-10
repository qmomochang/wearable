package com.htc.gc.interfaces;

import java.util.List;

import com.htc.gc.interfaces.Common.DataCallback;
import com.htc.gc.interfaces.Common.ErrorCallback;
import com.htc.gc.interfaces.Common.ICancelable;
import com.htc.gc.interfaces.Common.OperationCallback;

public interface IVideoRecorder {

	public enum VideoResolution {
		UI_VIDEO_SIZE_1920X1080	((byte)0x4),
		UI_VIDEO_SIZE_1280X720	((byte)0x0);
		
		private final byte mVal;
		VideoResolution(byte val) { mVal = val; }
		public byte getVal() { return mVal; } 
		public static VideoResolution getKey(byte val) throws Common.NoImpException {
			for(VideoResolution res : VideoResolution.values()) {
				if(res.getVal() == val) {
					return res;
				}
			}
			throw new Common.NoImpException();
		}
	}
	
	public enum VideoMode {
		VIDEO_MODE_NORMAL		((byte)0x0),
		VIDEO_MODE_SLOWMOTION	((byte)0x1);
		
		private final byte mVal;
		VideoMode(byte val) { mVal = val; }
		public byte getVal() { return mVal; }
		public static VideoMode getKey(byte val) throws Common.NoImpException {
			for(VideoMode mode : VideoMode.values()) {
				if(mode.getVal() == val) {
					return mode;
				}
			}
			throw new Common.NoImpException();
		}
	}
	
	public enum BroadcastEnableSetting {
		VIDEO_BROADCASTING_OFF	((byte)0x0),
		VIDEO_BROADCASTING_ON	((byte)0x1);
		
		private final byte mVal;
		BroadcastEnableSetting(byte val) { mVal = val; }
		public byte getVal() { return mVal; }
		public static BroadcastEnableSetting getKey(byte val) throws Common.NoImpException {
			for(BroadcastEnableSetting mode : BroadcastEnableSetting.values()) {
				if(mode.getVal() == val) {
					return mode;
				}
			}
			throw new Common.NoImpException();
		}
	}
	
	public enum BroadcastPlatform {
		BROADCAST_PLATFORM_NONE		((byte)0x0),
		BROADCAST_PLATFORM_YOUTUBE	((byte)0x1),
		BROADCAST_PLATFORM_LL		((byte)0x2);
		
		private final byte mVal;
		BroadcastPlatform(byte val) { mVal = val; }
		public byte getVal() { return mVal; }
		public static BroadcastPlatform getKey(byte val) throws Common.NoImpException {
			for (BroadcastPlatform platform : BroadcastPlatform.values()) {
				if (platform.getVal() == val) {
					return platform;
				}
			}
			throw new Common.NoImpException();
		}
	}
	
	public enum TokenType {
		TOKENTYPE_ACCESS 		((byte) 0x0),
		TOKENTYPE_REFRESH 		((byte) 0x1);
		
		private final byte mVal;
		TokenType(byte val) { mVal = val; }
		public byte getVal() { return mVal; } 
		public static TokenType getKey(byte val) throws Common.NoImpException {
			for(TokenType token : TokenType.values()) {
				if(token.getVal() == val) {
					return token;
				}
			}
			throw new Common.NoImpException();
		}
	}
	
	public enum BroadcastPrivacy {
		BROADCAST_PRIVACY_NONPUBLIC	((byte)0x00), 
		BROADCAST_PRIVACY_PUBLIC	((byte)0x01);
		
		private final byte mVal;
		BroadcastPrivacy(byte val) { mVal = val; }
		public byte getVal() { return mVal; } 
		public static BroadcastPrivacy getKey(byte val) throws Common.NoImpException {
			for(BroadcastPrivacy privacy : BroadcastPrivacy.values()) {
				if(privacy.getVal() == val) {
					return privacy;
				}
			}
			throw new Common.NoImpException();
		}
	}
	
	public enum BroadcastStatus {
		BROADCAST_STATUS_STOPPED	((byte)0x00),
		BROADCAST_STATUS_STARTED	((byte)0x01);
		
		private final byte mVal;
		BroadcastStatus(byte val) { mVal = val; }
		public byte getVal() { return mVal; }
		public static BroadcastStatus getKey(byte val) throws Common.NoImpException {
			for(BroadcastStatus status : BroadcastStatus.values()) {
				if (status.getVal() == val) {
					return status;
				}
			}
			throw new Common.NoImpException();
		}
	}
	
	public enum BroadcastError {
		BROADCAST_ERROR_SUCCESS 				((byte)0x00),
		BROADCAST_ERROR_TOKENS_INVALID			((byte)0x01),
		BROADCAST_ERROR_CONNECTION_FAILURE		((byte)0x02),
		BROADCAST_ERROR_GET_STATUS_FAILURE		((byte)0x03),
		BROADCAST_ERROR_NO_DATA_NETWORK			((byte)0x04),
		BROADCAST_ERROR_NO_ENOUGH_SPACE_ON_RE	((byte)0x05),
		BROADCAST_ERROR_LOW_POWER				((byte)0x06),
		BROADCAST_ERROR_NO_SIM_CARD				((byte)0x07),
		BROADCAST_ERROR_GET_PREFERENCE_FAILURE	((byte)0x08);
		
		private final byte mVal;
		private BroadcastError(byte val) { mVal = val; }
		public byte getVal() { return mVal; }
		public static BroadcastError getKey(byte val) throws Common.NoImpException {
			for (BroadcastError broadcastError : BroadcastError.values()) {
				if (broadcastError.getVal() == val) {
					return broadcastError;
				}
			}
			throw new Common.NoImpException();
		}
	}
	
	public enum SlowMotionEnableSetting {
		VIDEO_SLOWMOTION_OFF	((byte)0x0),
		VIDEO_SLOWMOTION_ON		((byte)0x1);
		
		private final byte mVal;
		SlowMotionEnableSetting(byte val) { mVal = val; }
		public byte getVal() { return mVal; }
		public static SlowMotionEnableSetting getKey(byte val) throws Common.NoImpException {
			for(SlowMotionEnableSetting mode : SlowMotionEnableSetting.values()) {
				if(mode.getVal() == val) {
					return mode;
				}
			}
			throw new Common.NoImpException();
		}
	}
	
	public interface RecordListener {
		public void onRecord(IVideoRecorder that);
		public void onRecordStop(IVideoRecorder that);
		public void onRecordQVComplete(IVideoRecorder that, IMediaItem item);

		public void onRecordComplete(IVideoRecorder that, int ready);
		public void onError(IVideoRecorder that, Exception e);
	}
	
	public interface BroadcastListener {
		public void onBroadcastVideoCreated(long sequenceNumber);
	}
	
	public interface BroadcastVideoUrlListener {
		public void onReceived(String videoUrl);
	}
	
	public interface BroadcastErrorListener {
		public void onError(BroadcastError error, String errorTimestamp);
	}
	
	public interface BroadcastLiveStatusListener {
		public void onLiveBegin();
		public void onLiveEnd();
	}
	
	public interface SlowMotionEnableListener {
		public void onSlowMotionEnabled(boolean enabled);
	}
	
	public interface ResolutionCallback extends ErrorCallback {
		void result(IVideoRecorder that, VideoResolution resolution);
	}
	
	public interface BroadcastEnableSettingCallback extends ErrorCallback {
		void result(IVideoRecorder that, BroadcastEnableSetting setting);
	}
	
	public interface BroadcastStatusCallback extends ErrorCallback {
		void result(IVideoRecorder that, BroadcastStatus status);
	}
	
	public interface BroadcastInvitationListCallback extends ErrorCallback {
		void result(IVideoRecorder that, List<String> invitationList);
	}
	
	public interface BroadcastPrivacyCallback extends ErrorCallback {
		void result(IVideoRecorder that, BroadcastPrivacy privacy);
	}
	
	public interface BroadcastPlatformCallback extends ErrorCallback {
		void result(IVideoRecorder that, BroadcastPlatform broadcastPlatform);
	}
	
	public interface BroadcastVideoUrlCallback extends ErrorCallback {
		void result(IVideoRecorder that, String videoUrl);
	}
	
	public interface BroadcastErrorListCallback extends ErrorCallback {
		void result(IVideoRecorder that, List<BroadcastError> errorList);
	}
	
	public interface BroadcastUserNameCallback extends ErrorCallback {
		void result(IVideoRecorder that, String userName);
	}
	
	public interface BroadcastSMSContentCallback extends ErrorCallback {
		void result(IVideoRecorder that, String smsContent);
	}
	
	public interface SlowMotionEnableSettingCallback extends ErrorCallback {
		void result(IVideoRecorder that, SlowMotionEnableSetting setting);
	}

	public void recordStart(OperationCallback callback) throws Exception;
	public void recordStartSlowMotion(OperationCallback callback) throws Exception;
	public void recordStop(OperationCallback callback) throws Exception;
	public void recordStartBroadcast(OperationCallback callback) throws Exception;
	public void recordStopBroadcast(OperationCallback callback) throws Exception;

	public ICancelable getRecordQVImage(DataCallback callback) throws Exception;
	
	public void getResolution(ResolutionCallback callback) throws Exception;
	public void setResolution(VideoResolution resolution, OperationCallback callback) throws Exception;
	
	public void getBroadcastEnableSetting(BroadcastEnableSettingCallback callback) throws Exception;
	public void setBroadcastEnableSetting(BroadcastEnableSetting setting, OperationCallback callback) throws Exception;
	
	public void setBroadcastPlatform(BroadcastPlatform platform, TokenType tokenType, String token, OperationCallback callback) throws Exception;
	public void setBroadcastInvitationList(List<String> invitationList, OperationCallback callback) throws Exception;
	
	public void setBroadcastPrivacy(BroadcastPrivacy privacy, OperationCallback callback) throws Exception;
	
	public void getBroadcastStatus(BroadcastStatusCallback callback) throws Exception;
	public void getBroadcastInvitationList(BroadcastInvitationListCallback callback) throws Exception;
	public void getBroadcastPrivacy(BroadcastPrivacyCallback callback) throws Exception;
	public void getBroadcastPlatform(BroadcastPlatformCallback callback) throws Exception;
	public void getBroadcastVideoUrl(BroadcastVideoUrlCallback callback) throws Exception;
	public void getBroadcastErrorList(BroadcastErrorListCallback callback) throws Exception;
	
	public void setBroadcastUserName(String userName, OperationCallback callback) throws Exception;
	public void setBroadcastSMSContent(String smsContent, OperationCallback callback) throws Exception;
	public void getBroadcastUserName(BroadcastUserNameCallback callback) throws Exception;
	public void getBroadcastSMSContent(BroadcastSMSContentCallback callback) throws Exception;

	public void getSlowMotionEnableSetting(SlowMotionEnableSettingCallback callback) throws Exception;
	public void setSlowMotionEnableSetting(SlowMotionEnableSetting setting, OperationCallback callback) throws Exception;
	
	public void setRecordListener(RecordListener l);
	public void setBroadcastListener(BroadcastListener l);
	public void setBroadcastVideoUrlListener(BroadcastVideoUrlListener l);
	public void setBroadcastErrorListener(BroadcastErrorListener l);
	public void setBroadcastLiveStatusListener(BroadcastLiveStatusListener l);
	public void setSlowMotionEnableListener(SlowMotionEnableListener l);
}
