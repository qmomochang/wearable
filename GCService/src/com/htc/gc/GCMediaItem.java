package com.htc.gc;

import java.util.Date;

import android.os.Parcel;
import android.os.Parcelable;

import com.htc.gc.interfaces.Common.NoImpException;
import com.htc.gc.interfaces.IMediaItem;

public class GCMediaItem implements IMediaItem, Comparable<GCMediaItem> {

    public static final Parcelable.Creator<GCMediaItem> CREATOR = new Parcelable.Creator<GCMediaItem>() {
        public GCMediaItem createFromParcel(Parcel in) {
            return new GCMediaItem(in);
        }

        public GCMediaItem[] newArray(int size) {
            return new GCMediaItem[size];
        }
    };

    private final int mVolumnID;
	private final int mHandle;
	private Date mCreationDate;
	private Type mType = null;
	
	// Detail data
	private Long mSize = null;
	private Long mLength = null;
	private Long mFrameCount = null;
	private Long mTotalFrameSize = null;
	private String mFileName = null;
	private String mPath = null;
	private WideAngle mWideAngle = null;

    // Parcelling part
    public GCMediaItem(Parcel in){
    	mVolumnID = in.readInt();
        mHandle = in.readInt();
        long date = in.readLong();

        if(date != -1) mCreationDate = new Date(date);
        else mCreationDate = null;

        String type = in.readString();
        if(type == null || type.equals("null")) mType = null; //TODO check why it would be null in gc2
        else mType = Type.valueOf(type);

        mSize = in.readLong();
        if(mSize == -1) mSize = null;

        mLength = in.readLong();
        if(mLength == -1) mLength = null;
        
        mFrameCount = in.readLong();
        if(mFrameCount == -1) mFrameCount = null;
        
        mTotalFrameSize = in.readLong();
        if(mTotalFrameSize == -1) mTotalFrameSize = null;
        
        mFileName = in.readString();
        if(mFileName != null && mFileName.equals("null")) mFileName = null; //TODO check why it would be null in gc2
        
        mPath = in.readString();
        if(mPath == null || mPath.equals("null")) mPath = null; //TODO check why it would be null in gc2
        
        int wideAngle = in.readInt();
        if(wideAngle == -1) {
        	mWideAngle = null;
        } else {
        	try {
				mWideAngle = WideAngle.getKey((byte)wideAngle);
			} catch (NoImpException e) {
				mWideAngle = null;
				e.printStackTrace();
			}
        }
    }

	@Override
	public int describeContents() {
		return 0;
	}

    @Override
    public void writeToParcel(Parcel dest, int flags) {
    	dest.writeInt(mVolumnID);
        dest.writeInt(mHandle);
        dest.writeLong(mCreationDate == null? -1: mCreationDate.getTime());
        dest.writeString(mType == null? "null": mType.toString());
        dest.writeLong(mSize == null? -1 : mSize);
        dest.writeLong(mLength == null? -1 : mLength);
        dest.writeLong(mFrameCount == null? -1 : mFrameCount);
        dest.writeLong(mTotalFrameSize == null? -1 : mTotalFrameSize);
        dest.writeString(mFileName == null? "null": mFileName.toString());
        dest.writeString(mPath == null? "null": mPath.toString());
        dest.writeInt(mWideAngle == null? -1: mWideAngle.getVal());        
    }

	public GCMediaItem(int volumnID, int handle) {
		mVolumnID = volumnID;
		mHandle = handle;
	}

	@Override
	public int compareTo(GCMediaItem another) {
		return another.mCreationDate.compareTo(mCreationDate);
	}

	@Override
	public int getHandle() {
		return mHandle;
	}

	@Override
	public String getFileName() {
		return mFileName;
	}
	
	public void setFileName(String name) {
		mFileName = name;
	}
	
	@Override
	public String getPath() {
		return mPath;
	}
	
	public void setPath(String path) {
		mPath = path;
	}

	@Override
	public String getUniqueKey() {
		return Long.toHexString(mVolumnID) + "-" + Long.toHexString(mHandle) + "-" + (mCreationDate != null? Long.toHexString(mCreationDate.getTime()): "0");
	}

	@Override
	public Date getCreateDate() {
		return mCreationDate;
	}

	public void setCreateDate(Date date) {
		mCreationDate = date;
	}

	@Override
	public Type getType() {
		return mType;
	}

	public void setType(Type type) {
		mType = type;
	}

	@Override
	public Long getSize() {
		return mSize;
	}

	public void setSize(long size) {
		mSize = size;
	}

	@Override
	public Long getLength() {
		return mLength;
	}

	public void setLength(long length) {
		mLength = length;
	}
	
	@Override
	public Long getFrameCount() {
		return mFrameCount;
	}
	
	public void setFrameCount(long count) {
		mFrameCount = count;
	}
	
	@Override
	public Long getTotalFrameSize() {
		return mTotalFrameSize;
	}
	
	public void setTotalFrameSize(long size) {
		mTotalFrameSize = size;
	}

	public void setWideAngle(WideAngle wideAngle) {
		mWideAngle = wideAngle;
	}
	
	@Override
	public WideAngle getWideAngle() {
		return mWideAngle;
	}
	
	@Override
	public boolean hasDetail() {
		if(mSize == null
		|| mLength == null
		|| mFrameCount == null
		|| mTotalFrameSize == null
		|| mPath == null
		|| mWideAngle == null) {
			return false;
		} else {
			return true;
		}
	}

}
