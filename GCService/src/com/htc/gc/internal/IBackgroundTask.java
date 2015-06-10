package com.htc.gc.internal;

import java.io.InputStream;

import com.htc.gc.interfaces.IGCService.ICommandCancel;
import com.htc.gc.internal.Protocol.ResponseHeader;

public interface IBackgroundTask {
	enum ChannelType {
		FILE_CHANNEL,
		THUMBNAIL_CHANNEL
	}
	
	public ChannelType getChannelType();
	public void response(ResponseHeader header, InputStream stream, ICommandCancel cancel) throws Exception; 
}
