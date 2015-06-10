package com.htc.gc.connectivity.v2.internal.component.le;

import java.util.UUID;

import com.htc.gc.connectivity.v2.interfaces.IGcConnectivityService.Operation;

import android.bluetooth.BluetoothGattCharacteristic;
import android.util.Log;




public class GcBleGattAttributeUtil {

	private final static String TAG = "GcBleGattAttributeUtil";
	

	public static short byteArrayToShort(byte[] byteArray, int offset) {
		
		short ret = 0;
		int shortSize = Short.SIZE / 8;
		
		if (byteArray.length >= (offset + shortSize)) {
			
			for (int idx = 0; idx < shortSize; idx++) {
				
				ret = (short)(ret | ((short)(byteArray[idx + offset] & 0xff) << (idx * 8)));
			}
		}
		
		return ret;
	}

	
	
	public static int byteArrayToInt(byte[] byteArray, int offset) {
		
		int ret = 0;
		int intSize = Integer.SIZE / 8;
		
		if (byteArray.length >= (offset + intSize)) {
			
			for (int idx = 0; idx < intSize; idx++) {
				
				ret = ret | ((int)(byteArray[idx + offset] & 0xff) << (idx * 8));
			}
		}
		
		return ret;
	}

	
	
	public static long byteArrayToLong(byte[] byteArray, int offset) {
		
		long ret = 0;
		int longSize = Long.SIZE / 8;
		
		for (int idx = 0; idx < longSize; idx++) {
			
			ret = ret | ((long)(byteArray[idx + offset] & 0xff) << (idx * 8));
		}
		
		return ret;
	}

	
	
	public static String byteArrayToString(byte[] byteArray, int offset, int arrayLength) {
		
		String ret = "";
		int tempLength = byteArray.length - 6;
		int length = (tempLength > arrayLength) ? tempLength : arrayLength;
		
		for (int i = 0; i < length; i++) {

			if ((byteArray[i + offset] > 0) && (byteArray[i + offset] < 128)) {
				
				ret = ret + String.format("%c", byteArray[i + offset]);

			} else {
				
				break;
			}
		}
		
		return ret;
	}
	
	
	
	public static boolean compareArray(byte[] beforeArray, byte[] afterArray) {
		
		boolean ret = true;
		
    	if (beforeArray.length == afterArray.length) {
    		
    		for (int cnt = 0; cnt < beforeArray.length; cnt++) {
    			
    			///Log.d(TAG, "[MGCC] beforeArray[" + cnt + "] = " + beforeArray[cnt] + ", afterArray[" + cnt + "] = " + afterArray[cnt]);
    			
    			if (beforeArray[cnt] != afterArray[cnt]) {
    				
    				Log.d(TAG, "[MGCC] compareArray fail because of data.");
    				return false;
    			}
    		}
    		
    	} else {
    		
    		Log.d(TAG, "[MGCC] compareArray fail because of data length.");
    		return false;
    	}
    	
    	return ret;
	}
	
	
	
	public static BluetoothGattCharacteristic convertLTNotify(BluetoothGattCharacteristic characteristic) {

		BluetoothGattCharacteristic retCharacteristic =	null;

		if (characteristic != null) {

			UUID uuidCurr = characteristic.getUuid();
			
			if (uuidCurr.toString().equals(GcBleGattAttributes.GC_SHORT_COMMAND_NOTIFY) || uuidCurr.toString().equals(GcBleGattAttributes.GC_LONG_COMMAND_NOTIFY)) {
				
				byte[] value = characteristic.getValue();
				Byte id = value[0];
				String uuidString = GcBleGattAttributes.lookupUuid(id);
				
				if ((value.length > 1) && (uuidString != null)) {

					UUID uuidNew = UUID.fromString(uuidString);
					
					retCharacteristic =	new BluetoothGattCharacteristic(uuidNew, characteristic.getProperties(), characteristic.getPermissions());
					
					byte[] valueNew = new byte[value.length - 1];
					for (int cnt = 0; cnt < (value.length - 1); cnt++) {
						
						valueNew[cnt] = value[cnt + 1];
					}

					retCharacteristic.setValue(valueNew);
				}

			} else if (uuidCurr.toString().equals(GcBleGattAttributes.GC_PSW_VERIFY)) {
				
				retCharacteristic = characteristic;
			}
		}

		return retCharacteristic;
	}
	
	
    public static boolean isBootUpReady(BluetoothGattCharacteristic characteristic) {

    	boolean ret = false;
    	
    	Log.d(TAG, "[MGCC] isBootUpReady UUID = " + characteristic.getUuid());

		if (characteristic.getUuid().toString().equals(GcBleGattAttributes.GC_BOOT_UP_READY)) {
			
			byte[] value = characteristic.getValue();
			
			if ((value[0] & 0x01) == 0x01) {
				
				ret = true;
			}
		}
		
		return ret;
    }

    
    
    public static byte[] getWifiConnectResult(BluetoothGattCharacteristic characteristic) {

    	Log.d(TAG, "[MGCC] getWifiConnectResult UUID = " + characteristic.getUuid());
    	
    	byte[] result = new byte[2];
    	
		if (characteristic.getUuid().toString().equals(GcBluetoothLeGattAttributes.GC_PHONE_WIFI_ERROR)) {
			
			byte[] value = characteristic.getValue();
			
			result[0] = value[0];
			result[1] = value[1];
		}

		Log.d(TAG, "[MGCC] Wifi connect result[0] = " + result[0] + ", result[1] = " + result[1]);

		return result;
    }
    
    
    
    public static String getIpAddress(BluetoothGattCharacteristic characteristic) {

    	Log.d(TAG, "[MGCC] getIPAddress UUID = " + characteristic.getUuid());
    	
    	String str = "";
    	
		if (characteristic.getUuid().toString().equals(GcBluetoothLeGattAttributes.GC_PHONE_WIFI_ERROR)) {
			
			byte[] value = characteristic.getValue();
			
			for (int i = 2; i < value.length; i++) {

				int val = 0;
				if ((value[i] & 0x80) == 0x80) {
					
					val = 128 + (value[i] & 0x7f);
					
				} else {
					
					val = value[i];
				}
				
				str = str + val;
				
				if (i < (value.length - 1)) {
					str = str + ".";
				}
			}
		}

		Log.d(TAG, "[MGCC] IP address = " + str);

		return str;
    }
    
    
    
    public static int getHwStatus(BluetoothGattCharacteristic characteristic, int item, boolean validCheck) {

    	int ret = -1;
    	
    	Log.d(TAG, "[MGCC] getHwStatus UUID = " + characteristic.getUuid());

		if (characteristic.getUuid().toString().equals(GcBleGattAttributes.GC_HW_STATUS)) {
			
			byte[] value = characteristic.getValue();
			int itemOffset = item * 2;
			
			if (value.length == 6) {
				
				if (validCheck) {
					
					if (value[itemOffset] == 0x01) {
						
						ret = value[itemOffset + 1];
					}
					
				} else {

					ret = value[itemOffset + 1];
				}
			}
		}
		
		return ret;
    }

    
    
    public static String getBleFWVersion(BluetoothGattCharacteristic characteristic) {

    	String ret = "";
    	
    	Log.d(TAG, "[MGCC] getBleFWVersion UUID = " + characteristic.getUuid());

		if (characteristic.getUuid().toString().equals(GcBleGattAttributes.GC_FW_REVISION)) {
			
			byte[] value = characteristic.getValue();
			
			Log.d(TAG, "[MGCC] getBleFWVersion value.length = " + value.length);
			
			for (int i = 0; i < value.length; i++) {

				ret = ret + String.format("%c", value[i]);
			}
		}
		
		Log.d(TAG, "[MGCC] getBleFWVersion ret = " + ret);
		
		return ret;
    }
    
    
    
    public static boolean getRequestGpsInfoSwitch(BluetoothGattCharacteristic characteristic) {

    	boolean ret = false;
    	
    	Log.d(TAG, "[MGCC] getRequestGpsInfoSwitch UUID = " + characteristic.getUuid());

		if (characteristic.getUuid().toString().equals(GcBleGattAttributes.GC_REQUEST_GPS_DATA)) {
			
			byte[] value = characteristic.getValue();
			
			if (value[0] == 0x01) {
				
				ret = true;
			}
		}
		
		return ret;
    }

    
    
    public static int getOperationResult(BluetoothGattCharacteristic characteristic, Operation operation) {

    	int ret = -1;
    	
    	Log.d(TAG, "[MGCC] getOperationResult UUID = " + characteristic.getUuid());

		if (characteristic.getUuid().toString().equals(GcBleGattAttributes.GC_OPERATION_RESULT)) {
			
			byte type = characteristic.getValue()[0];
			byte action = characteristic.getValue()[1];
			byte result = characteristic.getValue()[2];
			
			if ((type == 0) && (action == 1) && (operation.equals(Operation.OPERATION_CAPTURE_START))) {
				ret = result;
			} else if ((type == 1) && (action == 1) && (operation.equals(Operation.OPERATION_VIDEO_RECORDING_NORMAL_START))) {
				ret = result;
			} else if ((type == 1) && (action == 0) && (operation.equals(Operation.OPERATION_VIDEO_RECORDING_NORMAL_STOP))) {
				ret = result;
			} else if ((type == 3) && (action == 1) && (operation.equals(Operation.OPERATION_VIDEO_RECORDING_SLOW_MOTION_START))) {
				ret = result;
			} else if ((type == 3) && (action == 0) && (operation.equals(Operation.OPERATION_VIDEO_RECORDING_SLOW_MOTION_STOP))) {
				ret = result;
			} else if ((type == 2) && (action == 1) && (operation.equals(Operation.OPERATION_TIME_LAPS_RECORDING_START))) {
				ret = result;
			} else if ((type == 2) && (action == 0) && (operation.equals(Operation.OPERATION_TIME_LAPS_RECORDING_STOP))) {
				ret = result;
			} else if ((type == 2) && (action == 2) && (operation.equals(Operation.OPERATION_TIME_LAPS_RECORDING_PAUSE))) {
				ret = result;
			} else if ((type == 2) && (action == 3) && (operation.equals(Operation.OPERATION_TIME_LAPS_RECORDING_RESUME))) {
				ret = result;
			} else if ((type == 4) && (action == 4) && (operation.equals(Operation.OPERATION_GET_DR_STATUS))) {
				ret = result;
			} else if ((type == 5) && (action == 4) && (operation.equals(Operation.OPERATION_GET_FREE_SPACE))) {
				ret = result;
			}
		}
		
		return ret;
    }
    
    
    
    public static String getGcName(BluetoothGattCharacteristic characteristic) {

    	String ret = "";
    	
    	Log.d(TAG, "[MGCC] getGcName UUID = " + characteristic.getUuid());

		if (characteristic.getUuid().toString().equals(GcBleGattAttributes.GC_NAME)) {
			
			byte[] value = characteristic.getValue();
			
			Log.d(TAG, "[MGCC] getGcName value.length = " + value.length);
			
			for (int i = 0; i < value.length; i++) {

				if ((value[i] > 0) && (value[i] < 128)) {
					
					ret = ret + String.format("%c", value[i]);

				} else {
					
					break;
				}
			}
		}
		
		Log.d(TAG, "[MGCC] getGcName ret = " + ret);
		
		return ret;
    }
    
    
    
    public static String getProxy(BluetoothGattCharacteristic characteristic) {
    	
    	String ret = null;
    	
    	Log.d(TAG, "[MGCC] getProxy UUID = " + characteristic.getUuid());

		if (characteristic.getUuid().toString().equals(GcBleGattAttributes.GC_AUTO_BACKUP_GET_PROXY)) {
			
			byte[] value = characteristic.getValue();
			
			Log.d(TAG, "[MGCC] getProxy value.length = " + value.length);
			
			if ((value.length > 2) && value[0] == 0x00) {
				
				ret = "";
				
				for (int i = 0; i < value[3]; i++) {

					if ((value[i + 4] > 0) && (value[i + 4] < 128)) {
						
						ret = ret + String.format("%c", value[i + 4]);

					} else {
						
						break;
					}
				}
			}
		}

		return ret;
    }
}
