package com.htc.gc.tasks;

import com.htc.gc.interfaces.Common.OperationCallback;
import com.htc.gc.interfaces.IItemPlayer;
import com.htc.gc.internal.Protocol;
import com.htc.gc.internal.SimpleTask;

public class ItemPlaybackPlayTask extends SimpleTask {
	private static final int COMMAND_ID = Protocol.VIDEO_PLAY_RESUME;

	public ItemPlaybackPlayTask(IItemPlayer that, OperationCallback callback) {
		super(that, COMMAND_ID, callback);
	}
}
