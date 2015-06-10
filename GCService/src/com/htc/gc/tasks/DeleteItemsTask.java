package com.htc.gc.tasks;

import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.LinkedList;
import java.util.List;

import com.htc.gc.interfaces.Common;
import com.htc.gc.interfaces.Common.OperationCallback;
import com.htc.gc.interfaces.IItemOperator;
import com.htc.gc.interfaces.IMediaItem;
import com.htc.gc.internal.Protocol;
import com.htc.gc.internal.SimpleTask;

public class DeleteItemsTask extends SimpleTask {
	private static final int COMMAND_ID = Protocol.FILE_DELETE_BATCH;

	private final List<IMediaItem> mItems = new LinkedList<IMediaItem>();

	public DeleteItemsTask(IItemOperator that, List<IMediaItem> items, OperationCallback callback) {
		super(that, COMMAND_ID, callback);

		for(IMediaItem item: items) {
			mItems.add(item);
		}
	}

	@Override
	public void request(OutputStream stream) throws Exception {
		try {
			super.requestInternal(stream);

			ByteBuffer bodyBuffer = ByteBuffer.allocate(4 * mItems.size() + 4);
			bodyBuffer.order(ByteOrder.LITTLE_ENDIAN);

			for(IMediaItem item : mItems)
				bodyBuffer.putInt(item.getHandle());

			bodyBuffer.putInt(0);
			bodyBuffer.position(0);

			sendRequest(stream, COMMAND_ID, 0, bodyBuffer, true);
		}
		catch(Common.CommonException e) {
			mCallback.error(e);
		}
		catch(Exception e) {
			mCallback.error(e);
			throw e;
		}
	}
}
