package com.htc.gc.connectivity.v2.internal.callables;

import java.util.concurrent.Callable;

import android.util.Log;



public class GcExampleCallable implements Callable<Integer> {

	private final static String TAG = "GcExampleCallable";
	
	
	
	public GcExampleCallable() {

	}



	@Override
	public Integer call() throws Exception {

		Integer ret = 0;
		
		Thread.sleep(3000);
		
		Log.d(TAG, "[MGCC] Callable FINISHED!!");
		
		return ret;
	}
}
