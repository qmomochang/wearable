package com.htc.gc.tasks;

import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import com.htc.gc.interfaces.Common.OperationCallback;
import com.htc.gc.interfaces.IItemPlayer;
import com.htc.gc.internal.Protocol;
import com.htc.gc.internal.SimpleTask;

public class ItemPlaybackSeekTask extends SimpleTask {
	private static final int COMMAND_ID = Protocol.VIDEO_PLAY_SEEK;

	private final long mTimecode;

	public ItemPlaybackSeekTask(IItemPlayer that, long timecode, OperationCallback callback) {
		super(that, COMMAND_ID, callback);
		mTimecode = timecode;
	}

	@Override
	public void request(OutputStream stream) throws Exception {
		try {
			super.requestInternal(stream);

			ByteBuffer bodyBuffer = ByteBuffer.allocate(4);
			bodyBuffer.order(ByteOrder.LITTLE_ENDIAN);
			bodyBuffer.putInt((int)mTimecode / 1000); // TODO: it should be ms not s
			bodyBuffer.position(0);

			sendRequest(stream, COMMAND_ID, 0, bodyBuffer, true);
		}
		catch(Exception e) {
			mCallback.error(e);
			throw e;
		}
	}
}
