package com.htc.gc.internal.v1;

import java.util.LinkedList;
import java.util.List;

import android.util.Log;

import com.htc.gc.interfaces.Common;
import com.htc.gc.interfaces.Common.OperationCallback;
import com.htc.gc.interfaces.Common.StatusException;
import com.htc.gc.interfaces.IGCService.ConnectionMode;
import com.htc.gc.interfaces.IItemOperator;
import com.htc.gc.interfaces.IMediaItem;
import com.htc.gc.tasks.DeleteAllTask;
import com.htc.gc.tasks.DeleteItemsTask;
import com.htc.gc.tasks.MarkAsAutoSavedTask;

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
		
		mService.requestCommand(new MarkAsAutoSavedTask(this, item, callback));
	}
	
	@Override
	public void deleteInControlMode(IMediaItem item, final OperationCallback callback) throws Exception {
		if(DEBUG) Log.i(Common.TAG, "[GCItemOperator] deleteInControlMode");

		if(callback == null) throw new NullPointerException();
		if(mService.getCurrentConnectionMode() != ConnectionMode.Full) throw new StatusException();

		final List<IMediaItem> items = new LinkedList<IMediaItem>();
		items.add(item);
		
		mService.requestCommand(new DeleteItemsTask(this, items, new OperationCallback() {

			@Override
			public void error(Exception e) {
				callback.error(e);
			}

			@Override
			public void done(Object that) {
				mService.updateCursorValidityKey();
				callback.done(that);
			}

		}));
	}
	
	@Override
	public void delete(List<IMediaItem> items, final OperationCallback callback) throws Exception {
		if(DEBUG) Log.i(Common.TAG, "[GCItemOperator] delete");

		if(callback == null) throw new NullPointerException();
		if(mService.getCurrentConnectionMode() != ConnectionMode.Full) throw new StatusException();

		mService.requestCommand(new DeleteItemsTask(this, items, new OperationCallback() {

			@Override
			public void error(Exception e) {
				callback.error(e);
			}

			@Override
			public void done(Object that) {
				mService.updateCursorValidityKey();
				callback.done(that);
			}

		}));
	}

	@Override
	public void deleteAll(final OperationCallback callback) throws Exception {
		if(DEBUG) Log.i(Common.TAG, "[GCItemOperator] deleteAll");

		if(callback == null) throw new NullPointerException();
		if(mService.getCurrentConnectionMode() != ConnectionMode.Full) throw new StatusException();

		mService.requestCommand(new DeleteAllTask(this, new OperationCallback() {

			@Override
			public void error(Exception e) {
				callback.error(e);
			}

			@Override
			public void done(Object that) {
				mService.updateCursorValidityKey();
				callback.done(that);
			}

		}));
	}

}