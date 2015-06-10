package com.htc.gc.interfaces;

import com.htc.gc.interfaces.Common.ErrorCallback;
import com.htc.gc.interfaces.Common.OperationCallback;

public interface IAuthManager {
	public interface IAuthListener extends ErrorCallback {
		public void onSuggestChangePassword();
	}
	
	public void changePassword(String password, OperationCallback callback) throws Exception;
	
	public void setAuthListener(IAuthListener listener);
}
