package com.htc.gc.internal.v2;

import java.util.List;

import android.util.Log;

import com.htc.gc.interfaces.Common;
import com.htc.gc.interfaces.Common.NoImpException;
import com.htc.gc.interfaces.Common.OperationCallback;
import com.htc.gc.interfaces.Common.StatusException;
import com.htc.gc.interfaces.IGCService.ConnectionMode;
import com.htc.gc.interfaces.IItemOperator;
import com.htc.gc.interfaces.IMediaItem;

class GCItemOperator implements IItemOperator {
	protected static final boolean DEBUG = Common.DEBUG;

	private final IMediator mService;

	GCItemOperator(IMediator service) {
		mService = service;
	}

	@Override
	public void markAsAutoSaved(IMediaItem item, OperationCallback callback) throws Exception {
		if(DEBUG) Log.i(Common.TAG, "[GCItemOperator] markAsAutoSaved");

		if(callback == null) throw new NullPointerException();
		if(mService.getCurrentConnectionMode() != ConnectionMode.Full) throw new StatusException();
		
		throw new NoImpException();
	}
	
	@Override
	public void deleteInControlMode(IMediaItem item, final OperationCallback callback) throws Exception {
		if(DEBUG) Log.i(Common.TAG, "[GCItemOperator] deleteInControlMode");

		if(callback == null) throw new NullPointerException();
		if(mService.getCurrentConnectionMode() != ConnectionMode.Full) throw new StatusException();

		throw new NoImpException();
	}
	
	@Override
	public void delete(List<IMediaItem> items, final OperationCallback callback) throws Exception {
		if(DEBUG) Log.i(Common.TAG, "[GCItemOperator] delete");

		if(callback == null) throw new NullPointerException();
		if(mService.getCurrentConnectionMode() != ConnectionMode.Full) throw new StatusException();

		throw new NoImpException();
	}

	@Override
	public void deleteAll(final OperationCallback callback) throws Exception {
		if(DEBUG) Log.i(Common.TAG, "[GCItemOperator] deleteAll");

		if(callback == null) throw new NullPointerException();
		if(mService.getCurrentConnectionMode() != ConnectionMode.Full) throw new StatusException();

		throw new NoImpException();
	}

}