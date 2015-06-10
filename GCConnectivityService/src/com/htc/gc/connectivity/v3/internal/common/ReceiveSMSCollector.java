package com.htc.gc.connectivity.v3.internal.common;

import android.util.Log;


public class ReceiveSMSCollector {
    
	private final static String TAG = "ReceiveSMSCollector";

	private boolean mIsWaitingForMore;
	private String mDateTime;
	private String mPhoneNumber;
	private String mMessageContent;
	private boolean mIsComplete;
	
	public void collect(byte[] rawData) {
		try {
			byte remainedCount = rawData[0];
			String trimmedData = new String(rawData, 1, rawData.length - 1);
			Log.d(TAG, "[MGCC] collect(" + remainedCount + ")+++, is waiting for more: " + mIsWaitingForMore + ", is complete: " + mIsComplete);
			
			if (!mIsWaitingForMore) { // first part: dataTime;phoneNumber;messageContent 
				int beginIndex = 0;
				int endIndex = trimmedData.indexOf(';');
				mDateTime = trimmedData.substring(beginIndex, endIndex);
				
				beginIndex = endIndex + 1;
				endIndex = trimmedData.indexOf(';', beginIndex);
				mPhoneNumber = trimmedData.substring(beginIndex, endIndex);
				
				beginIndex = endIndex + 1;
				mMessageContent = trimmedData.substring(beginIndex);
			} else { // other parts: messageContent
				mMessageContent += trimmedData;
			}
		
			if (remainedCount == (byte)0) {
				mIsWaitingForMore = false;
				mIsComplete = true;
			} else {
				mIsWaitingForMore = true;
				mIsComplete = false;
			}
			
			Log.d(TAG, "[MGCC] collect()---, is waiting for more: " + mIsWaitingForMore + ", is complete: " + mIsComplete);
		} catch (IndexOutOfBoundsException e) {
			Log.w(TAG, "[MGCC] collect() parse components fail", e);
		} catch (Exception e) {
			Log.w(TAG, "[MGCC] collect() exception raised", e);
		}
	}
	
	public boolean isComplete() {
		return mIsComplete;
	}
	
	public String getDateTime() {
		return mDateTime;
	}
	
	public String getPhoneNumber() {
		return mPhoneNumber;
	}
	
	public String getMessageContent() {
		return mMessageContent;
	}
	
	public void reset() {
		mIsWaitingForMore = false;
		mDateTime = "";
		mPhoneNumber = "";
		mMessageContent = "";
		mIsComplete = false;
	}
}
