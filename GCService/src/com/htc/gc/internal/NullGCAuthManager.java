package com.htc.gc.internal;

import com.htc.gc.interfaces.IAuthManager;
import com.htc.gc.interfaces.Common.OperationCallback;

class NullGCAuthManager implements IAuthManager {

	@Override
	public void changePassword(String password, OperationCallback callback)
			throws Exception {
	}

	@Override
	public void setAuthListener(IAuthListener listener) {
	}

}
