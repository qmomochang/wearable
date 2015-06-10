package com.htc.gc.tasks;

import java.io.OutputStream;

import com.htc.gc.interfaces.Common.DataCallback;
import com.htc.gc.internal.Protocol;

public class QueryStillQVTask extends DownloadFragmentTask {
	private static final int COMMAND_ID = Protocol.CAPTURE_GET_QV_IMAGE;

	public QueryStillQVTask(DataCallback callback) {
		super(callback, COMMAND_ID);
	}

	@Override
	public void request(OutputStream stream) throws Exception {
		try {
			if(isCancel()) {
				return;
			}

			super.request(stream);
			sendRequest(stream, COMMAND_ID, 0, null, true);
		}
		catch(Exception e) {
			mCallback.error(e);
			throw e;
		}
	}
}
