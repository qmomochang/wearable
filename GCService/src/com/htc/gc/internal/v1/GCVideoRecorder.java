package com.htc.gc.internal.v1;

import java.nio.ByteBuffer;
import java.util.List;

import android.os.Bundle;
import android.util.Log;

import com.htc.gc.GCMediaItem;
import com.htc.gc.connectivity.interfaces.IGcConnectivityServiceBase.LongTermEvent;
import com.htc.gc.connectivity.v2.interfaces.IGcConnectivityService;
import com.htc.gc.connectivity.v2.interfaces.IGcConnectivityService.Operation;
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
import com.htc.gc.internal.v1.IMediator.IBleEventListener;
import com.htc.gc.internal.v1.IMediator.IEventListener;
import com.htc.gc.internal.NetworkHelper;
import com.htc.gc.internal.Protocol;
import com.htc.gc.tasks.QueryVideoQVTask;
import com.htc.gc.tasks.RecordGetBroadcastEnableSettingTask;
import com.htc.gc.tasks.RecordGetResolutionTask;
import com.htc.gc.tasks.RecordGetSlowMotionEnableTask;
import com.htc.gc.tasks.RecordSetBroadcastEnableSettingTask;
import com.htc.gc.tasks.RecordSetResolutionTask;
import com.htc.gc.tasks.RecordSetSlowMotionEnableTask;
import com.htc.gc.tasks.RecordVideoStartTask;
import com.htc.gc.tasks.RecordVideoStopTask;

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

		mService.addEventListener(Protocol.EVENT_START_RECORDING, new IEventListener() {

			@Override
			public void event(int eventID, ByteBuffer body) {
				if(DEBUG) Log.i(Common.TAG, "[GCVideoRecorder] onRecord event, GC start recording, ready: 0x"+Integer.toHexString(Common.READY_NONE));

				mService.setReady(Common.READY_NONE);

				int type = body.getInt();
				if(type == Protocol.FILE_TYPE_MOV) mService.setContext(Context.Recording);
				else if(type == Protocol.FILE_TYPE_SLOWMOTION) mService.setContext(Context.SlowMotion);
				else Log.e(Common.TAG, "[GCVideoRecorder] onRecord event, invalid type: "+type);

				RecordListener l = mRecordListener;
				if(l != null) l.onRecord(GCVideoRecorder.this);
			}
		});

		mService.addEventListener(Protocol.EVENT_STOP_RECORDING, new IEventListener() {

			@Override
			public void event(int eventID, ByteBuffer body) {
				if(DEBUG) Log.i(Common.TAG, "[GCVideoRecorder] onRecordStop event, GC stop recording");

				RecordListener l = mRecordListener;
				if(l != null) l.onRecordStop(GCVideoRecorder.this);
			}
		});

		mService.addEventListener(Protocol.EVENT_VIDEO_QV_THUMB_COMPLETE, new IEventListener() {

			@Override
			public void event(int eventID, ByteBuffer body) {
				if(DEBUG) Log.i(Common.TAG, "[GCVideoRecorder] onRecorded event, GC quick view complete");

				int handle = body.getInt();

				RecordListener l = mRecordListener;
				if(l != null) l.onRecordQVComplete(GCVideoRecorder.this, new GCMediaItem(0, handle)); // TODO
			}
		});

		mService.addEventListener(Protocol.EVENT_COMPLETE_RECORDING, new IEventListener() {

			@Override
			public void event(int eventID, ByteBuffer body) {
				int ready = body.getInt();

				mService.setContext(Context.None);
				mService.setReady(ready);
				if(DEBUG) Log.i(Common.TAG, "[GCVideoRecorder] onReady event, GC ready for record, ready:" + Integer.toHexString(ready));

				RecordListener l = mRecordListener;
				if(l != null) l.onRecordComplete(GCVideoRecorder.this, ready);
			}
		});

		mService.addEventListener(Protocol.EVENT_ERROR_RECORDING, new IEventListener() {

			@Override
			public void event(int eventID, ByteBuffer body) {
				if(DEBUG) Log.i(Common.TAG, "[GCVideoRecorder] onError event");
				int errorCode = body.getInt();
				
				mService.setContext(Context.None);

				RecordListener l = mRecordListener;
				if(l != null) l.onError(GCVideoRecorder.this, new Common.CommonException("Operation fail", Common.ErrorCode.getKey(errorCode)));
			}
		});
		
		mService.addEventListener(Protocol.EVENT_BROADCAST_VIDEO_REC_ONE, new IEventListener() {

			@Override
			public void event(int eventID, ByteBuffer body) {
				long sequenceNumber = NetworkHelper.getUnsignedInt(body.getInt());
				if(DEBUG) Log.i(Common.TAG, "[GCVideoRecorder][rtmp] onBroadcastVideoCreated, seq= "+sequenceNumber);
				
				BroadcastListener l = mBroadcastListener;
				if(l != null) l.onBroadcastVideoCreated(sequenceNumber);
			}
			
		});
		
		mService.addEventListener(Protocol.EVENT_SLOW_MOTION_CHANGE, new IEventListener() {
			
			@Override
			public void event(int eventID, ByteBuffer body) {
				if(DEBUG) Log.i(Common.TAG, "[GCVideoRecorder] onSlowMotionEnabled event");
				SlowMotionEnableSetting setting = SlowMotionEnableSetting.VIDEO_SLOWMOTION_OFF;
				
				try {
					setting = SlowMotionEnableSetting.getKey(body.get());
				} catch (NoImpException e) {
					e.printStackTrace();
				}
				
				SlowMotionEnableListener l = mSlowMotionEnableListener;
				if(l != null) l.onSlowMotionEnabled(setting.equals(SlowMotionEnableSetting.VIDEO_SLOWMOTION_ON) ? true : false);
			}
		});
		
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
						if(type == Protocol.FILE_TYPE_MOV) mService.setContext(Context.Recording);
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
			mService.requestCommand(new RecordVideoStartTask(VideoMode.VIDEO_MODE_NORMAL, this, callback));	
			break;
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
			mService.requestCommand(new RecordVideoStartTask(VideoMode.VIDEO_MODE_SLOWMOTION, this, callback));
			break;
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
			mService.requestCommand(new RecordVideoStopTask(this, callback));
			break;
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
		
		throw new NoImpException();
	}
	
	@Override
	public void recordStopBroadcast(OperationCallback callback) throws Exception {
		if(DEBUG) Log.i(Common.TAG, "[GCVideoRecorder] recordStopBroadcast");
		
		throw new NoImpException();
	}

	@Override
	public ICancelable getRecordQVImage(DataCallback callback) throws Exception {
		if(DEBUG) Log.i(Common.TAG, "[GCVideoRecorder] getRecordQVImage");

		if(mService.getCurrentConnectionMode() != ConnectionMode.Full) throw new StatusException();

		QueryVideoQVTask task;
		mService.requestCommand(task = new QueryVideoQVTask(callback));
		return task;
	}
	
	@Override
	public void getResolution(ResolutionCallback callback) throws Exception {
		if(DEBUG) Log.i(Common.TAG, "[GCVideoRecorder] getResolution");
		
		if(mService.getCurrentConnectionMode() != ConnectionMode.Full) throw new StatusException();
		
		mService.requestCommand(new RecordGetResolutionTask(this, callback));
	}

	@Override
	public void setResolution(VideoResolution resolution, OperationCallback callback) throws Exception {
		if(DEBUG) Log.i(Common.TAG, "[GCVideoRecorder] setResolution");
		
		if(mService.getCurrentConnectionMode() != ConnectionMode.Full) throw new StatusException();
		
		mService.requestCommand(new RecordSetResolutionTask(this, resolution, callback));
	}
	
	@Override
	public void getBroadcastEnableSetting(BroadcastEnableSettingCallback callback) throws Exception {
		if(DEBUG) Log.i(Common.TAG, "[GCVideoRecorder] getBroadcastEnableSetting");
		
		if(mService.getCurrentConnectionMode() != ConnectionMode.Full) throw new StatusException();
		
		mService.requestCommand(new RecordGetBroadcastEnableSettingTask(this, callback));		
	}

	@Override
	public void setBroadcastEnableSetting(BroadcastEnableSetting setting, OperationCallback callback) throws Exception {
		if(DEBUG) Log.i(Common.TAG, "[GCVideoRecorder] setBroadcastEnableSetting");
		
		if(mService.getCurrentConnectionMode() != ConnectionMode.Full) throw new StatusException();
		
		mService.requestCommand(new RecordSetBroadcastEnableSettingTask(this, setting, callback));
	}
	
	@Override
	public void setBroadcastPlatform(BroadcastPlatform platform, TokenType tokenType, String token, OperationCallback callback) throws Exception {
		if (DEBUG) Log.i(Common.TAG, "[GCVideoRecorder] setBroadcastPlatform");

		throw new NoImpException();
	}

	@Override
	public void setBroadcastInvitationList(List<String> invitationList, OperationCallback callback) throws Exception {
		if (DEBUG) Log.i(Common.TAG, "[GCVideoRecorder] setBroadcastInvitationList");

		throw new NoImpException();
	}

	@Override
	public void setBroadcastPrivacy(BroadcastPrivacy privacy, OperationCallback callback) throws Exception {
		if (DEBUG) Log.i(Common.TAG, "[GCVideoRecorder] setBroadcastPrivacy");

		throw new NoImpException();
	}

	@Override
	public void getBroadcastStatus(BroadcastStatusCallback callback) throws Exception {
		if (DEBUG) Log.i(Common.TAG, "[GCVideoRecorder] getBroadcastStatus");

		throw new NoImpException();
	}
	
	@Override
	public void getBroadcastInvitationList( BroadcastInvitationListCallback callback) throws Exception {
		if (DEBUG) Log.i(Common.TAG, "[GCVideoRecorder] getBroadcastInvitationList");
		
		throw new NoImpException();
	}
	
	@Override
	public void getBroadcastPrivacy(BroadcastPrivacyCallback callback) throws Exception {
		if (DEBUG) Log.i(Common.TAG, "[GCVideoRecorder] getBroadcastPrivacy");
		
		throw new NoImpException();
	}
	
	@Override
	public void getBroadcastPlatform(BroadcastPlatformCallback callback) throws Exception {
		if (DEBUG) Log.i(Common.TAG, "[GCVideoRecorder] getBroadcastPlatform");
		
		throw new NoImpException();
	}
	
	@Override
	public void getBroadcastVideoUrl(BroadcastVideoUrlCallback callback) throws Exception {
		if (DEBUG) Log.i(Common.TAG, "[GCVideoRecorder] getBroadcastVideoUrl");
		
		throw new NoImpException();
	}
	
	@Override
	public void getBroadcastErrorList(BroadcastErrorListCallback callback) throws Exception {
		if (DEBUG) Log.i(Common.TAG, "[GCVideoRecorder] getBroadcastErrorList");
		
		throw new NoImpException();
	}
	
	@Override
	public void setBroadcastUserName(String userName, OperationCallback callback) throws Exception {
		if (DEBUG) Log.i(Common.TAG, "[GCVideoRecorder] setBroadcastUserName");
		
		throw new NoImpException();
	}
	
	@Override
	public void setBroadcastSMSContent(String smsContent,OperationCallback callback) throws Exception {
		if (DEBUG) Log.i(Common.TAG, "[GCVideoRecorder] setBroadcastSMSContent");
		
		throw new NoImpException();
	}
	
	@Override
	public void getBroadcastUserName(BroadcastUserNameCallback callback) throws Exception {
		if (DEBUG) Log.i(Common.TAG, "[GCVideoRecorder] getBroadcastUserName");
		
		throw new NoImpException();
	}
	
	@Override
	public void getBroadcastSMSContent(BroadcastSMSContentCallback callback) throws Exception {
		if (DEBUG) Log.i(Common.TAG, "[GCVideoRecorder] getBroadcastSMSContent");
		
		throw new NoImpException();
	}
	
	@Override
	public void getSlowMotionEnableSetting(SlowMotionEnableSettingCallback callback) throws Exception {
		if(DEBUG) Log.i(Common.TAG, "[GCVideoRecorder] getSlowmotionEnableSetting");
		
		if(mService.getCurrentConnectionMode() != ConnectionMode.Full) throw new StatusException();
		
		mService.requestCommand(new RecordGetSlowMotionEnableTask(this, callback));
	}
	
	@Override
	public void setSlowMotionEnableSetting(SlowMotionEnableSetting setting, OperationCallback callback) throws Exception {
		if(DEBUG) Log.i(Common.TAG, "[GCVideoRecorder] setSlowmotionEnableSetting");
		
		if(mService.getCurrentConnectionMode() != ConnectionMode.Full) throw new StatusException();
		
		mService.requestCommand(new RecordSetSlowMotionEnableTask(this, setting, callback));
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
