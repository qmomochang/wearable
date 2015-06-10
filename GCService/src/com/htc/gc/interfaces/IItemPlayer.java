package com.htc.gc.interfaces;

import android.net.Uri;

import com.htc.gc.interfaces.Common.ErrorCallback;
import com.htc.gc.interfaces.Common.OperationCallback;

public interface IItemPlayer {
	public interface PrepareCallback extends ErrorCallback {
		public void result(IItemPlayer that, Uri result);
	}

	public interface PlaybackListener {
		public void onStop(IItemPlayer that);
	}

	public void prepare(IMediaItem item, PrepareCallback callback) throws Exception;
	public void play(OperationCallback callback) throws Exception;
	public void seek(long timecode, OperationCallback callback) throws Exception;
	public void pause(OperationCallback callback) throws Exception;
	public void stop(OperationCallback callback) throws Exception;

	public void setPlaybackListener(PlaybackListener l);
}
