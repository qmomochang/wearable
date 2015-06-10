package com.htc.gc.tasks;

import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import com.htc.gc.interfaces.Common.DataCallback;
import com.htc.gc.interfaces.IMediaItem;
import com.htc.gc.internal.Protocol;

public class QuerySmallThumbnailTask extends BackgroundDownloadFragmentTask {
	private static final int COMMAND_ID = Protocol.FILE_GET_FILE_THUMB;

	private final IMediaItem mMediaItem;

	public QuerySmallThumbnailTask(IMediaItem mediaItem, DataCallback callback) {
		super(callback, COMMAND_ID);
		
		mMediaItem = mediaItem;
		mChannelType = ChannelType.THUMBNAIL_CHANNEL;
	}

	@Override
	public void request(OutputStream stream) throws Exception {
		try {
			if(isCancel()) {
				return;
			}

			super.request(stream);

			ByteBuffer bodyBuffer = ByteBuffer.allocate(4);
			bodyBuffer.order(ByteOrder.LITTLE_ENDIAN);
			bodyBuffer.putInt(mMediaItem.getHandle());
			bodyBuffer.position(0);

			sendRequest(stream, COMMAND_ID, 0, bodyBuffer, true);
		}
		catch(Exception e) {
			mCallback.error(e);
			throw e;
		}
	}
}
