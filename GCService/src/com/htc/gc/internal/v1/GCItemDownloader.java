package com.htc.gc.internal.v1;

import android.util.Log;

import com.htc.gc.interfaces.Common;
import com.htc.gc.interfaces.Common.DataCallback;
import com.htc.gc.interfaces.Common.ICancelable;
import com.htc.gc.interfaces.Common.NoImpException;
import com.htc.gc.interfaces.Common.StatusException;
import com.htc.gc.interfaces.IGCService.ConnectionMode;
import com.htc.gc.interfaces.IItemDownloader;
import com.htc.gc.interfaces.IMediaItem;
import com.htc.gc.tasks.DownloadBroadcastVideoTask;
import com.htc.gc.tasks.DownloadItemTask;
import com.htc.gc.tasks.DownloadTimeLapseFrameTask;
import com.htc.gc.tasks.QueryFullHDThumbnailTask;
import com.htc.gc.tasks.QuerySmallThumbnailTask;

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

		ICancelable cancelable = null;

		if(thumbnailLevel == ThumbnailLevel.Small) {
			QuerySmallThumbnailTask task = new QuerySmallThumbnailTask(item, callback);
			cancelable = task;
			mService.requestCommand(task);
		}
		else if(thumbnailLevel == ThumbnailLevel.FullHD) {
			QueryFullHDThumbnailTask task = new QueryFullHDThumbnailTask(item, callback);
			cancelable = task;
			mService.requestCommand(task);
		}
		else throw new NoImpException();

		return cancelable;
	}

	@Override
	public ICancelable downloadItem(IMediaItem item, long rangeBegin, DataCallback callback) throws Exception {
		if(DEBUG) Log.i(Common.TAG, "[GCItemDownloader] downloadItem");

		if(callback == null) throw new NullPointerException();
		if(mService.getCurrentConnectionMode() != ConnectionMode.Full) throw new StatusException();

		DownloadItemTask task;
		mService.requestCommand(task = new DownloadItemTask(item, rangeBegin, callback));
		return task;
	}

	@Override
	public ICancelable downloadTimeLapseFrame(IMediaItem item, int frameNumber, DataCallback callback) throws Exception {
		if(DEBUG) Log.i(Common.TAG, "[GCItemDownloader] downloadTimeLapseFrame");

		if(callback == null) throw new NullPointerException();
		if(mService.getCurrentConnectionMode() != ConnectionMode.Full) throw new StatusException();

		DownloadTimeLapseFrameTask task;
		mService.requestCommand(task = new DownloadTimeLapseFrameTask(item, frameNumber, callback));
		return task;
	}

	@Override
	public ICancelable downloadBroadcastVideo(long sequenceNumber, DataCallback callback) throws Exception {
		if(DEBUG) Log.i(Common.TAG, "[GCItemDownloader][rtmp] downloadBroadcastVideo, seq= "+sequenceNumber);
		
		if(callback == null) throw new NullPointerException();
		if(mService.getCurrentConnectionMode() != ConnectionMode.Full) throw new StatusException();
		
		DownloadBroadcastVideoTask task;
		mService.requestCommand(task = new DownloadBroadcastVideoTask(sequenceNumber, callback));
		return task;
	}

}
