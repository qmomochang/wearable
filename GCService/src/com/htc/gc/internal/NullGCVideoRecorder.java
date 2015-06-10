package com.htc.gc.internal;

import java.util.List;

import com.htc.gc.interfaces.IVideoRecorder;
import com.htc.gc.interfaces.Common.DataCallback;
import com.htc.gc.interfaces.Common.ICancelable;
import com.htc.gc.interfaces.Common.OperationCallback;

class NullGCVideoRecorder implements IVideoRecorder {

	@Override
	public void recordStart(OperationCallback callback) throws Exception {
	}

	@Override
	public void recordStartSlowMotion(OperationCallback callback)
			throws Exception {
	}

	@Override
	public void recordStop(OperationCallback callback) throws Exception {
	}
	
	@Override
	public void recordStartBroadcast(OperationCallback callback) throws Exception {
	}
	
	@Override
	public void recordStopBroadcast(OperationCallback callback) throws Exception {
	}

	@Override
	public ICancelable getRecordQVImage(DataCallback callback) throws Exception {
		return null;
	}

	@Override
	public void getResolution(ResolutionCallback callback) throws Exception {
	}

	@Override
	public void setResolution(VideoResolution resolution,
			OperationCallback callback) throws Exception {
	}

	@Override
	public void getBroadcastEnableSetting(
			BroadcastEnableSettingCallback callback) throws Exception {
	}

	@Override
	public void setBroadcastEnableSetting(BroadcastEnableSetting setting,
			OperationCallback callback) throws Exception {
	}

	@Override
	public void setBroadcastPlatform(BroadcastPlatform platform,
			TokenType tokenType, String token, OperationCallback callback)
			throws Exception {
	}

	@Override
	public void setBroadcastInvitationList(List<String> invitationList,
			OperationCallback callback) throws Exception {
	}

	@Override
	public void setBroadcastPrivacy(BroadcastPrivacy privacy,
			OperationCallback callback) throws Exception {
	}

	@Override
	public void getBroadcastStatus(BroadcastStatusCallback callback)
			throws Exception {
	}

	@Override
	public void getBroadcastInvitationList(
			BroadcastInvitationListCallback callback) throws Exception {
	}

	public void getBroadcastPrivacy(BroadcastPrivacyCallback callback)
			throws Exception {
	}

	public void getBroadcastPlatform(BroadcastPlatformCallback callback)
			throws Exception {
	}

	@Override
	public void getBroadcastVideoUrl(BroadcastVideoUrlCallback callback)
			throws Exception {
	}

	@Override
	public void getBroadcastErrorList(BroadcastErrorListCallback callback)
			throws Exception {
	}
	
	@Override
	public void setBroadcastUserName(String userName, OperationCallback callback)
			throws Exception {
	}
	
	@Override
	public void setBroadcastSMSContent(String smsContent,OperationCallback callback) 
			throws Exception {
	}
	
	@Override
	public void getBroadcastUserName(BroadcastUserNameCallback callback) throws Exception {
	}
	
	@Override
	public void getBroadcastSMSContent(BroadcastSMSContentCallback callback) throws Exception {
	}

	@Override
	public void getSlowMotionEnableSetting(
			SlowMotionEnableSettingCallback callback) throws Exception {
	}

	@Override
	public void setSlowMotionEnableSetting(SlowMotionEnableSetting setting,
			OperationCallback callback) throws Exception {
	}

	@Override
	public void setRecordListener(RecordListener l) {
	}

	@Override
	public void setBroadcastListener(BroadcastListener l) {
	}

	@Override
	public void setBroadcastVideoUrlListener(BroadcastVideoUrlListener l) {
	}
	
	@Override
	public void setBroadcastErrorListener(BroadcastErrorListener l) {
	}
	
	@Override
	public void setBroadcastLiveStatusListener(BroadcastLiveStatusListener l) {
	}

	@Override
	public void setSlowMotionEnableListener(SlowMotionEnableListener l) {
	}

}
