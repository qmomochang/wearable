package com.htc.gc.internal;

import com.htc.gc.interfaces.IItemQuerier;
import com.htc.gc.interfaces.IMediaItem;
import com.htc.gc.interfaces.Common.Filter;

class NullGCItemQuerier implements IItemQuerier {

	@Override
	public IMediaItem getLastItem() {
		return null;
	}

	@Override
	public void queryItems(Order order, Filter filter, short count,
			Cursor cursor, PageResultCallback callback) throws Exception {
	}

	@Override
	public void queryDetail(IMediaItem item, ItemDetialCallback callback,
			boolean forceRefrash) throws Exception {
	}

	@Override
	public void setAddItemListener(AddItemListener l) {
	}

}
