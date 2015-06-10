package com.htc.gc.internal.v1;

import java.nio.ByteBuffer;

import android.util.Log;

import com.htc.gc.interfaces.Common;
import com.htc.gc.interfaces.Common.OperationCallback;
import com.htc.gc.interfaces.Common.StatusException;
import com.htc.gc.interfaces.IGCService.ConnectionMode;
import com.htc.gc.interfaces.IItemPlayer;
import com.htc.gc.interfaces.IMediaItem;
import com.htc.gc.internal.v1.IMediator.IEventListener;
import com.htc.gc.internal.Protocol;
import com.htc.gc.tasks.ItemPlaybackPauseTask;
import com.htc.gc.tasks.ItemPlaybackPlayTask;
import com.htc.gc.tasks.ItemPlaybackPrepareTask;
import com.htc.gc.tasks.ItemPlaybackSeekTask;
import com.htc.gc.tasks.ItemPlaybackStopTask;

class GCItemPlayer implements IItemPlayer {
	protected static final boolean DEBUG = Common.DEBUG;

	private final IMediator mService;

	protected PlaybackListener mRecordListener;

	GCItemPlayer(IMediator service) {
		mService = service;

		mService.addEventListener(Protocol.EVENT_VIDEO_PLAYBACK_FINISH, new IEventListener() {

			@Override
			public void event(int eventID, ByteBuffer body) {
				if(DEBUG) Log.i(Common.TAG, "[GCItemPlayer] onStop event");

				PlaybackListener l = mRecordListener;
				if(l != null) l.onStop(GCItemPlayer.this);
			}
		});
	}

	@Override
	public void prepare(IMediaItem item, PrepareCallback callback) throws Exception {
		if(DEBUG) Log.i(Common.TAG, "[GCItemPlayer] prepare");

		if(callback == null) throw new NullPointerException();
		if(mService.getCurrentConnectionMode() != ConnectionMode.Full) throw new StatusException();

		mService.requestCommand(new ItemPlaybackPrepareTask(this, item, callback));
	}

	@Override
	public void play(OperationCallback callback) throws Exception {
		if(DEBUG) Log.i(Common.TAG, "[GCItemPlayer] play");

		if(callback == null) throw new NullPointerException();
		if(mService.getCurrentConnectionMode() != ConnectionMode.Full) throw new StatusException();

		mService.requestCommand(new ItemPlaybackPlayTask(this, callback));
	}

	@Override
	public void seek(long timecode, OperationCallback callback)	throws Exception {
		if(DEBUG) Log.i(Common.TAG, "[GCItemPlayer] seek");

		if(callback == null) throw new NullPointerException();
		if(mService.getCurrentConnectionMode() != ConnectionMode.Full) throw new StatusException();

		mService.requestCommand(new ItemPlaybackSeekTask(this, timecode, callback));
	}

	@Override
	public void pause(OperationCallback callback) throws Exception {
		if(DEBUG) Log.i(Common.TAG, "[GCItemPlayer] pause");

		if(callback == null) throw new NullPointerException();
		if(mService.getCurrentConnectionMode() != ConnectionMode.Full) throw new StatusException();

		mService.requestCommand(new ItemPlaybackPauseTask(this, callback));
	}

	@Override
	public void stop(OperationCallback callback) throws Exception {
		if(DEBUG) Log.i(Common.TAG, "[GCItemPlayer] stop");

		if(callback == null) throw new NullPointerException();
		if(mService.getCurrentConnectionMode() != ConnectionMode.Full) throw new StatusException();

		mService.requestCommand(new ItemPlaybackStopTask(this, callback));
	}

	@Override
	public void setPlaybackListener(PlaybackListener l) {
		mRecordListener = l;
	}
}
