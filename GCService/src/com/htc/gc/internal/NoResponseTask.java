package com.htc.gc.internal;

import java.io.InputStream;

import com.htc.gc.interfaces.Common.CommonException;
import com.htc.gc.interfaces.Common.ErrorCode;
import com.htc.gc.interfaces.Common.RequestCallback;
import com.htc.gc.interfaces.IGCService.ICommandCancel;


public class NoResponseTask extends GCTask {

	protected final RequestCallback mCallback;
	
	public NoResponseTask(RequestCallback callback) {
		mCallback = callback;
	}

	@Override
	public void response(InputStream stream, ICommandCancel cancel) throws Exception {
		throw new CommonException("RequestAndCloseSocketTask response should not be called", ErrorCode.ERR_SYSTEM_ERROR);
	}

	@Override
	public void error(Exception e) {
		e.printStackTrace();
		mCallback.error(e);
	}
}
