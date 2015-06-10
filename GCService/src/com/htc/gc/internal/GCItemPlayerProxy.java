package com.htc.gc.internal;

import com.htc.gc.interfaces.IItemPlayer;
import com.htc.gc.interfaces.IMediaItem;
import com.htc.gc.interfaces.Common.OperationCallback;

public class GCItemPlayerProxy implements IItemPlayer {
	
	private IItemPlayer mItemPlayer = new NullGCItemPlayer();
	
	private PlaybackListener mPlaybackListener;
	
	public void setItemPlayer(IItemPlayer itemPlayer) {
		mItemPlayer = itemPlayer;
		
		mItemPlayer.setPlaybackListener(mPlaybackListener);
	}

	@Override
	public void prepare(IMediaItem item, PrepareCallback callback)
			throws Exception {
		mItemPlayer.prepare(item, callback);
	}

	@Override
	public void play(OperationCallback callback) throws Exception {
		mItemPlayer.play(callback);
	}

	@Override
	public void seek(long timecode, OperationCallback callback)
			throws Exception {
		mItemPlayer.seek(timecode, callback);
	}

	@Override
	public void pause(OperationCallback callback) throws Exception {
		mItemPlayer.pause(callback);
	}

	@Override
	public void stop(OperationCallback callback) throws Exception {
		mItemPlayer.stop(callback);
	}

	@Override
	public void setPlaybackListener(PlaybackListener l) {
		mPlaybackListener = l;
		mItemPlayer.setPlaybackListener(l);
	}

}
