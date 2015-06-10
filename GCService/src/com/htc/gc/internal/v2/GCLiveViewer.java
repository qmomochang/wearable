package com.htc.gc.internal.v2;

import android.util.Log;

import com.htc.gc.interfaces.Common;
import com.htc.gc.interfaces.Common.NoImpException;
import com.htc.gc.interfaces.Common.OperationCallback;
import com.htc.gc.interfaces.Common.StatusException;
import com.htc.gc.interfaces.IGCService.ConnectionMode;
import com.htc.gc.interfaces.ILiveViewer;

class GCLiveViewer implements ILiveViewer {
	protected static final boolean DEBUG = Common.DEBUG;

	private final IMediator mService;

	private ILiveViewStreamReadyListener mLiveViewStreamReadyListener;
	
	GCLiveViewer(IMediator service) {
		mService = service;
	}

	@Override
	public void startLiveView(StartLiveViewCallback callback) throws Exception {
		if(DEBUG) Log.i(Common.TAG, "[GCLiveViewer] startLiveView");

		if(callback == null) throw new NullPointerException();
		if(mService.getCurrentConnectionMode() != ConnectionMode.Full) throw new StatusException();

		throw new NoImpException();
	}

	@Override
	public void stopLiveView(OperationCallback callback) throws Exception {
		if(DEBUG) Log.i(Common.TAG, "[GCLiveViewer] stopLiveView");

		if(callback == null) throw new NullPointerException();
		if(mService.getCurrentConnectionMode() != ConnectionMode.Full) throw new StatusException();

		throw new NoImpException();
	}
	
	@Override
	public void getLiveStreamResolution(LiveStreamResolutionCallback callback) throws Exception {
		if(DEBUG) Log.i(Common.TAG, "[GCLiveViewer] getLiveStreamResolution");
		
		if(callback == null) throw new NullPointerException();
		if(mService.getCurrentConnectionMode() != ConnectionMode.Full) throw new StatusException();
		
		throw new NoImpException();		
	}

	
	@Override
	public void setLiveStreamResolution(LiveStreamResolution res, OperationCallback callback) throws Exception {
		if(DEBUG) Log.i(Common.TAG, "[GCLiveViewer] setLiveStreamResolution");
		
		if(callback == null) throw new NullPointerException();
		if(mService.getCurrentConnectionMode() != ConnectionMode.Full) throw new StatusException();
		
		throw new NoImpException();
	}
	
	@Override
	public void getLiveStreamFrameRate(LiveStreamFrameRateCallback callback) throws Exception {
		if(DEBUG) Log.i(Common.TAG, "[GCLiveViewer] getLiveStreamFrameRate");
		
		if(callback == null) throw new NullPointerException();
		if(mService.getCurrentConnectionMode() != ConnectionMode.Full) throw new StatusException();
		
		throw new NoImpException();
	}
	
	@Override
	public void setLiveStreamFrameRate(LiveStreamFrameRate rate, OperationCallback callback) throws Exception {
		if(DEBUG) Log.i(Common.TAG, "[GCLiveViewer] setLiveStreamFrameRate");
		
		if(callback == null) throw new NullPointerException();
		if(mService.getCurrentConnectionMode() != ConnectionMode.Full) throw new StatusException();
		
		throw new NoImpException();
	}
	
	@Override
	public void getLiveStreamCompressRate(LiveStreamCompressRateCallback callback) throws Exception {
		if(DEBUG) Log.i(Common.TAG, "[GCLiveViewer] getLiveStreamCompressRate");
		
		if(callback == null) throw new NullPointerException();
		if(mService.getCurrentConnectionMode() != ConnectionMode.Full) throw new StatusException();
		
		throw new NoImpException();
	}
	
	@Override
	public void setLiveStreamCompressRate(LiveStreamCompressRate rate, OperationCallback callback) throws Exception {
		if(DEBUG) Log.i(Common.TAG, "[GCLiveViewer] setLiveStreamCompressRate");
		
		if(callback == null) throw new NullPointerException();
		if(mService.getCurrentConnectionMode() != ConnectionMode.Full) throw new StatusException();
		
		throw new NoImpException();
	}
	
	@Override
	public void setLiveViewMode(LiveViewMode mode, OperationCallback callback) throws Exception {
		if(DEBUG) Log.i(Common.TAG, "[GCLiveViewer] setLiveViewMode");
		
		if(callback == null) throw new NullPointerException();
		if(mService.getCurrentConnectionMode() != ConnectionMode.Full) throw new StatusException();
		
		throw new NoImpException();
	}

	@Override
	public void getLiveViewMode(GetLiveViewModeCallback callback) throws Exception {
		if(DEBUG) Log.i(Common.TAG, "[GCLiveViewer] getLiveViewMode");
		
		if(callback == null) throw new NullPointerException();
		if(mService.getCurrentConnectionMode() != ConnectionMode.Full) throw new StatusException();
		
		throw new NoImpException();
	}

	@Override
	public void setLiveViewStreamReadyListener(ILiveViewStreamReadyListener l) {
		mLiveViewStreamReadyListener = l;
	}

}
