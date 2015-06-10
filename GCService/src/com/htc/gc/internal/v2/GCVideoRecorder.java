package com.htc.gc.internal.v2;

import java.security.InvalidParameterException;
import java.util.List;

import android.os.Bundle;
import android.util.Log;

import com.htc.gc.connectivity.interfaces.IGcConnectivityServiceBase.LongTermEvent;
import com.htc.gc.connectivity.v3.interfaces.IGcConnectivityService;
import com.htc.gc.connectivity.v3.interfaces.IGcConnectivityService.Operation;
import com.htc.gc.interfaces.Common;
import com.htc.gc.interfaces.Common.BleCommandException;
import com.htc.gc.interfaces.Common.Context;
import com.htc.gc.interfaces.Common.DataCallback;
import com.htc.gc.interfaces.Common.ICancelable;
import com.htc.gc.interfaces.Common.NoImpException;
import com.htc.gc.interfaces.Common.OperationCallback;
import com.htc.gc.interfaces.Common.StatusException;
import com.htc.gc.interfaces.IGCService.ConnectionMode;
import com.htc.gc.interfaces.IVideoRecorder;
import com.htc.gc.internal.DeviceItem;
import com.htc.gc.internal.v2.IMediator.IBleEventListener;
import com.htc.gc.internal.Protocol;

class GCVideoRecorder implements IVideoRecorder {
	protected static final boolean DEBUG = Common.DEBUG;
	
	private final IMediator mService;

	private RecordListener mRecordListener;
	private BroadcastListener mBroadcastListener;
	private BroadcastVideoUrlListener mBroadcastVideoUrlListener;
	private BroadcastErrorListener mBroadcastErrorListener;
	private BroadcastLiveStatusListener mBroadcastLiveStatusListener;
	private SlowMotionEnableListener mSlowMotionEnableListener;
	
	GCVideoRecorder(IMediator service) {
		mService = service;

		mService.addBleEventListener(IGcConnectivityService.CB_LONG_TERM_EVENT_RESULT, new IBleEventListener() {

			@Override
			public void event(int callbackID, Bundle bundle) {
				LongTermEvent event = (LongTermEvent) bundle.getSerializable(IGcConnectivityService.PARAM_LONG_TERM_EVENT);
				if(event.equals(LongTermEvent.LTEVENT_CAMERA_STATUS)) {
					IGcConnectivityService.OperationEvent opEvent = (IGcConnectivityService.OperationEvent) bundle.getSerializable(IGcConnectivityService.PARAM_OPERATION_EVENT);
					switch(opEvent) {
					case OPEVENT_START_RECORDING:
						if(!mService.getCurrentConnectionMode().equals(ConnectionMode.Partial)) {
							break;
						}
						
						if(DEBUG) Log.i(Common.TAG, "[GCVideoRecorder] BLE onRecord event, GC start recording, ready: 0x"+Integer.toHexString(Common.READY_NONE));

						mService.setReady(Common.READY_NONE);

						int type = bundle.getInt(IGcConnectivityService.PARAM_FILE_TYPE);
						if(type == Protocol.FILE_TYPE_MP4) mService.setContext(Context.Recording);
						else if(type == Protocol.FILE_TYPE_LIVE_BROADCASTING) mService.setContext(Context.Recording);
						else if(type == Protocol.FILE_TYPE_SLOWMOTION) mService.setContext(Context.SlowMotion);
						else Log.e(Common.TAG, "[GCVideoRecorder] onRecord event, invalid type: "+type);

						if(mRecordListener != null) mRecordListener.onRecord(GCVideoRecorder.this);
						break;
					
					case OPEVENT_COMPLETE_RECORDING:
						if(!mService.getCurrentConnectionMode().equals(ConnectionMode.Partial)) {
							break;
						}
						
						int ready = bundle.getInt(IGcConnectivityService.PARAM_READY_BIT);

						mService.setContext(Context.None);
						mService.setReady(ready);
						if(DEBUG) Log.i(Common.TAG, "[GCVideoRecorder] BLE onReady event, GC ready for record type: "+bundle.getInt(IGcConnectivityService.PARAM_FILE_TYPE)+", ready:" + Integer.toHexString(ready));

						if(mRecordListener != null) mRecordListener.onRecordComplete(GCVideoRecorder.this, ready);
						break;
						
					default: // other case is acceptable
					}
				} else if(event.equals(LongTermEvent.LTEVENT_CAMERA_ERROR)) {
					int index = bundle.getInt(IGcConnectivityService.PARAM_CAMERA_ERROR_INDEX);
					int code = bundle.getInt(IGcConnectivityService.PARAM_CAMERA_ERROR_CODE);
					
					if(index == Common.ERR_MODULE_VIDEO) {
						if(!mService.getCurrentConnectionMode().equals(ConnectionMode.Partial)) {
							return;
						}
						if(DEBUG) Log.i(Common.TAG, "[GCVideoRecorder] BLE onError event");
						if(mRecordListener != null) mRecordListener.onError(GCVideoRecorder.this, new Common.CommonException("Operation fail", Common.ErrorCode.getKey(code)));
					}
				} else if (event.equals(LongTermEvent.LTEVENT_BROADCAST_VIDEO_URL_RECEIVED)) {
					String videoUrl = bundle.getString(IGcConnectivityService.PARAM_BROADCAST_VIDEO_URL);
					
					if(DEBUG) Log.i(Common.TAG, "[GCVideoRecorder] BLE broadcast video url received: " + videoUrl);
					
					if (mBroadcastVideoUrlListener != null) mBroadcastVideoUrlListener.onReceived(videoUrl);
				} else if (event.equals(LongTermEvent.LTEVENT_BROADCAST_ERROR)) {
					byte errorCode = bundle.getByte(IGcConnectivityService.PARAM_BROADCAST_ERROR_CODE);
					String errorTimestamp = bundle.getString(IGcConnectivityService.PARAM_BROADCAST_ERROR_TIMESTAMP);
					
					BroadcastError error = null;
					try {
						error = BroadcastError.getKey(errorCode);
					} catch (NoImpException e) {
						e.printStackTrace();
						Log.d(Common.TAG, "[GCVideoRecorder] BLE broadcast error, get key fail");
					}
					
					if(DEBUG) Log.i(Common.TAG, "[GCVideoRecorder] BLE broadcast error: " + error + ", " + errorTimestamp);
					
					if (mBroadcastErrorListener != null) mBroadcastErrorListener.onError(error, errorTimestamp);
				} else if (event.equals(LongTermEvent.LTEVENT_BROADCAST_LIVE_BEGIN)) {
					if(DEBUG) Log.i(Common.TAG, "[GCVideoRecorder] BLE broadcast live begin");
					
					if (mBroadcastLiveStatusListener != null) mBroadcastLiveStatusListener.onLiveBegin();
				} else if (event.equals(LongTermEvent.LTEVENT_BROADCAST_LIVE_END)) {
					if(DEBUG) Log.i(Common.TAG, "[GCVideoRecorder] BLE broadcast live end");
					
					if (mBroadcastLiveStatusListener != null) mBroadcastLiveStatusListener.onLiveEnd();
				}
			}
			
		});
	}

	@Override
	public void recordStart(final OperationCallback callback) throws Exception {
		if(DEBUG) Log.i(Common.TAG, "[GCVideoRecorder] recordStart");

		//if((mService.getReady() & Common.READY_VIDEO) != Common.READY_VIDEO) throw new NotReadyException();
		
		switch(mService.getCurrentConnectionMode()) {
		case Full:
		case Partial:
			DeviceItem device = (DeviceItem)mService.getTargetDevice();
			if(!mService.getConnectivityService().gcSetOperation(device.getDevice(), Operation.OPERATION_VIDEO_RECORDING_NORMAL_START)) {
				throw new BleCommandException();
			} else {
				mService.addBleCallback(GCVideoRecorder.this, IGcConnectivityService.CB_SET_OPERATION_RESULT, callback);
			}
			break;
		default:
			throw new Common.StatusException();
		}
		
	}
	
	@Override
	public void recordStartSlowMotion(final OperationCallback callback) throws Exception {
		if(DEBUG) Log.i(Common.TAG, "[GCVideoRecorder] recordStartSlowMotion");

		//if((mService.getReady() & Common.READY_VIDEO) != Common.READY_VIDEO) throw new NotReadyException();
		
		switch(mService.getCurrentConnectionMode()) {
		case Full:
		case Partial:
			DeviceItem device = (DeviceItem)mService.getTargetDevice();
			if(!mService.getConnectivityService().gcSetOperation(device.getDevice(), Operation.OPERATION_VIDEO_RECORDING_SLOW_MOTION_START)) {
				throw new BleCommandException();
			} else {
				mService.addBleCallback(GCVideoRecorder.this, IGcConnectivityService.CB_SET_OPERATION_RESULT, callback);
			}
			break;
		default:
			throw new Common.StatusException();
		}
		 
	}


	@Override
	public void recordStop(OperationCallback callback) throws Exception {
		if(DEBUG) Log.i(Common.TAG, "[GCVideoRecorder] recordStop");

		switch(mService.getCurrentConnectionMode()) {
		case Full:
		case Partial:
			DeviceItem device = (DeviceItem)mService.getTargetDevice();
			if(!mService.getConnectivityService().gcSetOperation(device.getDevice(), Operation.OPERATION_VIDEO_RECORDING_SLOW_MOTION_STOP)) {
				throw new BleCommandException();
			} else {
				mService.addBleCallback(GCVideoRecorder.this, IGcConnectivityService.CB_SET_OPERATION_RESULT, callback);
			}
			break;
		default:
			throw new Common.StatusException();
		}
		
	}
	
	
	@Override
	public void recordStartBroadcast(OperationCallback callback) throws Exception {
		if(DEBUG) Log.i(Common.TAG, "[GCVideoRecorder] recordStartBroadcast");

		//if((mService.getReady() & Common.READY_VIDEO) != Common.READY_VIDEO) throw new NotReadyException();
		
		switch(mService.getCurrentConnectionMode()) {
		case Full:
		case Partial:
			DeviceItem device = (DeviceItem)mService.getTargetDevice();
			if(!mService.getConnectivityService().gcSetOperation(device.getDevice(), Operation.OPERATION_BROADCAST_START)) {
				throw new BleCommandException();
			} else {
				mService.addBleCallback(GCVideoRecorder.this, IGcConnectivityService.CB_SET_OPERATION_RESULT, callback);
			}
			break;
		default:
			throw new Common.StatusException();
		}
	}
	
	
	@Override
	public void recordStopBroadcast(OperationCallback callback) throws Exception {
		if(DEBUG) Log.i(Common.TAG, "[GCVideoRecorder] recordStopBroadcast");

		//if((mService.getReady() & Common.READY_VIDEO) != Common.READY_VIDEO) throw new NotReadyException();
		
		switch(mService.getCurrentConnectionMode()) {
		case Full:
		case Partial:
			DeviceItem device = (DeviceItem)mService.getTargetDevice();
			if(!mService.getConnectivityService().gcSetOperation(device.getDevice(), Operation.OPERATION_BROADCAST_STOP)) {
				throw new BleCommandException();
			} else {
				mService.addBleCallback(GCVideoRecorder.this, IGcConnectivityService.CB_SET_OPERATION_RESULT, callback);
			}
			break;
		default:
			throw new Common.StatusException();
		}
	}
	

	@Override
	public ICancelable getRecordQVImage(DataCallback callback) throws Exception {
		if(DEBUG) Log.i(Common.TAG, "[GCVideoRecorder] getRecordQVImage");

		if(mService.getCurrentConnectionMode() != ConnectionMode.Full) throw new StatusException();

		throw new NoImpException();
	}
	
	@Override
	public void getResolution(ResolutionCallback callback) throws Exception {
		if(DEBUG) Log.i(Common.TAG, "[GCVideoRecorder] getResolution");
		
		if(mService.getCurrentConnectionMode() != ConnectionMode.Full) throw new StatusException();
		
		throw new NoImpException();
	}

	@Override
	public void setResolution(VideoResolution resolution, OperationCallback callback) throws Exception {
		if(DEBUG) Log.i(Common.TAG, "[GCVideoRecorder] setResolution");
		
		if(mService.getCurrentConnectionMode() != ConnectionMode.Full) throw new StatusException();
		
		throw new NoImpException();
	}
	
	@Override
	public void getBroadcastEnableSetting(BroadcastEnableSettingCallback callback) throws Exception {
		if(DEBUG) Log.i(Common.TAG, "[GCVideoRecorder] getBroadcastEnableSetting");
		
		if(callback == null) throw new NullPointerException();
		
		switch (mService.getCurrentConnectionMode()) {
		case Full:
		case Partial:
			DeviceItem device = (DeviceItem)mService.getTargetDevice();
			if(!mService.getConnectivityService().gcGetBroadcastSetting(device.getDevice())) {
				throw new BleCommandException();
			} else {
				mService.addBleCallback(GCVideoRecorder.this, IGcConnectivityService.CB_GET_BROADCAST_SETTING_RESULT, callback);
			}
			break;
		default:
			throw new StatusException();
		}
	}

	@Override
	public void setBroadcastEnableSetting(BroadcastEnableSetting setting, OperationCallback callback) throws Exception {
		if(DEBUG) Log.i(Common.TAG, "[GCVideoRecorder] setBroadcastEnableSetting");
		
		if(callback == null) throw new NullPointerException();
		
		switch (mService.getCurrentConnectionMode()) {
		case Full:
		case Partial:
			DeviceItem device = (DeviceItem)mService.getTargetDevice();
			if(!mService.getConnectivityService().gcSetBroadcastSetting(device.getDevice(), IGcConnectivityService.BroadcastSetting.findSetting(setting.getVal()))) {
				throw new BleCommandException();
			} else {
				mService.addBleCallback(GCVideoRecorder.this, IGcConnectivityService.CB_SET_BROADCAST_SETTING_RESULT, callback);
			}
			break;
		default:
			throw new StatusException();
		}
	}

	@Override
	public void setBroadcastPlatform(BroadcastPlatform platform, TokenType tokenType, String token, OperationCallback callback) throws Exception {
		if(DEBUG) Log.i(Common.TAG, "[GCVideoRecorder] setBroadcastPlatform");
		
		if(callback == null) throw new NullPointerException();
		
		switch (mService.getCurrentConnectionMode()) {
		case Full:
		case Partial:
			DeviceItem device = (DeviceItem)mService.getTargetDevice();
			if(!mService.getConnectivityService().gcSetBroadcastPlatform(device.getDevice(), IGcConnectivityService.BroadcastPlatform.findPlatform(platform.getVal()), IGcConnectivityService.BroadcastTokenType.findToken(tokenType.getVal()), token)) {
				throw new BleCommandException();
			} else {
				mService.addBleCallback(GCVideoRecorder.this, IGcConnectivityService.CB_SET_BROADCAST_PLATFORM_RESULT, callback);
			}
			break;
		default:
			throw new StatusException();
		}
	}

	@Override
	public void setBroadcastInvitationList(List<String> invitationList, OperationCallback callback) throws Exception {
		if(DEBUG) Log.i(Common.TAG, "[GCVideoRecorder] setBroadcastInvitationList");
		
		if (invitationList == null || invitationList.size() <= 0) throw new InvalidParameterException();
		if(callback == null) throw new NullPointerException();
		
		switch (mService.getCurrentConnectionMode()) {
		case Full:
		case Partial:
			DeviceItem device = (DeviceItem)mService.getTargetDevice();
			if(!mService.getConnectivityService().gcSetBroadcastInvitationList(device.getDevice(), invitationList)) {
				throw new BleCommandException();
			} else {
				mService.addBleCallback(GCVideoRecorder.this, IGcConnectivityService.CB_SET_BROADCAST_INVITATION_LIST_RESULT, callback);
			}
			break;
		default:
			throw new StatusException();
		}
	}
	
	@Override
	public void setBroadcastPrivacy(BroadcastPrivacy privacy, OperationCallback callback) throws Exception {
		if(DEBUG) Log.i(Common.TAG, "[GCVideoRecorder] setBroadcastPrivacy");
		
		if(callback == null) throw new NullPointerException();
		
		switch (mService.getCurrentConnectionMode()) {
		case Full:
		case Partial:
			IGcConnectivityService.BroadcastPrivacy broadcastPrivacy = (privacy == BroadcastPrivacy.BROADCAST_PRIVACY_NONPUBLIC ? IGcConnectivityService.BroadcastPrivacy.BROADCASTPRIVACY_NONPUBLIC : IGcConnectivityService.BroadcastPrivacy.BROADCASTPRIVACY_PUBLIC);
			DeviceItem device = (DeviceItem)mService.getTargetDevice();
			if(!mService.getConnectivityService().gcSetBroadcastPrivacy(device.getDevice(), broadcastPrivacy)) {
				throw new BleCommandException();
			} else {
				mService.addBleCallback(GCVideoRecorder.this, IGcConnectivityService.CB_SET_BROADCAST_PRIVACY_RESULT, callback);
			}
			break;
		default:
			throw new StatusException();
		}
	}
	
	@Override
	public void getBroadcastStatus(BroadcastStatusCallback callback) throws Exception {
		if(DEBUG) Log.i(Common.TAG, "[GCVideoRecorder] getBroadcastStatus");
		
		if(callback == null) throw new NullPointerException();
		
		switch (mService.getCurrentConnectionMode()) {
		case Full:
		case Partial:
			DeviceItem device = (DeviceItem)mService.getTargetDevice();
			if(!mService.getConnectivityService().gcGetBroadcastStatus(device.getDevice())) {
				throw new BleCommandException();
			} else {
				mService.addBleCallback(GCVideoRecorder.this, IGcConnectivityService.CB_GET_BROADCAST_STATUS_RESULT, callback);
			}
			break;
		default:
			throw new StatusException();
		}
	}
	
	@Override
	public void getBroadcastInvitationList(BroadcastInvitationListCallback callback) throws Exception {
		if(DEBUG) Log.i(Common.TAG, "[GCVideoRecorder] getBroadcastInvitationList");
		
		if(callback == null) throw new NullPointerException();
		
		switch (mService.getCurrentConnectionMode()) {
		case Full:
		case Partial:
			DeviceItem device = (DeviceItem)mService.getTargetDevice();
			if(!mService.getConnectivityService().gcGetBroadcastInvitationList(device.getDevice())) {
				throw new BleCommandException();
			} else {
				mService.addBleCallback(GCVideoRecorder.this, IGcConnectivityService.CB_GET_BROADCAST_INVITATION_LIST_RESULT, callback);
			}
		break;
		default:
			throw new StatusException();
		}
	}
	
	@Override
	public void getBroadcastPrivacy(BroadcastPrivacyCallback callback) throws Exception {
		if (DEBUG) Log.i(Common.TAG, "[GCVideoRecorder] getBroadcastPrivacy");
		
		if(callback == null) throw new NullPointerException();
		
		switch (mService.getCurrentConnectionMode()) {
		case Full:
		case Partial:
			DeviceItem device = (DeviceItem)mService.getTargetDevice();
			if(!mService.getConnectivityService().gcGetBroadcastPrivacy(device.getDevice())) {
				throw new BleCommandException();
			} else {
				mService.addBleCallback(GCVideoRecorder.this, IGcConnectivityService.CB_GET_BROADCAST_PRIVACY_RESULT, callback);
			}
			break;
		default:
			throw new StatusException();
		}
	}
	
	@Override
	public void getBroadcastPlatform(BroadcastPlatformCallback callback) throws Exception {
		if (DEBUG) Log.i(Common.TAG, "[GCVideoRecorder] getBroadcastPlatform");
		
		if(callback == null) throw new NullPointerException();
		
		switch (mService.getCurrentConnectionMode()) {
		case Full:
		case Partial:
			DeviceItem device = (DeviceItem)mService.getTargetDevice();
			if(!mService.getConnectivityService().gcGetBroadcastPlatform(device.getDevice())) {
				throw new BleCommandException();
			} else {
				mService.addBleCallback(GCVideoRecorder.this, IGcConnectivityService.CB_GET_BROADCAST_PLATFORM_RESULT, callback);
			}
			break;
		default:
			throw new StatusException();
		}
	}
	
	@Override
	public void getBroadcastVideoUrl(BroadcastVideoUrlCallback callback) throws Exception {
		if (DEBUG) Log.i(Common.TAG, "[GCVideoRecorder] getBroadcastVideoUrl");
		
		if(callback == null) throw new NullPointerException();
		
		switch (mService.getCurrentConnectionMode()) {
		case Full:
		case Partial:
			DeviceItem device = (DeviceItem)mService.getTargetDevice();
			if(!mService.getConnectivityService().gcGetBroadcastVideoUrl(device.getDevice())) {
				throw new BleCommandException();
			} else {
				mService.addBleCallback(GCVideoRecorder.this, IGcConnectivityService.CB_GET_BROADCAST_VIDEO_URL_RESULT, callback);
			}
			break;
		default:
			throw new StatusException();
		}
	}
	
	@Override
	public void getBroadcastErrorList(BroadcastErrorListCallback callback) throws Exception {
		if (DEBUG) Log.i(Common.TAG, "[GCVideoRecorder] getBroadcastErrorList");
		
		if(callback == null) throw new NullPointerException();
		
		switch (mService.getCurrentConnectionMode()) {
		case Full:
		case Partial:
			DeviceItem device = (DeviceItem)mService.getTargetDevice();
			if(!mService.getConnectivityService().gcGetBroadcastErrorList(device.getDevice())) {
				throw new BleCommandException();
			} else {
				mService.addBleCallback(GCVideoRecorder.this, IGcConnectivityService.CB_GET_BROADCAST_ERROR_LIST_RESULT, callback);
			}
			break;
		default:
			throw new StatusException();
		}
	}
	
	@Override
	public void setBroadcastUserName(String userName, OperationCallback callback) throws Exception {
		if (DEBUG) Log.i(Common.TAG, "[GCVideoRecorder] setBroadcastUserName");
		
		if (callback == null) throw new NullPointerException();
		
		switch (mService.getCurrentConnectionMode()) {
		case Full:
		case Partial:
			DeviceItem device = (DeviceItem)mService.getTargetDevice();
			if(!mService.getConnectivityService().gcSetBroadcastUserName(device.getDevice(), userName)) {
				throw new BleCommandException();
			} else {
				mService.addBleCallback(GCVideoRecorder.this, IGcConnectivityService.CB_SET_BROADCAST_USER_NAME_RESULT, callback);
			}
			break;
		default:
			throw new StatusException();
		}
	}
	
	@Override
	public void setBroadcastSMSContent(String smsContent,OperationCallback callback) throws Exception {
		if (DEBUG) Log.i(Common.TAG, "[GCVideoRecorder] setBroadcastSMSContent");
		
		if (callback == null) throw new NullPointerException();
		
		switch (mService.getCurrentConnectionMode()) {
		case Full:
		case Partial:
			DeviceItem device = (DeviceItem)mService.getTargetDevice();
			if(!mService.getConnectivityService().gcSetBroadcastSMSContent(device.getDevice(), smsContent)) {
				throw new BleCommandException();
			} else {
				mService.addBleCallback(GCVideoRecorder.this, IGcConnectivityService.CB_SET_BROADCAST_SMS_CONTENT_RESULT, callback);
			}
			break;
		default:
			throw new StatusException();
		}
	}
	
	@Override
	public void getBroadcastUserName(BroadcastUserNameCallback callback) throws Exception {
		if (DEBUG) Log.i(Common.TAG, "[GCVideoRecorder] getBroadcastUserName");
		
		if (callback == null) throw new NullPointerException();
		
		switch (mService.getCurrentConnectionMode()) {
		case Full:
		case Partial:
			DeviceItem device = (DeviceItem)mService.getTargetDevice();
			if(!mService.getConnectivityService().gcGetBroadcastUserName(device.getDevice())) {
				throw new BleCommandException();
			} else {
				mService.addBleCallback(GCVideoRecorder.this, IGcConnectivityService.CB_GET_BROADCAST_USER_NAME_RESULT, callback);
			}
			break;
		default:
			throw new StatusException();
		}
	}
	
	@Override
	public void getBroadcastSMSContent(BroadcastSMSContentCallback callback) throws Exception {
		if (DEBUG) Log.i(Common.TAG, "[GCVideoRecorder] getBroadcastSMSContent");
		
		if (callback == null) throw new NullPointerException();
		
		switch (mService.getCurrentConnectionMode()) {
		case Full:
		case Partial:
			DeviceItem device = (DeviceItem)mService.getTargetDevice();
			if(!mService.getConnectivityService().gcGetBroadcastSMSContent(device.getDevice())) {
				throw new BleCommandException();
			} else {
				mService.addBleCallback(GCVideoRecorder.this, IGcConnectivityService.CB_GET_BROADCAST_SMS_CONTENT_RESULT, callback);
			}
			break;
		default:
			throw new StatusException();
		}
	}
	
	@Override
	public void getSlowMotionEnableSetting(SlowMotionEnableSettingCallback callback) throws Exception {
		if(DEBUG) Log.i(Common.TAG, "[GCVideoRecorder] getSlowmotionEnableSetting");
		
		if(mService.getCurrentConnectionMode() != ConnectionMode.Full) throw new StatusException();
		
		throw new NoImpException();
	}
	
	@Override
	public void setSlowMotionEnableSetting(SlowMotionEnableSetting setting, OperationCallback callback) throws Exception {
		if(DEBUG) Log.i(Common.TAG, "[GCVideoRecorder] setSlowmotionEnableSetting");
		
		if(mService.getCurrentConnectionMode() != ConnectionMode.Full) throw new StatusException();
		
		throw new NoImpException();
	}
	
	@Override
	public void setRecordListener(RecordListener l) {
		mRecordListener = l;
	}

	@Override
	public void setBroadcastListener(BroadcastListener l) {
		mBroadcastListener = l;
	}
	
	@Override
	public void setBroadcastVideoUrlListener(BroadcastVideoUrlListener l) {
		mBroadcastVideoUrlListener = l;
	}
	
	@Override
	public void setBroadcastErrorListener(BroadcastErrorListener l) {
		mBroadcastErrorListener = l;
	}
	
	@Override
	public void setBroadcastLiveStatusListener(BroadcastLiveStatusListener l) {
		mBroadcastLiveStatusListener = l;
	}

	@Override
	public void setSlowMotionEnableListener(SlowMotionEnableListener l) {
		mSlowMotionEnableListener = l;
	}
}
