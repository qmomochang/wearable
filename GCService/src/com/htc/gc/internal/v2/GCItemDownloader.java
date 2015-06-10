package com.htc.gc.internal.v2;

import android.util.Log;

import com.htc.gc.interfaces.Common;
import com.htc.gc.interfaces.Common.DataCallback;
import com.htc.gc.interfaces.Common.ICancelable;
import com.htc.gc.interfaces.Common.NoImpException;
import com.htc.gc.interfaces.Common.StatusException;
import com.htc.gc.interfaces.IGCService.ConnectionMode;
import com.htc.gc.interfaces.IItemDownloader;
import com.htc.gc.interfaces.IMediaItem;

class GCItemDownloader implements IItemDownloader {
	protected static final boolean DEBUG = Common.DEBUG;

	private final IMediator mService;

	GCItemDownloader(IMediator service) {
		mService = service;
	}

	@Override
	public ICancelable queryThumbnail(IMediaItem item, ThumbnailLevel thumbnailLevel, DataCallback callback) throws Exception {
		if(DEBUG) Log.i(Common.TAG, "[GCItemDownloader] queryThumbnail");

		if(callback == null) throw new NullPointerException();
		if(mService.getCurrentConnectionMode() != ConnectionMode.Full) throw new StatusException();

		throw new NoImpException();
	}

	@Override
	public ICancelable downloadItem(IMediaItem item, long rangeBegin, DataCallback callback) throws Exception {
		if(DEBUG) Log.i(Common.TAG, "[GCItemDownloader] downloadItem");

		if(callback == null) throw new NullPointerException();
		if(mService.getCurrentConnectionMode() != ConnectionMode.Full) throw new StatusException();

		throw new NoImpException();
	}

	@Override
	public ICancelable downloadTimeLapseFrame(IMediaItem item, int frameNumber, DataCallback callback) throws Exception {
		if(DEBUG) Log.i(Common.TAG, "[GCItemDownloader] downloadTimeLapseFrame");

		if(callback == null) throw new NullPointerException();
		if(mService.getCurrentConnectionMode() != ConnectionMode.Full) throw new StatusException();

		throw new NoImpException();
	}

	@Override
	public ICancelable downloadBroadcastVideo(long sequenceNumber, DataCallback callback) throws Exception {
		if(DEBUG) Log.i(Common.TAG, "[GCItemDownloader][rtmp] downloadBroadcastVideo, seq= "+sequenceNumber);
		
		if(callback == null) throw new NullPointerException();
		if(mService.getCurrentConnectionMode() != ConnectionMode.Full) throw new StatusException();
		
		throw new NoImpException();
	}

}
