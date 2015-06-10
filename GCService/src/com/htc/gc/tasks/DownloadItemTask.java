package com.htc.gc.tasks;

import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import com.htc.gc.interfaces.Common.DataCallback;
import com.htc.gc.interfaces.IMediaItem;
import com.htc.gc.internal.Protocol;

public class DownloadItemTask extends BackgroundDownloadFragmentTask {
	private static final int COMMAND_ID = Protocol.FILE_DOWNLOAD_FILE;

	private final IMediaItem mMediaItem;
	private final long mRangeBegin;

	public DownloadItemTask(IMediaItem mediaItem, long rangeBegin, DataCallback callback) {
		super(callback, COMMAND_ID);
		mMediaItem = mediaItem;
		mRangeBegin = rangeBegin;
		mChannelType = ChannelType.FILE_CHANNEL;
	}
	
	@Override
	public void request(OutputStream stream) throws Exception {
		try {
			if(isCancel()) {
				return;
			}

			super.request(stream);

			ByteBuffer bodyBuffer = ByteBuffer.allocate(8);
			bodyBuffer.order(ByteOrder.LITTLE_ENDIAN);
			bodyBuffer.putInt(mMediaItem.getHandle());
			bodyBuffer.putInt((int)mRangeBegin /* GC only support to 4GB */);
			bodyBuffer.position(0);

			sendRequest(stream, COMMAND_ID, 0, bodyBuffer, true);
		}
		catch(Exception e) {
			mCallback.error(e);
			throw e;
		}
	}
}
