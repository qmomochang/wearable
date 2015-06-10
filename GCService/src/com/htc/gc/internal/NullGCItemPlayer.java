package com.htc.gc.internal;

import com.htc.gc.interfaces.IItemPlayer;
import com.htc.gc.interfaces.IMediaItem;
import com.htc.gc.interfaces.Common.OperationCallback;

class NullGCItemPlayer implements IItemPlayer {

	@Override
	public void prepare(IMediaItem item, PrepareCallback callback)
			throws Exception {
	}

	@Override
	public void play(OperationCallback callback) throws Exception {
	}

	@Override
	public void seek(long timecode, OperationCallback callback)
			throws Exception {
	}

	@Override
	public void pause(OperationCallback callback) throws Exception {
	}

	@Override
	public void stop(OperationCallback callback) throws Exception {
	}

	@Override
	public void setPlaybackListener(PlaybackListener l) {
	}

}
