package com.htc.gc.interfaces;

import java.util.ArrayList;

import com.htc.gc.interfaces.Common.ErrorCallback;
import com.htc.gc.interfaces.Common.Filter;

public interface IItemQuerier {
	public static final String DCIM = "/DCIM/";
	
	public enum Order {
		ASC		((byte)0x0),
		DESC	((byte)0x1);
		
		private final byte mVal;
		Order(byte val) { mVal = val; }
		public byte getVal() { return mVal; } 
		public static Order getKey(byte val) throws Common.NoImpException {
			for(Order res : Order.values()) {
				if(res.getVal() == val) {
					return res;
				}
			}
			throw new Common.NoImpException();
		}
	}

	public class Cursor {
		private final short mIndex;
		private final short mTotal;

		@Override
		public String toString() {
			return "[Index= "+mIndex+", Total= "+mTotal+"]";
		}
		
		public short getIndex() {
			return mIndex;
		}
		
		public short getTotal() {
			return mTotal;
		}
		
		public Cursor(short index, short total) {
			mIndex 	= index;
			mTotal 	= total;
		}
	}

	public class PageResult {
		public ArrayList<IMediaItem> mItems = new ArrayList<IMediaItem>();
		public Cursor mPageCursor = null;
	}

	public interface PageResultCallback extends ErrorCallback {
		void result(IItemQuerier that, PageResult result);
	}

	public interface ItemDetialCallback extends ErrorCallback {
		void result(IItemQuerier that, IMediaItem item);
	}

	public interface AddItemListener {
		public void onAddItem(IItemQuerier that, IMediaItem item, int freeSpaceUnit, long freeSpaceByte);
	}

	public IMediaItem getLastItem();

	public void queryItems(Order order, Filter filter, short count, Cursor cursor, PageResultCallback callback) throws Exception;
	public void queryDetail(IMediaItem item, ItemDetialCallback callback, boolean forceRefrash) throws Exception;

	public void setAddItemListener(AddItemListener l);
}
