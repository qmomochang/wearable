package com.htc.gc.internal.v1;

import java.nio.ByteBuffer;
import java.util.Calendar;

import android.os.Bundle;
import android.util.Log;

import com.htc.gc.GCMediaItem;
import com.htc.gc.connectivity.interfaces.IGcConnectivityServiceBase.LongTermEvent;
import com.htc.gc.connectivity.v2.interfaces.IGcConnectivityService;
import com.htc.gc.interfaces.Common;
import com.htc.gc.interfaces.Common.CursorInvalidException;
import com.htc.gc.interfaces.Common.Filter;
import com.htc.gc.interfaces.Common.NoImpException;
import com.htc.gc.interfaces.Common.StatusException;
import com.htc.gc.interfaces.IGCService.ConnectionMode;
import com.htc.gc.interfaces.IItemQuerier;
import com.htc.gc.interfaces.IMediaItem;
import com.htc.gc.interfaces.IMediaItem.Type;
import com.htc.gc.internal.v1.IMediator.IBleEventListener;
import com.htc.gc.internal.v1.IMediator.IEventListener;
import com.htc.gc.internal.NetworkHelper;
import com.htc.gc.internal.Protocol;
import com.htc.gc.tasks.QueryItemDetailTask;
import com.htc.gc.tasks.QueryItemsTask;

class GCItemQuerier implements IItemQuerier {
	protected static final boolean DEBUG = Common.DEBUG;

	private final IMediator mService;
	protected AddItemListener mAddItemListener;

	GCItemQuerier(IMediator service) {
		mService = service;

		mService.addEventListener(Protocol.EVENT_OBJECT_ADDED, new IEventListener() {

			@Override
			public void event(int eventID, ByteBuffer body) {
				AddItemListener l = mAddItemListener;
				int type = body.getInt();

				GCMediaItem item = new GCMediaItem(0, body.getInt()); // TODO
				try {
					item.setType(Type.getKey(type));
				} catch (NoImpException e) {
					e.printStackTrace();
					item.setType(Type.None);
				}
				
				int currentRemainingSpace = 0;
				for(int i = 0; i < 4 /* JPG, MOV, TIMELAPSE, SLOWMOION */; i++) {
					int fileType = body.getInt();
					int value = body.getInt();
					
					if(fileType == type) {
						currentRemainingSpace = value;
					}
				}
				
				long freeSpace = body.getLong();
				
				item.setSize(NetworkHelper.getUnsignedInt(body.getInt()));
				
				short date = body.getShort();
				short time = body.getShort();
				Calendar dateTime = Calendar.getInstance();
				dateTime.set(Calendar.YEAR, ((date & 0xFE00) >> 9) + 1980);
				dateTime.set(Calendar.MONTH, ((date & 0x01E0) >> 5) - 1);
				dateTime.set(Calendar.DAY_OF_MONTH, date & 0x001F);
				dateTime.set(Calendar.HOUR_OF_DAY, (time & 0xF800) >> 11);
				dateTime.set(Calendar.MINUTE, (time & 0x07E0) >> 5);
				dateTime.set(Calendar.SECOND, ((time & 0x001F) >> 0) << 1);
				dateTime.set(Calendar.MILLISECOND, 0);
				item.setCreateDate(dateTime.getTime());
				
				item.setFrameCount(body.getLong());
				item.setTotalFrameSize(body.getLong());
				
				mService.setLastItem(item);

				if(DEBUG) Log.i(Common.TAG, "[GCItemQuerier] onAddItem event, GC object added 0x"+Integer.toHexString(item.getHandle())+", datetime= "+item.getCreateDate());

				if(l != null) l.onAddItem(GCItemQuerier.this, item, currentRemainingSpace, freeSpace);
			}
		});
		
		mService.addBleEventListener(IGcConnectivityService.CB_LONG_TERM_EVENT_RESULT, new IBleEventListener() {

			@Override
			public void event(int callbackID, Bundle bundle) {
				LongTermEvent event = (LongTermEvent) bundle.getSerializable(IGcConnectivityService.PARAM_LONG_TERM_EVENT);
				if(event.equals(LongTermEvent.LTEVENT_METADATA)) {
					if(mService.getCurrentConnectionMode().equals(ConnectionMode.Partial)) {
						GCMediaItem item = new GCMediaItem(0, bundle.getInt(IGcConnectivityService.PARAM_FILE_ID));
						
						String path = bundle.getString(IGcConnectivityService.PARAM_FOLDER_NAME);
						String file = bundle.getString(IGcConnectivityService.PARAM_FILE_NAME);
						item.setFileName(file);
						item.setPath(IItemQuerier.DCIM + path.substring(0, path.length() - 1) + "/" + file.substring(0, file.length() - 1));
						
						int fileType = 0;
						try {
							fileType = bundle.getInt(IGcConnectivityService.PARAM_FILE_TYPE);
							item.setType(Type.getKey(fileType));
						} catch (NoImpException e) {
							if(DEBUG) Log.w(Common.TAG, "[GCItemQuerier] unknown file fype:" + fileType);
							e.printStackTrace();
							item.setType(Type.None);
						}
						
						Calendar createDate = (Calendar) bundle.getSerializable(IGcConnectivityService.PARAM_FILE_CREATE_TIME);
						item.setCreateDate(createDate.getTime());
						
						item.setSize(NetworkHelper.getUnsignedInt(bundle.getInt(IGcConnectivityService.PARAM_FILE_SIZE)));
						item.setLength(NetworkHelper.getUnsignedInt(bundle.getInt(IGcConnectivityService.PARAM_VIDEO_DURATION)));
						
						
						if(DEBUG) Log.i(Common.TAG, "[GCItemQuerier] BLE onAddItem event, GC object added 0x"+Integer.toHexString(item.getHandle())+", datetime= "+item.getCreateDate());
						
						if(mAddItemListener != null) mAddItemListener.onAddItem(GCItemQuerier.this, item, 0, 0);
					}
				}
			}
			
		});
	}


	@Override
	public IMediaItem getLastItem() {
		return mService.getLastItem();
	}

	@Override
	public void queryItems(Order order, Filter filter, short count, Cursor cursor, PageResultCallback callback) throws Exception {
		if(DEBUG) Log.i(Common.TAG, "[GCItemQuerier] queryItems");

		if(callback == null) throw new NullPointerException();
		if(mService.getCurrentConnectionMode() != ConnectionMode.Full) throw new StatusException();

		try {
			mService.requestCommand(new QueryItemsTask(this, order, filter, count, cursor, callback));	
		} catch(CursorInvalidException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void queryDetail(IMediaItem item, ItemDetialCallback callback, boolean forceRefrash) throws Exception {
		if(DEBUG) Log.i(Common.TAG, "[GCItemQuerier] queryDetial, forceRefrash: "+forceRefrash);

		if(callback == null) throw new NullPointerException();
		if(mService.getCurrentConnectionMode() != ConnectionMode.Full) throw new StatusException();

		if(forceRefrash || !item.hasDetail()) {
			mService.requestCommand(new QueryItemDetailTask(this, (GCMediaItem)item, callback));	
		} else {
			callback.result(this, item);
		}
	}

	@Override
	public void setAddItemListener(AddItemListener l) {
		mAddItemListener = l;
	}

}
