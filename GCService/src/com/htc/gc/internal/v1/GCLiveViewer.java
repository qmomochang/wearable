package com.htc.gc.internal.v1;

import java.nio.ByteBuffer;

import android.util.Log;

import com.htc.gc.interfaces.Common;
import com.htc.gc.interfaces.Common.NoImpException;
import com.htc.gc.interfaces.Common.OperationCallback;
import com.htc.gc.interfaces.Common.StatusException;
import com.htc.gc.interfaces.IGCService.ConnectionMode;
import com.htc.gc.interfaces.ILiveViewer;
import com.htc.gc.internal.v1.IMediator.IEventListener;
import com.htc.gc.internal.Protocol;
import com.htc.gc.tasks.LiveStreamGetCompressRateTask;
import com.htc.gc.tasks.LiveStreamGetFrameRateTask;
import com.htc.gc.tasks.LiveStreamGetResolutionTask;
import com.htc.gc.tasks.LiveStreamSetCompressRateTask;
import com.htc.gc.tasks.LiveStreamSetFrameRateTask;
import com.htc.gc.tasks.LiveStreamSetResolutionTask;
import com.htc.gc.tasks.LiveViewGetModeTask;
import com.htc.gc.tasks.LiveViewSetModeTask;
import com.htc.gc.tasks.LiveViewStartTask;
import com.htc.gc.tasks.LiveViewStopTask;

class GCLiveViewer implements ILiveViewer {
	protected static final boolean DEBUG = Common.DEBUG;

	private final IMediator mService;

	private ILiveViewStreamReadyListener mLiveViewStreamReadyListener;
	
	GCLiveViewer(IMediator service) {
		mService = service;
		
		mService.addEventListener(Protocol.EVENT_LVSTRAM_READY, new IEventListener() {

			@Override
			public void event(int eventID, ByteBuffer body) {
				if(DEBUG) Log.i(Common.TAG, "[GCLiveViewer] onReady event, GC liveview stream ready");

				ILiveViewStreamReadyListener l = mLiveViewStreamReadyListener;
				if(l != null) l.onReady(GCLiveViewer.this);
			}
		});
	}

	@Override
	public void startLiveView(StartLiveViewCallback callback) throws Exception {
		if(DEBUG) Log.i(Common.TAG, "[GCLiveViewer] startLiveView");

		if(callback == null) throw new NullPointerException();
		if(mService.getCurrentConnectionMode() != ConnectionMode.Full) throw new StatusException();

		mService.requestCommand(new LiveViewStartTask(this, callback));

	}

	@Override
	public void stopLiveView(OperationCallback callback) throws Exception {
		if(DEBUG) Log.i(Common.TAG, "[GCLiveViewer] stopLiveView");

		if(callback == null) throw new NullPointerException();
		if(mService.getCurrentConnectionMode() != ConnectionMode.Full) throw new StatusException();

		mService.requestCommand(new LiveViewStopTask(this, callback));

	}
	
	@Override
	public void getLiveStreamResolution(LiveStreamResolutionCallback callback) throws Exception {
		if(DEBUG) Log.i(Common.TAG, "[GCLiveViewer] getLiveStreamResolution");
		
		if(callback == null) throw new NullPointerException();
		if(mService.getCurrentConnectionMode() != ConnectionMode.Full) throw new StatusException();
		
		mService.requestCommand(new LiveStreamGetResolutionTask(this, callback));		
	}

	
	@Override
	public void setLiveStreamResolution(LiveStreamResolution res, OperationCallback callback) throws Exception {
		if(DEBUG) Log.i(Common.TAG, "[GCLiveViewer] setLiveStreamResolution");
		
		if(callback == null) throw new NullPointerException();
		if(mService.getCurrentConnectionMode() != ConnectionMode.Full) throw new StatusException();
		
		mService.requestCommand(new LiveStreamSetResolutionTask(this, res, callback));
	}
	
	@Override
	public void getLiveStreamFrameRate(LiveStreamFrameRateCallback callback) throws Exception {
		if(DEBUG) Log.i(Common.TAG, "[GCLiveViewer] getLiveStreamFrameRate");
		
		if(callback == null) throw new NullPointerException();
		if(mService.getCurrentConnectionMode() != ConnectionMode.Full) throw new StatusException();
		
		mService.requestCommand(new LiveStreamGetFrameRateTask(this, callback));
	}
	
	@Override
	public void setLiveStreamFrameRate(LiveStreamFrameRate rate, OperationCallback callback) throws Exception {
		if(DEBUG) Log.i(Common.TAG, "[GCLiveViewer] setLiveStreamFrameRate");
		
		if(callback == null) throw new NullPointerException();
		if(mService.getCurrentConnectionMode() != ConnectionMode.Full) throw new StatusException();
		
		mService.requestCommand(new LiveStreamSetFrameRateTask(this, rate, callback));
	}
	
	@Override
	public void getLiveStreamCompressRate(LiveStreamCompressRateCallback callback) throws Exception {
		if(DEBUG) Log.i(Common.TAG, "[GCLiveViewer] getLiveStreamCompressRate");
		
		if(callback == null) throw new NullPointerException();
		if(mService.getCurrentConnectionMode() != ConnectionMode.Full) throw new StatusException();
		
		mService.requestCommand(new LiveStreamGetCompressRateTask(this, callback));
	}
	
	@Override
	public void setLiveStreamCompressRate(LiveStreamCompressRate rate, OperationCallback callback) throws Exception {
		if(DEBUG) Log.i(Common.TAG, "[GCLiveViewer] setLiveStreamCompressRate");
		
		if(callback == null) throw new NullPointerException();
		if(mService.getCurrentConnectionMode() != ConnectionMode.Full) throw new StatusException();
		
		mService.requestCommand(new LiveStreamSetCompressRateTask(this, rate, callback));
	}
	
	@Override
	public void setLiveViewMode(LiveViewMode mode, OperationCallback callback) throws Exception {
		if(DEBUG) Log.i(Common.TAG, "[GCLiveViewer] setLiveViewMode");
		
		if(callback == null) throw new NullPointerException();
		if(mService.getCurrentConnectionMode() != ConnectionMode.Full) throw new StatusException();
		
		GCServiceWorker service = (GCServiceWorker) mService;
		if(service.getFWVersion() < 7090) throw new NoImpException();
		
		mService.requestCommand(new LiveViewSetModeTask(this, mode, callback));
	}

	@Override
	public void getLiveViewMode(GetLiveViewModeCallback callback) throws Exception {
		if(DEBUG) Log.i(Common.TAG, "[GCLiveViewer] getLiveViewMode");
		
		if(callback == null) throw new NullPointerException();
		if(mService.getCurrentConnectionMode() != ConnectionMode.Full) throw new StatusException();
		
		GCServiceWorker service = (GCServiceWorker) mService;
		if(service.getFWVersion() < 7090) throw new NoImpException();
		
		mService.requestCommand(new LiveViewGetModeTask(this, callback));
	}

	@Override
	public void setLiveViewStreamReadyListener(ILiveViewStreamReadyListener l) {
		mLiveViewStreamReadyListener = l;
	}

}
