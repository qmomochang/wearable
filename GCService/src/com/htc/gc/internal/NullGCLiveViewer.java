package com.htc.gc.internal;

import com.htc.gc.interfaces.ILiveViewer;
import com.htc.gc.interfaces.Common.OperationCallback;

class NullGCLiveViewer implements ILiveViewer {

	@Override
	public void startLiveView(StartLiveViewCallback callback) throws Exception {
	}

	@Override
	public void stopLiveView(OperationCallback callback) throws Exception {
	}

	@Override
	public void setLiveStreamResolution(LiveStreamResolution res,
			OperationCallback callback) throws Exception {
	}

	@Override
	public void getLiveStreamResolution(LiveStreamResolutionCallback callback)
			throws Exception {
	}

	@Override
	public void setLiveStreamFrameRate(LiveStreamFrameRate rate,
			OperationCallback callback) throws Exception {
	}

	@Override
	public void getLiveStreamFrameRate(LiveStreamFrameRateCallback callback)
			throws Exception {
	}

	@Override
	public void setLiveStreamCompressRate(LiveStreamCompressRate rate,
			OperationCallback callback) throws Exception {
	}

	@Override
	public void getLiveStreamCompressRate(
			LiveStreamCompressRateCallback callback) throws Exception {
	}

	@Override
	public void setLiveViewMode(LiveViewMode mode, OperationCallback callback)
			throws Exception {
	}

	@Override
	public void getLiveViewMode(GetLiveViewModeCallback callback)
			throws Exception {
	}

	@Override
	public void setLiveViewStreamReadyListener(ILiveViewStreamReadyListener l) {
	}

}
