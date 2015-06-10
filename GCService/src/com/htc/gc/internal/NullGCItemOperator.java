package com.htc.gc.internal;

import java.util.List;

import com.htc.gc.interfaces.IItemOperator;
import com.htc.gc.interfaces.IMediaItem;
import com.htc.gc.interfaces.Common.OperationCallback;

class NullGCItemOperator implements IItemOperator {

	@Override
	public void markAsAutoSaved(IMediaItem item, OperationCallback callback)
			throws Exception {
	}

	@Override
	public void deleteInControlMode(IMediaItem item, OperationCallback callback)
			throws Exception {
	}

	@Override
	public void delete(List<IMediaItem> items, OperationCallback callback)
			throws Exception {
	}

	@Override
	public void deleteAll(OperationCallback callback) throws Exception {
	}

}
