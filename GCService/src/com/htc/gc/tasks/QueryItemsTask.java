package com.htc.gc.tasks;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;

import android.util.Log;

import com.htc.gc.GCMediaItem;
import com.htc.gc.interfaces.Common;
import com.htc.gc.interfaces.Common.CursorInvalidException;
import com.htc.gc.interfaces.Common.Filter;
import com.htc.gc.interfaces.IGCService.ICommandCancel;
import com.htc.gc.interfaces.IItemQuerier;
import com.htc.gc.interfaces.IItemQuerier.Cursor;
import com.htc.gc.interfaces.IItemQuerier.Order;
import com.htc.gc.interfaces.IItemQuerier.PageResult;
import com.htc.gc.interfaces.IItemQuerier.PageResultCallback;
import com.htc.gc.interfaces.IMediaItem.Type;
import com.htc.gc.internal.GCTask;
import com.htc.gc.internal.Protocol;

// TODO: it should support UUID in GC device side if we have to cache thumbnail 

public class QueryItemsTask extends GCTask {
	private static final int COMMAND_ID = Protocol.FILE_GET_OBJECT_HANDLES;

	private final IItemQuerier mThat;
	private final Order 	mOrder;
	private final Filter 	mFilter;
	private final short 	mCount;
	private final Cursor 	mCursor;
	private final short		mIndex;
	private final PageResultCallback mCallback;

	public QueryItemsTask(IItemQuerier that, Order order, Filter filter, short count, Cursor cursor, PageResultCallback callback) throws CursorInvalidException {
		mThat = that;
		mOrder = order;
		mFilter = filter;
		mCount = count;
		mCursor = cursor;
		mCallback = callback;
		
		if(mCursor != null) {
			mIndex = (short) (cursor.getIndex() + cursor.getTotal());
		} else {
			mIndex = 0;
		}
	}

	@Override
	public void request(OutputStream stream) throws Exception {
		try {
			super.request(stream);
			
			ByteBuffer bodyBuffer = ByteBuffer.allocate(6);
			bodyBuffer.order(ByteOrder.LITTLE_ENDIAN);
			bodyBuffer.put(mFilter.getVal());
			bodyBuffer.putShort(mIndex);
			bodyBuffer.putShort(mCount);
			bodyBuffer.put(mOrder.getVal());
			
			bodyBuffer.position(0);
			
			sendRequest(stream, COMMAND_ID, 0, bodyBuffer, true);
		}
		catch(Exception e) {
			mCallback.error(e);
			throw e;
		}
	}

	@Override
	public void response(InputStream stream, ICommandCancel cancel) throws Exception {
		try {
			super.response(stream, cancel);

			Protocol.ResponseHeader header = new Protocol.ResponseHeader();
			ByteBuffer bodyBuffer = receiveResponse(stream, COMMAND_ID, header, cancel, true);

			byte result = bodyBuffer.get();
			if(result != Common.ErrorCode.ERR_SUCCESS.getVal()) {
				Log.w(Common.TAG, "[" + getClass().toString() + "] Operation fail error: " + Common.ErrorCode.getKey(result).toString() + "(0x" + Integer.toHexString(result) + ")");
				throw new Common.CommonException("Operation fail", Common.ErrorCode.getKey(result));
			}

			int total = (header.mLength - Protocol.ResponseHeader.MIN_RESPONSE_LENGTH - 1)/9;
			
			ArrayList<GCMediaItem> mediaItems = new ArrayList<GCMediaItem>(total);
			for(int index= 0 ; index < total; ++ index) {

				int handle = bodyBuffer.getInt();
				short time = bodyBuffer.getShort();
				short date = bodyBuffer.getShort();
				byte type = bodyBuffer.get();

				Calendar dateTime = Calendar.getInstance();
				dateTime.set(Calendar.YEAR, ((date & 0xFE00) >> 9) + 1980);
				dateTime.set(Calendar.MONTH, ((date & 0x01E0) >> 5) - 1);
				dateTime.set(Calendar.DAY_OF_MONTH, date & 0x001F);

				dateTime.set(Calendar.HOUR_OF_DAY, (time & 0xF800) >> 11);
				dateTime.set(Calendar.MINUTE, (time & 0x07E0) >> 5);
				dateTime.set(Calendar.SECOND, ((time & 0x001F) >> 0) << 1);
				dateTime.set(Calendar.MILLISECOND, 0);
				
				GCMediaItem item = new GCMediaItem(0, handle);
				item.setCreateDate(dateTime.getTime());
				
				Type itemType = Type.None;
				switch(type) {
				case Protocol.FILE_TYPE_JPG:
					itemType = Type.Photo;
					break;
				case Protocol.FILE_TYPE_MOV:
					itemType = Type.Video;
					break;
				case Protocol.FILE_TYPE_TIMELAPSE:
					itemType = Type.TimeLapse;
					break;
				case Protocol.FILE_TYPE_SLOWMOTION:
					itemType = Type.SlowMotion;
					break;
				}
				item.setType(itemType);
				
				mediaItems.add(item);
			}
			
			if(mOrder.equals(Order.DESC)) {
				// Only DESC case needs to sort by phone due to GC's constrain
				Collections.sort(mediaItems);	
			}

			PageResult pageResult = new PageResult();
			for(int i = 0 ; i < total; ++i) {
				pageResult.mItems.add(mediaItems.get(i));
			}
			pageResult.mPageCursor = new Cursor(mIndex, (short)total);

			try {
				mCallback.result(mThat, pageResult);
			}
			catch(Exception e) {
				e.printStackTrace();
				mCallback.error(e);
			}
		}
		catch(Common.CommonException e) {
			mCallback.error(e);
		}
		catch(Exception e) {
			mCallback.error(e);
			throw e;
		}
	}

	@Override
	public void error(Exception e) {
		mCallback.error(e);
	}
}
