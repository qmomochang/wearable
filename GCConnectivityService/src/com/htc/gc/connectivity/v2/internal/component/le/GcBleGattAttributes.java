package com.htc.gc.connectivity.v2.internal.component.le;

import java.util.HashMap;



public class GcBleGattAttributes {

	public static String GC_SERVICE 					= "0000a000-0000-1000-8000-00805f9b34fb";
	public static String GC_DEVICE_INFORMATION			= "0000180a-0000-1000-8000-00805f9b34fb";
	public static String GC_DESCRIPTOR 					= "00002902-0000-1000-8000-00805f9b34fb";
	
	public static String GC_FW_REVISION 				= "00002a26-0000-1000-8000-00805f9b34fb";
	
	public static String GC_BOOT_UP_READY 				= "0000a101-0000-1000-8000-00805f9b34fb";
	public static String GC_HW_STATUS 					= "0000a102-0000-1000-8000-00805f9b34fb";
	public static String GC_NAME 						= "0000a104-0000-1000-8000-00805f9b34fb";
	public static String GC_PSW_ACTION 					= "0000a105-0000-1000-8000-00805f9b34fb";
	public static String GC_PSW_VERIFY					= "0000a106-0000-1000-8000-00805f9b34fb";
	public static String GC_BOOT_UP_GC					= "0000a107-0000-1000-8000-00805f9b34fb";
	public static String GC_ALL_FW_VERSION				= "0000a108-0000-1000-8000-00805f9b34fb";
	public static String GC_WIFI_SERVER_BAND 			= "0000a201-0000-1000-8000-00805f9b34fb";
	public static String GC_DSC_SSID	 				= "0000a202-0000-1000-8000-00805f9b34fb";
	public static String GC_DSC_PASSWORD				= "0000a203-0000-1000-8000-00805f9b34fb";
	public static String GC_DSC_WIFI_CFG 				= "0000a204-0000-1000-8000-00805f9b34fb";
	public static String GC_PHONE_SSID	 				= "0000a301-0000-1000-8000-00805f9b34fb";
	public static String GC_PHONE_PASSWORD 				= "0000a302-0000-1000-8000-00805f9b34fb";
	public static String GC_PHONE_WIFI_CFG 				= "0000a303-0000-1000-8000-00805f9b34fb";
	public static String GC_PHONE_WIFI_ERROR 			= "0000a304-0000-1000-8000-00805f9b34fb";
	public static String GC_AUTO_BACKUP_ACTION 			= "0000a401-0000-1000-8000-00805f9b34fb";
	public static String GC_AUTO_BACKUP_RESPONSE		= "0000a402-0000-1000-8000-00805f9b34fb";
	public static String GC_AUTO_BACKUP_SCAN_RESULT		= "0000a404-0000-1000-8000-00805f9b34fb";
	public static String GC_AUTO_BACKUP_GENERAL_RESULT	= "0000a405-0000-1000-8000-00805f9b34fb";
	public static String GC_AUTO_BACKUP_PROXY			= "0000a406-0000-1000-8000-00805f9b34fb";
	public static String GC_AUTO_BACKUP_ERASE_AP		= "0000a407-0000-1000-8000-00805f9b34fb";
	public static String GC_AUTO_BACKUP_GET_PROXY		= "0000a408-0000-1000-8000-00805f9b34fb";
	public static String GC_REQUEST_GPS_DATA			= "0000a501-0000-1000-8000-00805f9b34fb";
	public static String GC_GPS_DATA 					= "0000a502-0000-1000-8000-00805f9b34fb";
	public static String GC_DATE_TIME 					= "0000a601-0000-1000-8000-00805f9b34fb";
	public static String GC_METADATA					= "0000a801-0000-1000-8000-00805f9b34fb";
	public static String GC_OPERATION					= "0000a802-0000-1000-8000-00805f9b34fb";
	public static String GC_OPERATION_RESULT			= "0000a803-0000-1000-8000-00805f9b34fb";
	public static String GC_CAMERA_STATUS				= "0000a804-0000-1000-8000-00805f9b34fb";
	public static String GC_CAMERA_ERROR				= "0000a805-0000-1000-8000-00805f9b34fb";
	public static String GC_SHORT_COMMAND_NOTIFY		= "0000ae01-0000-1000-8000-00805f9b34fb";
	public static String GC_LONG_COMMAND_NOTIFY			= "0000ae02-0000-1000-8000-00805f9b34fb";
	public static String GC_BLE_RESET	 				= "0000af01-0000-1000-8000-00805f9b34fb";

	public static String GC_TEST_SERVICE 				= "0000ffe0-0000-1000-8000-00805f9b34fb";
	public static String GC_TEST_NOTIFY 				= "0000ffe1-0000-1000-8000-00805f9b34fb";
	public static String GC_TEST_DESCRIPTOR 			= "00002902-0000-1000-8000-00805f9b34fb";

	private static HashMap<String, String> mAttributes = new HashMap<String, String>();
	private static HashMap<Byte, String> mUuidMap = new HashMap<Byte, String>();
	
	
	
    static {

        // GC Services
    	mAttributes.put(GC_SERVICE, "GC Service");

        // GC Characteristics
    	mAttributes.put(GC_BOOT_UP_READY, "Bootup Ready");
    	mAttributes.put(GC_WIFI_SERVER_BAND, "Wifi Server Band");
    	mAttributes.put(GC_DSC_SSID, "DSC SSID");
    	mAttributes.put(GC_DSC_PASSWORD, "DSC Password");
    	mAttributes.put(GC_DSC_WIFI_CFG, "DSC Wifi CFG");
    	mAttributes.put(GC_PHONE_SSID, "Phone SSID");
    	mAttributes.put(GC_PHONE_PASSWORD, "Phone Password");
    	mAttributes.put(GC_PHONE_WIFI_CFG, "Phone Wifi CFG");
    	mAttributes.put(GC_PHONE_WIFI_ERROR, "Phone Wifi Error");
    	mAttributes.put(GC_BLE_RESET, "BLE Pairing Reset");

    	/// UUID Map
    	mUuidMap.put((byte)0x11, GC_BOOT_UP_READY);
    	mUuidMap.put((byte)0x12, GC_HW_STATUS);
    	mUuidMap.put((byte)0x14, GC_NAME);
    	mUuidMap.put((byte)0x15, GC_PSW_ACTION);
    	mUuidMap.put((byte)0x16, GC_PSW_VERIFY);
    	mUuidMap.put((byte)0x17, GC_BOOT_UP_GC);
    	mUuidMap.put((byte)0x18, GC_ALL_FW_VERSION);
    	mUuidMap.put((byte)0x21, GC_WIFI_SERVER_BAND);
    	mUuidMap.put((byte)0x22, GC_DSC_SSID);
    	mUuidMap.put((byte)0x23, GC_DSC_PASSWORD);
    	mUuidMap.put((byte)0x24, GC_DSC_WIFI_CFG);
    	mUuidMap.put((byte)0x31, GC_PHONE_SSID);
    	mUuidMap.put((byte)0x32, GC_PHONE_PASSWORD);
    	mUuidMap.put((byte)0x33, GC_PHONE_WIFI_CFG);
    	mUuidMap.put((byte)0x34, GC_PHONE_WIFI_ERROR);
    	mUuidMap.put((byte)0x41, GC_AUTO_BACKUP_ACTION);
    	mUuidMap.put((byte)0x42, GC_AUTO_BACKUP_RESPONSE);
    	mUuidMap.put((byte)0x44, GC_AUTO_BACKUP_SCAN_RESULT);
    	mUuidMap.put((byte)0x45, GC_AUTO_BACKUP_GENERAL_RESULT);
    	mUuidMap.put((byte)0x46, GC_AUTO_BACKUP_PROXY);
    	mUuidMap.put((byte)0x47, GC_AUTO_BACKUP_ERASE_AP);
    	mUuidMap.put((byte)0x48, GC_AUTO_BACKUP_GET_PROXY);
    	mUuidMap.put((byte)0x51, GC_REQUEST_GPS_DATA);
    	mUuidMap.put((byte)0x52, GC_GPS_DATA);
    	mUuidMap.put((byte)0x61, GC_DATE_TIME);
    	mUuidMap.put((byte)0x81, GC_METADATA);
    	mUuidMap.put((byte)0x82, GC_OPERATION);
    	mUuidMap.put((byte)0x83, GC_OPERATION_RESULT);
    	mUuidMap.put((byte)0x84, GC_CAMERA_STATUS);
    	mUuidMap.put((byte)0x85, GC_CAMERA_ERROR);
    	mUuidMap.put((byte)0xe1, GC_SHORT_COMMAND_NOTIFY);
    	mUuidMap.put((byte)0xe2, GC_LONG_COMMAND_NOTIFY);
    }

    
    
    public static String lookup(String uuid, String defaultName) {

        String name = mAttributes.get(uuid);
        return name == null ? defaultName : name;
    }

    
    
    public static String lookupUuid(Byte id) {

        String uuid = mUuidMap.get(id);
        return uuid;
    }

    
    
    public static boolean isLongFormat(String cmd) {
    	
    	boolean ret = false;

    	if (cmd.equals(GC_DSC_SSID) ||
    		cmd.equals(GC_DSC_PASSWORD) ||
    		cmd.equals(GC_PHONE_SSID) ||
    		cmd.equals(GC_PHONE_PASSWORD) ||
    		cmd.equals(GC_GPS_DATA) ||
    		cmd.equals(GC_AUTO_BACKUP_SCAN_RESULT) ||
    		cmd.equals(GC_AUTO_BACKUP_PROXY) ||
    		cmd.equals(GC_AUTO_BACKUP_ERASE_AP) ||
    		cmd.equals(GC_AUTO_BACKUP_GET_PROXY) ||
    		cmd.equals(GC_METADATA)) {
    		
    		ret = true;
    	}

    	return ret;
    }
}
