package com.htc.gc.internal;

import java.util.List;

import com.htc.gc.interfaces.IItemOperator;
import com.htc.gc.interfaces.IMediaItem;
import com.htc.gc.interfaces.Common.OperationCallback;

public class GCItemOperatorProxy implements IItemOperator {
	
	private IItemOperator mItemOperator = new NullGCItemOperator();
	
	public void setItemOperator(IItemOperator itemOperator) {
		mItemOperator = itemOperator;
	}

	@Override
	public void markAsAutoSaved(IMediaItem item, OperationCallback callback)
			throws Exception {
		mItemOperator.markAsAutoSaved(item, callback);
	}

	@Override
	public void deleteInControlMode(IMediaItem item, OperationCallback callback)
			throws Exception {
		mItemOperator.deleteInControlMode(item, callback);
	}

	@Override
	public void delete(List<IMediaItem> items, OperationCallback callback)
			throws Exception {
		mItemOperator.delete(items, callback);
	}

	@Override
	public void deleteAll(OperationCallback callback) throws Exception {
		mItemOperator.deleteAll(callback);
	}

}
