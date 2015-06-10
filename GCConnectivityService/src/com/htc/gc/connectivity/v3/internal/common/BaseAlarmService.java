package com.htc.gc.connectivity.v3.internal.common;

import java.util.Calendar;
import java.util.Hashtable;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Environment;
import android.util.Log;



public class BaseAlarmService implements IAlarmService {

	private final static String TAG = "BaseAlarmService";

	public static final String CONFIG_FILE_PATH = Environment.getExternalStorageDirectory().getAbsolutePath() + "/.data/HtcBluetoothLeProfiles.conf";
	
    private String mName = "";

    PendingIntent mReceiverPendingIntent = null;
    Context mContext = null;
    Calendar mCalendar = null;
    long mRepeat = -1;

    private static BaseAlarmService instance = null;
    private static Hashtable<Integer, IAlarmService> mReceivers = new Hashtable<Integer, IAlarmService>();

    private static AlarmReceiver mAlarmReceiver = null;
    private static final String ACTION_BASE_ALARM_SERVICE = "com.htc.intent.action.base_alarm_service";
    
    
    
    public BaseAlarmService(String name, Context ctx) {

    	instance = this;
        mName = name;
        mContext = ctx;
        
        if (mAlarmReceiver == null) {
        	
            mAlarmReceiver = new AlarmReceiver();
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(ACTION_BASE_ALARM_SERVICE);
            mContext.registerReceiver(mAlarmReceiver, intentFilter);
        }
    }

    
    
    public boolean isAlarmActive() {

        return (mReceiverPendingIntent != null);
    }

    
    
    public void setAlarm(Calendar cal, long repeat) {

        Log.i(TAG, "setAlarm: cal is" + (cal == null ? "" : " not") + " null, " + repeat);

        if (cal == null && repeat == -1) {
            return;
        }

        mCalendar = cal;
        mRepeat = repeat;
    }

    
    
    public boolean initAlarm(long next, int id, IAlarmService iface) {

    	Log.i(TAG, "initAlarm: mContext = " + mContext + ", " + next + ", " + id + ", " + iface);
        return startAlarm(next, id, iface);
    }

    

    private boolean startAlarm(long next, int id, IAlarmService iface) {

    	Log.i(TAG, "startAlarm: mContext = " + mContext + ", " + next + ", " + id + ", " + iface);
        if (mContext == null) {
            Log.d(TAG, "[WTF] startAlarm: mContext is null.");
            return false;
        }
        
        AlarmManager alarmMgr = (AlarmManager) mContext.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent();
        intent.setAction(ACTION_BASE_ALARM_SERVICE);
        intent.putExtra("receiver", id);
        intent.putExtra("oneoff", (next > 0 ? true : false));
        mReceiverPendingIntent = PendingIntent.getBroadcast(mContext, id, intent, 0);

        mReceivers.put(id, iface);

        if (next > 0) {
            alarmMgr.set(AlarmManager.RTC_WAKEUP, next, mReceiverPendingIntent);
        } else if (mRepeat > -1) {
            alarmMgr.setRepeating(AlarmManager.RTC_WAKEUP, (mCalendar != null ? mCalendar
                    .getTimeInMillis() : System.currentTimeMillis() + 1000), mRepeat,
                    mReceiverPendingIntent);
        } else if (mCalendar != null) {
            alarmMgr.set(AlarmManager.RTC_WAKEUP, mCalendar.getTimeInMillis(), mReceiverPendingIntent);
        }

        Log.i(TAG, "initAlarm OK - " + mName);

        return true;
    }

    
    
    public void deinitAlarm(int id) {

        Log.i(TAG, "deinitAlarm: " + mName);
        if (mContext == null) {
            Log.d(TAG, "[WTF] deinitAlarm: mContext is null.");
            return;
        }

        AlarmManager alarmMgr = (AlarmManager) mContext.getSystemService(Context.ALARM_SERVICE);
        alarmMgr.cancel(mReceiverPendingIntent);
        mReceivers.remove(id);
        mReceiverPendingIntent = null;
        mContext = null;
    }

    
    
    public class AlarmReceiver extends BroadcastReceiver {

    	@Override
        public void onReceive(Context context, Intent intent) {
        	
        	Log.d(TAG, "[MGCC] onReceive intent = " + intent);
        	
            if (instance == null) {
                Log.d(TAG, "[WTF] AlarmReceiver_onReceive: instance is null.");
                return;
            }
            int receiver = intent.getIntExtra("receiver", -1);
            if (receiver == -1) {
                Log.d(TAG, " + je ne comprends cet intent.");
                return;
            }
            if (BaseAlarmService.mReceivers.get(receiver) != null) {
                try {
                    Log.d(TAG, "Pushing alarm notification to receiver " + receiver);
                    BaseAlarmService.mReceivers.get(receiver).onAlarm();
                } catch (NullPointerException exNull) {
                    Log.d(TAG, "Receiver is null. Forget it.");
                }
            } else {
                Log.d(TAG, " + no receiver registered");
                return;
            }
            boolean oneoff = intent.getBooleanExtra("oneoff", true);
            if (oneoff) {
                Log.d(TAG, "This is a one off event for receiver " + receiver + ". Receiver will now be removed.");
                BaseAlarmService.mReceivers.remove(receiver);
            }
        }
    }

    
    
    @Override
    public void onAlarm() {
        Log.i(TAG, "onAlarm: dummy");
    }
}
