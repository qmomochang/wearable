package com.htc.gc.tasks;

import java.io.InputStream;

import com.htc.gc.interfaces.Common;
import com.htc.gc.interfaces.Common.DataCallback;
import com.htc.gc.interfaces.IGCService.ICommandCancel;
import com.htc.gc.internal.IBackgroundTask;
import com.htc.gc.internal.Protocol.ResponseHeader;

public class BackgroundDownloadFragmentTask extends DownloadFragmentTask implements IBackgroundTask {
	protected ChannelType mChannelType;
	
	public BackgroundDownloadFragmentTask(DataCallback callback, int command) {
		super(callback, command);
	}
	
	@Override
	public void response(InputStream stream, ICommandCancel cancel) throws Exception {
		throw new Common.NoImpException();
	}
	
	@Override
	public void response(ResponseHeader header, InputStream stream, ICommandCancel cancel) throws Exception {
		try {
			super.responseInternal(stream, cancel);
			processResponse(header, stream, cancel);			
		} catch(Common.CommonException e) {
			mCallback.error(e);
		}
		catch(Exception e) {
			mCallback.error(e);
			throw e;
		}
	}
	
	@Override
	public ChannelType getChannelType() {
		return mChannelType;
	}
}
