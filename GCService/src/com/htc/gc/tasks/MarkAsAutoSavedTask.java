package com.htc.gc.tasks;

import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import com.htc.gc.interfaces.Common;
import com.htc.gc.interfaces.Common.OperationCallback;
import com.htc.gc.interfaces.IItemOperator;
import com.htc.gc.interfaces.IMediaItem;
import com.htc.gc.internal.Protocol;
import com.htc.gc.internal.SimpleTask;

public class MarkAsAutoSavedTask extends SimpleTask {
	private static final int COMMAND_ID = Protocol.AUTOBACKUP_WRITE_UNATOBACKUPTABLE;

	private final IMediaItem mItem;
	
	public MarkAsAutoSavedTask(IItemOperator that, IMediaItem item, OperationCallback callback) {
		super(that, COMMAND_ID, callback);
		
		mItem = item;
	}
	
	@Override
	public void request(OutputStream stream) throws Exception {
		try {
			super.requestInternal(stream);
			
			ByteBuffer bodyBuffer = ByteBuffer.allocate(4);
			bodyBuffer.order(ByteOrder.LITTLE_ENDIAN);
			
			bodyBuffer.putInt(mItem.getHandle());
			
			bodyBuffer.position(0);
			sendRequest(stream, COMMAND_ID, 0, bodyBuffer, true);
		} catch(Common.CommonException e) {
			mCallback.error(e);
		} catch(Exception e) {
			mCallback.error(e);
			throw e;
		}
	}
	
}
