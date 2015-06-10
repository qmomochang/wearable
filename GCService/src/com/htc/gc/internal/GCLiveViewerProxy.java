package com.htc.gc.internal;

import com.htc.gc.interfaces.ILiveViewer;
import com.htc.gc.interfaces.Common.OperationCallback;

public class GCLiveViewerProxy implements ILiveViewer {
	
	private ILiveViewer mLiveViewer = new NullGCLiveViewer();
	
	private ILiveViewStreamReadyListener mILiveViewStreamReadyListener;
	
	public void setLiveViewer(ILiveViewer liveViewer) {
		mLiveViewer = liveViewer;
		
		mLiveViewer.setLiveViewStreamReadyListener(mILiveViewStreamReadyListener);
	}

	@Override
	public void startLiveView(StartLiveViewCallback callback) throws Exception {
		mLiveViewer.startLiveView(callback);
	}

	@Override
	public void stopLiveView(OperationCallback callback) throws Exception {
		mLiveViewer.stopLiveView(callback);
	}

	@Override
	public void setLiveStreamResolution(LiveStreamResolution res,
			OperationCallback callback) throws Exception {
		mLiveViewer.setLiveStreamResolution(res, callback);
	}

	@Override
	public void getLiveStreamResolution(LiveStreamResolutionCallback callback)
			throws Exception {
		mLiveViewer.getLiveStreamResolution(callback);
	}

	@Override
	public void setLiveStreamFrameRate(LiveStreamFrameRate rate,
			OperationCallback callback) throws Exception {
		mLiveViewer.setLiveStreamFrameRate(rate, callback);
	}

	@Override
	public void getLiveStreamFrameRate(LiveStreamFrameRateCallback callback)
			throws Exception {
		mLiveViewer.getLiveStreamFrameRate(callback);
	}

	@Override
	public void setLiveStreamCompressRate(LiveStreamCompressRate rate,
			OperationCallback callback) throws Exception {
		mLiveViewer.setLiveStreamCompressRate(rate, callback);
	}

	@Override
	public void getLiveStreamCompressRate(
			LiveStreamCompressRateCallback callback) throws Exception {
		mLiveViewer.getLiveStreamCompressRate(callback);
	}

	@Override
	public void setLiveViewMode(LiveViewMode mode, OperationCallback callback)
			throws Exception {
		mLiveViewer.setLiveViewMode(mode, callback);
	}

	@Override
	public void getLiveViewMode(GetLiveViewModeCallback callback)
			throws Exception {
		mLiveViewer.getLiveViewMode(callback);
	}

	@Override
	public void setLiveViewStreamReadyListener(ILiveViewStreamReadyListener l) {
		mILiveViewStreamReadyListener = l;
		mLiveViewer.setLiveViewStreamReadyListener(l);
	}

}
