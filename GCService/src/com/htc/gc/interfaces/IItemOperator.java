package com.htc.gc.interfaces;

import java.util.List;

import com.htc.gc.interfaces.Common.OperationCallback;

public interface IItemOperator {

	public void markAsAutoSaved(IMediaItem item, OperationCallback callback) throws Exception;
	public void deleteInControlMode(IMediaItem item, OperationCallback callback) throws Exception;
	public void delete(List<IMediaItem> items, OperationCallback callback) throws Exception;
	public void deleteAll(OperationCallback callback) throws Exception;
}
