package com.htc.gc.interfaces;

import com.htc.gc.interfaces.Common.DataCallback;
import com.htc.gc.interfaces.Common.ICancelable;

public interface IItemDownloader {
	public enum ThumbnailLevel {
		None,
		Small,
		FullHD,
	}

	public ICancelable queryThumbnail(IMediaItem item, ThumbnailLevel thumbnailLevel, DataCallback callback) throws Exception;
	public ICancelable downloadItem(IMediaItem item, long rangeBegin, DataCallback callback) throws Exception;
	public ICancelable downloadTimeLapseFrame(IMediaItem item, int frameNumber, DataCallback callback) throws Exception;
	public ICancelable downloadBroadcastVideo(long sequenceNumber, DataCallback callback) throws Exception;
}
