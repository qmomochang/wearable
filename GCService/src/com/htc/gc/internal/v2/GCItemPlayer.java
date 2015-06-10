package com.htc.gc.internal.v2;

import android.util.Log;

import com.htc.gc.interfaces.Common;
import com.htc.gc.interfaces.Common.NoImpException;
import com.htc.gc.interfaces.Common.OperationCallback;
import com.htc.gc.interfaces.Common.StatusException;
import com.htc.gc.interfaces.IGCService.ConnectionMode;
import com.htc.gc.interfaces.IItemPlayer;
import com.htc.gc.interfaces.IMediaItem;

class GCItemPlayer implements IItemPlayer {
	protected static final boolean DEBUG = Common.DEBUG;

	private final IMediator mService;

	protected PlaybackListener mRecordListener;

	GCItemPlayer(IMediator service) {
		mService = service;
	}

	@Override
	public void prepare(IMediaItem item, PrepareCallback callback) throws Exception {
		if(DEBUG) Log.i(Common.TAG, "[GCItemPlayer] prepare");

		if(callback == null) throw new NullPointerException();
		if(mService.getCurrentConnectionMode() != ConnectionMode.Full) throw new StatusException();

		throw new NoImpException();
	}

	@Override
	public void play(OperationCallback callback) throws Exception {
		if(DEBUG) Log.i(Common.TAG, "[GCItemPlayer] play");

		if(callback == null) throw new NullPointerException();
		if(mService.getCurrentConnectionMode() != ConnectionMode.Full) throw new StatusException();

		throw new NoImpException();
	}

	@Override
	public void seek(long timecode, OperationCallback callback)	throws Exception {
		if(DEBUG) Log.i(Common.TAG, "[GCItemPlayer] seek");

		if(callback == null) throw new NullPointerException();
		if(mService.getCurrentConnectionMode() != ConnectionMode.Full) throw new StatusException();

		throw new NoImpException();
	}

	@Override
	public void pause(OperationCallback callback) throws Exception {
		if(DEBUG) Log.i(Common.TAG, "[GCItemPlayer] pause");

		if(callback == null) throw new NullPointerException();
		if(mService.getCurrentConnectionMode() != ConnectionMode.Full) throw new StatusException();

		throw new NoImpException();
	}

	@Override
	public void stop(OperationCallback callback) throws Exception {
		if(DEBUG) Log.i(Common.TAG, "[GCItemPlayer] stop");

		if(callback == null) throw new NullPointerException();
		if(mService.getCurrentConnectionMode() != ConnectionMode.Full) throw new StatusException();

		throw new NoImpException();
	}

	@Override
	public void setPlaybackListener(PlaybackListener l) {
		mRecordListener = l;
	}
}
