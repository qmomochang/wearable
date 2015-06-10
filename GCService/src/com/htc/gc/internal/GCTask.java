package com.htc.gc.internal;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.atomic.AtomicInteger;

import android.util.Log;

import com.htc.gc.interfaces.Common;
import com.htc.gc.interfaces.Common.CommonException;
import com.htc.gc.interfaces.Common.ErrorCode;
import com.htc.gc.interfaces.IGCService;
import com.htc.gc.interfaces.IGCService.ICommandCancel;

public abstract class GCTask implements Comparable<GCTask> {
	protected static final boolean DEBUG = Common.DEBUG;
	
	private static final AtomicInteger mIDGenerator = new AtomicInteger();
	private static int generatID() {
		return mIDGenerator.incrementAndGet();
	}

	private final int mID = generatID();

	private int mSequenceID;
	private int mPriority = 0;

    public int compareTo(GCTask that) {

    	int result = mPriority > that.mPriority ? +1 : mPriority < that.mPriority ? -1 : 0;
    	if(result == 0) result = mID > that.mID ? +1 : mID < that.mID ? -1 : 0;
    		return result;
    }

	public int getSequenceID() {
		return mSequenceID;
	}

	public void setSequenceID(int sequenceID) {
		mSequenceID = sequenceID;
	}

	public int getPriority() {
		return mPriority;
	}

	public void setPriority(int priority) {
		mPriority = priority;
	}

	protected void sendRequest(OutputStream stream, int command, int flag, ByteBuffer bodyBuffer, boolean dumpStream) throws IOException {
		int bodySize = (bodyBuffer != null? bodyBuffer.remaining(): 0);

		ByteBuffer buffer = ByteBuffer.allocate(Protocol.RequestHeader.MIN_REQUEST_LENGTH + bodySize);
		buffer.order(ByteOrder.LITTLE_ENDIAN);

		buffer.putInt(command);
		buffer.putInt(Protocol.RequestHeader.MIN_REQUEST_LENGTH + bodySize);
		buffer.putInt(mSequenceID);
		buffer.putInt(flag);

		if(bodyBuffer != null) buffer.put(bodyBuffer.array(), bodyBuffer.position(), bodyBuffer.remaining());

		if(NetworkHelper.DUMP_STREAM && dumpStream) {
			buffer.position(0);
			buffer.limit(Protocol.RequestHeader.MIN_REQUEST_LENGTH);

			Log.d(Common.TAG, "  Dump request stream, header " + buffer.remaining() + " bytes");
			NetworkHelper.dumpBuffer(buffer);

			if(buffer.capacity() > Protocol.RequestHeader.MIN_REQUEST_LENGTH) {
				buffer.position(Protocol.RequestHeader.MIN_REQUEST_LENGTH);
				buffer.limit(buffer.capacity());
				
				if(NetworkHelper.MAX_DUMP_STREAM >= buffer.remaining()) {
					Log.d(Common.TAG, "  Dump request stream, body " + buffer.remaining() + " bytes");
					NetworkHelper.dumpBuffer(buffer);
				}
				else Log.d(Common.TAG, "  Dump request stream, body " + buffer.remaining() + " bytes, size to big dump ignone");
			}

			buffer.position(0);
			buffer.limit(buffer.capacity());
		}

		stream.write(buffer.array());
		stream.flush();
	}

	protected ByteBuffer receiveResponse(InputStream stream, int command, /*OUT*/Protocol.ResponseHeader header, IGCService.ICancel cancel, boolean dumpStream) throws Exception {
		receiveAndWriteResponseHeader(stream, command, header, cancel, dumpStream);
		return receiveResponseBody(header, stream, command, cancel, dumpStream);
	}
	
	protected void receiveAndWriteResponseHeader(InputStream stream, int command, /*OUT*/Protocol.ResponseHeader header, IGCService.ICancel cancel, boolean dumpStream) throws Exception {
		ByteBuffer buffer = ByteBuffer.allocate(Protocol.ResponseHeader.MIN_RESPONSE_LENGTH);
		buffer.order(ByteOrder.LITTLE_ENDIAN);
		NetworkHelper.receive(stream, buffer, cancel);

		if(NetworkHelper.DUMP_STREAM && dumpStream) {
			Log.d(Common.TAG, "  Dump response stream, header " + buffer.remaining() + " bytes");
			NetworkHelper.dumpBuffer(buffer);
		}

		header.mResponseID = buffer.getInt();
		header.mLength = buffer.getInt();
		header.mSequenceID = buffer.getInt();
		header.mFlag = buffer.getInt();
	
		if(header.mResponseID != command) throw new Common.CommonException("Command ID does not match, expected id: " + command + " receive id:" + header.mResponseID, ErrorCode.ERR_SYSTEM_ERROR);
		if(header.mSequenceID != getSequenceID()) throw new Common.CommonException("Task ID does not match, expected id: " + getSequenceID() + " receive id:" + header.mSequenceID, ErrorCode.ERR_SYSTEM_ERROR);

		if(header.mLength > Protocol.ResponseHeader.MAX_RESPONSE_LENGTH)
			throw new CommonException("Length of event is not correct", ErrorCode.ERR_SYSTEM_ERROR);	
	}
	
	protected ByteBuffer receiveResponseBody(Protocol.ResponseHeader header, InputStream stream, int command, IGCService.ICancel cancel, boolean dumpStream) throws Exception {
		if(header.mLength > Protocol.ResponseHeader.MIN_RESPONSE_LENGTH) {
			ByteBuffer bodyBuffer = ByteBuffer.allocate(header.mLength - Protocol.ResponseHeader.MIN_RESPONSE_LENGTH);
			bodyBuffer.order(ByteOrder.LITTLE_ENDIAN);

			NetworkHelper.receive(stream, bodyBuffer, cancel);
			bodyBuffer.position(0);

			if(NetworkHelper.DUMP_STREAM && dumpStream) {

				if(NetworkHelper.MAX_DUMP_STREAM >= bodyBuffer.remaining()) {
					Log.d(Common.TAG, "  Dump response stream, body " + bodyBuffer.remaining() + " bytes");
					NetworkHelper.dumpBuffer(bodyBuffer);
				}
				else Log.d(Common.TAG, "  Dump response stream, body " + bodyBuffer.remaining() + " bytes, size to big dump ignone");
			}

			return bodyBuffer;
		}
		else return null; // has no body
	}

	public void request(OutputStream stream) throws Exception {
		if(DEBUG) Log.d(Common.TAG,"[" + getClass().toString() + "] request, ID: " + mID + ", SequenceID: " + mSequenceID + ", priority: " + mPriority);
	}

	public void response(InputStream stream, ICommandCancel cancel) throws Exception {
		if(DEBUG) Log.d(Common.TAG,"[" + getClass().toString() + "] response, ID: " + mID + ", SequenceID: " + mSequenceID + ", priority: " + mPriority);
	}

	public abstract void error(Exception e);
}
