package com.htc.gc.connectivity.v3.internal.component.le;

import java.util.HashMap;

import android.util.Log;



public class GcBleGattAttributes {

	static public enum GcV2CommandEnum{
		GC_SERVICE                      ((byte)0x00),
		GC_DESCRIPTOR                   ((byte)0x01),
		GC_SHORT_COMMAND_NOTIFY         ((byte)0x05),
		GC_LONG_COMMAND_NOTIFY          ((byte)0x06),
		POWER_ON_REQUEST                ((byte)0x11),
		POWER_ON_STATUS_EVENT           ((byte)0x12),
		TRIGGER_FWUPDATE_REQUEST        ((byte)0x13),
		TRIGGER_FWUPDATE_RESULT_EVENT   ((byte)0x14),
		LAST_FWUPDATE_RESULT_EVENT      ((byte)0x15),
		WIFI_CONFIG_REQUEST             ((byte)0x21),
		WIFI_SET_SSID_REQUEST           ((byte)0x22),
		WIFI_SET_PASSWORD_REQUEST       ((byte)0x23),
		WIFI_SOFTAP_GETSSID_EVENT       ((byte)0x24),
		WIFI_SOFTAP_GETPASSWORD_EVENT   ((byte)0x25),
		WIFI_CONFIG_STATUS_EVENT        ((byte)0x26),
		WIFI_SCAN_REQUEST               ((byte)0x27),
		WIFI_SCAN_RESULT_EVENT          ((byte)0x28),
		WIFI_ERASE_AP_CONFIG_REQUEST    ((byte)0x29),
		//TODO
		// sim apn request notify event ((byte)0x2d)
		GET_MODEM_STATUS_REQUEST_EVENT	((byte)0x2A),
		SIM_PIN_ACTION_REQUEST			((byte)0x2B),
		SIM_PIN_ACTION_RESULT_EVENT		((byte)0x2C),
		LTE_CAMPING_STATUS_REQUEST_EVENT((byte)0x2E),
		HWSTATUS_EVENT                  ((byte)0x31),
		SIM_HW_STATUS_EVENT             ((byte)0x32),
		OPERATION_REQUEST               ((byte)0x41),
		OPERATION_STATUS_EVENT          ((byte)0x42),
		CAMERA_ERROR_EVENT              ((byte)0x43),
		GET_METADATA_EVENT              ((byte)0x44),
		SET_CAMERA_MODE_REQUEST         ((byte)0x45),
		SET_CAMERA_MODE_EVENT           ((byte)0x46),
		GET_CAMERA_MODE_REQUEST_EVENT   ((byte)0x47),
		SET_TIMEOUT_OFFSET_REQUEST      ((byte)0x51),
		SET_GPS_DATA_REQUEST            ((byte)0x61),
		SET_DATE_REQUEST                ((byte)0x62),
		GET_VERSION_EVENT               ((byte)0x63),
		REQUEST_GSP_DATE_EVENT          ((byte)0x64),
		SET_GC_NAME_REQUEST             ((byte)0x71),
		VERIFY_PASSWORD_REQUEST         ((byte)0x72),
		VERIFY_PASSWORD_EVENT           ((byte)0x73),
		BACKUP_SET_PROVIDER_REQUEST     ((byte)0x81),
		BACKUP_GET_STATUS_EVENT         ((byte)0x82),
		BACKUP_GENERAL_EVENT            ((byte)0x83),
		BACKUP_PERFERENCE_REQUEST       ((byte)0x84),
		GENERAL_PURPOSE_WRITE_REQUEST   ((byte)0x90),
		GENERAL_PURPOSE_READ_REQUEST    ((byte)0x91),
		GENERAL_PURPOSE_NOTIFY_EVENT    ((byte)0x92);

		private final byte id;
		GcV2CommandEnum(byte idx)
		{
			this.id = (byte)idx;
		}

		public byte getID()
		{
			return this.id;
		}

		public static GcV2CommandEnum findCommandID(byte id)
		{
			for (GcV2CommandEnum idselected:GcV2CommandEnum.values())
			{
				if ( idselected.id == id)
				{
					return idselected;
				}
			}
			//Should not be here
			return null;
		}
	}
	public static String GC_V2_COMMANDTYPE0				= "00005678-0000-1000-8000-00805f9b34fb";
	public static String GC_V2_COMMANDTYPE1				= "0000cf01-0000-1000-8000-00805f9b34fb";
	public static String GC_V2_COMMANDTYPE2 			= "0000cf02-0000-1000-8000-00805f9b34fb";
	public static String GC_V2_COMMANDTYPE3	    		= "0000cf03-0000-1000-8000-00805f9b34fb";
	public static String GC_V2_COMMANDTYPE4	    		= "00002902-0000-1000-8000-00805f9b34fb";


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

	
	private static HashMap<Byte, String> mUuidV2Map = new HashMap<Byte, String>();
	
	
    static {
    	mUuidV2Map.put(GcV2CommandEnum.GC_SERVICE.getID()                      , GC_V2_COMMANDTYPE0);
    	mUuidV2Map.put(GcV2CommandEnum.GC_DESCRIPTOR.getID()                   , GC_V2_COMMANDTYPE4);
    	mUuidV2Map.put(GcV2CommandEnum.GC_SHORT_COMMAND_NOTIFY.getID()         , GC_V2_COMMANDTYPE1);
    	mUuidV2Map.put(GcV2CommandEnum.GC_LONG_COMMAND_NOTIFY.getID()          , GC_V2_COMMANDTYPE2);
    	mUuidV2Map.put(GcV2CommandEnum.POWER_ON_REQUEST.getID()                , GC_V2_COMMANDTYPE1);
    	mUuidV2Map.put(GcV2CommandEnum.POWER_ON_STATUS_EVENT.getID()           , GC_V2_COMMANDTYPE1);
	   	mUuidV2Map.put(GcV2CommandEnum.TRIGGER_FWUPDATE_REQUEST.getID()        , GC_V2_COMMANDTYPE1);
       	mUuidV2Map.put(GcV2CommandEnum.TRIGGER_FWUPDATE_RESULT_EVENT.getID()   , GC_V2_COMMANDTYPE1);
       	mUuidV2Map.put(GcV2CommandEnum.LAST_FWUPDATE_RESULT_EVENT.getID()      , GC_V2_COMMANDTYPE1);
    	mUuidV2Map.put(GcV2CommandEnum.WIFI_CONFIG_REQUEST.getID()             , GC_V2_COMMANDTYPE1);
    	mUuidV2Map.put(GcV2CommandEnum.WIFI_SET_SSID_REQUEST.getID()           , GC_V2_COMMANDTYPE2);
    	mUuidV2Map.put(GcV2CommandEnum.WIFI_SET_PASSWORD_REQUEST.getID()       , GC_V2_COMMANDTYPE2);
    	mUuidV2Map.put(GcV2CommandEnum.WIFI_SOFTAP_GETSSID_EVENT.getID()       , GC_V2_COMMANDTYPE2);
    	mUuidV2Map.put(GcV2CommandEnum.WIFI_SOFTAP_GETPASSWORD_EVENT.getID()   , GC_V2_COMMANDTYPE2);
    	mUuidV2Map.put(GcV2CommandEnum.WIFI_CONFIG_STATUS_EVENT.getID()        , GC_V2_COMMANDTYPE1);
    	mUuidV2Map.put(GcV2CommandEnum.WIFI_SCAN_REQUEST.getID()               , GC_V2_COMMANDTYPE1);
    	mUuidV2Map.put(GcV2CommandEnum.WIFI_SCAN_RESULT_EVENT.getID()          , GC_V2_COMMANDTYPE2);
    	mUuidV2Map.put(GcV2CommandEnum.WIFI_ERASE_AP_CONFIG_REQUEST.getID()    , GC_V2_COMMANDTYPE2);
    	mUuidV2Map.put(GcV2CommandEnum.GET_MODEM_STATUS_REQUEST_EVENT.getID()  , GC_V2_COMMANDTYPE1);
    	mUuidV2Map.put(GcV2CommandEnum.SIM_PIN_ACTION_REQUEST.getID()          , GC_V2_COMMANDTYPE1);
    	mUuidV2Map.put(GcV2CommandEnum.SIM_PIN_ACTION_RESULT_EVENT.getID()     , GC_V2_COMMANDTYPE1);
    	mUuidV2Map.put(GcV2CommandEnum.LTE_CAMPING_STATUS_REQUEST_EVENT.getID(), GC_V2_COMMANDTYPE1);
    	mUuidV2Map.put(GcV2CommandEnum.HWSTATUS_EVENT.getID()                  , GC_V2_COMMANDTYPE1);
    	mUuidV2Map.put(GcV2CommandEnum.SIM_HW_STATUS_EVENT.getID()             , GC_V2_COMMANDTYPE1);
    	mUuidV2Map.put(GcV2CommandEnum.OPERATION_REQUEST.getID()               , GC_V2_COMMANDTYPE1);
    	mUuidV2Map.put(GcV2CommandEnum.OPERATION_STATUS_EVENT.getID()          , GC_V2_COMMANDTYPE1);
    	mUuidV2Map.put(GcV2CommandEnum.CAMERA_ERROR_EVENT.getID()              , GC_V2_COMMANDTYPE1);
    	mUuidV2Map.put(GcV2CommandEnum.GET_METADATA_EVENT.getID()              , GC_V2_COMMANDTYPE2);
    	mUuidV2Map.put(GcV2CommandEnum.SET_CAMERA_MODE_REQUEST.getID()         , GC_V2_COMMANDTYPE1);
    	mUuidV2Map.put(GcV2CommandEnum.SET_CAMERA_MODE_EVENT.getID()           , GC_V2_COMMANDTYPE1);
    	mUuidV2Map.put(GcV2CommandEnum.GET_CAMERA_MODE_REQUEST_EVENT.getID()   , GC_V2_COMMANDTYPE1);
    	mUuidV2Map.put(GcV2CommandEnum.SET_TIMEOUT_OFFSET_REQUEST.getID()      , GC_V2_COMMANDTYPE1);
    	mUuidV2Map.put(GcV2CommandEnum.SET_GPS_DATA_REQUEST.getID()            , GC_V2_COMMANDTYPE2);
    	mUuidV2Map.put(GcV2CommandEnum.SET_DATE_REQUEST.getID()                , GC_V2_COMMANDTYPE1);
    	mUuidV2Map.put(GcV2CommandEnum.GET_VERSION_EVENT.getID()               , GC_V2_COMMANDTYPE1);
    	mUuidV2Map.put(GcV2CommandEnum.REQUEST_GSP_DATE_EVENT.getID()          , GC_V2_COMMANDTYPE1);
    	mUuidV2Map.put(GcV2CommandEnum.SET_GC_NAME_REQUEST.getID()             , GC_V2_COMMANDTYPE1);
    	mUuidV2Map.put(GcV2CommandEnum.VERIFY_PASSWORD_REQUEST.getID()         , GC_V2_COMMANDTYPE3);
    	mUuidV2Map.put(GcV2CommandEnum.VERIFY_PASSWORD_EVENT.getID()           , GC_V2_COMMANDTYPE3);
    	mUuidV2Map.put(GcV2CommandEnum.BACKUP_SET_PROVIDER_REQUEST.getID()     , GC_V2_COMMANDTYPE2);
    	mUuidV2Map.put(GcV2CommandEnum.BACKUP_GET_STATUS_EVENT.getID()         , GC_V2_COMMANDTYPE1);
    	mUuidV2Map.put(GcV2CommandEnum.BACKUP_GENERAL_EVENT.getID()            , GC_V2_COMMANDTYPE1);
    	mUuidV2Map.put(GcV2CommandEnum.BACKUP_PERFERENCE_REQUEST.getID()       , GC_V2_COMMANDTYPE1);
    	mUuidV2Map.put(GcV2CommandEnum.GENERAL_PURPOSE_WRITE_REQUEST.getID()   , GC_V2_COMMANDTYPE2);
    	mUuidV2Map.put(GcV2CommandEnum.GENERAL_PURPOSE_READ_REQUEST.getID()    , GC_V2_COMMANDTYPE2);
    	mUuidV2Map.put(GcV2CommandEnum.GENERAL_PURPOSE_NOTIFY_EVENT.getID()    , GC_V2_COMMANDTYPE2);
    }

    
    public static String getUuid(GcV2CommandEnum id){
    	String result = mUuidV2Map.get((byte)id.getID());

    	if (result == null)
    	{
    		//do some error handle here
    		Log.d("GcBleGattAttributes", "[MGCC] getUuid cannot get UUID, uuidx:" + (byte)id.getID());
    	}
    	return result;
    }

    
    public static boolean isLongFormat(GcV2CommandEnum id) {
    	
    	boolean ret = false;
    	String result = mUuidV2Map.get(id.getID());

    	if ( result != null && result.compareTo(GC_V2_COMMANDTYPE2) == 0)
    	{
    		ret = true;
    	}

    	return ret;
    }
}
