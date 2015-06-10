package com.htc.gc.connectivity.v3.internal.component.le;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import com.htc.gc.connectivity.interfaces.IGcConnectivityDevice.GcStateBle;
import com.htc.gc.connectivity.internal.common.GcConnectivityDevice;
import com.htc.gc.connectivity.internal.common.GcConnectivityDeviceGroup;
import com.htc.gc.connectivity.v3.internal.common.BaseAlarmService;
import com.htc.gc.connectivity.v3.internal.common.Common;
import com.htc.gc.connectivity.v3.internal.common.IAlarmService;
import com.htc.gc.connectivity.v3.internal.component.le.queue.GattQueueManager.GattRequest;
import com.htc.gc.connectivity.v3.internal.component.le.queue.GattQueueManager;
import com.htc.gc.connectivity.v3.internal.component.le.queue.GattQueueManager.IGattRequest;



public class GcBleTransceiver implements IGattRequest {

	private final static String TAG = "GcBleTransceiver";

    private Context mContext;
    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private LinkedList<GcBleTransceiverListener> mListeners = new LinkedList<GcBleTransceiverListener>();
    
    private BroadcastReceiver mBroadcastReceiver = null;
    
    GattQueueManager mGattQueueManager;
    Hashtable<BluetoothDevice, BluetoothGatt> mGatt = new Hashtable<BluetoothDevice, BluetoothGatt>();

    private GcConnectivityDeviceGroup mGcConnectivityDeviceGroup;
    
    private GCBleAPIInvoker mBleAPIInvoker;
    
    public GcBleTransceiver(Context context, BluetoothManager bluetoothManager) throws Exception {
    
    	mContext = context;
    	mBluetoothManager = bluetoothManager;

    	mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
        	
        	throw new Exception("Unable to obtain a BluetoothAdapter.");
        }
        
        mGattQueueManager = GattQueueManager.getInstance();
        mGattQueueManager.init(mContext);
        
        mGcConnectivityDeviceGroup = GcConnectivityDeviceGroup.getInstance();
        
        /// Register broadcast receiver. TODO: NO unregister now.
        if (mBroadcastReceiver == null) {

            mBroadcastReceiver = new GcBleBroadcastReceiver();
            mContext.registerReceiver(mBroadcastReceiver, makeBleServiceIntentFilter());
        }
        
        mBleAPIInvoker = new GCBleAPIInvoker(mContext, false);
    }
    
    
    
    public void deInit() {
    	if (mBroadcastReceiver != null) {
    		mContext.unregisterReceiver(mBroadcastReceiver);
    	}
    }
    
    
    
    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {

        	GcConnectivityDevice gcDevice;
        	BluetoothDevice device = gatt.getDevice();

        	Log.d(TAG, "[MGCC] onConnectionStateChange status: " + status + ", newState: " + newState);
        	
        	if (status == BluetoothGatt.GATT_SUCCESS) {
        		
            	if (newState == BluetoothProfile.STATE_CONNECTED) {

                    Log.d(TAG, "[MGCC] Connected to GATT server.");

            		gcDevice = getGcConnectivityDeviceGroup().getDevice(device);
            		
            		if ((gcDevice != null) && (gcDevice.getGcStateBle() == GcStateBle.GCSTATE_BLE_CONNECTING)) {

                		/// Add one connect count and go to service discovery if count equals 2.
            			addOneConnectCount(gcDevice, gatt);

            		} else {
            			
                		final LinkedList<GcBleTransceiverListener> listeners;
                		synchronized(mListeners){

                			listeners = new LinkedList<GcBleTransceiverListener>(mListeners);
                		}
                		
                        for (GcBleTransceiverListener listener : listeners) {
                        	
                        	listener.onError(device, null, Common.GcBleTransceiverErrorCode.ERROR_CONNECTING);
                        }
            		}

                    reset(device);
                    
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {

                	gcDevice = getGcConnectivityDeviceGroup().getDevice(device);
                	
                	if (gcDevice != null) {

                    	Log.d(TAG, "[MGCC] gcDevice.getGcStateBle() = " + gcDevice.getGcStateBle());

                		if (gcDevice.getGcStateBle() == GcStateBle.GCSTATE_BLE_CONNECTED || gcDevice.getGcStateBle() == GcStateBle.GCSTATE_BLE_CONNECTING) {

                			Log.d(TAG, "[MGCC] Disconnected from GATT server.");
                			
                            reset(device);
                            closeGattClient(device);

                    		final LinkedList<GcBleTransceiverListener> listeners;
                    		synchronized(mListeners){

                    			listeners = new LinkedList<GcBleTransceiverListener>(mListeners);
                    		}
                    		
                            for (GcBleTransceiverListener listener : listeners) {
                            	
                            	listener.onDisconnectedFromGattServer(device);
                            }

                		} else if (gcDevice.getGcStateBle() == GcStateBle.GCSTATE_BLE_DISCONNECTING) {
                			
                			Log.d(TAG, "[MGCC] Disconnected successfully.");
                			
                			reset(device);

                	        closeGattClient(device);

                			final LinkedList<GcBleTransceiverListener> listeners;
                			synchronized(mListeners){

                				listeners = new LinkedList<GcBleTransceiverListener>(mListeners);
                			}
                			
                	        for (GcBleTransceiverListener listener : listeners) {
                	        	
                	        	listener.onDisconnected(device);
                	        }
                		}
                	}
                }

        	} else {
        		
        		if (newState == BluetoothProfile.STATE_CONNECTED) {

                    reset(device);
                    removeGattClient(device);
                    closeGattClient(device);
                    
            		final LinkedList<GcBleTransceiverListener> listeners;
            		synchronized(mListeners){

            			listeners = new LinkedList<GcBleTransceiverListener>(mListeners);
            		}
            		
                    for (GcBleTransceiverListener listener : listeners) {
                    	
                    	listener.onError(device, null, Common.GcBleTransceiverErrorCode.ERROR_CONNECTING);
                    }
                    
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {

                    reset(device);
                    closeGattClient(device);
                    
            		final LinkedList<GcBleTransceiverListener> listeners;
            		synchronized(mListeners){

            			listeners = new LinkedList<GcBleTransceiverListener>(mListeners);
            		}
            		
                    for (GcBleTransceiverListener listener : listeners) {
                    	
                    	listener.onDisconnectedFromGattServer(device);
                    }
                }
        	}
        }

        
        
        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {

            Log.d(TAG, "[MGCC] onServicesDiscovered received: " + status);

        	if (status == BluetoothGatt.GATT_SUCCESS) {

        		GcConnectivityDevice gcDevice = getGcConnectivityDeviceGroup().getDevice(gatt.getDevice());
        		gcDevice.setGcStateBle(GcStateBle.GCSTATE_BLE_CONNECTED);
        		
        		final LinkedList<GcBleTransceiverListener> listeners;
        		synchronized(mListeners){

        			listeners = new LinkedList<GcBleTransceiverListener>(mListeners);
        		}
        		
                for (GcBleTransceiverListener listener : listeners) {
                	
                	listener.onConnected(gatt.getDevice());
                }

        	} else {

        		final LinkedList<GcBleTransceiverListener> listeners;
        		synchronized(mListeners){

        			listeners = new LinkedList<GcBleTransceiverListener>(mListeners);
        		}
        		
                for (GcBleTransceiverListener listener : listeners) {
                	
                	listener.onError(gatt.getDevice(), null, Common.GcBleTransceiverErrorCode.ERROR_SERVICE_DISCOVER);
                }
            }
        }

        
        
        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {

            Log.d(TAG, "[MGCC] onCharacteristicRead status: " + status + ", uuid = " + characteristic.getUuid());

            flush(gatt.getDevice());
            
        	if (status == BluetoothGatt.GATT_SUCCESS) {

                // Test code
                printCharacteristic("READ", characteristic);
        	
        		final LinkedList<GcBleTransceiverListener> listeners;
        		synchronized(mListeners){

        			listeners = new LinkedList<GcBleTransceiverListener>(mListeners);
        		}
        		
                for (GcBleTransceiverListener listener : listeners) {
                	
                	listener.onCharacteristicRead(gatt.getDevice(), characteristic);
                }
                
        	} else {
        		
        		final LinkedList<GcBleTransceiverListener> listeners;
        		synchronized(mListeners){

        			listeners = new LinkedList<GcBleTransceiverListener>(mListeners);
        		}
        		
                for (GcBleTransceiverListener listener : listeners) {
                	
                	listener.onError(gatt.getDevice(), characteristic, Common.GcBleTransceiverErrorCode.ERROR_CHARACTERISTIC_READ);
                }
        	}
        }

        
        
        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
        	
            Log.d(TAG, "[MGCC] onCharacteristicWrite status: " + status + ", uuid = " + characteristic.getUuid());
            BluetoothGattCharacteristic tmpChar = new BluetoothGattCharacteristic(characteristic.getUuid(), characteristic.getProperties(), characteristic.getPermissions());
            tmpChar.setValue(characteristic.getValue());
            tmpChar.setWriteType(characteristic.getWriteType());
        	
            flush(gatt.getDevice());
            
        	if (status == BluetoothGatt.GATT_SUCCESS) {
        		
                // Test code
                printCharacteristic("WRITE", tmpChar);
        		
        		final LinkedList<GcBleTransceiverListener> listeners;
        		synchronized(mListeners){

        			listeners = new LinkedList<GcBleTransceiverListener>(mListeners);
        		}

                for (GcBleTransceiverListener listener : listeners) {
                	
                	listener.onCharacteristicWrite(gatt.getDevice(), tmpChar);
                }
        		
        	} else {

        		final LinkedList<GcBleTransceiverListener> listeners;
        		synchronized(mListeners){

        			listeners = new LinkedList<GcBleTransceiverListener>(mListeners);
        		}

                for (GcBleTransceiverListener listener : listeners) {
                	
                	listener.onError(gatt.getDevice(), tmpChar, Common.GcBleTransceiverErrorCode.ERROR_CHARACTERISTIC_WRITE);
                }
        	}
        }
        
        
        
        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        	
            Log.d(TAG, "[MGCC] onCharacteristicChanged NOTIFICATION!!");
            
            // Test code
            printCharacteristic("NOTIFY", characteristic);

            BluetoothGattCharacteristic ltNotify = GcBleGattAttributeUtil.convertLTNotify(characteristic);
            if (ltNotify != null) {
            	
        		final LinkedList<GcBleTransceiverListener> listeners;
        		synchronized(mListeners){

        			listeners = new LinkedList<GcBleTransceiverListener>(mListeners);
        		}

                for (GcBleTransceiverListener listener : listeners) {
                	
                	listener.onNotificationReceive(gatt.getDevice(), ltNotify);
                }
            }
        }
        
        
        
        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
        	
        	Log.d(TAG, "[MGCC] onDescriptorWrite status: " + status);
        	
    		flush(gatt.getDevice());

        	if (status == BluetoothGatt.GATT_SUCCESS) {

        		final LinkedList<GcBleTransceiverListener> listeners;
        		synchronized(mListeners){

        			listeners = new LinkedList<GcBleTransceiverListener>(mListeners);
        		}
        		
                for (GcBleTransceiverListener listener : listeners) {
                	
                	listener.onDescriptorWrite(gatt.getDevice(), descriptor);
                }
        		
        	} else {

        		final LinkedList<GcBleTransceiverListener> listeners;
        		synchronized(mListeners){

        			listeners = new LinkedList<GcBleTransceiverListener>(mListeners);
        		}
        		
                for (GcBleTransceiverListener listener : listeners) {
                	
                	listener.onError(gatt.getDevice(), descriptor.getCharacteristic(), Common.GcBleTransceiverErrorCode.ERROR_DESCRIPTOR_WRITE);
                }
        	}
        }
        

        
        private void flush(BluetoothDevice device) {

        	GattQueueManager.getInstance().flush(device);
        	
        	/*
        	final BluetoothDevice temp = device;
            Timer timer = new Timer();
            timer.schedule(new TimerTask() {
                public void run() {
                    GattQueueManager.getInstance().flush(temp);
                }
            }, 0);
            */
        }
        
        
        
        private void reset(BluetoothDevice device) {

            GattQueueManager.getInstance().reset(device);
        }
    };

    
    
    public void printCharacteristic(String type, BluetoothGattCharacteristic characteristic) {
    	
		Log.d(TAG, "[MGCC][" + type + "] uuid = " + characteristic.getUuid());

		byte[] value = characteristic.getValue();
		StringBuilder str = new StringBuilder();
		for (int i = 0; i < value.length; i++) {

			str.append(String.format("%02xh ", value[i]));
		}

		Log.d(TAG, "[MGCC][" + type + "] value = " + str.toString());
    }

    
    
    public Context getContext() {
    	
    	return mContext;
    }
    
    
    
    public synchronized void registerListener(GcBleTransceiverListener listener) {
    	
    	synchronized(mListeners) {

    		mListeners.add(listener);
    		
    		Log.d(TAG, "[MGCC] After registerListener mListeners.size() = " + mListeners.size());
    	}
    }

    
    
    public synchronized void unregisterListener(GcBleTransceiverListener listener) {

    	synchronized(mListeners) {

    		mListeners.remove(listener);

    		Log.d(TAG, "[MGCC] After unregisterListener mListeners.size() = " + mListeners.size());
    	}
    }

    
    
    public GcConnectivityDeviceGroup getGcConnectivityDeviceGroup() {
    	
    	return mGcConnectivityDeviceGroup;
    }
    
    
	
	private void updateGcConnectivityGroup(BluetoothDevice device) {
		
    	if (getGcConnectivityDeviceGroup().getDevice(device) == null) {
    		
        	if (mBluetoothManager != null) {
        		
        		List<BluetoothDevice> connectedDeviceList = mBluetoothManager.getConnectedDevices(BluetoothProfile.GATT);

        		if ((connectedDeviceList != null) && (connectedDeviceList.size() > 0)) {
        			
        			for (int cnt = 0; cnt < connectedDeviceList.size(); cnt++) {
        				
        				Log.d(TAG, "[MGCC] Connected BLE device = " + connectedDeviceList.get(cnt));
        			}
        		}
        		
        		if (getGcConnectivityDeviceGroup().addDevice(device)) {
        			
        			GcConnectivityDevice gcDevice = getGcConnectivityDeviceGroup().getDevice(device);
        			
            		if (connectedDeviceList.contains(device)) {
            			
            			gcDevice.setGcStateBle(GcStateBle.GCSTATE_BLE_CONNECTED);
            		}
        		}
        		
        	} else {
        		
        		Log.d(TAG, "[MGCC] Should not happen");
        	}
    	}
	}
    
	
	
	public boolean bond(BluetoothDevice device) {

    	if (mBluetoothAdapter == null || device == null) {

    		Log.d(TAG, "[MGCC] BluetoothAdapter not initialized or unspecified device.");
            return false;
            
        } else {
        	
        	if (!mBluetoothAdapter.isEnabled()) {
        		
        		Log.d(TAG, "[MGCC] Bluetooth is unavailableand please enable it.");
        		return false;
        	}
        }

    	if ((mBluetoothAdapter != null) && (mBluetoothAdapter.getBondedDevices().contains(device))) {

    		Log.d(TAG, "[MGCC] device is already bonded.");
    		
        	GcConnectivityDevice gcDevice = getGcConnectivityDeviceGroup().getDevice(device);
        	if ((gcDevice != null) && (gcDevice.getGcStateBle() == GcStateBle.GCSTATE_BLE_CONNECTED)) {
        	
        		final LinkedList<GcBleTransceiverListener> listeners;
        		synchronized(mListeners){

        			listeners = new LinkedList<GcBleTransceiverListener>(mListeners);
        		}
        		
                for (GcBleTransceiverListener listener : listeners) {
                	
                	listener.onBonded(device);
                }

                return true;
        		
        	} else {
        		
        		Log.d(TAG, "[MGCC] BLE is not connected at bonding.");
                return false;
        	}
    		
    	} else {
    		
        	GcConnectivityDevice gcDevice = getGcConnectivityDeviceGroup().getDevice(device);
        	if ((gcDevice != null) && (gcDevice.getGcStateBle() == GcStateBle.GCSTATE_BLE_CONNECTED)) {

        		Log.d(TAG, "[MGCC] device is not bonded. Creating bond...");

        		return device.createBond();

        	} else {
        		
        		Log.d(TAG, "[MGCC] BLE is not connected at bonding.");
        		return false;
        	}
    	}
	}
	
	
    
    public boolean connect(final BluetoothDevice device, boolean auto) {

    	Log.d(TAG, "[MGCC] connect++");

    	if (mBluetoothAdapter == null || device == null) {

    		Log.d(TAG, "[MGCC] BluetoothAdapter not initialized or unspecified device.");
            return false;
            
        } else {
        	
        	if (!mBluetoothAdapter.isEnabled()) {
        		
        		Log.d(TAG, "[MGCC] Bluetooth is unavailableand please enable it.");
        		return false;
        	}
        }
    	
    	// WORKAROUND suggested by QCA
    	// gatt may be ready a little after BLE is turned on
    	// so it wait 500 milliseconds here to avoid gatt-not-ready issue
    	try {
    		Log.d(TAG, "[MGCC] wait for gatt ready");
			Thread.sleep(500);
		} catch (InterruptedException e) {
			Log.d(TAG, "[MGCC] wait for gatt ready interrupted");
		}

    	updateGcConnectivityGroup(device);

    	GcConnectivityDevice gcDevice = getGcConnectivityDeviceGroup().getDevice(device);
    	if (gcDevice.getGcStateBle() == GcStateBle.GCSTATE_BLE_DISCONNECTED) {
    		
    		gcDevice.setGcStateBle(GcStateBle.GCSTATE_BLE_CONNECTING);
    		gcDevice.setConnectCount(0);
            return setGattClient(device, auto, mGattCallback, mContext);
            
    	} else if (gcDevice.getGcStateBle() == GcStateBle.GCSTATE_BLE_CONNECTING) {

            return setGattClient(device, auto, mGattCallback, mContext);
    		
    	} else {
    		
    		Log.d(TAG, "[MGCC] Gc's BLE state is not at GCSTATE_BLE_DISCONNECTED. BLE state = " + gcDevice.getGcStateBle());
    		return false;
    	}
    }

    
    
    public boolean disconnect(final BluetoothDevice device) {

    	Log.d(TAG, "[MGCC] disconnect++");
    	
    	if (mBluetoothAdapter == null || device == null) {

    		Log.d(TAG, "[MGCC] BluetoothAdapter not initialized or unspecified device.");
            return false;

        } else {
        	
        	if (!mBluetoothAdapter.isEnabled()) {
        		
        		Log.d(TAG, "[MGCC] Bluetooth is unavailableand please enable it.");
        		
        		/// Bluetooth is disabled means that BLE is disconnected.
        		closeGattClient(device);

        		return false;
        	}
        }

    	updateGcConnectivityGroup(device);

    	GcConnectivityDevice gcDevice = getGcConnectivityDeviceGroup().getDevice(device);
    	if (gcDevice.getGcStateBle() == GcStateBle.GCSTATE_BLE_CONNECTED) {
    		
    		gcDevice.setGcStateBle(GcStateBle.GCSTATE_BLE_DISCONNECTING);
            return removeGattClient(device);
            
    	} else {
    		
    		Log.d(TAG, "[MGCC] Gc's BLE state is not at GCSTATE_BLE_CONNECTED. BLE state = " + gcDevice.getGcStateBle());
    		return false;
    	}
    }

    
    
    public void disconnectForce(final BluetoothDevice device) {

    	Log.d(TAG, "[MGCC] disconnectForce device = " + device);

    	if (mBluetoothAdapter == null || device == null) {

    		Log.d(TAG, "[MGCC] BluetoothAdapter not initialized or unspecified device.");
            return;

        } else {
        	
        	if (!mBluetoothAdapter.isEnabled()) {
        		
        		Log.d(TAG, "[MGCC] Bluetooth is unavailableand please enable it.");
        		
        		/// Bluetooth is disabled means that BLE is disconnected.
        		closeGattClient(device);

        		return;
        	}
        }

    	if (removeGattClient(device)) {

    		closeGattClient(device);
    	}
    }
    
    
    
    private synchronized boolean removeGattClient(BluetoothDevice device) {

        Log.d(TAG, "[MGCC] removeGattClient++: " + device);

        if ((device == null) || (!mGatt.containsKey(device))) {
        	return false;
        }

        mBleAPIInvoker.disconnect(mGatt.get(device));

        Log.d(TAG, "[MGCC] removeGattClient--");

        return true;
    }

    
    
    private synchronized void closeGattClient(BluetoothDevice device) {

        Log.d(TAG, "[MGCC] closeGattClient device = " + device);

        if ((device == null) || (!mGatt.containsKey(device))) {
        	return;
        }

        removeBleConnectCheckRequestAlarm();

        mGatt.get(device).close();
        mGatt.remove(device);
        
        GcConnectivityDevice gcDevice = getGcConnectivityDeviceGroup().getDevice(device);
        gcDevice.setGcStateBle(GcStateBle.GCSTATE_BLE_DISCONNECTED);
        
        Log.d(TAG, "[MGCC] closeGattClient--");
    }
    
    
    
    private synchronized BluetoothGatt getGattClient(BluetoothDevice device) {

    	Log.d(TAG, "[MGCC] getGattClient:" + device);

        if (device == null || !mGatt.containsKey(device)) {
        	return null;
        } else {
        	return mGatt.get(device);
        }
    }

    
    
    private synchronized boolean setGattClient(BluetoothDevice device, boolean auto, BluetoothGattCallback callback, Context context) {

    	Log.d(TAG, "[MGCC] addGattClient:" + device + ", mGatt.size(): " + mGatt.size() + ", autoConnect: " + auto);

        if (device == null) {
        	return false;
        }

        if (mGatt.containsKey(device)) {

           	removeGattClient(device);
           	closeGattClient(device);
        }

        BluetoothGatt gatt = mBleAPIInvoker.connectGatt(device, context, auto, callback);
        
        if (gatt != null) {

        	mGatt.put(device, gatt);

        	return true;
        	
        } else {
        	
        	return false;
        }
    }

    
    
    public int readGcCommand(BluetoothDevice device, String service, String command) {

    	if (device == null) {
    		return -1;
    	}
    	
    	GcConnectivityDevice gcDevice = getGcConnectivityDeviceGroup().getDevice(device);
    	if ((gcDevice == null) || (gcDevice != null && gcDevice.getGcStateBle() != GcStateBle.GCSTATE_BLE_CONNECTED)) {

    		Log.d(TAG, "[MGCC] Can't get gcDevice or BLE is not connected.");
    		return -1;
    	}

    	/// Check connection state from BluetoothManager directly.
    	if (mBluetoothManager != null) {
    		
    		List<BluetoothDevice> connectedDeviceList = mBluetoothManager.getConnectedDevices(BluetoothProfile.GATT);
    		
    		if (!connectedDeviceList.contains(device)) {
    			
    			Log.d(TAG, "[MGCC] Selected device is not connected.");
    			
				if ((gcDevice != null) && (gcDevice.getGcStateBle() != GcStateBle.GCSTATE_BLE_DISCONNECTED)) {
					
					Log.d(TAG, "[MGCC] Selected device is not connected and force to disconnect device = " + device);
					
					GattQueueManager.getInstance().reset(gcDevice.getBluetoothDevice());
					disconnectForce(gcDevice.getBluetoothDevice());
					
            		final LinkedList<GcBleTransceiverListener> listeners;
            		synchronized(mListeners){

            			listeners = new LinkedList<GcBleTransceiverListener>(mListeners);
            		}
            		
                    for (GcBleTransceiverListener listener : listeners) {
                    	
                    	listener.onDisconnectedFromGattServer(gcDevice.getBluetoothDevice());
                    }
				}
    			
    			return -1;
    		}
    		
    	} else {
    		
    		Log.d(TAG, "[MGCC] BluetoothManager is not available!!!");
    		return -1;
    	}
    	
    	UUID uuidService = UUID.fromString(service);
    	UUID uuidChar = UUID.fromString(command);

        Log.d(TAG, "[MGCC] readGcCommand uuidChar = " + uuidChar);

    	GattRequest request = mGattQueueManager.new GattRequest(device, 
    										  uuidService,
    										  uuidChar, 
    										  null, 
    										  null, 
    										  GattQueueManager.REQUEST_READ_CHAR, 
    										  0, 
    										  this);
    	
        boolean ret = mGattQueueManager.addPendingRequest(request);
        
        Log.d(TAG, "[MGCC] readGcCommand ret = " + ret);
        
        return 0;
    }

    
    
    public int writeGcCommand(BluetoothDevice device, GcBleGattAttributes.GcV2CommandEnum commandID, byte[] writeData, long delay) {

    	int retSize = 0;
    	
    	if (device == null) {
    		return -1;
    	}

    	GcConnectivityDevice gcDevice = getGcConnectivityDeviceGroup().getDevice(device);
    	if ((gcDevice == null) || (gcDevice != null && gcDevice.getGcStateBle() != GcStateBle.GCSTATE_BLE_CONNECTED)) {
    		
    		Log.d(TAG, "[MGCC] Can't get gcDevice or BLE is not connected.");
    		return -1;
    	}
    	
    	/// Check connection state from BluetoothManager directly.
    	if (mBluetoothManager != null) {
    		
    		List<BluetoothDevice> connectedDeviceList = mBluetoothManager.getConnectedDevices(BluetoothProfile.GATT);
    		
    		if (!connectedDeviceList.contains(device)) {
    			
    			Log.d(TAG, "[MGCC] Selected device is not connected.");
    			
				if ((gcDevice != null) && (gcDevice.getGcStateBle() != GcStateBle.GCSTATE_BLE_DISCONNECTED)) {
					
					Log.d(TAG, "[MGCC] Selected device is not connected and force to disconnect device = " + device);
					
					GattQueueManager.getInstance().reset(gcDevice.getBluetoothDevice());
					disconnectForce(gcDevice.getBluetoothDevice());
					
            		final LinkedList<GcBleTransceiverListener> listeners;
            		synchronized(mListeners){

            			listeners = new LinkedList<GcBleTransceiverListener>(mListeners);
            		}
            		
                    for (GcBleTransceiverListener listener : listeners) {
                    	
                    	listener.onDisconnectedFromGattServer(gcDevice.getBluetoothDevice());
                    }
				}
    			
    			return -1;
    		}
    		
    	} else {
    		
    		Log.d(TAG, "[MGCC] BluetoothManager is not available!!!");
    		return -1;
    	}

    	UUID uuidService = UUID.fromString(GcBleGattAttributes.getUuid(GcBleGattAttributes.GcV2CommandEnum.GC_SERVICE));
    	UUID uuidChar = UUID.fromString(GcBleGattAttributes.getUuid(commandID));

        Log.d(TAG, "[MGCC] writeGcCommand command id= " + commandID.getID() + ", length = " + writeData.length);
    	
        if (GcBleGattAttributes.isLongFormat(commandID)) {
        	
        	ArrayList<byte[]> writeDataList = getPayload(writeData, commandID.getID());
        	
        	for (int cnt = 0; cnt < writeDataList.size(); cnt++) {
        		
            	GattRequest request = mGattQueueManager.new GattRequest(device, 
  					  uuidService,
  					  uuidChar, 
  					  null, 
  					  writeDataList.get(cnt), 
  					  GattQueueManager.REQUEST_WRITE_CHAR_RSP, 
  					  delay, 
  					  this);

            	boolean ret = mGattQueueManager.addPendingRequest(request);
            	Log.d(TAG, "[MGCC] writeGcCommand ret = " + ret);
        	}
        	
        	retSize = writeDataList.size();
        	
        } else {
        	//add command ID
        	byte[] writeDataAndID = new byte[writeData.length + 1];
        	for (int i = 0; i < writeData.length; i++)
        	{
        		writeDataAndID[i+1] = writeData[i];
        	}
        	writeDataAndID[0] = commandID.getID();

        	GattRequest request = mGattQueueManager.new GattRequest(device, 
					  uuidService,
					  uuidChar, 
					  null, 
					  writeDataAndID,
					  GattQueueManager.REQUEST_WRITE_CHAR_RSP, 
					  delay, 
					  this);

        	boolean ret = mGattQueueManager.addPendingRequest(request);
            Log.d(TAG, "[MGCC] writeGcCommand ret = " + ret);
            
            retSize = 1;
        }
        
        return retSize;
    }

    
    
    private ArrayList<byte[]> getPayload(byte[] writeData, byte commandid) {
    	
    	ArrayList<byte[]> retWriteDataPayload = new ArrayList<byte[]>();
    	int data_offset = 0;

    	//The first pack is only 17 bytes payload
    	if (writeData.length <= (Common.PAYLOAD_SIZE -1))
    	{
    		data_offset = writeData.length;
    	}
    	else
    	{
    		data_offset = (Common.PAYLOAD_SIZE -1);
    	}

    	byte[] payload = new byte[data_offset + 3];
    	payload[0] = commandid;
    	payload[1] = (byte)((writeData.length >> 8) & 0xff);
    	payload[2] = (byte)(writeData.length & 0xff);
    	for (int i = 0; i < data_offset; i++)
    	{
    		payload[i+3] = writeData[i];
    	}
    	retWriteDataPayload.add(payload);

    	if (writeData.length <= (Common.PAYLOAD_SIZE -1))
    	{
    		return retWriteDataPayload;
    	}
    	
    	//It is need for more than two packs
    	int payloadSize = Common.PAYLOAD_SIZE;
    	int payloadRes = (writeData.length - data_offset) % payloadSize;
    	int payloadNum = ((writeData.length - data_offset) / payloadSize) + ((payloadRes > 0) ? 1 : 0);
    	
    	if (payloadRes == 0) {
    		payloadRes = payloadSize;
    	}

    	for (int cnt = 0; cnt < payloadNum; cnt++) {

    		int res;

    		if (cnt == (payloadNum - 1)) {
       			res = payloadRes;
    		} else {
    			res = payloadSize;
    		}
    		
    		byte[] payloads = new byte[res + 2];
    		
    		payloads[0] = commandid;
    		payloads[1] = (byte) ((cnt + 1) * 2);
    		
    		///Log.d(TAG, "[MGCC] getPayload payload[0] = " + payload[0]);
    		///Log.d(TAG, "[MGCC] getPayload payload[1] = " + payload[1]);
    		
    		for (int idx = 0; idx < res; idx++) {
    			payloads[idx + 2] = writeData[cnt * payloadSize + idx + data_offset];
    			///Log.d(TAG, "[MGCC] getPayload payload[" + (idx + 2) + "] = " + payload[idx + 2]);
    		}

    		retWriteDataPayload.add(payloads);

    	}
    	
    	return retWriteDataPayload;
    }
    
    
    
    public int setGcNotification(BluetoothDevice device, GcBleGattAttributes.GcV2CommandEnum commandID, boolean enable, long delay) {

    	if (device == null) {
    		return -1;
    	}

    	GcConnectivityDevice gcDevice = getGcConnectivityDeviceGroup().getDevice(device);
    	if ((gcDevice == null) || (gcDevice != null && gcDevice.getGcStateBle() != GcStateBle.GCSTATE_BLE_CONNECTED)) {
    		
    		Log.d(TAG, "[MGCC] Can't get gcDevice or BLE is not connected.");
    		return -1;
    	}
    	
    	/// Check connection state from BluetoothManager directly.
    	if (mBluetoothManager != null) {
    		
    		List<BluetoothDevice> connectedDeviceList = mBluetoothManager.getConnectedDevices(BluetoothProfile.GATT);
    		
    		if (!connectedDeviceList.contains(device)) {
    			
    			Log.d(TAG, "[MGCC] Selected device is not connected.");
    			
				if ((gcDevice != null) && (gcDevice.getGcStateBle() != GcStateBle.GCSTATE_BLE_DISCONNECTED)) {
					
					Log.d(TAG, "[MGCC] Selected device is not connected and force to disconnect device = " + device);
					
					GattQueueManager.getInstance().reset(gcDevice.getBluetoothDevice());
					disconnectForce(gcDevice.getBluetoothDevice());
					
            		final LinkedList<GcBleTransceiverListener> listeners;
            		synchronized(mListeners){

            			listeners = new LinkedList<GcBleTransceiverListener>(mListeners);
            		}
            		
                    for (GcBleTransceiverListener listener : listeners) {
                    	
                    	listener.onDisconnectedFromGattServer(gcDevice.getBluetoothDevice());
                    }
				}
    			
    			return -1;
    		}
    		
    	} else {
    		
    		Log.d(TAG, "[MGCC] BluetoothManager is not available!!!");
    		return -1;
    	}
    	Log.d(TAG, "[MGCC] UUID:" + commandID + " uuid(bytes):" + commandID.getID());
    	UUID uuidService = UUID.fromString(GcBleGattAttributes.getUuid(GcBleGattAttributes.GcV2CommandEnum.GC_SERVICE));
    	UUID uuidChar = UUID.fromString(GcBleGattAttributes.getUuid(commandID));
        UUID uuidDescriptor = UUID.fromString(GcBleGattAttributes.getUuid(GcBleGattAttributes.GcV2CommandEnum.GC_DESCRIPTOR));
        byte[] writeData = (enable ? BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE : BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);

        Log.d(TAG, "[MGCC] setGcNotification enable = " + enable + ", uuidChar = " + uuidChar + ", uuidDescriptor = " + uuidDescriptor);
    	
        if (setNotification(device, uuidService, uuidChar, enable)) {
        	
           	GattRequest request = mGattQueueManager.new GattRequest(device, 
						uuidService,
						uuidChar, 
						uuidDescriptor, 
						writeData,	 
						GattQueueManager.REQUEST_WRITE_DESCRIP, 
						delay, 
						this);

           	boolean ret = mGattQueueManager.addPendingRequest(request);
           	Log.d(TAG, "[MGCC] setGcNotification ret = " + ret);
        }
        
        return 0;
    }
    
    
    
    private boolean readCharacteristicIntValue(BluetoothDevice device, UUID service, UUID characteristic) {

    	///Log.d(TAG, "gattReadWrite - readCharacteristicIntValue: " + device + ", " + service + ", " + characteristic);

    	BluetoothGatt gatt = mGatt.get(device);
        if (gatt == null) {
            Log.d(TAG, " + cannot get BluetoothGatt: " + device);
            return false;
        }

        BluetoothGattService gattService = gatt.getService(service);
        if (gattService == null) {
            Log.d(TAG, " + cannot get BluetoothGattService: " + device);
            return false;
        }

        BluetoothGattCharacteristic gattChar = gattService.getCharacteristic(characteristic);
        if (gattChar == null) {
            Log.d(TAG, " + cannot get BluetoothGattCharacteristic: " + gattChar);
            return false;
        }

        return gatt.readCharacteristic(gattChar);
    }
    
    
    
    private boolean writeCharacteristic(BluetoothDevice device, byte[] value, UUID service, UUID characteristic, int writeType) {

    	///Log.i(TAG, "gattReadWrite - writeCharacteristic: " + device + ", " + service + ", " + characteristic + ", " + writeType);

    	BluetoothGatt gatt = mGatt.get(device);
        if (gatt == null) {
            Log.d(TAG, " + cannot get BluetoothGatt: " + device);
            return false;
        }

        BluetoothGattService gattService = gatt.getService(service);
        if (gattService == null) {
            Log.d(TAG, " + cannot get BluetoothGattService: " + device);
            return false;
        }

        BluetoothGattCharacteristic gattChar = gattService.getCharacteristic(characteristic);
        if (gattChar == null) {
            Log.d(TAG, " + cannot get BluetoothGattCharacteristic: " + gattChar);
            return false;
        }

        gattChar.setValue(value);
        gattChar.setWriteType(writeType);
        
        return gatt.writeCharacteristic(gattChar);
    }

    
    
    private boolean writeDescriptor(BluetoothDevice device, byte[] value, UUID service, UUID characteristic, UUID descriptor) {

    	///Log.i(TAG, "gattReadWrite - writeDescriptor: " + device + ", " + service + ", " + characteristic + ", " + descriptor);

    	BluetoothGatt gatt = mGatt.get(device);
        if (gatt == null) {
            Log.d(TAG, " + cannot get BluetoothGatt: " + device);
            return false;
        }

        BluetoothGattService gattService = gatt.getService(service);
        if (gattService == null) {
            Log.d(TAG, " + cannot get BluetoothGattService: " + device);
            return false;
        }

        BluetoothGattCharacteristic gattChar = gattService.getCharacteristic(characteristic);
        if (gattChar == null) {
            Log.d(TAG, " + cannot get BluetoothGattCharacteristic: " + gattChar);
            return false;
        }

        BluetoothGattDescriptor gattDescp = gattChar.getDescriptor(descriptor);
        if (gattDescp == null) {
            Log.d(TAG, " + cannot get BluetoothGattDescriptor: " + gattDescp);
            return false;
        }
        if (!gattDescp.setValue(value)) {
            Log.d(TAG, " + cannot set BluetoothGattDescriptor value: " + value);
            return false;
        }
        
        return gatt.writeDescriptor(gattDescp);
    }

    
    
    private boolean setNotification(BluetoothDevice device, UUID service, UUID characteristic, boolean enable) {

    	///Log.i(TAG, "[MGCC] gattReadWrite - setNotification: " + device + ", " + service + ", " + characteristic + ", " + enable);

    	BluetoothGatt gatt = mGatt.get(device);
        if (gatt == null) {
            Log.d(TAG, "[MGCC] + cannot get BluetoothGatt: " + device);
            return false;
        }

        BluetoothGattService gattService = gatt.getService(service);
        if (gattService == null) {
            Log.d(TAG, "[MGCC] + cannot get BluetoothGattService: " + device);
            return false;
        }

        BluetoothGattCharacteristic gattChar = gattService.getCharacteristic(characteristic);
        if (gattChar == null) {
            Log.d(TAG, "[MGCC] + cannot get BluetoothGattCharacteristic: " + gattChar);
            return false;
        }

        return gatt.setCharacteristicNotification(gattChar, enable);
    }
    
    
    
    public boolean processGattRequest(GattRequest request) {

    	Log.d(TAG, "processGattRequest: " + (request == null ? null : request.request_type + ", " + request.device + ", " + request.characteristic));

    	if (request == null) return false;

        boolean ret = false;
        if (request.request_type == GattQueueManager.REQUEST_WRITE_DESCRIP) {
            ret = writeDescriptor(request.device, request.value, request.service, request.characteristic, request.descriptor);
        } else if (request.request_type == GattQueueManager.REQUEST_WRITE_CHAR_RSP) {
            ret = writeCharacteristic(request.device, request.value, request.service, request.characteristic,
                    BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
        } else if (request.request_type == GattQueueManager.REQUEST_WRITE_CHAR_NORSP) {
            ret = writeCharacteristic(request.device, request.value, request.service, request.characteristic,
                    BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
        } else if (request.request_type == GattQueueManager.REQUEST_READ_CHAR) {
            ret = readCharacteristicIntValue(request.device, request.service, request.characteristic);
        }

        Log.d(TAG, "[MGCC] processGattRequest OK: " + request.device + ", " + request.request_type + ", " + request.characteristic);
        return ret;
    }
    
    
    
    public static IntentFilter makeBleServiceIntentFilter() {

        final IntentFilter intentFilter = new IntentFilter();

		///intentFilter.addAction(BluetoothDevice.ACTION_FOUND);
		intentFilter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
		///intentFilter.addAction(BluetoothDevice.ACTION_PAIRING_REQUEST);
		intentFilter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
		intentFilter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
		
		intentFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
		///intentFilter.addAction(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED);
		intentFilter.addAction(BluetoothAdapter.ACTION_REQUEST_ENABLE);
		
		///intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
		///intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
		
        return intentFilter;
    }
    
    
    
	public class GcBleBroadcastReceiver extends BroadcastReceiver {

	    public GcBleBroadcastReceiver() {

	    	super();
	    }

	    
	    
	    @Override
	    public void onReceive(final Context context, final Intent intent) {
	    	
	    	GcConnectivityDevice gcDevice;
	    	String action = intent.getAction();
	    	
	    	if (action.equals(BluetoothDevice.ACTION_BOND_STATE_CHANGED)) {
	    	
	    		BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
	    		Integer bondState = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, -1);
	    		Integer bondPrevState = intent.getIntExtra(BluetoothDevice.EXTRA_PREVIOUS_BOND_STATE, -1);
	    		
	    		Log.d(TAG, "[MGCC] onReceive bondState = " + bondState + ", bondPrevState = " + bondPrevState);
	    		
	    		if (bondState == BluetoothDevice.BOND_BONDED) {
	    			
	        		final LinkedList<GcBleTransceiverListener> listeners;
	        		synchronized(mListeners){

	        			listeners = new LinkedList<GcBleTransceiverListener>(mListeners);
	        		}
	        		
	                for (GcBleTransceiverListener listener : listeners) {
	                	
	                	listener.onBonded(device);
	                }
	    		}

	    	} else if (action.equals(BluetoothDevice.ACTION_ACL_CONNECTED)) {

	    		BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

	    		Log.d(TAG, "[MGCC] onReceive action = " + action + ", device = " + device);

        		gcDevice = getGcConnectivityDeviceGroup().getDevice(device);
        		if ((gcDevice != null) && (gcDevice.getGcStateBle() == GcStateBle.GCSTATE_BLE_CONNECTING)) {
        			
        			/// Add one connect count and go to service discovery if count equals 2.
        			addOneConnectCount(gcDevice, getGattClient(device));
        		}
	    		
	    	} else if (action.equals(BluetoothDevice.ACTION_ACL_DISCONNECTED)) {

	    		BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

	    		Log.d(TAG, "[MGCC] onReceive action = " + action + ", device = " + device);
	    		
	    	} else if (action.equals(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED)) {

	    		///BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
	    		///Integer state = intent.getIntExtra(BluetoothAdapter.EXTRA_CONNECTION_STATE, -1);
	    		///Integer prevState = intent.getIntExtra(BluetoothAdapter.EXTRA_PREVIOUS_CONNECTION_STATE, -1);

	    		///Log.d(TAG, "[MGCC] onReceive device = " + device + ", connection state = " + state + ", prev connection state = " + prevState);

	    	} else if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
	    		
	    		Integer state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1);
	    		Integer prevState = intent.getIntExtra(BluetoothAdapter.EXTRA_PREVIOUS_STATE, -1);
	    		
	    		Log.d(TAG, "[MGCC] onReceive BT state = " + state + ", BT PrevState = " + prevState);
	    		
	    		if (state == BluetoothAdapter.STATE_OFF) {
	    			
	    			ArrayList<GcConnectivityDevice> gcDeviceList = getGcConnectivityDeviceGroup().getDeviceList();
	    			
	    			for (int cnt = 0; cnt < gcDeviceList.size(); cnt++) {
	    				
	    				gcDevice = gcDeviceList.get(cnt);
	    				
	    				if ((gcDevice != null) && (gcDevice.getGcStateBle() != GcStateBle.GCSTATE_BLE_DISCONNECTED)) {
	    					
	    					Log.d(TAG, "[MGCC] Bluetooth is disabled and force to disconnect device = " + gcDevice.getBluetoothDevice());
	    					
	    					GattQueueManager.getInstance().reset(gcDevice.getBluetoothDevice());
	    					disconnectForce(gcDevice.getBluetoothDevice());
	    					
                    		final LinkedList<GcBleTransceiverListener> listeners;
                    		synchronized(mListeners){

                    			listeners = new LinkedList<GcBleTransceiverListener>(mListeners);
                    		}
                    		
                            for (GcBleTransceiverListener listener : listeners) {
                            	
                            	listener.onDisconnectedFromGattServer(gcDevice.getBluetoothDevice());
                            }
	    				}
	    			}
	    		}
	    		
	    	} else {
	    		
	    		Log.d(TAG, "[MGCC] onReceive action = " + action);
	    	}
	    }
	}
    
	
    
    synchronized private void addOneConnectCount(GcConnectivityDevice gcDevice, BluetoothGatt gatt)
    {
    	gcDevice.setConnectCount(gcDevice.getConnectCount() + 1);
    	
		Log.d(TAG, "[MGCC] gcDevice.getConnectCount() = " + gcDevice.getConnectCount());
		if (gcDevice.getConnectCount() == 2) {

			removeBleConnectCheckRequestAlarm();
			
            // Attempts to discover services after successful connection.
			if (gatt != null) {

	            Log.d(TAG, "[MGCC] Attempting to start service discovery: " + gatt.discoverServices());
			}

		} else if (gcDevice.getConnectCount() == 1) {
			
			addBleConnectCheckRequestAlarm(3000, gcDevice.getBluetoothDevice());

		} else {
			
			removeBleConnectCheckRequestAlarm();
		}
    }
    
    
    
	private static BaseAlarmService mAlarmTimeoutRequest = null;
	private static BluetoothDevice mAlarmTimeoutDevice = null;
	private synchronized void addBleConnectCheckRequestAlarm(long periodMs, BluetoothDevice device) {
    	
        Context context = mContext;
        final int id = Common.ALARM_BLE_CONNECT_CHECK;
        
        Log.d(TAG, "[MGCC] addBleConnectCheckRequestAlarm periodMs = " + periodMs);
        
        if (context == null) return;
        
        if (mAlarmTimeoutRequest != null) {
        	mAlarmTimeoutRequest.deinitAlarm(id);
        	mAlarmTimeoutRequest = null;
        	mAlarmTimeoutDevice = null;
        }
        
        if (context != null) {
        	
        	mAlarmTimeoutRequest = new BaseAlarmService("GcBleConnectCheckRequestAlarm", context);
        	mAlarmTimeoutDevice = device;

            try {

        		IAlarmService alarmService = new IAlarmService() {
                	
                    @Override
                    public void onAlarm() {

                    	Log.d(TAG, "[MGCC] onAlarm: ALARM_BLE_CONNECT_CHECK");
            			
                        if (mAlarmTimeoutRequest != null) {
                        	mAlarmTimeoutRequest.deinitAlarm(id);
                        	mAlarmTimeoutRequest = null;
                        }
                        
                        try {

                        	if (mAlarmTimeoutDevice != null) {
                        		
                            	GcConnectivityDevice gcDevice = getGcConnectivityDeviceGroup().getDevice(mAlarmTimeoutDevice);
                        		if ((gcDevice != null) && (gcDevice.getGcStateBle() == GcStateBle.GCSTATE_BLE_CONNECTING)) {
                        			
                        			/// Add one connect count and go to service discovery if count equals 2.
                        			addOneConnectCount(gcDevice, getGattClient(mAlarmTimeoutDevice));
                        		}
                        	}

                        	mAlarmTimeoutDevice = null;
						
            			} catch (Exception e) {
            				
							e.printStackTrace();
						}
                    }
        		};
        		
        		mAlarmTimeoutRequest.initAlarm(System.currentTimeMillis() + periodMs, id, alarmService);
            	
            } catch (Exception e) {
            	
                Log.d(TAG, "[MGCC] addBleConnectCheckRequestAlarm e: " + e);
            }
        }
    }
	
	
	
	private synchronized void removeBleConnectCheckRequestAlarm() {
    	
        final int id = Common.ALARM_BLE_CONNECT_CHECK;
        
        Log.d(TAG, "[MGCC] removeBleConnectCheckRequestAlarm mAlarmTimeoutRequest = " + mAlarmTimeoutRequest);
        
        if (mAlarmTimeoutRequest != null) {
        	mAlarmTimeoutRequest.deinitAlarm(id);
        	mAlarmTimeoutRequest = null;
        	mAlarmTimeoutDevice = null;
        }
	}
    
    
    
    
    /*
    private BaseAlarmService alarmTimeoutRequest = null;
    private static final int TIMEOUT_BOOT_UP_READY = 0xfffa;
    private static final int TIMEOUT_WIFI_CONNECTING = 0xfffb;
    
    private synchronized void setTimeoutRequestAlarm(boolean enable, final int id, final BluetoothDevice device) {
    	
        Log.d(TAG, "[MGCC] setTimeoutRequestAlarm: " + enable + ", id = " + id + ", context = " + mContext + ", " + alarmTimeoutRequest);
        
        if (mContext == null) return;
        
        if (alarmTimeoutRequest != null) {
        	alarmTimeoutRequest.deinitAlarm(id);
        	alarmTimeoutRequest = null;
        }
        
        if (!enable) return;
        
        if (mContext != null) {
        	
        	alarmTimeoutRequest = new BaseAlarmService("GattTimeout", mContext);

            try {

            	if (id == TIMEOUT_BOOT_UP_READY) {
            		
            		IAlarmService alarmService = new IAlarmService() {
                    	
                        @Override
                        public void onAlarm() {

                        	Log.d(TAG, "[MGCC] setTimeoutRequestAlarm TIMEOUT_BOOT_UP_READY onAlarm");

                            if (alarmTimeoutRequest != null) {
                            	alarmTimeoutRequest.deinitAlarm(id);
                            	alarmTimeoutRequest = null;
                            }
                        }
            		};
            		
            		alarmTimeoutRequest.initAlarm(System.currentTimeMillis() + 10000, TIMEOUT_BOOT_UP_READY, alarmService);

            	} else if (id == TIMEOUT_WIFI_CONNECTING) {
            		
            		IAlarmService alarmService = new IAlarmService() {
                    	
                        @Override
                        public void onAlarm() {

                        	Log.d(TAG, "[MGCC] setTimeoutRequestAlarm TIMEOUT_WIFI_CONNECTING onAlarm");

                            if (alarmTimeoutRequest != null) {
                            	alarmTimeoutRequest.deinitAlarm(id);
                            	alarmTimeoutRequest = null;
                            }
                        }
            		};

            		alarmTimeoutRequest.initAlarm(System.currentTimeMillis() + 18000, TIMEOUT_WIFI_CONNECTING, alarmService);
            	}
            	
            } catch (java.lang.NullPointerException e) {
            	
            	e.printStackTrace();
                Log.d(TAG, "[MGCC] setTimeoutRequestAlarm CONNARD: I don't know what's going on here!!");
            }
            
        }
    }
    */
}
