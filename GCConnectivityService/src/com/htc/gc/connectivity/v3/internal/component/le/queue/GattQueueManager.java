package com.htc.gc.connectivity.v3.internal.component.le.queue;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import com.htc.gc.connectivity.v3.internal.common.BaseAlarmService;
import com.htc.gc.connectivity.v3.internal.common.IAlarmService;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.util.Log;



public class GattQueueManager {
	
	private final static String TAG = "GattQueueManager";
	
	public static final int GATT_REQUEST_DELAY_INTERVAL = 2000;
	
    private static GattQueueManager mManager = null;
    private static Context mContext = null;
    
    private Thread mProcessRequestThread;
    private final LinkedBlockingQueue<Integer> mFlushQueue = new LinkedBlockingQueue<Integer>();
    
    public static final int REQUEST_RSSI = 0;
    public static final int REQUEST_READ_CHAR = 1;
    public static final int REQUEST_WRITE_CHAR_RSP = 2;
    public static final int REQUEST_WRITE_CHAR_NORSP = 4;
    public static final int REQUEST_WRITE_DESCRIP = 8;

    
    
    public interface IGattRequest {

        public boolean processGattRequest(GattRequest request);
    }

    
    
    public class GattRequest {

    	public IGattRequest callback;
        public BluetoothDevice device;
        public UUID service;
        public UUID characteristic;
        public UUID descriptor;
        public byte[] value;
        public int request_type;
        public long delay;

        public GattRequest(BluetoothDevice dev, UUID ser, UUID ch, UUID des, byte[] data, int request_type, long delay, IGattRequest callback) {

            this.device = dev;
            this.service = ser;
            this.characteristic = ch;
            this.descriptor = des;
            this.value = data;
            this.request_type = request_type;
            this.delay = delay;
            this.callback = callback;
        }
    }

    private static GattRequest mLastProcessedRequest = null;
    private static ConcurrentLinkedQueue<GattRequest> mPendingRequest = new ConcurrentLinkedQueue<GattRequest>();
    private static Hashtable<BluetoothDevice, Long> mLastDeviceGattRequestTime = new Hashtable<BluetoothDevice, Long>();

    
    
    public static GattQueueManager getInstance() {

        if (mManager == null) {

        	mManager = new GattQueueManager();
        }

        return mManager;
    }

    
    
    public void init(Context context) {

        Log.d(TAG, "init: " + context);
        mContext = context;

        mPendingRequest.clear();
        mLastDeviceGattRequestTime.clear();
        setPendingRequestAlarm(false);
        
		try {

			if (mProcessRequestThread == null) {

				mProcessRequestThread = new Thread(mProcessRequestRunnable, "GcConnectivityProcessRequestThread");
				mProcessRequestThread.start();
			}

		} catch (Exception e) {

			Log.d(TAG, "[MGCC] init e" + e);
		}
    }

    /*protected void finalize() throws Throwable {
        Log.d(TAG, "finalize: " + mContext);
        init(null, null);
    }*/

    public boolean addPendingRequest(final GattRequest request) {

        return p_addPendingRequest(request);
    }

    
    
    private synchronized boolean p_addPendingRequest(GattRequest request) {

    	if (mContext == null || request == null) {
            Log.d(TAG, "GattQueueManager initialize fail: " + mContext + ", " + request);
            return false;
        }

        if (!hasPendingRequest(request, false)) {
            Log.d(TAG, "onPendingRequestQueue - ADD: " + request.device + ", " + request.request_type + ", "
                    + request.service + ", " + request.characteristic + ", " + request.descriptor + ", "
                    + request.value + ", " + request.delay);
            mPendingRequest.add(request);
        } else {
            Log.d(TAG, "onPendingRequestQueue - DUPLICATED: " + request.device + ", " + request.request_type + ", "
                    + request.service + ", " + request.characteristic + ", " + request.descriptor + ", "
                    + request.value + ", " + request.delay);
        }

        Log.d(TAG, "p_addPendingRequest: " + mPendingRequest.size());

        ///if (mPendingRequest.size() > 1) setPendingRequestAlarm(true);
        ///else if (mPendingRequest.size() == 1) flush();
        if (mPendingRequest.size() == 1) {

        	if (request.device != null) mLastDeviceGattRequestTime.put(request.device, (long) 0);

        	flush();
        }
        
        return true;
    }

    
    
    public void removePendingRequest(BluetoothDevice device) {

    	if (mContext == null) {
            Log.d(TAG, "GattQueueManager initialize fail: " + mContext);
            return;
        }

        //boolean ret = false;
        GattRequest request;
        if(!mPendingRequest.isEmpty()) {
            Iterator<GattRequest> it = mPendingRequest.iterator();
            while(it.hasNext()) {
                request = it.next();
                if (device.getAddress().equalsIgnoreCase(request.device.getAddress())) {
                    Log.d(TAG, "onPendingRequestQueue - REMOVE: " + request.device + ", " + request.request_type + ", " + request.service + ", " + request.characteristic + ", " + request.descriptor + ", " + request.value + ", " + request.delay);
                    it.remove();
                    // ret = true;
                }
            }
        }
    }

    
    
    public boolean hasPendingRequest(GattRequest request, boolean remove) {

        boolean ret = false;
        if (mContext == null) {
            Log.d(TAG, "GattQueueManager initialize fail: " + mContext);
            return ret;
        }

        if (!mPendingRequest.isEmpty()) {
            Iterator<GattRequest> it = mPendingRequest.iterator();
            while (it.hasNext()) {
                GattRequest pending = it.next();
                if (pending != null && pending.device == request.device && pending.request_type == request.request_type
                        && (pending.request_type == REQUEST_RSSI ? true : false)) {
                    ret = true;
                    if (remove) {
                        Log.d(TAG, "onPendingRequestQueue - REMOVE: " + request.device + ", " + request.request_type + ", "
                                + request.service + ", " + request.characteristic + ", " + request.descriptor + ", "
                                + request.value + ", " + request.delay);
                        it.remove();
                    }
                }

                /*
                if (pending != null && pending.device == request.device && pending.request_type == request.request_type
                        && (pending.request_type == REQUEST_RSSI ? true :
                                (pending.service == request.service && pending.characteristic == request.characteristic))) {
                    ret = true;
                    if (remove) {
                        Log.d(TAG, "onPendingRequestQueue - REMOVE: " + request.device + ", " + request.request_type + ", "
                                + request.service + ", " + request.characteristic + ", " + request.descriptor + ", "
                                + request.value + ", " + request.delay);
                        it.remove();
                    }
                }
                */
            }
        }
        return ret;
    }

    
    
    /*private synchronized GattRequest getLastProcessedRequest() {
        return mLastProcessedRequest;
    }*/
    public void tryPurgeLastRequest(int request_type, UUID characteristic) {

    	GattRequest lastRequest = mLastProcessedRequest;
        Log.d(TAG, "tryPurgeLastRequest: " + request_type + ", " + characteristic + ", " + lastRequest);
        if (lastRequest == null || characteristic == null) return;
        else if (lastRequest.request_type != request_type
                || lastRequest.characteristic.toString().equalsIgnoreCase(characteristic.toString())) {
            Log.d(TAG, " + : not matching lastRequest");
            return;
        } else {
            Log.d(TAG, " + : retry lastRequest");
            addPendingRequest(lastRequest);
            mLastProcessedRequest = null;
        }
    }

    
    
    public void reset(BluetoothDevice device) {

    	if (device != null) {
    		
    		mLastDeviceGattRequestTime.put(device, (long) 0);
        	removePendingRequest(device);
        	setFlushing(false);
    	}
    }

    
    
    private synchronized void addFlushQueue(Integer value) {
    	
    	if (value != null) {
    		
    		mFlushQueue.add(value);
    	}
    }
    
    
    
    private static boolean bFlushing = false;

    private synchronized void setFlushing(boolean value) {
    	
    	bFlushing = value;
    }

    
    
    private synchronized boolean getFlushing() {
    	
    	return bFlushing;
    }


    
    public synchronized void flush(BluetoothDevice device) {

    	if (device != null) mLastDeviceGattRequestTime.put(device, (long) 0);
        
    	setFlushing(false);
        flush();
    }

    
    
    public synchronized void flush() {

    	///Log.d(TAG, "[MGCC] flush getFlushing() = " + getFlushing());
    	
    	if (getFlushing()) {
    		return;
    	}
    	
    	addFlushQueue(7);
    }

    
    
    boolean isProcessPendingRequest = false;
    private synchronized void processPendingRequest() {

    	isProcessPendingRequest = true;
        Log.d(TAG, "processPendingRequest: " + mPendingRequest.size());
        if (mContext == null) {
            Log.d(TAG, " + GattQueueManager initialize fail: " + mContext);
            isProcessPendingRequest = false;
            return;
        }

        GattRequest request;

        setPendingRequestAlarm(false);

        if(!mPendingRequest.isEmpty()) {
            ArrayList<GattRequest> defers = new ArrayList<GattRequest>();
            while ((request = mPendingRequest.poll()) != null) {
                Long last = mLastDeviceGattRequestTime.get(request.device);
                Log.d(TAG, " + mLastDeviceGattRequestTime[" + request.device + "] = " + mLastDeviceGattRequestTime.get(request.device));

                if (last == null) last = Long.valueOf(0);
                if (System.currentTimeMillis() > request.delay && System.currentTimeMillis() > last) {
                    Log.d(TAG, "processPendingRequest - OK: " + request.device + ", " + request.request_type + ", "
                            + request.service + ", " + request.characteristic + ", " + request.descriptor + ", "
                            + request.value + ", " + request.delay);
                    mLastProcessedRequest = request;
                    mLastDeviceGattRequestTime.put(request.device, System.currentTimeMillis() + GATT_REQUEST_DELAY_INTERVAL - 500);
                    
                    setFlushing(true);
                    
                    request.callback.processGattRequest(request);
                    break;
                } else {
                    Log.d(TAG, "processPendingRequest - DELAY: " + request.device + ", " + request.request_type + ", "
                            + request.service + ", " + request.characteristic + ", " + request.descriptor + ", "
                            + request.value + ", " + request.delay + ", " + last);
                    defers.add(request);
                }
            }

            Log.d(TAG, " + defers: " + defers.size());
            if (!defers.isEmpty()) {
                synchronized (mPendingRequest) {
                    for (GattRequest req : defers) {
                        if (!hasPendingRequest(req, false)) {
                            Log.d(TAG, "processPendingRequest - REQUEUE: " + req.device + ", " + req.request_type + (req.request_type == REQUEST_RSSI ? "" : ", " + req.service + ", " + req.characteristic + ", " + req.descriptor + ", " + req.value + ", " + req.delay));
                            mPendingRequest.add(req);
                        } else {
                            Log.d(TAG, "processPendingRequest - DUPLICATED: " + req.device + ", " + req.request_type + (req.request_type == REQUEST_RSSI ? "" : ", " + req.service + ", " + req.characteristic + ", " + req.descriptor + ", " + req.value + ", " + req.delay));
                        }
                    }
                }
            }
            Log.d(TAG, " + mPendingRequest = " + mPendingRequest.size());
            setPendingRequestAlarm(mPendingRequest.size() > 0);

        } else {

            Log.d(TAG, "+ mPendingRequest = All cleared.");
            // setPendingRequestAlarm(false);
            setFlushing(false);
        }
        isProcessPendingRequest = false;
    }

    
    
    private BaseAlarmService alarmPendingRequest = null;

    private synchronized void setPendingRequestAlarm(boolean enable) {
        Log.d(TAG, "setPendingRequestAlarm: " + enable + ", mContext = " + mContext + ", " + alarmPendingRequest);
        if (mContext == null) return;
        if (alarmPendingRequest != null) {
            alarmPendingRequest.deinitAlarm(0xfffe);
            alarmPendingRequest = null;
        }
        if (!enable) return;
        if (!mPendingRequest.isEmpty() && mContext != null) {
            alarmPendingRequest = new BaseAlarmService("GattQueueManager", mContext);
            Log.d(TAG, " + mPendingRequest not empty. initiating Alarm: " + mPendingRequest.size());

            try {
                alarmPendingRequest.initAlarm(System.currentTimeMillis() + GATT_REQUEST_DELAY_INTERVAL, 0xfffe, new IAlarmService() {
                    @Override
                    public void onAlarm() {
                        Log.d(TAG, "[MGCC] setPendingRequestAlarm - onAlarm -> updatePendingRequest: " + mPendingRequest.size());
                        if (alarmPendingRequest != null) {
                            alarmPendingRequest.deinitAlarm(0xfffe);
                            alarmPendingRequest = null;
                        }
                        ///if (!isProcessPendingRequest) processPendingRequest();
                        if (!isProcessPendingRequest) flush();
                    }
                });
            } catch (java.lang.NullPointerException e) {
                Log.d(TAG, "setPendingRequestAlarm CONNARD: I don't know what's going on here///");
            }
        } else {
            Log.d(TAG, "setPendingRequestAlarm failed: mContext is null[" + mContext + "] or mPendingRequest is empty[" + mPendingRequest.size() + "] or pending queue already in processing[" + alarmPendingRequest + "]...");
        }
    }
    
    
    
	private final Runnable mProcessRequestRunnable = new Runnable() {

		@Override
		public void run() {

			try {

				while (mProcessRequestThread.isInterrupted() == false) {

					///Log.d(TAG, "[MGCC] Before mFlushQueue poll");
					Integer value = mFlushQueue.poll(Long.MAX_VALUE, TimeUnit.SECONDS);
					///Log.d(TAG, "[MGCC] After mFlushQueue poll, value = " + value);

					if (value > 0) {
						
						///Log.d(TAG, "[MGCC] mPendingRequest.size() = " + mPendingRequest.size());
						if (!mPendingRequest.isEmpty()) {

							GattQueueManager.getInstance().processPendingRequest();
						}
					}
				}

			} catch (Exception e) {

				Log.d(TAG, "[MGCC] mProcessRequestRunnable e = " + e);
				e.printStackTrace();
			}
		}
	};
}
