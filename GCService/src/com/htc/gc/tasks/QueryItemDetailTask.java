package com.htc.gc.tasks;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import android.util.Log;

import com.htc.gc.GCMediaItem;
import com.htc.gc.interfaces.Common;
import com.htc.gc.interfaces.IGCService.ICommandCancel;
import com.htc.gc.interfaces.IItemQuerier;
import com.htc.gc.interfaces.IItemQuerier.ItemDetialCallback;
import com.htc.gc.interfaces.IMediaItem.Type;
import com.htc.gc.interfaces.IMediaItem.WideAngle;
import com.htc.gc.internal.GCTask;
import com.htc.gc.internal.NetworkHelper;
import com.htc.gc.internal.Protocol;

public class QueryItemDetailTask extends GCTask {
	private static final int COMMAND_ID = Protocol.FILE_GET_OBJECT_INFO;

	private final IItemQuerier mThat;
	private final GCMediaItem mItem;
	private final ItemDetialCallback mCallback;

	public QueryItemDetailTask(IItemQuerier that, GCMediaItem item, ItemDetialCallback callback) {
		mThat = that;
		mItem = item;
		mCallback = callback;
	}

	@Override
	public void request(OutputStream stream) throws Exception {
		try {
			super.request(stream);

			ByteBuffer bodyBuffer = ByteBuffer.allocate(4);
			bodyBuffer.order(ByteOrder.LITTLE_ENDIAN);
			bodyBuffer.putInt(mItem.getHandle());
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

			if(bodyBuffer.getInt() != mItem.getHandle()) throw new Common.CommonException("Handle does not expected ", Common.ErrorCode.ERR_SYSTEM_ERROR);

			byte[] pathBuffer = new byte[9];
			bodyBuffer.get(pathBuffer, 0, pathBuffer.length);
			String path = new String(pathBuffer, "UTF-8");

			byte[] fileBuffer = new byte[13];
			bodyBuffer.get(fileBuffer, 0, fileBuffer.length);
			String file = new String(fileBuffer, "UTF-8");
			mItem.setFileName(file);
			mItem.setPath(IItemQuerier.DCIM + path.substring(0, path.length() - 1) + "/" + file.substring(0, file.length() - 1));

			byte type = bodyBuffer.get();
			mItem.setType(Type.getKey((int) type));

			byte[] cBuffer = new byte[20]; // EXIF Create time
			bodyBuffer.get(cBuffer);

			SimpleDateFormat df = new SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.ENGLISH);
		    Date createDate = df.parse(new String(cBuffer));
		    mItem.setCreateDate(createDate);
			mItem.setSize(NetworkHelper.getUnsignedInt(bodyBuffer.getInt()));
			mItem.setLength(NetworkHelper.getUnsignedInt(bodyBuffer.getInt()));
			mItem.setFrameCount(bodyBuffer.getLong());
			mItem.setTotalFrameSize(bodyBuffer.getLong());
			mItem.setWideAngle(WideAngle.getKey(bodyBuffer.get()));
			try {			
				mCallback.result(mThat, mItem);
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
