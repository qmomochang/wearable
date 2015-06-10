package com.htc.gc.internal;

public class Protocol {
	public static final int 	Flag_No_Fragment 		= 0x0000000;
	public static final int 	Flag_More_Fragment 		= 0x2000000;
	public static final int 	Flag_Last_Fragment 		= 0x1000000;
	public static final int 	Flag_Cancel_Fragment 	= 0x4000000;

	public static class RequestHeader {
	    public static final int MIN_REQUEST_LENGTH = 16;

		public int mRequestID;
		public int mLength;
		public int mSequenceID;
		public int mFlag;
	}

	public static class ResponseHeader {
		public static final int MIN_RESPONSE_LENGTH = 16;
		public static final int MAX_RESPONSE_LENGTH = 1024 * 1024;

		public int mResponseID;
		public int mLength;
		public int mSequenceID;
		public int mFlag;
	}

	public static class EventHeader {
		public static final int MIN_EVENT_LENGTH = 12;
		public static final int MAX_EVENT_LENGTH = 1024;

		public int mEventID;
		public int mLength;
		public int mSequenceID;
	}
	
	public static final int		BATTERY_LEVEL_AC_POWER		= 0xFF;
	
	public static final int		FILE_TYPE_JPG				= 0x00;
	public static final int		FILE_TYPE_MP4				= 0x01;
	public static final int		FILE_TYPE_LIVE_BROADCASTING	= 0x02; 	
	public static final int		FILE_TYPE_MOV				= 0x03;
	public static final int		FILE_TYPE_TIMELAPSE			= 0x08;
	public static final int		FILE_TYPE_SLOWMOTION		= 0x09;
	
	public static final int 	PROP_FUNCTIONMODE_STANDARD 	= 0x00;
	public static final int 	PROP_FUNCTIONMODE_BROWSE 	= 0x01;
	public static final int		PROP_FUNCTIONMODE_CONTROL 	= 0x02;
	public static final int		PROP_FUNCTIONMODE_STANDBY 	= 0xFF;
	
	public static final int		STATUS_NONE					= 0x00;
	public static final int		STATUS_TIME_LAPSE			= 0x01;
	public static final int		STATUS_VIDEO_RECORDING		= 0x02;
	public static final int		STATUS_VIDEO_PLAYING		= 0x03;
	public static final int		STATUS_IMAGE_PROCESSING		= 0x04;
	public static final int		STATUS_TIME_LAPSE_PAUSED	= 0x05;
	
	/* Video command 101 ~ 200 */
	//public static final int 	VIDEO_GET_VOICE_RECORD_STATUS 	= 101;
	//public static final int 	VIDEO_SET_VOICE_RECORD_STATUS	= 102;
	public static final int 	VIDEO_GET_VIDEO_RESOLUTION 		= 103;
	public static final int 	VIDEO_SET_VIDEO_RESOLUTION 		= 104;
	//public static final int 	VIDEO_GET_RECORD_STATUS 		= 105;
	public static final int 	VIDEO_START_RECORD				= 106;
	public static final int 	VIDEO_STOP_RECORD				= 107;
	/*public static final int 	VIDEO_GET_TIMESTAMP_STATUS		= 108;
	public static final int 	VIDEO_SET_TIMESTAMP_STATUS		= 109;
	public static final int 	VIDEO_GET_CLIP_LENGTH			= 110;
	public static final int 	VIDEO_SET_CLIP_LENGTH			= 111;
	public static final int 	VIDEO_GET_EXPOSURE_STATUS		= 112;
	public static final int 	VIDEO_SET_EXPOSURE_STATUS		= 113;
	public static final int 	VIDEO_GET_AUTO_RECORD_STATUS	= 114;
	public static final int 	VIDEO_SET_AUTO_RECORD_STATUS	= 115;
	public static final int 	VIDEO_GET_FILE_OVERRIDE_STATUS	= 116;
	public static final int 	VIDEO_SET_FILE_OVERRIDE_STATUS	= 117;
	public static final int 	VIDEO_DELETE_FILE				= 118;
	public static final int 	VIDEO_RENAME_FILE				= 119;
	public static final int 	VIDEO_COPY_FILE					= 120;
	public static final int 	VIDEO_DOWNLOAD_FILE				= 121;
	public static final int 	VIDEO_GET_FILE_THUMBNAIL		= 122;
	public static final int 	VIDEO_SET_FILE_PROTECT			= 123;
	public static final int 	VIDEO_SET_FILE_UNPROTECT		= 124;
	public static final int 	VIDEO_SET_FILE_FAVORITE			= 125;
	public static final int 	VIDEO_SET_FILE_UNFAVORITE		= 126;
	public static final int 	VIDEO_GET_FILE_LIST				= 127;
	public static final int 	VIDEO_GET_PROTECT_FILE_LIST		= 128;
	public static final int 	VIDEO_GET_FILE_INFO				= 129;*/
	public static final int 	VIDEO_START_LIVESTREAM			= 130;
	public static final int 	VIDEO_STOP_LIVESTREAM			= 131;
	public static final int 	VIDEO_PLAY_START				= 132;
	public static final int 	VIDEO_PLAY_STOP					= 133;
	public static final int 	VIDEO_PLAY_PAUSE				= 134;
	public static final int 	VIDEO_PLAY_SEEK					= 135;
	public static final int 	VIDEO_GET_QV_THUMB				= 136;
	public static final int 	VIDEO_PLAY_RESUME				= 137;
	public static final int		VIDEO_SET_ENABLE_BROADCASTING	= 138;
	public static final int		VIDEO_GET_ENABLE_BROADCASTING	= 139;
	public static final int		VIDEO_SET_ENABLE_SLOWMOTION		= 140;
	public static final int		VIDEO_GET_ENABLE_SLOWMOTION		= 141;
    /* System command 201 ~ 300 */
	public static final int 	SYS_GET_DR_STATUS				= 201;
	/*public static final int 	SYS_GET_RTC						= 202;
	public static final int 	SYS_SET_RTC						= 203;
	public static final int 	SYSTEM_GET_DATE_TIME_FORMAT		= 204;
	public static final int 	SYSTEM_SET_DATE_TIME_FORMAT		= 205;
	public static final int 	SYS_GET_GSENSOR					= 206;
	public static final int 	SYS_SET_GSENSOR					= 207;
	public static final int 	SYS_GET_LCD_AUTO_SHUTDOWN_VALUE	= 208;
	public static final int 	SYS_SET_LCD_AUTO_SHUTDOWN_VALUE	= 209;*/
	public static final int 	SYS_GET_SPEAKER					= 210;
	public static final int 	SYS_SET_SPEAKER					= 211;
	//public static final int 	SYS_GET_FILESYS_FORMAT			= 212;
	public static final int 	SYS_GET_FREE_SPACE				= 213;
	//public static final int 	SYS_GET_TOTAL_SPACE				= 214;
	public static final int 	SYS_GET_FW_VERSION				= 215;
	public static final int 	SYS_FORMAT_SD_CARD				= 216;
	public static final int 	SYS_RESET						= 217;
	public static final int 	SYS_UPLOAD_FILE					= 218;
	public static final int 	SYS_FW_UPGRADE					= 219;
	public static final int 	SYS_CANCEL_CMD					= 220;
	public static final int 	SYS_GET_FUNCTION_MODE			= 221;
	public static final int 	SYS_SET_FUNCTION_MODE			= 222;
	public static final int 	SYS_GET_CAMERA_NAME				= 223;
	public static final int 	SYS_SET_CAMERA_NAME				= 224;
	public static final int 	SYS_GET_DOUBLE_CLICK_MODE		= 225;
	public static final int 	SYS_SET_DOUBLE_CLICK_MODE		= 226;
	//public static final int 	SYS_GET_AUTOPOWEROFF_TIME		= 227;
	public static final int		SYS_SET_AUTOPOWEROFF_TIME		= 228;
	//public static final int 	SYS_CLOSE_CONNECTION			= 229;
	public static final int 	SYS_GET_USE_STORAGE				= 230;
	public static final int 	SYS_GET_BATTERY_LEVEL			= 231;
	public static final int 	SYS_GET_SD_CHECK				= 232;
	public static final int 	SYS_SET_LIVESTREAM_RESOLUTION	= 233;
	public static final int 	SYS_SET_LIVESTREAM_FRAMERATE	= 234;
	public static final int 	SYS_SET_LIVESTREAM_COMPRESSRATE	= 235;
	public static final int 	SYS_GET_LIVESTREAM_RESOLUTION 	= 236;
	public static final int 	SYS_GET_LIVESTREAM_FRAMERATE 	= 237;
	public static final int 	SYS_GET_LIVESTREAM_COMPRESSRATE = 238;
	public static final int		SYS_HEART_BEAT					= 239;
	public static final int		SYS_GET_VIDEO_REC_BUTTON_CONFIG	= 240;
	public static final int		SYS_SET_VIDEO_REC_BUTTON_CONFIG	= 241;
	public static final int		SYS_GET_AUTO_LEVEL_STATUS		= 242;
	public static final int		SYS_SET_AUTO_LEVEL_STATUS		= 243;
	public static final int		SYS_GET_UPSIDE_DOWN_STATUS		= 244;
	public static final int		SYS_SET_UPSIDE_DOWN_STATUS		= 245;
	public static final int		SYS_GET_GPS_STATUS				= 246;
	public static final int		SYS_SET_GPS_STATUS				= 247;
	public static final int		SYS_GET_PHONE_GPS_STATUS		= 248;
	public static final int		SYS_SET_PHONE_GPS_STATUS		= 249;
	public static final int		SYS_GET_SERIAL_NUMBER			= 250;
	public static final int		SYS_REQUEST_REBOOT				= 251;
	public static final int		SYS_GET_BLE_MACADDRESS			= 252; // iOS only
	public static final int		SYS_SET_POWERSAVMODE			= 253;
	public static final int		SYS_GET_POWERSAVMODE			= 254;
	public static final int		SYS_REQUEST_OOBEMODE			= 256;
	public static final int		SYS_SET_GRIPSHOOT				= 257;
	public static final int		SYS_GET_GRIPSHOOT				= 258;
	public static final int		SYS_SET_FAKESHOOT				= 259;
	public static final int		SYS_GET_FAKESHOOT				= 260;
	public static final int		SYS_SET_LIVEVIEW_MODE			= 261;
	public static final int		SYS_GET_LIVEVIEW_MODE			= 262;
	public static final int		SYS_GET_LANGUAGE_AREA			= 263;
	public static final int		SYS_REQUEST_REBOOT_RESPONSE		= 264;
	public static final int		SYS_REQUEST_OOBEMODE_RESPONSE	= 265;
	
    /* Capture Command 301 ~ 400 */
	public static final int 	CAPTURE_GET_IMG_RATIO 				= 301;
	public static final int 	CAPTURE_SET_IMG_RATIO				= 302;
    public static final int 	CAPTURE_GET_TIMELAPSE_RATE			= 303;
    public static final int 	CAPTURE_SET_TIMELAPSE_RATE			= 304;
    public static final int 	CAPTURE_GET_TIMELAPSE_DURATION		= 305;
    public static final int 	CAPTURE_SET_TIMELAPSE_DURATION		= 306;
    public static final int 	CAPTURE_GET_PREVIEW_SETTING			= 307;
    public static final int 	CAPTURE_SET_PREVIEW_SETTING			= 308;
    public static final int 	CAPTURE_GET_FLASH_MODE				= 309;
    public static final int 	CAPTURE_SET_FLASH_MODE				= 310;
    public static final int 	CAPTURE_SHUTTER_PRESS				= 311;
    public static final int 	CAPTURE_GET_QV_IMAGE				= 312;
    public static final int 	CAPTURE_GET_EXPOSURE_STATUS			= 313;
    public static final int 	CAPTURE_SET_EXPOSURE_STATUS			= 314;
    public static final int 	CAPTURE_GET_CONTRAST_STATUS			= 315;
    public static final int 	CAPTURE_SET_CONTRAST_STATUS			= 316;
    public static final int 	CAPTURE_GET_SATURATION_STATUS		= 317;
    public static final int 	CAPTURE_SET_SATURATION_STATUS		= 318;
    public static final int 	CAPTURE_GET_SHARPNESS_STATUS		= 319;
    public static final int 	CAPTURE_SET_SHARPNESS_STATUS		= 320;
    public static final int 	CAPTURE_GET_ISO_STATUS				= 321;
    public static final int 	CAPTURE_SET_ISO_STATUS				= 322;
    public static final int 	CAPTURE_GET_WB_STATUS				= 323;
    public static final int 	CAPTURE_SET_WB_STATUS				= 324;
    public static final int 	CAPTURE_TIMELAPSECAPTURE_PAUSE		= 325;
    public static final int 	CAPTURE_TIMELAPSECAPTURE_RESUME		= 326;
    public static final int 	CAPTURE_TIMELAPSECAPTURE_START		= 327;
    public static final int 	CAPTURE_TIMELAPSECAPTURE_STOP		= 328;
    public static final int		CAPTURE_GET_WIDE_ANGLE_MODE			= 329;
    public static final int		CAPTURE_SET_WIDE_ANGLE_MODE			= 330;
    public static final int 	CAPTURE_GET_TIMELAPSE_FRAMERATE 	= 331;
    public static final int 	CAPTURE_SET_TIMELAPSE_FRAMERATE 	= 332;
    public static final int		CAPTURE_GET_TIMELAPSE_LED_STATUS	= 333;
    public static final int		CAPTURE_SET_TIMELAPSE_LED_STATUS	= 334;
    public static final int		CAPTURE_GET_IMAGE_RESOLUTION		= 335;
    public static final int		CAPTURE_SET_IMAGE_RESOLUTION		= 336;
    public static final int		CAPTURE_GET_TIMELAPSE_STOPSETTING 	= 337;
    public static final int		CAPTURE_SET_TIMELAPSE_STOPSETTING	= 338;
    public static final int		CAPTURE_SET_FACEDETECT_TIMES		= 339;
    public static final int		CAPTURE_GET_FACEDETECT_TIMES		= 340;
    
    /* File command 401 ~ 500 */
    public static final int 	FILE_GET_OBJECT_HANDLES 				= 401;
    public static final int 	FILE_GET_FILE_THUMB 					= 402;
    public static final int 	FILE_GET_JPEG_FULLHD 					= 403;
    public static final int 	FILE_GET_OBJECT_INFO 					= 404;
    public static final int 	FILE_DOWNLOAD_FILE 						= 405;
    //public static final int 	FILE_DELETE_SINGLE 						= 406;
    public static final int 	FILE_DELETE_ALL 						= 407;
    public static final int 	FILE_DELETE_BATCH		 				= 408;
    public static final int 	FILE_GET_STORAGE_COUNTS 				= 409;
    public static final int		FILE_DOWNLOAD_TIMELAPSE_FRAME			= 410;
    public static final int		FILE_DOWNLOAD_BROADCAST_VIDEO_FRAGMENT 	= 411;
    /* Initial command 501 ~ 600*/
	public static final int 	INITIAL_CHECK_VALIDATION 		= 501;
	public static final int 	INITIAL_GET_INITIAL_PARAMETERS	= 502;
	/* Auto backup command 600 ~ */
	public static final int 	AUTOBACKUP_SET_PROVIDER				= 601;
	public static final int 	AUTOBACKUP_GET_INFO					= 602;
	public static final int 	AUTOBACKUP_GET_SCANLIST 			= 603;
	public static final int 	AUTOBACKUP_SET_AP_CONFIG 			= 604;
	public static final int 	AUTOBACKUP_GET_PREFERENCE 			= 605;
	public static final int 	AUTOBACKUP_SET_PREFERENCE 			= 606;
	public static final int		AUTOBACKUP_ERASE_AP_AUTH			= 607;
	public static final int		AUTOBACKUP_SET_HTTP_PROXY			= 608;
	public static final int		AUTOBACKUP_GET_HTTP_PROXY			= 609;
	public static final int		AUTOBACKUP_SET_CLOUD_ACCOUNT		= 610;
	public static final int		AUTOBACKUP_GET_CLOUD_ACCOUNT		= 611;
	public static final int		AUTOBACKUP_WRITE_UNATOBACKUPTABLE 	= 612;
	/* Other command 1000~ */
	public static final int 	ENGINEER_MODE_GET_STRUCTURE		=1001;
	public static final int 	ENGINEER_MODE_SET_STRUCTURE		=1002;
	public static final int		ENGINEER_SET_DEBUGLOG_ENABLE	=1003;
	public static final int		ENGINEER_GET_DEBUGLOG_ENABLE	=1004;
	public static final int		ENGINEER_GET_ERROR_LOG			=1005;


	//public static final int 	EVENT_SD_CARD_FULL 					= 0x0001;
	//public static final int 	EVENT_SD_CARD_UNPLUG				= 0x0002;
	public static final int 	EVENT_SD_CARD_WRONG_FORMAT			= 0x0003;
	//public static final int 	EVENT_SD_CARD_ONREADY				= 0x0004;
	public static final int 	EVENT_SD_CARD_FORMAT_BEGIN			= 0x0005;
	public static final int 	EVENT_SD_CARD_FORMAT_END			= 0x0006;
	//public static final int 	EVENT_NEW_CLIP_FILE					= 0x0007;
	//public static final int 	EVNET_OVERRIDE_CLIP_FILE			= 0x0008;
	//public static final int 	EVENT_OVERRIDE_EMERGENCY_CLIP		= 0x0009;
	//public static final int 	EVENT_GSENSOR_MSG					= 0x000A;
	//public static final int 	EVENT_GSENSOR_EMERGENCY_BEGIN		= 0x000B;
	//public static final int 	EVENT_GSENSOR_EMERGENCY_END			= 0x000C;
	//public static final int 	EVENT_PUSH_EMERGENCY_BEGIN			= 0x000D;
	//public static final int 	EVENT_PUSH_EMERGENCY_END			= 0x000E;
	//public static final int 	EVENT_WIFI_SIGNAL_STRENGTH			= 0x000F;
	public static final int 	EVENT_CMD_CANCELED					= 0x0010;
	public static final int 	EVENT_SD_CARD_WRITE_PROTECT			= 0x0011;
	public static final int 	EVENT_SD_UNUSABLE					= 0x0012;
	public static final int 	EVENT_HIGH_TEMPERATURE				= 0x0013;
	//public static final int 	EVENT_LENS_ERROR					= 0x0014;
	//public static final int 	EVENT_FILE_NAMING_FULL				= 0x0015;
	//public static final int 	EVENT_VIDEO_SLOW_CARD				= 0x0016;
	public static final int		EVENT_VIDEO_PLAY_ERROR				= 0x0017;
	public static final int		EVENT_NO_SD_CARD					= 0x0018;
	public static final int 	EVENT_QUICKVIEW_COMPLETE 			= 0xC0A1;
	public static final int 	EVENT_ID_POWEROFF 					= 0xC0A2;
	public static final int 	EVENT_VIDEO_REC_BUTTON_PRESSED 		= 0xC0A3;
	public static final int 	EVENT_VIDEO_QV_THUMB_COMPLETE 		= 0xC0A4;
//	public static final int 	EVENT_ID_AC_IN 						= 0xC0B1;
	public static final int 	EVENT_ID_AUTOPOWER_OFF_EVENT 		= 0xC0B2;
	public static final int 	EVENT_ID_CLOSE_CONNECTION_SOCKET    = 0xC0B3;
	public static final int 	EVENT_OBJECT_ADDED 					= 0x4002;
	public static final int 	EVENT_BATTERY_LEVEL_CHANGED 		= 0x4006;
	public static final int 	EVENT_ERROR_CAPTURING 				= 0x4007;
	public static final int 	EVENT_ERROR_RECORDING 				= 0x4008;
	public static final int 	EVENT_COMPLETE_CAPTURING 	        = 0x4009;
	public static final int 	EVENT_COMPLETE_RECORDING 	    	= 0x400A;
	public static final int 	EVENT_STOP_CAPTURING	 	    	= 0x400B;
	public static final int 	EVENT_STOP_RECORDING	 	    	= 0x400C;
	public static final int 	EVENT_START_CAPTURING 				= 0x400D;
	public static final int 	EVENT_START_RECORDING 				= 0x400E;
	public static final int 	EVENT_VIDEO_PLAYBACK_FINISH 		= 0x400F;
	public static final int 	EVENT_TIME_LAPSE_CAPTURE_ONE 	    = 0x4010;
	public static final int 	EVENT_FUNCTION_MODE_CHANGE_DONE 	= 0x4011;
	public static final int		EVENT_LVSTRAM_READY					= 0x4012;
	public static final int		EVENT_SLOW_MOTION_CHANGE			= 0x4013;
	public static final int 	EVENT_BROADCAST_VIDEO_REC_ONE		= 0x4014;
	public static final int		EVENT_POCKET_MODE_CHAGNE			= 0x4015;
	public static final int 	EVENT_WIFI_STATUS_SYNC 				= 0x5001;
	public static final int 	EVENT_WIFI_HEART_BEAT 				= 0x5002;
}
