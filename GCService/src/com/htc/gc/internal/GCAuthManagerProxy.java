package com.htc.gc.internal;

import com.htc.gc.interfaces.IAuthManager;
import com.htc.gc.interfaces.Common.OperationCallback;

public class GCAuthManagerProxy implements IAuthManager {
	
	private IAuthManager mAuthManager = new NullGCAuthManager();
	
	private IAuthListener mAuthListener;
	
	public void setAuthManager(IAuthManager authManager) {
		mAuthManager = authManager;
		
		mAuthManager.setAuthListener(mAuthListener);
	}

	@Override
	public void changePassword(String password, OperationCallback callback)
			throws Exception {
		mAuthManager.changePassword(password, callback);
	}

	@Override
	public void setAuthListener(IAuthListener listener) {
		mAuthListener = listener;
		mAuthManager.setAuthListener(listener);
	}

}
