package com.htc.gc.tasks;

import java.io.OutputStream;

import com.htc.gc.interfaces.Common.DataCallback;
import com.htc.gc.internal.Protocol;

public class QueryVideoQVTask extends DownloadFragmentTask {
	private static final int COMMAND_ID = Protocol.VIDEO_GET_QV_THUMB;

	public QueryVideoQVTask(DataCallback callback) {
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
