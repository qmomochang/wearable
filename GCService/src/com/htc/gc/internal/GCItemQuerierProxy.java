package com.htc.gc.internal;

import com.htc.gc.interfaces.IItemQuerier;
import com.htc.gc.interfaces.IMediaItem;
import com.htc.gc.interfaces.Common.Filter;

public class GCItemQuerierProxy implements IItemQuerier {
	
	private IItemQuerier mItemQuerier = new NullGCItemQuerier();
	
	private AddItemListener mAddItemListener;
	
	public void setItemQuerier(IItemQuerier itemQuerier) {
		mItemQuerier = itemQuerier;
		
		mItemQuerier.setAddItemListener(mAddItemListener);
	}

	@Override
	public IMediaItem getLastItem() {
		return mItemQuerier.getLastItem();
	}

	@Override
	public void queryItems(Order order, Filter filter, short count,
			Cursor cursor, PageResultCallback callback) throws Exception {
		mItemQuerier.queryItems(order, filter, count, cursor, callback);
	}

	@Override
	public void queryDetail(IMediaItem item, ItemDetialCallback callback,
			boolean forceRefrash) throws Exception {
		mItemQuerier.queryDetail(item, callback, forceRefrash);
	}

	@Override
	public void setAddItemListener(AddItemListener l) {
		mAddItemListener = l;
		mItemQuerier.setAddItemListener(l);
	}

}
