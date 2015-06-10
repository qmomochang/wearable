package com.htc.gc.internal.v2;

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
import com.htc.gc.interfaces.Common.InvalidArgumentsException;
import com.htc.gc.interfaces.Common.NoImpException;
import com.htc.gc.interfaces.Common.NotReadyException;
import com.htc.gc.interfaces.Common.OperationCallback;
import com.htc.gc.interfaces.Common.StatusException;
import com.htc.gc.interfaces.IGCService.ConnectionMode;
import com.htc.gc.interfaces.IStillCapturer;
import com.htc.gc.internal.DeviceItem;
import com.htc.gc.internal.v2.IMediator.IBleEventListener;
import com.htc.gc.internal.Protocol;

class GCStillCapturer implements IStillCapturer {
	protected static final boolean DEBUG = Common.DEBUG;

	private final IMediator mService;
	private CaptureListener mCapturedListener;
	private TimeLapseListener mTimeLapseListener;
	
	GCStillCapturer(IMediator service) {
		mService = service;

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
		
		throw new NoImpException();
	}
	
	@Override
	public void setTimeLapseRate(int sec, OperationCallback callback) throws Exception {
		if(DEBUG) Log.i(Common.TAG, "[GCStillCapturer] setTimeLapseRate");

		if(callback == null) throw new NullPointerException();
		if(mService.getCurrentConnectionMode() != ConnectionMode.Full) throw new StatusException();
		
		if(sec < 1 || sec > 4294967) throw new InvalidArgumentsException("Invalid timelapse rate");
		throw new NoImpException();
	}

	@Override
	public void getTimeLapseDuration(TimeLapseDurationCallback callback) throws Exception {
		if(DEBUG) Log.i(Common.TAG, "[GCStillCapturer] getTimeLapseDuration");
		
		if(callback == null) throw new NullPointerException();
		if(mService.getCurrentConnectionMode() != ConnectionMode.Full) throw new StatusException();
		
		throw new NoImpException();
	}
	
	@Override
	public void setTimeLapseDuration(int min, OperationCallback callback) throws Exception {
		if(DEBUG) Log.i(Common.TAG, "[GCStillCapturer] setTimeLapseDuration");

		if(callback == null) throw new NullPointerException();
		if(mService.getCurrentConnectionMode() != ConnectionMode.Full) throw new StatusException();

		if(min < 1 || (min * 60) > 4294967) throw new InvalidArgumentsException("Invalid timelapse duration");
		throw new NoImpException();
	}
	
	@Override
	public void getTimeLapseFrameRate(TimeLapseFrameRateCallback callback) throws Exception {
		if(DEBUG) Log.i(Common.TAG, "[GCStillCapturer] getTimeLapseFrameRate");
		
		if(callback == null) throw new NullPointerException();
		if(mService.getCurrentConnectionMode() != ConnectionMode.Full) throw new StatusException();
		
		throw new NoImpException();
	}
	
	@Override
	public void setTimeLapseFrameRate(byte rate, OperationCallback callback) throws Exception {
		if(DEBUG) Log.i(Common.TAG, "[GCStillCapturer] setTimeLapseFrameRate");

		if(callback == null) throw new NullPointerException();
		if(mService.getCurrentConnectionMode() != ConnectionMode.Full) throw new StatusException();

		throw new NoImpException();
	}
	
	
	@Override
	public ICancelable getCaptureQVImage(DataCallback callback) throws Exception {
		if(DEBUG) Log.i(Common.TAG, "[GCStillCapturer] getCaptureQVImage");

		if(callback == null) throw new NullPointerException();
		if(mService.getCurrentConnectionMode() != ConnectionMode.Full) throw new StatusException();

		throw new NoImpException();
	}
	
	@Override
	public void getImgRatio(ImgRatioCallback callback) throws Exception {
		if(DEBUG) Log.i(Common.TAG, "[GCStillCapturer] getImgRatio");
		
		if(callback == null) throw new NullPointerException();
		if(mService.getCurrentConnectionMode() != ConnectionMode.Full) throw new StatusException();
		
		throw new NoImpException();
	}
	
	@Override
	public void setImgRatio(ImageRatio ratio, OperationCallback callback) throws Exception {
		if(DEBUG) Log.i(Common.TAG, "[GCStillCapturer] setImgRatio");
		
		if(callback == null) throw new NullPointerException();
		if(mService.getCurrentConnectionMode() != ConnectionMode.Full) throw new StatusException();
		
		throw new NoImpException();
	}
	
	@Override
	public void getImgResolution(ImgResolutionCallback callback) throws Exception {
		if(DEBUG) Log.i(Common.TAG, "[GCStillCapturer] getImgResolution");
		
		if(callback == null) throw new NullPointerException();
		if(mService.getCurrentConnectionMode() != ConnectionMode.Full) throw new StatusException();
		
		throw new NoImpException();
	}

	@Override
	public void setImgResolution(ImageResolution resolution, OperationCallback callback) throws Exception {
		if(DEBUG) Log.i(Common.TAG, "[GCStillCapturer] setImgResolution");
		
		if(callback == null) throw new NullPointerException();
		if(mService.getCurrentConnectionMode() != ConnectionMode.Full) throw new StatusException();	
		
		throw new NoImpException();
	}
	
	@Override
	public void getIsoStatus(IsoStatusCallback callback) throws Exception {
		if(DEBUG) Log.i(Common.TAG, "[GCStillCapturer] getIsoStatus");
		
		if(callback == null) throw new NullPointerException();
		if(mService.getCurrentConnectionMode() != ConnectionMode.Full) throw new StatusException();
		
		throw new NoImpException();
	}
	
	@Override
	public void setIsoStatus(IsoStatus status, OperationCallback callback) throws Exception {
		if(DEBUG) Log.i(Common.TAG, "[GCStillCapturer] setIsoStatus");
		
		if(callback == null) throw new NullPointerException();
		if(mService.getCurrentConnectionMode() != ConnectionMode.Full) throw new StatusException();
		
		throw new NoImpException();
	}
	
	@Override
	public void getWbStatus(WbStatusCallback callback) throws Exception {
		if(DEBUG) Log.i(Common.TAG, "[GCStillCapturer] getWbStatus");
		
		if(callback == null) throw new NullPointerException();
		if(mService.getCurrentConnectionMode() != ConnectionMode.Full) throw new StatusException();
		
		throw new NoImpException();
	}
	
	@Override
	public void setWbStatus(WbStatus status, OperationCallback callback) throws Exception {
		if(DEBUG) Log.i(Common.TAG, "[GCStillCapturer] setWbStatus");
		
		if(callback == null) throw new NullPointerException();
		if(mService.getCurrentConnectionMode() != ConnectionMode.Full) throw new StatusException();
		
		throw new NoImpException();
	}
	
	@Override
	public void getWideAngleMode(WideAngleModeCallback callback) throws Exception {
		if(DEBUG) Log.i(Common.TAG, "[GCStillCapturer] getWideAngleMode");
		
		if(callback == null) throw new NullPointerException();
		if(mService.getCurrentConnectionMode() != ConnectionMode.Full) throw new StatusException();
		
		throw new NoImpException();
	}

	@Override
	public void setWideAngleMode(WideAngleMode mode, OperationCallback callback) throws Exception {
		if(DEBUG) Log.i(Common.TAG, "[GCStillCapturer] setWideAngleMode");
		
		if(callback == null) throw new NullPointerException();
		if(mService.getCurrentConnectionMode() != ConnectionMode.Full) throw new StatusException();
		
		throw new NoImpException();
	}
	
	@Override
	public void getTimeLapseLedSetting(TimeLapseLedSettingCallback callback) throws Exception {
		if(DEBUG) Log.i(Common.TAG, "[GCStillCapturer] getTimeLapseLedSetting");
		
		if(callback == null) throw new NullPointerException();
		if(mService.getCurrentConnectionMode() != ConnectionMode.Full) throw new StatusException();
		
		throw new NoImpException();
	}
	
	@Override
	public void setTimeLapseLedSetting(LedSwitch onOff, OperationCallback callback) throws Exception {
		if(DEBUG) Log.i(Common.TAG, "[GCStillCapturer] setTimeLapseLedSetting");
		
		if(callback == null) throw new NullPointerException();
		if(mService.getCurrentConnectionMode() != ConnectionMode.Full) throw new StatusException();
		
		throw new NoImpException();		
	}
	
	@Override
	public void getTimeLapseAutoStopSetting(TimeLapseAutoStopCallback callback)	throws Exception {
		if(DEBUG) Log.i(Common.TAG, "[GCStillCapturer] getTimeLapseAutoStopSetting");
		
		if(callback == null) throw new NullPointerException();
		if(mService.getCurrentConnectionMode() != ConnectionMode.Full) throw new StatusException();
		
		throw new NoImpException();
	}

	@Override
	public void setTimeLapseAutoStopSetting(TimeLapseAutoStop autoStop,	OperationCallback callback) throws Exception {
		if(DEBUG) Log.i(Common.TAG, "[GCStillCapturer] setTimeLapseAutoStopSetting");
		
		if(callback == null) throw new NullPointerException();
		if(mService.getCurrentConnectionMode() != ConnectionMode.Full) throw new StatusException();
		
		throw new NoImpException();
	}
	
	@Override
	public void setFaceCountTimesSetting(FaceCountTimes times, OperationCallback callback) throws Exception {
		if(DEBUG) Log.i(Common.TAG, "[GCStillCapturer] setFaceCountTimesSetting");
		
		if(callback == null) throw new NullPointerException();
		if(mService.getCurrentConnectionMode() != ConnectionMode.Full) throw new StatusException();
		
		throw new NoImpException();
	}

	@Override
	public void getFaceCountTimesSetting(FaceCountTimesCallback callback) throws Exception {
		if(DEBUG) Log.i(Common.TAG, "[GCStillCapturer] getFaceCountTimesSetting");
		
		if(callback == null) throw new NullPointerException();
		if(mService.getCurrentConnectionMode() != ConnectionMode.Full) throw new StatusException();
		
		throw new NoImpException();
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
