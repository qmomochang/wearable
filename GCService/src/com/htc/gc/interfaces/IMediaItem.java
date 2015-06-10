package com.htc.gc.interfaces;

import java.util.Date;

import android.os.Parcelable;

import com.htc.gc.internal.Protocol;

public interface IMediaItem extends Parcelable {

	public enum Type {
		None 		(0xFF),
		Photo 		(Protocol.FILE_TYPE_JPG),
		Broadcast	(Protocol.FILE_TYPE_LIVE_BROADCASTING),
		Video 		(Protocol.FILE_TYPE_MOV),
		TimeLapse 	(Protocol.FILE_TYPE_TIMELAPSE),
		SlowMotion 	(Protocol.FILE_TYPE_SLOWMOTION);
		
		private final int mVal;
		Type(int val) { mVal = val; }
		public int getVal() { return mVal; } 
		public static Type getKey(int val) throws Common.NoImpException {
			for(Type res : Type.values()) {
				if(res.getVal() == val) {
					return res;
				}
			}
			
			//WORKAROUND
			//the video file type in gc2 is mp4, not mov
			if (val == Protocol.FILE_TYPE_MP4) {
				return Video;
			}
			
			throw new Common.NoImpException();
		}
	}
	
	public enum WideAngle {
		WIDE_ANGLE_OFF 		((byte) 0x0),
		WIDE_ANGLE_ON 		((byte) 0x1);
		
		private final byte mVal;
		WideAngle(byte val) { mVal = val; }
		public byte getVal() { return mVal; } 
		public static WideAngle getKey(byte val) throws Common.NoImpException {
			for(WideAngle res : WideAngle.values()) {
				if(res.getVal() == val) {
					return res;
				}
			}
			throw new Common.NoImpException();
		}
	}

	public class Location {
		double mLongitude;
		double mLatitude;
	}

	public int getHandle();
	public String getPath();
	public String getFileName();
	public String getUniqueKey();

	public Date getCreateDate();
	public Type getType();

	public Long getSize();
	public Long getLength();
	public Long getFrameCount();
	public Long getTotalFrameSize();
	public WideAngle getWideAngle();
	
	public boolean hasDetail();
}