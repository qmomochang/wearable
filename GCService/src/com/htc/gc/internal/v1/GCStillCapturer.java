package com.htc.gc.internal.v1;

import java.nio.ByteBuffer;

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
import com.htc.gc.interfaces.Common.InvalidArgumentsException;
import com.htc.gc.interfaces.Common.NotReadyException;
import com.htc.gc.interfaces.Common.OperationCallback;
import com.htc.gc.interfaces.Common.StatusException;
import com.htc.gc.interfaces.IGCService.ConnectionMode;
import com.htc.gc.interfaces.IStillCapturer;
import com.htc.gc.internal.DeviceItem;
import com.htc.gc.internal.v1.IMediator.IBleEventListener;
import com.htc.gc.internal.v1.IMediator.IEventListener;
import com.htc.gc.internal.Protocol;
import com.htc.gc.tasks.CaptureGetFaceCountTimesSettingTask;
import com.htc.gc.tasks.CaptureGetIsoStatusTask;
import com.htc.gc.tasks.CaptureGetTimeLapseAutoStopTask;
import com.htc.gc.tasks.CaptureGetTimeLapseLedSettingTask;
import com.htc.gc.tasks.CaptureGetWbStatusTask;
import com.htc.gc.tasks.CaptureGetWideAngleModeTask;
import com.htc.gc.tasks.CaptureImageGetRatioTask;
import com.htc.gc.tasks.CaptureImageGetResolutionTask;
import com.htc.gc.tasks.CaptureImageSetRatioTask;
import com.htc.gc.tasks.CaptureImageSetResolutionTask;
import com.htc.gc.tasks.CaptureSetFaceCountTimesSettingTask;
import com.htc.gc.tasks.CaptureSetIsoStatusTask;
import com.htc.gc.tasks.CaptureSetTimeLapseAutoStopTask;
import com.htc.gc.tasks.CaptureSetTimeLapseLedSettingTask;
import com.htc.gc.tasks.CaptureSetWbStatusTask;
import com.htc.gc.tasks.CaptureSetWideAngleModeTask;
import com.htc.gc.tasks.CaptureStillTask;
import com.htc.gc.tasks.CaptureTimeLapsePauseTask;
import com.htc.gc.tasks.CaptureTimeLapseResumeTask;
import com.htc.gc.tasks.CaptureTimeLapseStartTask;
import com.htc.gc.tasks.CaptureTimeLapseStopTask;
import com.htc.gc.tasks.QueryStillQVTask;
import com.htc.gc.tasks.TimeLapseGetDurationTask;
import com.htc.gc.tasks.TimeLapseGetFrameRateTask;
import com.htc.gc.tasks.TimeLapseGetRateTask;
import com.htc.gc.tasks.TimeLapseSetDurationTask;
import com.htc.gc.tasks.TimeLapseSetFrameRateTask;
import com.htc.gc.tasks.TimeLapseSetRateTask;

class GCStillCapturer implements IStillCapturer {
	protected static final boolean DEBUG = Common.DEBUG;

	private final IMediator mService;
	private CaptureListener mCapturedListener;
	private TimeLapseListener mTimeLapseListener;
	
	GCStillCapturer(IMediator service) {
		mService = service;

		mService.addEventListener(Protocol.EVENT_START_CAPTURING, new IEventListener() {

			@Override
			public void event(int eventID, ByteBuffer body) {
				if(DEBUG) Log.i(Common.TAG, "[GCStillCapturer] onCapture event, GC start capturing, ready: 0x"+Integer.toHexString(Common.READY_NONE));

				mService.setReady(Common.READY_NONE);

				int type = body.getInt();
				if(type == Protocol.FILE_TYPE_JPG) mService.setContext(Context.Capturing);
				else if(type == Protocol.FILE_TYPE_TIMELAPSE) mService.setContext(Context.TimeLapse);
				else Log.e(Common.TAG, "[GCStillCapturer] onCapture event, invalid type: "+type);

				CaptureListener l = mCapturedListener;
				if(l != null) l.onCapture(GCStillCapturer.this);
			}
		});

		mService.addEventListener(Protocol.EVENT_STOP_CAPTURING, new IEventListener() {

			@Override
			public void event(int eventID, ByteBuffer body) {
				if(DEBUG) Log.i(Common.TAG, "[GCStillCapturer] onCaptureStop event, GC stop capturing");

				CaptureListener l = mCapturedListener;
				if(l != null) l.onCaptureStop(GCStillCapturer.this);
			}
		});

		mService.addEventListener(Protocol.EVENT_QUICKVIEW_COMPLETE, new IEventListener() {

			@Override
			public void event(int eventID, ByteBuffer body) {
				if(DEBUG) Log.i(Common.TAG, "[GCStillCapturer] onCaptureQVComplete event, GC quick view complete");

				int handle = body.getInt();

				CaptureListener l = mCapturedListener;
				if(l != null) l.onCaptureQVComplete(GCStillCapturer.this, new GCMediaItem(0, handle)); // TODO
			}
		});

		mService.addEventListener(Protocol.EVENT_COMPLETE_CAPTURING, new IEventListener() {

			@Override
			public void event(int eventID, ByteBuffer body) {
				int type = body.getInt();
				int ready = body.getInt();

				if(ready == Common.READY_ALL) {
					mService.setContext(Context.None);				
				} 
				
				mService.setReady(ready);

				if(DEBUG) Log.i(Common.TAG, "[GCStillCapturer] onReady event, GC ready for capture type: " + type + ", ready: 0x" + Integer.toHexString(ready));

				CaptureListener l = mCapturedListener;
				if(l != null) l.onCaptureComplete(GCStillCapturer.this, type, ready);
			}
		});

		mService.addEventListener(Protocol.EVENT_ERROR_CAPTURING, new IEventListener() {

			@Override
			public void event(int eventID, ByteBuffer body) {
				if(DEBUG) Log.i(Common.TAG, "[GCStillCapturer] onError event");
				int errorCode = body.getInt();
				
				mService.setContext(Context.None);

				CaptureListener l = mCapturedListener;
				if(l != null) l.onError(GCStillCapturer.this, new Common.CommonException("Operation fail", Common.ErrorCode.getKey(errorCode)));
			}
		});
		
		mService.addEventListener(Protocol.EVENT_TIME_LAPSE_CAPTURE_ONE, new IEventListener() {
			@Override
			public void event(int eventID, ByteBuffer body) {
				if(DEBUG) Log.i(Common.TAG, "[GCStillCapturer] onTimeLapse capture one event");
				
				int currentShotIdx = body.getInt();
				int freeRemainCount = body.getInt();
				int totalFrameCount = body.getInt();
				TimeLapseListener l = mTimeLapseListener;
				if(l != null) l.onCaptureTimeLapseOne(GCStillCapturer.this, currentShotIdx, freeRemainCount, totalFrameCount);
			}
		});
		
		mService.addBleEventListener(IGcConnectivityService.CB_LONG_TERM_EVENT_RESULT, new IBleEventListener() {

			@Override
			public void event(int callbackID, Bundle bundle) {
				LongTermEvent event = (LongTermEvent) bundle.getSerializable(IGcConnectivityService.PARAM_LONG_TERM_EVENT);
				if(event.equals(LongTermEvent.LTEVENT_CAMERA_STATUS)) {
					IGcConnectivityService.OperationEvent opEvent = (IGcConnectivityService.OperationEvent) bundle.getSerializable(IGcConnectivityService.PARAM_OPERATION_EVENT);
					int type = 0;
					int ready = 0;
					switch(opEvent) {
					case OPEVENT_START_CAPTURING:
						if(!mService.getCurrentConnectionMode().equals(ConnectionMode.Partial)) {
							break;
						}
						
						if(DEBUG) Log.i(Common.TAG, "[GCStillCapturer] BLE onCapture event, GC start capturing, ready: 0x"+Integer.toHexString(Common.READY_NONE));

						mService.setReady(Common.READY_NONE);

						type = bundle.getInt(IGcConnectivityService.PARAM_FILE_TYPE);
						if(type == Protocol.FILE_TYPE_JPG) mService.setContext(Context.Capturing);
						else if(type == Protocol.FILE_TYPE_TIMELAPSE) mService.setContext(Context.TimeLapse);
						else Log.e(Common.TAG, "[GCStillCapturer] BLE onCapture event, invalid type: "+type);

						if(mCapturedListener != null) mCapturedListener.onCapture(GCStillCapturer.this);
						break;
					
					case OPEVENT_COMPLETE_CAPTURING:
						if(!mService.getCurrentConnectionMode().equals(ConnectionMode.Partial)) {
							break;
						}
						
						type = bundle.getInt(IGcConnectivityService.PARAM_FILE_TYPE);
						ready = bundle.getInt(IGcConnectivityService.PARAM_READY_BIT);

						if(ready == Common.READY_ALL) {
							mService.setContext(Context.None);	
						}
						mService.setReady(ready);
						if(DEBUG) Log.i(Common.TAG, "[GCStillCapturer] BLE onReady event, GC ready for capture type: " + type + ", ready: 0x" + Integer.toHexString(ready));

						if(mCapturedListener != null) mCapturedListener.onCaptureComplete(GCStillCapturer.this, type, ready);
						break;
						
					case OPEVENT_TIME_LAPSE_CAPTURE_ONE:
						if(!mService.getCurrentConnectionMode().equals(ConnectionMode.Partial)) {
							break;
						}
						
						if(DEBUG) Log.i(Common.TAG, "[GCStillCapturer] BLE onTimeLapse capture one event");
						
						if(mTimeLapseListener != null) mTimeLapseListener
							.onCaptureTimeLapseOne(GCStillCapturer.this, bundle.getInt(IGcConnectivityService.PARAM_TIME_LAPSE_CURRENT_COUNT),
																		bundle.getInt(IGcConnectivityService.PARAM_TIME_LAPSE_REMAIN_COUNT),
																		bundle.getInt(IGcConnectivityService.PARAM_TIME_LAPSE_TOTAL_COUNT));
						break;
						
					default: // other case is acceptable
					}
				} else if(event.equals(LongTermEvent.LTEVENT_CAMERA_ERROR)) {
					int index = bundle.getInt(IGcConnectivityService.PARAM_CAMERA_ERROR_INDEX);
					int code = bundle.getInt(IGcConnectivityService.PARAM_CAMERA_ERROR_CODE);
					
					if(index == Common.ERR_MODULE_CAPTURE) {
						if(!mService.getCurrentConnectionMode().equals(ConnectionMode.Partial)) {
							return;
						}
						if(DEBUG) Log.i(Common.TAG, "[GCStillCapturer] BLE onError event");
						if(mCapturedListener != null) mCapturedListener.onError(GCStillCapturer.this, new Common.CommonException("Operation fail", Common.ErrorCode.getKey(code)));
					}
				}

			}
			
		});
	}

	@Override
	public void captureStill(final OperationCallback callback) throws Exception {
		if(DEBUG) Log.i(Common.TAG, "[GCStillCapturer] captureStill");

		if(callback == null) throw new NullPointerException();
		//if((mService.getReady() & Common.READY_STILL) != Common.READY_STILL) throw new NotReadyException();
		
		switch(mService.getCurrentConnectionMode()) {
		case Full:
			mService.requestCommand(new CaptureStillTask(this, callback));	
			break;
		case Partial:
			DeviceItem device = (DeviceItem)mService.getTargetDevice();
			if(!mService.getConnectivityService().gcSetOperation(device.getDevice(), Operation.OPERATION_CAPTURE_START)) {
				throw new BleCommandException();
			} else {
				mService.addBleCallback(GCStillCapturer.this, IGcConnectivityService.CB_SET_OPERATION_RESULT, callback);
			}
			break;
		default:
			throw new StatusException();
		}
	}

	@Override
	public void captureTimeLapseStart(final OperationCallback callback) throws Exception {
		if(DEBUG) Log.i(Common.TAG, "[GCStillCapturer] captureTimeLapseStart");

		if(callback == null) throw new NullPointerException();
		if((mService.getReady() & Common.READY_TIMELAPSE) != Common.READY_TIMELAPSE) throw new NotReadyException();
		
		switch(mService.getCurrentConnectionMode()) {
		case Full:
			mService.requestCommand(new CaptureTimeLapseStartTask(this, callback));			
			break;
		case Partial:
			DeviceItem device = (DeviceItem)mService.getTargetDevice();
			if(!mService.getConnectivityService().gcSetOperation(device.getDevice(), Operation.OPERATION_TIME_LAPS_RECORDING_START)) {
				throw new BleCommandException();
			} else {
				mService.addBleCallback(GCStillCapturer.this, IGcConnectivityService.CB_SET_OPERATION_RESULT, callback);
			}
			break;
		default:
			throw new StatusException();
		}

	}

	@Override
	public void captureTimeLapseStop(final OperationCallback callback) throws Exception {
		if(DEBUG) Log.i(Common.TAG, "[GCStillCapturer] captureTimeLapseStop");

		if(callback == null) throw new NullPointerException();
		
		switch(mService.getCurrentConnectionMode()) {
		case Full:
			mService.requestCommand(new CaptureTimeLapseStopTask(this, callback));			
			break;
		case Partial:
			DeviceItem device = (DeviceItem)mService.getTargetDevice();
			if(!mService.getConnectivityService().gcSetOperation(device.getDevice(), Operation.OPERATION_TIME_LAPS_RECORDING_STOP)) {
				throw new BleCommandException();
			} else {
				mService.addBleCallback(GCStillCapturer.this, IGcConnectivityService.CB_SET_OPERATION_RESULT, callback);
			}
			break;
		default:
			throw new StatusException();
		}

	}
	
	@Override
	public void captureTimeLapsePause(final OperationCallback callback) throws Exception {
		if(DEBUG) Log.i(Common.TAG, "[GCStillCapturer] captureTimeLapsePause");

		if(callback == null) throw new NullPointerException();
		
		switch(mService.getCurrentConnectionMode()) {
		case Full:
			mService.requestCommand(new CaptureTimeLapsePauseTask(this, callback));
			break;
		case Partial:
			DeviceItem device = (DeviceItem)mService.getTargetDevice();
			if(!mService.getConnectivityService().gcSetOperation(device.getDevice(), Operation.OPERATION_TIME_LAPS_RECORDING_PAUSE)) {
				throw new BleCommandException();
			} else {
				mService.addBleCallback(GCStillCapturer.this, IGcConnectivityService.CB_SET_OPERATION_RESULT, callback);
			}
			break;
		default:
			throw new StatusException();
		}

	}
	
	@Override
	public void captureTimeLapseResume(final OperationCallback callback) throws Exception {
		if(DEBUG) Log.i(Common.TAG, "[GCStillCapturer] captureTimeLapseResume");

		if(callback == null) throw new NullPointerException();
		
		switch(mService.getCurrentConnectionMode()) {
		case Full:
			mService.requestCommand(new CaptureTimeLapseResumeTask(this, callback));	
			break;
		case Partial:
			DeviceItem device = (DeviceItem)mService.getTargetDevice();
			if(!mService.getConnectivityService().gcSetOperation(device.getDevice(), Operation.OPERATION_TIME_LAPS_RECORDING_RESUME)) {
				throw new BleCommandException();
			} else {
				mService.addBleCallback(GCStillCapturer.this, IGcConnectivityService.CB_SET_OPERATION_RESULT, callback);
			}
			break;
		default:
			throw new StatusException();
		}

	}

	@Override
	public void getTimeLapseRate(TimeLapseRateCallback callback) throws Exception {
		if(DEBUG) Log.i(Common.TAG, "[GCStillCapturer] getTimeLapseRate");
		
		if(callback == null) throw new NullPointerException();
		if(mService.getCurrentConnectionMode() != ConnectionMode.Full) throw new StatusException();		
		
		mService.requestCommand(new TimeLapseGetRateTask(this, callback));
	}
	
	@Override
	public void setTimeLapseRate(int sec, OperationCallback callback) throws Exception {
		if(DEBUG) Log.i(Common.TAG, "[GCStillCapturer] setTimeLapseRate");

		if(callback == null) throw new NullPointerException();
		if(mService.getCurrentConnectionMode() != ConnectionMode.Full) throw new StatusException();
		
		if(sec < 1 || sec > 4294967) throw new InvalidArgumentsException("Invalid timelapse rate");
		mService.requestCommand(new TimeLapseSetRateTask(this, sec, callback));
	}

	@Override
	public void getTimeLapseDuration(TimeLapseDurationCallback callback) throws Exception {
		if(DEBUG) Log.i(Common.TAG, "[GCStillCapturer] getTimeLapseDuration");
		
		if(callback == null) throw new NullPointerException();
		if(mService.getCurrentConnectionMode() != ConnectionMode.Full) throw new StatusException();
		
		mService.requestCommand(new TimeLapseGetDurationTask(this, callback));
	}
	
	@Override
	public void setTimeLapseDuration(int min, OperationCallback callback) throws Exception {
		if(DEBUG) Log.i(Common.TAG, "[GCStillCapturer] setTimeLapseDuration");

		if(callback == null) throw new NullPointerException();
		if(mService.getCurrentConnectionMode() != ConnectionMode.Full) throw new StatusException();

		if(min < 1 || (min * 60) > 4294967) throw new InvalidArgumentsException("Invalid timelapse duration");
		mService.requestCommand(new TimeLapseSetDurationTask(this, min, callback));
	}
	
	@Override
	public void getTimeLapseFrameRate(TimeLapseFrameRateCallback callback) throws Exception {
		if(DEBUG) Log.i(Common.TAG, "[GCStillCapturer] getTimeLapseFrameRate");
		
		if(callback == null) throw new NullPointerException();
		if(mService.getCurrentConnectionMode() != ConnectionMode.Full) throw new StatusException();
		
		mService.requestCommand(new TimeLapseGetFrameRateTask(this, callback));
	}
	
	@Override
	public void setTimeLapseFrameRate(byte rate, OperationCallback callback) throws Exception {
		if(DEBUG) Log.i(Common.TAG, "[GCStillCapturer] setTimeLapseFrameRate");

		if(callback == null) throw new NullPointerException();
		if(mService.getCurrentConnectionMode() != ConnectionMode.Full) throw new StatusException();

		mService.requestCommand(new TimeLapseSetFrameRateTask(this, rate, callback));
	}
	
	
	@Override
	public ICancelable getCaptureQVImage(DataCallback callback) throws Exception {
		if(DEBUG) Log.i(Common.TAG, "[GCStillCapturer] getCaptureQVImage");

		if(callback == null) throw new NullPointerException();
		if(mService.getCurrentConnectionMode() != ConnectionMode.Full) throw new StatusException();

		QueryStillQVTask task;
		mService.requestCommand(task = new QueryStillQVTask(callback));
		return task;
	}
	
	@Override
	public void getImgRatio(ImgRatioCallback callback) throws Exception {
		if(DEBUG) Log.i(Common.TAG, "[GCStillCapturer] getImgRatio");
		
		if(callback == null) throw new NullPointerException();
		if(mService.getCurrentConnectionMode() != ConnectionMode.Full) throw new StatusException();
		
		mService.requestCommand(new CaptureImageGetRatioTask(this, callback));
	}
	
	@Override
	public void setImgRatio(ImageRatio ratio, OperationCallback callback) throws Exception {
		if(DEBUG) Log.i(Common.TAG, "[GCStillCapturer] setImgRatio");
		
		if(callback == null) throw new NullPointerException();
		if(mService.getCurrentConnectionMode() != ConnectionMode.Full) throw new StatusException();
		
		mService.requestCommand(new CaptureImageSetRatioTask(this, ratio, callback));
	}
	
	@Override
	public void getImgResolution(ImgResolutionCallback callback) throws Exception {
		if(DEBUG) Log.i(Common.TAG, "[GCStillCapturer] getImgResolution");
		
		if(callback == null) throw new NullPointerException();
		if(mService.getCurrentConnectionMode() != ConnectionMode.Full) throw new StatusException();
		
		mService.requestCommand(new CaptureImageGetResolutionTask(this, callback));
	}

	@Override
	public void setImgResolution(ImageResolution resolution, OperationCallback callback) throws Exception {
		if(DEBUG) Log.i(Common.TAG, "[GCStillCapturer] setImgResolution");
		
		if(callback == null) throw new NullPointerException();
		if(mService.getCurrentConnectionMode() != ConnectionMode.Full) throw new StatusException();	
		
		mService.requestCommand(new CaptureImageSetResolutionTask(this, resolution, callback));
	}
	
	@Override
	public void getIsoStatus(IsoStatusCallback callback) throws Exception {
		if(DEBUG) Log.i(Common.TAG, "[GCStillCapturer] getIsoStatus");
		
		if(callback == null) throw new NullPointerException();
		if(mService.getCurrentConnectionMode() != ConnectionMode.Full) throw new StatusException();
		
		mService.requestCommand(new CaptureGetIsoStatusTask(this, callback));
	}
	
	@Override
	public void setIsoStatus(IsoStatus status, OperationCallback callback) throws Exception {
		if(DEBUG) Log.i(Common.TAG, "[GCStillCapturer] setIsoStatus");
		
		if(callback == null) throw new NullPointerException();
		if(mService.getCurrentConnectionMode() != ConnectionMode.Full) throw new StatusException();
		
		mService.requestCommand(new CaptureSetIsoStatusTask(this, status, callback));
	}
	
	@Override
	public void getWbStatus(WbStatusCallback callback) throws Exception {
		if(DEBUG) Log.i(Common.TAG, "[GCStillCapturer] getWbStatus");
		
		if(callback == null) throw new NullPointerException();
		if(mService.getCurrentConnectionMode() != ConnectionMode.Full) throw new StatusException();
		
		mService.requestCommand(new CaptureGetWbStatusTask(this, callback));
	}
	
	@Override
	public void setWbStatus(WbStatus status, OperationCallback callback) throws Exception {
		if(DEBUG) Log.i(Common.TAG, "[GCStillCapturer] setWbStatus");
		
		if(callback == null) throw new NullPointerException();
		if(mService.getCurrentConnectionMode() != ConnectionMode.Full) throw new StatusException();
		
		mService.requestCommand(new CaptureSetWbStatusTask(this, status, callback));
	}
	
	@Override
	public void getWideAngleMode(WideAngleModeCallback callback) throws Exception {
		if(DEBUG) Log.i(Common.TAG, "[GCStillCapturer] getWideAngleMode");
		
		if(callback == null) throw new NullPointerException();
		if(mService.getCurrentConnectionMode() != ConnectionMode.Full) throw new StatusException();
		
		mService.requestCommand(new CaptureGetWideAngleModeTask(this, callback));
	}

	@Override
	public void setWideAngleMode(WideAngleMode mode, OperationCallback callback) throws Exception {
		if(DEBUG) Log.i(Common.TAG, "[GCStillCapturer] setWideAngleMode");
		
		if(callback == null) throw new NullPointerException();
		if(mService.getCurrentConnectionMode() != ConnectionMode.Full) throw new StatusException();
		
		mService.requestCommand(new CaptureSetWideAngleModeTask(this, mode, callback));
	}
	
	@Override
	public void getTimeLapseLedSetting(TimeLapseLedSettingCallback callback) throws Exception {
		if(DEBUG) Log.i(Common.TAG, "[GCStillCapturer] getTimeLapseLedSetting");
		
		if(callback == null) throw new NullPointerException();
		if(mService.getCurrentConnectionMode() != ConnectionMode.Full) throw new StatusException();
		
		mService.requestCommand(new CaptureGetTimeLapseLedSettingTask(this, callback));
	}
	
	@Override
	public void setTimeLapseLedSetting(LedSwitch onOff, OperationCallback callback) throws Exception {
		if(DEBUG) Log.i(Common.TAG, "[GCStillCapturer] setTimeLapseLedSetting");
		
		if(callback == null) throw new NullPointerException();
		if(mService.getCurrentConnectionMode() != ConnectionMode.Full) throw new StatusException();
		
		mService.requestCommand(new CaptureSetTimeLapseLedSettingTask(this, onOff, callback));		
	}
	
	@Override
	public void getTimeLapseAutoStopSetting(TimeLapseAutoStopCallback callback)	throws Exception {
		if(DEBUG) Log.i(Common.TAG, "[GCStillCapturer] getTimeLapseAutoStopSetting");
		
		if(callback == null) throw new NullPointerException();
		if(mService.getCurrentConnectionMode() != ConnectionMode.Full) throw new StatusException();
		
		mService.requestCommand(new CaptureGetTimeLapseAutoStopTask(this, callback));
	}

	@Override
	public void setTimeLapseAutoStopSetting(TimeLapseAutoStop autoStop,	OperationCallback callback) throws Exception {
		if(DEBUG) Log.i(Common.TAG, "[GCStillCapturer] setTimeLapseAutoStopSetting");
		
		if(callback == null) throw new NullPointerException();
		if(mService.getCurrentConnectionMode() != ConnectionMode.Full) throw new StatusException();
		
		mService.requestCommand(new CaptureSetTimeLapseAutoStopTask(this, autoStop, callback));
	}
	
	@Override
	public void setFaceCountTimesSetting(FaceCountTimes times, OperationCallback callback) throws Exception {
		if(DEBUG) Log.i(Common.TAG, "[GCStillCapturer] setFaceCountTimesSetting");
		
		if(callback == null) throw new NullPointerException();
		if(mService.getCurrentConnectionMode() != ConnectionMode.Full) throw new StatusException();
		
		mService.requestCommand(new CaptureSetFaceCountTimesSettingTask(this, times, callback));
	}

	@Override
	public void getFaceCountTimesSetting(FaceCountTimesCallback callback) throws Exception {
		if(DEBUG) Log.i(Common.TAG, "[GCStillCapturer] getFaceCountTimesSetting");
		
		if(callback == null) throw new NullPointerException();
		if(mService.getCurrentConnectionMode() != ConnectionMode.Full) throw new StatusException();
		
		mService.requestCommand(new CaptureGetFaceCountTimesSettingTask(this, callback));
	}
	
	@Override
	public void setCaptureListener(CaptureListener l) {
		mCapturedListener = l;
	}
	
	@Override
	public void setTimeLapseListener(TimeLapseListener l) {
		mTimeLapseListener = l;
	}

}
