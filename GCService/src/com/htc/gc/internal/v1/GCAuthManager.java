package com.htc.gc.internal.v1;

import android.os.Bundle;
import android.util.Log;

import com.htc.gc.connectivity.interfaces.IGcConnectivityServiceBase.Result;
import com.htc.gc.connectivity.interfaces.IGcConnectivityServiceBase.VerifyPasswordStatus;
import com.htc.gc.connectivity.v2.interfaces.IGcConnectivityService;
import com.htc.gc.interfaces.Common;
import com.htc.gc.interfaces.Common.AuthException;
import com.htc.gc.interfaces.Common.BleCommandException;
import com.htc.gc.interfaces.Common.OperationCallback;
import com.htc.gc.interfaces.IAuthManager;
import com.htc.gc.internal.DeviceItem;
import com.htc.gc.internal.v1.IMediator.IBleEventListener;

class GCAuthManager implements IAuthManager {
	protected static final boolean DEBUG = Common.DEBUG;

	private final IMediator mService;
	
	private IAuthListener mAuthListener;
	
	GCAuthManager(IMediator service) {
		mService = service;
		
		mService.addBleEventListener(IGcConnectivityService.CB_VERIFY_PASSWORD_RESULT, new IBleEventListener() {

			@Override
			public void event(int callbackID, Bundle bundle) {
				Result result = (Result) bundle.getSerializable(IGcConnectivityService.PARAM_RESULT);
				if(result.equals(Result.RESULT_SUCCESS)) {
					VerifyPasswordStatus status = (VerifyPasswordStatus) bundle.getSerializable(IGcConnectivityService.PARAM_VERIFY_PASSWORD_STATUS);
					switch(status) {
					case VPSTATUS_NOT_CHANGED_AND_CORRECT:
						if(mAuthListener != null) mAuthListener.onSuggestChangePassword();
						break;
					case VPSTATUS_NOT_CHANGED_AND_INCORRECT:
						if(mAuthListener != null) mAuthListener.error(new AuthException(true));
						break;
					case VPSTATUS_CHANGED_AND_CORRECT:
						break;
					case VPSTATUS_CHANGED_AND_INCORRECT:
						if(mAuthListener != null) mAuthListener.error(new AuthException(false));
						break;
					}					
				}
			}
		});
	}
	
	@Override
	public void changePassword(final String password, final OperationCallback callback) throws Exception{
		Log.i(Common.TAG, "[GCAuthManager] changePassword");
		
		IGcConnectivityService conn = mService.getConnectivityService();
		final DeviceItem device = (DeviceItem)mService.getTargetDevice();
		if(!conn.gcChangePassword(device.getDevice(), password)) {
			throw new BleCommandException();
		} else {
			mService.addBleCallback(GCAuthManager.this, IGcConnectivityService.CB_CHANGE_PASSWORD_RESULT, new OperationCallback() {
				
				@Override
				public void error(Exception e) {
					callback.error(e);
				}
				
				@Override
				public void done(Object that) {
					device.setPassword(password);
					callback.done(that);
				}
			});
		}
	}

	@Override
	public void setAuthListener(IAuthListener listener) {
		mAuthListener = listener;		
	}

}
