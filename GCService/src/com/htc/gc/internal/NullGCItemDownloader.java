package com.htc.gc.internal;

import com.htc.gc.interfaces.IItemDownloader;
import com.htc.gc.interfaces.IMediaItem;
import com.htc.gc.interfaces.Common.DataCallback;
import com.htc.gc.interfaces.Common.ICancelable;

class NullGCItemDownloader implements IItemDownloader {

	@Override
	public ICancelable queryThumbnail(IMediaItem item,
			ThumbnailLevel thumbnailLevel, DataCallback callback)
			throws Exception {
		return null;
	}

	@Override
	public ICancelable downloadItem(IMediaItem item, long rangeBegin,
			DataCallback callback) throws Exception {
		return null;
	}

	@Override
	public ICancelable downloadTimeLapseFrame(IMediaItem item, int frameNumber,
			DataCallback callback) throws Exception {
		return null;
	}

	@Override
	public ICancelable downloadBroadcastVideo(long sequenceNumber,
			DataCallback callback) throws Exception {
		return null;
	}

}
