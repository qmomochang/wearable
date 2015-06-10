package com.htc.gc.internal;

import java.io.InputStream;
import java.io.OutputStream;

import android.util.Log;

import com.htc.gc.interfaces.Common;
import com.htc.gc.interfaces.Common.ICancelable;
import com.htc.gc.interfaces.Common.OperationCallback;
import com.htc.gc.interfaces.IGCService.ICommandCancel;

public abstract class CancelableTask extends GCTask implements ICancelable {
	protected static final boolean DEBUG = Common.DEBUG;

	protected final int mCommand;
	private boolean mIsCancel = false;
	private boolean mIsExecute = false;
	private OperationCallback mCancelCallback;

	public CancelableTask(int command) {
		super();
		mCommand = command;
	}

	public void cancel(OperationCallback callback) {
		Log.i(Common.TAG, "[CancelableTask] do cancel (" + getSequenceID() + ")");

		mCancelCallback = callback;
		mIsCancel = true;
	}

	public boolean isExecute() {
		return mIsExecute;
	}

	public boolean isCancel() {
		return mIsCancel;
	}

	protected CancelTask createCancelTask() {
		return new CancelTask(getSequenceID(), mCancelCallback);
	}

	public void request(OutputStream stream) throws Exception {
		super.request(stream);

		mIsExecute = true;
	}

	public void response(InputStream stream, ICommandCancel cancel) throws Exception {
		super.response(stream, cancel);
	}

}

