package com.htc.gc.internal;

import java.util.List;

import com.htc.gc.interfaces.IVideoRecorder;
import com.htc.gc.interfaces.Common.DataCallback;
import com.htc.gc.interfaces.Common.ICancelable;
import com.htc.gc.interfaces.Common.OperationCallback;

public class GCVideoRecorderProxy implements IVideoRecorder {

	private IVideoRecorder mVideoRecorder = new NullGCVideoRecorder();

	private RecordListener mRecordListener;
	private BroadcastListener mBroadcastListener;
	private BroadcastVideoUrlListener mBroadcastVideoUrlListener;
	private BroadcastErrorListener mBroadcastErrorListener;
	private BroadcastLiveStatusListener mBroadcastLiveStatusListener;
	private SlowMotionEnableListener mSlowMotionEnableListener;

	public void setVideoRecorder(IVideoRecorder videoRecorder) {
		mVideoRecorder = videoRecorder;

		mVideoRecorder.setRecordListener(mRecordListener);
		mVideoRecorder.setBroadcastListener(mBroadcastListener);
		mVideoRecorder.setBroadcastVideoUrlListener(mBroadcastVideoUrlListener);
		mVideoRecorder.setBroadcastErrorListener(mBroadcastErrorListener);
		mVideoRecorder.setBroadcastLiveStatusListener(mBroadcastLiveStatusListener);
		mVideoRecorder.setSlowMotionEnableListener(mSlowMotionEnableListener);
	}

	@Override
	public void recordStart(OperationCallback callback) throws Exception {
		mVideoRecorder.recordStart(callback);
	}

	@Override
	public void recordStartSlowMotion(OperationCallback callback)
			throws Exception {
		mVideoRecorder.recordStartSlowMotion(callback);
	}

	@Override
	public void recordStop(OperationCallback callback) throws Exception {
		mVideoRecorder.recordStop(callback);
	}
	
	@Override
	public void recordStartBroadcast(OperationCallback callback) throws Exception {
		mVideoRecorder.recordStartBroadcast(callback);
	}
	
	@Override
	public void recordStopBroadcast(OperationCallback callback) throws Exception {
		mVideoRecorder.recordStopBroadcast(callback);
	}

	@Override
	public ICancelable getRecordQVImage(DataCallback callback) throws Exception {
		return mVideoRecorder.getRecordQVImage(callback);
	}

	@Override
	public void getResolution(ResolutionCallback callback) throws Exception {
		mVideoRecorder.getResolution(callback);
	}

	@Override
	public void setResolution(VideoResolution resolution,
			OperationCallback callback) throws Exception {
		mVideoRecorder.setResolution(resolution, callback);
	}

	@Override
	public void getBroadcastEnableSetting(
			BroadcastEnableSettingCallback callback) throws Exception {
		mVideoRecorder.getBroadcastEnableSetting(callback);
	}

	@Override
	public void setBroadcastEnableSetting(BroadcastEnableSetting setting,
			OperationCallback callback) throws Exception {
		mVideoRecorder.setBroadcastEnableSetting(setting, callback);
	}

	@Override
	public void setBroadcastPlatform(BroadcastPlatform platform,
			TokenType tokenType, String token, OperationCallback callback)
			throws Exception {
		mVideoRecorder.setBroadcastPlatform(platform, tokenType, token,
				callback);
	}

	@Override
	public void setBroadcastInvitationList(List<String> invitationList,
			OperationCallback callback) throws Exception {
		mVideoRecorder.setBroadcastInvitationList(invitationList, callback);
	}

	@Override
	public void setBroadcastPrivacy(BroadcastPrivacy privacy,
			OperationCallback callback) throws Exception {
		mVideoRecorder.setBroadcastPrivacy(privacy, callback);
	}

	@Override
	public void getBroadcastStatus(BroadcastStatusCallback callback)
			throws Exception {
		mVideoRecorder.getBroadcastStatus(callback);
	}

	@Override
	public void getBroadcastInvitationList(
			BroadcastInvitationListCallback callback) throws Exception {
		mVideoRecorder.getBroadcastInvitationList(callback);
	}

	@Override
	public void getBroadcastPrivacy(BroadcastPrivacyCallback callback)
			throws Exception {
		mVideoRecorder.getBroadcastPrivacy(callback);
	}

	@Override
	public void getBroadcastPlatform(BroadcastPlatformCallback callback)
			throws Exception {
		mVideoRecorder.getBroadcastPlatform(callback);
	}

	@Override
	public void getBroadcastVideoUrl(BroadcastVideoUrlCallback callback)
			throws Exception {
		mVideoRecorder.getBroadcastVideoUrl(callback);
	}

	@Override
	public void getBroadcastErrorList(BroadcastErrorListCallback callback)
			throws Exception {
		mVideoRecorder.getBroadcastErrorList(callback);
	}

	@Override
	public void setBroadcastUserName(String userName, OperationCallback callback)
			throws Exception {
		mVideoRecorder.setBroadcastUserName(userName, callback);
	}

	@Override
	public void setBroadcastSMSContent(String smsContent,OperationCallback callback) 
			throws Exception {
		mVideoRecorder.setBroadcastSMSContent(smsContent, callback);
	}
	
	@Override
	public void getBroadcastUserName(BroadcastUserNameCallback callback) throws Exception {
		mVideoRecorder.getBroadcastUserName(callback);
	}
	
	@Override
	public void getBroadcastSMSContent(BroadcastSMSContentCallback callback) throws Exception {
		mVideoRecorder.getBroadcastSMSContent(callback);
	}

	@Override
	public void getSlowMotionEnableSetting(
			SlowMotionEnableSettingCallback callback) throws Exception {
		mVideoRecorder.getSlowMotionEnableSetting(callback);
	}

	@Override
	public void setSlowMotionEnableSetting(SlowMotionEnableSetting setting,
			OperationCallback callback) throws Exception {
		mVideoRecorder.setSlowMotionEnableSetting(setting, callback);
	}

	@Override
	public void setRecordListener(RecordListener l) {
		mRecordListener = l;
		mVideoRecorder.setRecordListener(l);
	}

	@Override
	public void setBroadcastListener(BroadcastListener l) {
		mBroadcastListener = l;
		mVideoRecorder.setBroadcastListener(l);
	}

	@Override
	public void setBroadcastVideoUrlListener(BroadcastVideoUrlListener l) {
		mBroadcastVideoUrlListener = l;
		mVideoRecorder.setBroadcastVideoUrlListener(l);
	}

	@Override
	public void setBroadcastErrorListener(BroadcastErrorListener l) {
		mBroadcastErrorListener = l;
		mVideoRecorder.setBroadcastErrorListener(l);
	}

	@Override
	public void setBroadcastLiveStatusListener(BroadcastLiveStatusListener l) {
		mBroadcastLiveStatusListener = l;
		mVideoRecorder.setBroadcastLiveStatusListener(l);
	}

	@Override
	public void setSlowMotionEnableListener(SlowMotionEnableListener l) {
		mSlowMotionEnableListener = l;
		mVideoRecorder.setSlowMotionEnableListener(l);
	}

}
