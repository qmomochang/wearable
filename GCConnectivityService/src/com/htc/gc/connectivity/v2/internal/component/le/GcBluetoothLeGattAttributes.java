package com.htc.gc.connectivity.v2.internal.component.le;

import java.util.HashMap;



public class GcBluetoothLeGattAttributes {

	public static String GC_SERVICE 					= "0000a000-0000-1000-8000-00805f9b34fb";
	public static String GC_DESCRIPTOR 					= "00002902-0000-1000-8000-00805f9b34fb";
	
	public static String GC_BOOT_UP_READY 				= "0000a101-0000-1000-8000-00805f9b34fb";
	public static String GC_WIFI_SERVER_BAND 			= "0000a201-0000-1000-8000-00805f9b34fb";
	public static String GC_DSC_SSID	 				= "0000a202-0000-1000-8000-00805f9b34fb";
	public static String GC_DSC_PASSWORD				= "0000a203-0000-1000-8000-00805f9b34fb";
	public static String GC_DSC_WIFI_CFG 				= "0000a204-0000-1000-8000-00805f9b34fb";
	public static String GC_PHONE_SSID	 				= "0000a301-0000-1000-8000-00805f9b34fb";
	public static String GC_PHONE_PASSWORD 				= "0000a302-0000-1000-8000-00805f9b34fb";
	public static String GC_PHONE_WIFI_CFG 				= "0000a303-0000-1000-8000-00805f9b34fb";
	public static String GC_PHONE_WIFI_ERROR 			= "0000a304-0000-1000-8000-00805f9b34fb";
	public static String GC_DATE_TIME	 				= "0000a601-0000-1000-8000-00805f9b34fb";
	public static String GC_BLE_CONNECT 				= "0000a701-0000-1000-8000-00805f9b34fb";
	public static String GC_BLE_RESET	 				= "0000af01-0000-1000-8000-00805f9b34fb";

	public static String GC_TEST_SERVICE 				= "0000ffe0-0000-1000-8000-00805f9b34fb";
	public static String GC_TEST_NOTIFY 				= "0000ffe1-0000-1000-8000-00805f9b34fb";
	public static String GC_TEST_DESCRIPTOR 			= "00002902-0000-1000-8000-00805f9b34fb";

	private static HashMap<String, String> mAttributes = new HashMap<String, String>();

	
	
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
    	mAttributes.put(GC_BLE_CONNECT, "BLE Connect");
    	mAttributes.put(GC_BLE_RESET, "BLE Pairing Reset");

    }

    
    
    public static String lookup(String uuid, String defaultName) {

        String name = mAttributes.get(uuid);
        return name == null ? defaultName : name;
    }
    
    
    
    public static boolean isLongFormat(String cmd) {
    	
    	boolean ret = false;

    	if (cmd.equals(GC_DSC_SSID) ||
    		cmd.equals(GC_DSC_PASSWORD) ||
    		cmd.equals(GC_PHONE_SSID) ||
    		cmd.equals(GC_PHONE_PASSWORD)) {
    		
    		ret = true;
    	}

    	return ret;
    }
}
