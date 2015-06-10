package com.htc.gc.tasks;

import java.io.OutputStream;
import java.nio.ByteBuffer;

import com.htc.gc.interfaces.Common;
import com.htc.gc.interfaces.Common.OperationCallback;
import com.htc.gc.interfaces.ILiveViewer;
import com.htc.gc.interfaces.ILiveViewer.LiveViewMode;
import com.htc.gc.internal.Protocol;
import com.htc.gc.internal.SimpleTask;

public class LiveViewSetModeTask extends SimpleTask {
	private static final int COMMAND_ID = Protocol.SYS_SET_LIVEVIEW_MODE;
	
	private final LiveViewMode mMode;
	
	public LiveViewSetModeTask(ILiveViewer that, LiveViewMode mode, OperationCallback callback) {
		super(that, COMMAND_ID, callback);
		
		mMode = mode;
	}

	@Override
	public void request(OutputStream stream) throws Exception {
		try {
			super.requestInternal(stream);
			
			ByteBuffer bodyBuffer = ByteBuffer.allocate(1);
			bodyBuffer.put(mMode.getVal());
			
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
