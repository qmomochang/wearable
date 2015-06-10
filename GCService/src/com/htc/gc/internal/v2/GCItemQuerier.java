package com.htc.gc.internal.v2;

import java.util.Calendar;

import android.os.Bundle;
import android.util.Log;

import com.htc.gc.GCMediaItem;
import com.htc.gc.connectivity.interfaces.IGcConnectivityServiceBase.LongTermEvent;
import com.htc.gc.connectivity.v3.interfaces.IGcConnectivityService;
import com.htc.gc.interfaces.Common;
import com.htc.gc.interfaces.Common.Filter;
import com.htc.gc.interfaces.Common.NoImpException;
import com.htc.gc.interfaces.Common.StatusException;
import com.htc.gc.interfaces.IGCService.ConnectionMode;
import com.htc.gc.interfaces.IItemQuerier;
import com.htc.gc.interfaces.IMediaItem;
import com.htc.gc.interfaces.IMediaItem.Type;
import com.htc.gc.internal.v2.IMediator.IBleEventListener;
import com.htc.gc.internal.NetworkHelper;

class GCItemQuerier implements IItemQuerier {
	protected static final boolean DEBUG = Common.DEBUG;

	private final IMediator mService;
	protected AddItemListener mAddItemListener;

	GCItemQuerier(IMediator service) {
		mService = service;
		
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

		throw new NoImpException();
	}

	@Override
	public void queryDetail(IMediaItem item, ItemDetialCallback callback, boolean forceRefrash) throws Exception {
		if(DEBUG) Log.i(Common.TAG, "[GCItemQuerier] queryDetial, forceRefrash: "+forceRefrash);

		if(callback == null) throw new NullPointerException();
		if(mService.getCurrentConnectionMode() != ConnectionMode.Full) throw new StatusException();

		throw new NoImpException();
	}

	@Override
	public void setAddItemListener(AddItemListener l) {
		mAddItemListener = l;
	}

}
