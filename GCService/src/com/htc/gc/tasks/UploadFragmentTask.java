package com.htc.gc.tasks;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import android.util.Log;

import com.htc.gc.interfaces.Common;
import com.htc.gc.interfaces.Common.ICancelable;
import com.htc.gc.interfaces.Common.OperationCallback;
import com.htc.gc.interfaces.Common.UploadCallback;
import com.htc.gc.interfaces.IDeviceController;
import com.htc.gc.interfaces.IGCService.ICommandCancel;
import com.htc.gc.internal.GCTask;
import com.htc.gc.internal.Protocol;

public class UploadFragmentTask extends GCTask implements ICancelable {
	private static final int COMMAND_ID = Protocol.SYS_UPLOAD_FILE;
	
	private static final int UPLOAD_FRAGMENT_SIZE = 1048576; // 1MBytes
	private static final int FRAGMENT_HEADER_SIZE = 8;  /* Fragment Offset 4Bytes + Fragment Length 4Bytes */
	
	private final IDeviceController mThat;
	private final UploadCallback mCallback;
	private final URI mSrcFileURI;
	private final String mDestPath;
	
	private volatile boolean mIsCanceled = false;
	
	public UploadFragmentTask(IDeviceController that, URI srcFileURI, String destPath, UploadCallback callback) {
		mThat = that;
		mCallback = callback;
		
		mSrcFileURI = srcFileURI;
		mDestPath = destPath;
	}
	
	@Override
	public void request(OutputStream stream) throws Exception {
		long begin = System.currentTimeMillis();
		try {
			super.request(stream);
			
			File srcFile = new File(mSrcFileURI);
			String dstAbsolutePath = mDestPath + srcFile.getName() + "\0";
			final long srcFileLength = srcFile.length();
			long srcFileRemain = srcFileLength;
			long bytesSent = 0;
			boolean singleFragment = (srcFileRemain > UPLOAD_FRAGMENT_SIZE) ? false : true;
			FileInputStream srcInputStream = null;
			boolean firstLoop = true;
			try {
				srcInputStream = new FileInputStream(srcFile);
				
				while(srcFileRemain > 0) {
					int dataToSendSize = (srcFileRemain < UPLOAD_FRAGMENT_SIZE) ? (int)srcFileRemain : UPLOAD_FRAGMENT_SIZE;
					int fragmentPayloadSize = dataToSendSize;
					if(firstLoop) {
						fragmentPayloadSize += dstAbsolutePath.getBytes().length;
					}
					
					int bodyBufferSize = fragmentPayloadSize;
					if(singleFragment == false) {
						bodyBufferSize += FRAGMENT_HEADER_SIZE;
					}
					ByteBuffer bodyBuffer = ByteBuffer.allocate(bodyBufferSize);
					bodyBuffer.order(ByteOrder.LITTLE_ENDIAN);
					
					if(singleFragment == false) {
						bodyBuffer.putInt((int)bytesSent); /* Fragment Offset */
						bodyBuffer.putInt(fragmentPayloadSize); /* Fragment Length */
					}
					
					if(firstLoop) {
						bodyBuffer.put(dstAbsolutePath.getBytes("UTF-8")); /* write path */	
					}
					
					byte srcFileBuffer[] = new byte[dataToSendSize]; /* write data */
					srcInputStream.read(srcFileBuffer);
					bodyBuffer.put(srcFileBuffer);
					bodyBuffer.position(0);
					
					srcFileRemain -= dataToSendSize;
					bytesSent += fragmentPayloadSize;
					
					if(singleFragment == true) {
						sendRequest(stream, COMMAND_ID, Protocol.Flag_No_Fragment, bodyBuffer, true);
						mCallback.progress(bytesSent, srcFileLength);
					} else { // fragment case
						if(mIsCanceled == true) { // cancel case
							final int flag = Protocol.Flag_Cancel_Fragment | Protocol.Flag_Last_Fragment;
							sendRequest(stream, COMMAND_ID, flag, bodyBuffer, true);
							mCallback.progress(srcFileLength - srcFileRemain, srcFileLength);
							break;
						} else { // non cancel case
							if(srcFileRemain > 0) { // has more fragment
								sendRequest(stream, COMMAND_ID, Protocol.Flag_More_Fragment, bodyBuffer, true);
								mCallback.progress(srcFileLength - srcFileRemain, srcFileLength);
							} else { // last fragment
								sendRequest(stream, COMMAND_ID, Protocol.Flag_Last_Fragment, bodyBuffer, true);
								mCallback.progress(srcFileLength - srcFileRemain, srcFileLength);
							}						
						}
					}
					
					firstLoop = false;
				}
				
				long spend = System.currentTimeMillis() - begin;
				if(DEBUG) Log.d(Common.TAG, "[" + getClass().toString() + "] upload complete, spend: " + spend + "ms, size: " + srcFileLength + "Byte, bandwidth: " + (((float)srcFileLength) / spend) + "KB/s");
			} finally {
				if(srcInputStream != null) {
					srcInputStream.close();
				}
			}			
		} catch(Common.CommonException e) {
			mCallback.error(e);
		} catch(Exception e) {
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

			try {
				if(mIsCanceled) {
					mCallback.cancel();
				} else {
					mCallback.end();
				}
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
	public void cancel(OperationCallback callback) {
		Log.i(Common.TAG, "[UploadFragmentTask] do cancel (" + getSequenceID() + ")");
		
		mIsCanceled = true;
		callback.done(mThat);
	}

	@Override
	public void error(Exception e) {
		mCallback.error(e);
	}
}
