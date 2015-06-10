package com.htc.gc.internal;

import com.htc.gc.interfaces.IItemDownloader;
import com.htc.gc.interfaces.IMediaItem;
import com.htc.gc.interfaces.Common.DataCallback;
import com.htc.gc.interfaces.Common.ICancelable;

public class GCItemDownloaderProxy implements IItemDownloader {
	
	private IItemDownloader mItemDownloader = new NullGCItemDownloader();
	
	public void setItemDownloader(IItemDownloader itemDownloader) {
		mItemDownloader = itemDownloader;
	}

	@Override
	public ICancelable queryThumbnail(IMediaItem item,
			ThumbnailLevel thumbnailLevel, DataCallback callback)
			throws Exception {
		return mItemDownloader.queryThumbnail(item, thumbnailLevel, callback);
	}

	@Override
	public ICancelable downloadItem(IMediaItem item, long rangeBegin,
			DataCallback callback) throws Exception {
		return mItemDownloader.downloadItem(item, rangeBegin, callback);
	}

	@Override
	public ICancelable downloadTimeLapseFrame(IMediaItem item, int frameNumber,
			DataCallback callback) throws Exception {
		return mItemDownloader.downloadTimeLapseFrame(item, frameNumber, callback);
	}

	@Override
	public ICancelable downloadBroadcastVideo(long sequenceNumber,
			DataCallback callback) throws Exception {
		return mItemDownloader.downloadBroadcastVideo(sequenceNumber, callback);
	}

}
