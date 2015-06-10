package com.htc.gc.tasks;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.HashMap;

import android.util.Log;

import com.htc.gc.interfaces.Common;
import com.htc.gc.interfaces.IDeviceController;
import com.htc.gc.interfaces.IDeviceController.SpaceInfoCallback;
import com.htc.gc.interfaces.IGCService;
import com.htc.gc.interfaces.IMediaItem;
import com.htc.gc.interfaces.IMediaItem.Type;
import com.htc.gc.internal.GCTask;
import com.htc.gc.internal.Protocol;

public class GetSpaceInfoTask extends GCTask {
	private static final int COMMAND_ID = Protocol.SYS_GET_FREE_SPACE;

	private final IDeviceController mThat;
	private final SpaceInfoCallback mCallback;

	public GetSpaceInfoTask(IDeviceController that, SpaceInfoCallback callback) {
		mThat = that;
		mCallback = callback;
	}

	@Override
	public void request(OutputStream stream) throws Exception {
		try {
			super.request(stream);

			ByteBuffer bodyBuffer = ByteBuffer.allocate(1);

			bodyBuffer.position(0);
			sendRequest(stream, COMMAND_ID, 0, bodyBuffer, true);
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
	public void response(InputStream stream, IGCService.ICommandCancel cancel) throws Exception {
		try {
			super.response(stream, cancel);

			Protocol.ResponseHeader header = new Protocol.ResponseHeader();
			ByteBuffer bodyBuffer = receiveResponse(stream, COMMAND_ID, header, cancel, true);

			byte result = bodyBuffer.get();
			if(result != Common.ErrorCode.ERR_SUCCESS.getVal()) {
				Log.w(Common.TAG, "[" + getClass().toString() + "] Operation fail error: " + Common.ErrorCode.getKey(result).toString() + "(0x" + Integer.toHexString(result) + ")");
				throw new Common.CommonException("Operation fail", Common.ErrorCode.getKey(result));
			}

			try {
				HashMap<IMediaItem.Type, Integer> remainingSpace = new HashMap<IMediaItem.Type, Integer>();
				
				int totalSize = bodyBuffer.remaining();
				int pairs = (totalSize - (8*2) /* free space + total space)*/) / (1+4); /* byte type + int remain*/
				for(int i = 0; i < pairs; i++) {
					int type = bodyBuffer.get();
					
					switch(type) {
					case Protocol.FILE_TYPE_JPG:
						remainingSpace.put(Type.Photo, bodyBuffer.getInt());
						break;
					case Protocol.FILE_TYPE_MOV:
						remainingSpace.put(Type.Video, bodyBuffer.getInt());
						break;
					case Protocol.FILE_TYPE_TIMELAPSE:
						remainingSpace.put(Type.TimeLapse, bodyBuffer.getInt());
						break;
					case Protocol.FILE_TYPE_SLOWMOTION:
						remainingSpace.put(Type.SlowMotion, bodyBuffer.getInt());
						break;
					default:
						Log.i(Common.TAG, "Comsuming unkown file type: "+type+" free space data");
						bodyBuffer.getInt();
						break;
					}
				}
				mCallback.result(mThat, remainingSpace, bodyBuffer.getLong(), bodyBuffer.getLong());
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
