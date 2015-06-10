package com.htc.gc.tasks;

import java.io.InputStream;
import java.nio.ByteBuffer;

import android.util.Log;

import com.htc.gc.interfaces.Common;
import com.htc.gc.interfaces.Common.DataCallback;
import com.htc.gc.interfaces.IGCService.ICommandCancel;
import com.htc.gc.internal.CancelableTask;
import com.htc.gc.internal.Protocol;

public class DownloadFragmentTask extends CancelableTask {
	protected static final boolean DEBUG = Common.DEBUG;

	protected final DataCallback mCallback;

	public DownloadFragmentTask(DataCallback callback, int command) {
		super(command);
		
		mCallback = callback;
	}

	protected void processResponse(Protocol.ResponseHeader header, InputStream stream, ICommandCancel cancel) throws Exception {
		long begin = System.currentTimeMillis();
		if(DEBUG) Log.i(Common.TAG, "[" + getClass().toString() + "] Receiving data start");
		ByteBuffer bodyBuffer = receiveResponseBody(header, stream, mCommand, cancel, true);
		if(DEBUG) Log.i(Common.TAG, "[" + getClass().toString() + "] Receiving data done");
		
		// no fragment case
		if(header.mFlag == Protocol.Flag_No_Fragment) {
			byte result = bodyBuffer.get();
			if(result != Common.ErrorCode.ERR_SUCCESS.getVal()) {
				Log.w(Common.TAG, "[" + getClass().toString() + "] Operation fail error: " + Common.ErrorCode.getKey(result).toString() + "(0x" + Integer.toHexString(result) + ")");
				throw new Common.CommonException("Operation fail", Common.ErrorCode.getKey(result));
			}

			int length = bodyBuffer.remaining();

			try {
				long callbackStartTime = System.currentTimeMillis();
				mCallback.data(bodyBuffer);
				if(DEBUG) Log.i(Common.TAG, "[" + getClass().toString() + "] download data callback process spend: "+(System.currentTimeMillis() - callbackStartTime)+"ms");
			}
			catch(Exception e) {
				e.printStackTrace();
				long callbackStartTime = System.currentTimeMillis();
				mCallback.error(e);
				if(DEBUG) Log.i(Common.TAG, "[" + getClass().toString() + "] download error callback process spend: "+(System.currentTimeMillis() - callbackStartTime)+"ms");
			}

			long spend = System.currentTimeMillis() - begin;
			if(DEBUG) Log.d(Common.TAG, "[DownloadFragmentTask] download complete, spend: " + spend + "ms, bandwidth: " + (((float)length) / spend) + "KB/s");

			try {
				long callbackStartTime = System.currentTimeMillis();
				mCallback.end();
				if(DEBUG) Log.i(Common.TAG, "[" + getClass().toString() + "] download end callback process spend: "+(System.currentTimeMillis() - callbackStartTime)+"ms");
			}
			catch(Exception e) {
				e.printStackTrace();
				long callbackStartTime = System.currentTimeMillis();
				mCallback.error(e);
				if(DEBUG) Log.i(Common.TAG, "[" + getClass().toString() + "] download error callback process spend: "+(System.currentTimeMillis() - callbackStartTime)+"ms");
			}

		}
		// has fragment case
		else {
			int offset = bodyBuffer.getInt();
			int length = bodyBuffer.getInt();
			int size = 0;

			byte result = bodyBuffer.get();
			if(result != Common.ErrorCode.ERR_SUCCESS.getVal()) {
				Log.w(Common.TAG, "[" + getClass().toString() + "] Operation fail error: " + Common.ErrorCode.getKey(result).toString() + "(0x" + Integer.toHexString(result) + ")");
				throw new Common.CommonException("Operation fail", Common.ErrorCode.getKey(result));
			}

			if(offset != 0) throw new Common.CommonException("Fragment offset error", Common.ErrorCode.getKey(result));
			if(bodyBuffer.remaining() != length - 1) throw new Common.CommonException("Fragment length fail", Common.ErrorCode.getKey(result));

			try {
				long callbackStartTime = System.currentTimeMillis();
				mCallback.data(bodyBuffer);
				if(DEBUG) Log.i(Common.TAG, "[" + getClass().toString() + "] download data callback process spend: "+(System.currentTimeMillis() - callbackStartTime)+"ms");
			}
			catch(Exception e) {
				e.printStackTrace();
				long callbackStartTime = System.currentTimeMillis();
				mCallback.error(e);
				if(DEBUG) Log.i(Common.TAG, "[" + getClass().toString() + "] download error callback process spend: "+(System.currentTimeMillis() - callbackStartTime)+"ms");
			}

			size += length;

			boolean canceled = false;
			while((header.mFlag & Protocol.Flag_More_Fragment) == Protocol.Flag_More_Fragment) {
				if(canceled == false && isCancel()) {
					Log.i(Common.TAG, "[" + getClass().toString() + "] cancel task (" + getSequenceID() + ")");

					cancel.requestCancel(createCancelTask());						
					canceled = true;
				}

				if(DEBUG) Log.i(Common.TAG, "[" + getClass().toString() + "] Receiving data start");
				header = new Protocol.ResponseHeader();
				bodyBuffer = receiveResponse(stream, mCommand, header, cancel, true);
				if(DEBUG) Log.i(Common.TAG, "[" + getClass().toString() + "] Receiving data done");

				offset = bodyBuffer.getInt();
				length = bodyBuffer.getInt();

				if(offset != size) throw new Common.CommonException("Fragment offset error", Common.ErrorCode.getKey(result));
				if(bodyBuffer.remaining() != length) throw new Common.CommonException("Fragment length fail", Common.ErrorCode.getKey(result));

				if((header.mFlag & Protocol.Flag_Cancel_Fragment) == Protocol.Flag_Cancel_Fragment) {
					if(canceled == false) {
						Log.i(Common.TAG, "[" + getClass().toString() + "] cancel download by GC");
						canceled = true;
						
						Log.i(Common.TAG, "[" + getClass().toString() + "] cancel fragment");
						break;
					}
				} else {
					if(canceled) {
						Log.i(Common.TAG, "[" + getClass().toString() + "] keep droping fragment because this command has been canceled.");
					} else {
						try {
							long callbackStartTime = System.currentTimeMillis();
							mCallback.data(bodyBuffer);
							if(DEBUG) Log.i(Common.TAG, "[" + getClass().toString() + "] download data callback process spend: "+(System.currentTimeMillis() - callbackStartTime)+"ms");
						}
						catch(Exception e) {
							e.printStackTrace();
							long callbackStartTime = System.currentTimeMillis();
							mCallback.error(e);
							if(DEBUG) Log.i(Common.TAG, "[" + getClass().toString() + "] download error callback process spend: "+(System.currentTimeMillis() - callbackStartTime)+"ms");
						}						
					}
				}
				
				size += length;
			}

			if(canceled == true) {
				long callbackStartTime = System.currentTimeMillis();
				mCallback.cancel(); // call cancel callback
				if(DEBUG) Log.i(Common.TAG, "[" + getClass().toString() + "] download cancel callback process spend: "+(System.currentTimeMillis() - callbackStartTime)+"ms");
			}
			else {
				long callbackStartTime = System.currentTimeMillis();
				mCallback.end();
				if(DEBUG) Log.i(Common.TAG, "[" + getClass().toString() + "] download end callback process spend: "+(System.currentTimeMillis() - callbackStartTime)+"ms");
			}
				
			long spend = System.currentTimeMillis() - begin;
			if(DEBUG) Log.d(Common.TAG, "[" + getClass().toString() + "] download complete, spend: " + spend + "ms, size: " + size + "Byte, bandwidth: " + (((float)size) / spend) + "KB/s");
		}
	}
	
	@Override
	public void response(InputStream stream, ICommandCancel cancel) throws Exception {
		try {
			super.response(stream, cancel);

			Protocol.ResponseHeader header = new Protocol.ResponseHeader();
			receiveAndWriteResponseHeader(stream, mCommand, header, cancel, true);
			processResponse(header, stream, cancel);
		}
		catch(Common.CommonException e) {
			mCallback.error(e);
		}
		catch(Exception e) {
			mCallback.error(e);
			throw e;
		}
	}
	
	public void responseInternal(InputStream stream, ICommandCancel cancel) throws Exception {
		super.response(stream, cancel);
	}
	
	@Override
	public void error(Exception e) {
		mCallback.error(e);
	}
}
