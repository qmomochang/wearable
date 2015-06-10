package com.htc.gc.tasks;

import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import com.htc.gc.interfaces.Common.DataCallback;
import com.htc.gc.internal.Protocol;

public class DownloadBroadcastVideoTask extends BackgroundDownloadFragmentTask {
	private static final int COMMAND_ID = Protocol.FILE_DOWNLOAD_BROADCAST_VIDEO_FRAGMENT;
	
	private final long mSequenceNumber;

	public DownloadBroadcastVideoTask(long sequenceNumber, DataCallback callback) {
		super(callback, COMMAND_ID);
		
		mSequenceNumber = sequenceNumber;
		mChannelType = ChannelType.FILE_CHANNEL;
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
			bodyBuffer.putInt((int)mSequenceNumber); // unsigned int
			bodyBuffer.position(0);

			sendRequest(stream, COMMAND_ID, 0, bodyBuffer, true);
		}
		catch(Exception e) {
			mCallback.error(e);
			throw e;
		}
	}
}
