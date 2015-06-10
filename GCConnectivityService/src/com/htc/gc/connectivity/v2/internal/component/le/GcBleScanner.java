package com.htc.gc.connectivity.v2.internal.component.le;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.util.Log;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import com.htc.gc.connectivity.interfaces.IGcConnectivityDevice.GcStateBle;
import com.htc.gc.connectivity.interfaces.IGcConnectivityScanner.ScanState;
import com.htc.gc.connectivity.internal.common.GcConnectivityDevice;
import com.htc.gc.connectivity.internal.common.GcConnectivityDeviceGroup;



public class GcBleScanner {

	private final static String TAG = "GcBleScanner";

    private Context mContext;
    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private LinkedList<GcBleScannerListener> mListeners = new LinkedList<GcBleScannerListener>();

    private GcConnectivityDeviceGroup mGcConnectivityDeviceGroup;
    private ScanState mScanState = ScanState.SCAN_STATE_NONE;
    
    
    
    public GcBleScanner(Context context, BluetoothManager bluetoothManager) throws Exception {
    
    	mContext = context;
    	mBluetoothManager = bluetoothManager;

    	mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
        	
        	throw new Exception("Unable to obtain a BluetoothAdapter.");
        }
        
        mGcConnectivityDeviceGroup = GcConnectivityDeviceGroup.getInstance();
        setScanState(ScanState.SCAN_STATE_STANDBY);
    }
    
    
    
    private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {

        @Override
        public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {

        	if (getGcConnectivityDeviceGroup().addDevice(device, scanRecord)) {
        		
        		GcConnectivityDevice gcDevice = getGcConnectivityDeviceGroup().getDevice(device);

        		Log.d(TAG, "[MGCC] addDevice OK: " + device.getAddress());
        		
        		final LinkedList<GcBleScannerListener> listeners;
        		synchronized(mListeners){

        			listeners = new LinkedList<GcBleScannerListener>(mListeners);
        		}
        		
                for (GcBleScannerListener listener : listeners) {
                	
                	listener.onScanHit(device, gcDevice.getVersion());
                }
        	}
        }
    };
    
    
    
    public Context getContext() {
    	
    	return mContext;
    }
    
    
    
    public synchronized void registerListener(GcBleScannerListener listener) {
    	
    	synchronized(mListeners) {

    		mListeners.add(listener);
    		
    		Log.d(TAG, "[MGCC] After registerListener mListeners.size() = " + mListeners.size());
    	}
    }

    
    
    public synchronized void unregisterListener(GcBleScannerListener listener) {

    	synchronized(mListeners) {

    		mListeners.remove(listener);

    		Log.d(TAG, "[MGCC] After unregisterListener mListeners.size() = " + mListeners.size());
    	}
    }
    
    
    
    public GcConnectivityDeviceGroup getGcConnectivityDeviceGroup() {
    	
    	return mGcConnectivityDeviceGroup;
    }
    
    
    
    synchronized public ScanState getScanState() {
    	
    	return mScanState;
    }

    
    
    synchronized private void setScanState(ScanState state) {
    	
    	mScanState = state;
    }
    
    
    
    private void updateGcConnectivityGroup() {
    	
    	if (mBluetoothManager != null) {
    		
    		List<BluetoothDevice> connectedDeviceList = mBluetoothManager.getConnectedDevices(BluetoothProfile.GATT);
    		
    		ArrayList<GcConnectivityDevice> gcDeviceList = new ArrayList<GcConnectivityDevice>();
    		gcDeviceList.addAll(mGcConnectivityDeviceGroup.getDeviceList());
    		
    		for (int cnt = 0; cnt < gcDeviceList.size(); cnt++) {
    			
    			GcConnectivityDevice gcDevice = gcDeviceList.get(cnt);
    			
    			if (!connectedDeviceList.contains(gcDevice.getBluetoothDevice())) {
    				
    				mGcConnectivityDeviceGroup.removeDevice(gcDevice);

    			} else {
    				
    				if (gcDevice.getGcStateBle() != GcStateBle.GCSTATE_BLE_CONNECTED) {
    					
    					Log.d(TAG, "[MGCC] GC BLE state is " + gcDevice.getGcStateBle() + ", which is not at GCSTATE_BLE_CONNECTED before scanning");
    					gcDevice.setGcStateBle(GcStateBle.GCSTATE_BLE_CONNECTED);
    				}
    			}
    		}

    	} else {
    		
    		Log.d(TAG, "[MGCC] updateGcConnectivityGroup. mBluetoothManager is null.");
    	}
    }
    
    
    
	public boolean scanStart() {

		boolean ret = false;
		
        Log.d(TAG, "[MGCC] scanStart++");
        Log.d(TAG, "[MGCC] scanStart getScanState() = " + getScanState());

    	if (mBluetoothAdapter == null) {

    		Log.d(TAG, "[MGCC] BluetoothAdapter not initialized.");
            return false;
            
        } else {
        	
        	if (!mBluetoothAdapter.isEnabled()) {
        		
        		Log.d(TAG, "[MGCC] Bluetooth is unavailable and please enable it.");
        		return false;
        	}
        }
        
		if (getScanState() == ScanState.SCAN_STATE_STANDBY) {
			
			setScanState(ScanState.SCAN_STATE_SCANNING);
			
			updateGcConnectivityGroup();
	        
	        /// Add exist and connected device.
	        for (int cnt = 0; cnt < getGcConnectivityDeviceGroup().getCount(); cnt++) {
	        	
	        	GcConnectivityDevice gcDevice = getGcConnectivityDeviceGroup().getDeviceList().get(cnt);
        	
	        	Log.d(TAG, "[MGCC] add exist and connected device OK: " + gcDevice.getAddress());
	        	
        		final LinkedList<GcBleScannerListener> listeners;
        		synchronized(mListeners){

        			listeners = new LinkedList<GcBleScannerListener>(mListeners);
        		}
        		
                for (GcBleScannerListener listener : listeners) {
                	
                	listener.onScanHitConnected(gcDevice.getBluetoothDevice(), gcDevice.getVersion());
                }
	        }
	        
	        if (mBluetoothAdapter.startLeScan(mLeScanCallback)) {
	        	
	        	ret = true;
	        	
	        } else {
	        	
	        	setScanState(ScanState.SCAN_STATE_STANDBY);
	        }

		} else {
			
			Log.d(TAG, "[MGCC] The scan state is not correct for scanStart(). getScanState = " + getScanState());
		}

        Log.d(TAG, "[MGCC] scanStart--");
        
        return ret;
	}

	
	
	public boolean scanStop() {
		
		boolean ret = false;
		
        Log.d(TAG, "[MGCC] scanStop++");
        Log.d(TAG, "[MGCC] scanStop getScanState() = " + getScanState());

    	if (mBluetoothAdapter == null) {

    		Log.d(TAG, "[MGCC] BluetoothAdapter not initialized.");
            return false;
            
        } else {
        	
        	if (!mBluetoothAdapter.isEnabled()) {
        		
        		Log.d(TAG, "[MGCC] Bluetooth is unavailable and please enable it.");
        		return false;
        	}
        }
        
		if (getScanState() == ScanState.SCAN_STATE_SCANNING) {

	        mBluetoothAdapter.stopLeScan(mLeScanCallback);

	        setScanState(ScanState.SCAN_STATE_STANDBY);

	        ret = true;
	        
		} else {
			
			Log.d(TAG, "[MGCC] The scan state is not correct for scanStop(). getScanState = " + getScanState());
		}
			
		Log.d(TAG, "[MGCC] scanStop--");
        
		return ret;
	}
	
}
