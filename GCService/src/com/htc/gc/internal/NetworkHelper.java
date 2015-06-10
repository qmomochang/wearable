package com.htc.gc.internal;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Locale;

import android.content.Context;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.htc.gc.interfaces.Common;
import com.htc.gc.interfaces.Common.CancelException;
import com.htc.gc.interfaces.Common.ErrorCode;
import com.htc.gc.interfaces.Common.ConnectionErrorCode;
import com.htc.gc.interfaces.Common.GC2WifiMgrErrorCode;
import com.htc.gc.interfaces.IGCService;

public class NetworkHelper {	
	public static final boolean DUMP_STREAM = Common.DEBUG;
	public static final int MAX_DUMP_STREAM = 4096;
	
	// this function will fill the buffer from current buffer position 
	// by buffer remaining data size
	public static void receive(InputStream stream, ByteBuffer buffer, IGCService.ICancel cancel) throws Exception
	{
		int offset = buffer.position();
		int totalSize = buffer.remaining();
		int readSize = 0;
		int remainSize = totalSize;

		while(remainSize > 0) {
			if(cancel.isCancel()) throw new CancelException();

			int read = stream.read(buffer.array(), offset + readSize, remainSize);
			if(read == -1) throw new Exception("Socket EOS");

			readSize += read;
			remainSize = totalSize - readSize;

			if(cancel.isCancel()) throw new CancelException();
		}
	}
	
	public static void dumpBuffer(ByteBuffer buffer) {
		StringBuffer sb = new StringBuffer();

		byte[] array = buffer.array();

		int count = 0;
		for(int index = buffer.position(); index < buffer.position() + buffer.remaining(); ++ index) {
			++ count;

			if(count == 1) sb.append(String.format("    %08d", index - buffer.position()) + " | ");
			sb.append(String.format("%02X ", array[index]));

			if(count == 20) {
				Log.d(Common.TAG, sb.toString());
				sb.setLength(0);
				count = 0;
			}
		}

		if(count != 0) Log.d(Common.TAG, sb.toString());
	}
	
	public static ErrorCode connectionCodeErrorCode2ErrorCode(ConnectionErrorCode errorCode) {
		switch(errorCode) {
		case WIFIMGR_ERR_SUCCESS:
			return ErrorCode.ERR_SUCCESS;
			
		case WIFIMGR_ERR_BAD_SSID:
		case WIFIMGR_ERR_BAD_SSID_LENGTH:
			return ErrorCode.ERR_INVALID_SSID;
			
		case WIFIMGR_ERR_BAD_KEY:
		case WIFIMGR_ERR_BAD_KEY_LENGTH:
			return ErrorCode.ERR_INVALID_KEY;
		
		case WIFIMGR_ERR_WPS_GET_CREDENTIAL_FAILED:
			return ErrorCode.ERR_GET_CREDENTIAL_FAILED;
			
		case ERROR_UNKNOWN:
			return ErrorCode.ERR_SYSTEM_ERROR;

		default:
			Log.i(Common.TAG, "[GCService] Can't map connection error code "+errorCode+" to error code");
			return ErrorCode.ERR_FAIL;
		}
	}
	
	public static ErrorCode GC2WifiMgrErrorCode2ErrorCode(GC2WifiMgrErrorCode errorCode) {
		switch(errorCode) {
		case WIFIMGR_ERR_SUCCESS:
			return ErrorCode.ERR_SUCCESS;
			
		case WIFIMGR_ERR_BAD_SSID:
		case WIFIMGR_ERR_BAD_SSID_LENGTH:
			return ErrorCode.ERR_INVALID_SSID;
			
		case WIFIMGR_ERR_BAD_KEY:
		case WIFIMGR_ERR_BAD_KEY_LENGTH:
			return ErrorCode.ERR_INVALID_KEY;
			
		case WIFIMGR_ERR_UNKNOWN:
			return ErrorCode.ERR_SYSTEM_ERROR;

		default:
			Log.i(Common.TAG, "[GCService] Can't map gc2 wifi mgr error code "+errorCode+" to error code");
			return ErrorCode.ERR_FAIL;
		}
	}
	
	public static String getWifiErrorRecoveryAction(int errorCode) {
		switch(errorCode) {
		case 0x01:
		case 0x0D:
		case 0x1B:
		case 0x1C:
		case 0x1D:
		case 0x92:
			return "Reset GC";
			
		case 0x02:
		case 0x03:
		case 0x04:
		case 0x05:
		case 0x06:
		case 0x0B:
		case 0x16:
		case 0x17:
		case 0x18:
		case 0x1A:
		case 0x1E:
		case 0x91:
		case 0xA0:
		case 0xA1:
		case 0xA2:
		case 0xA3:
			return "Toggle WiFi enable/disable, if no use then reset GC";
			
		case 0x15:
			return "Toggle WiFi enable/disable, if no use then reset GC; No use for country code";
			
		case 0x90:
			return "Phone may not support BT";
			
		default:
			return "None";
		}
	}
	
	public static boolean isNetworkCountryIsoReady(Context context) {
		TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
		if(telephonyManager != null) {
			Log.i(Common.TAG, "[GCService] telephonyManager.getSimState() = " + telephonyManager.getSimState());
			Log.i(Common.TAG, "[GCService] telephonyManager.getPhoneType() = " + telephonyManager.getPhoneType());
			Log.i(Common.TAG, "[GCService] telephonyManager.getNetworkCountryIso() = " + telephonyManager.getNetworkCountryIso());
			Log.i(Common.TAG, "[GCService] telephonyManager.getSimCountryIso() = " + telephonyManager.getSimCountryIso());
			Log.i(Common.TAG, "[GCService] telephonyManager.getNetworkType() = " + telephonyManager.getNetworkType());
			
			if (telephonyManager.getSimState() == TelephonyManager.SIM_STATE_READY) {
				
				if ((telephonyManager.getPhoneType() == TelephonyManager.PHONE_TYPE_GSM) ||
					(telephonyManager.getPhoneType() == TelephonyManager.PHONE_TYPE_SIP)) {
					
					String telCD = telephonyManager.getNetworkCountryIso().toUpperCase();
					
					String[] isoCountries = Locale.getISOCountries();
					if ((telCD != null) && (telCD.length() == 2)) {
						for (int cnt = 0; cnt < isoCountries.length; cnt++) {
							
							if (telCD.equals(isoCountries[cnt])) {
								Log.i(Common.TAG, "[GCService] CountryIsoReady, code= "+telCD);
								return true;
							}
						}
					}
				}

			}
		}
		
		Log.i(Common.TAG, "[GCService] CountryIsoNotReady");
		return false;
	}
	
	public static long getUnsignedInt(int x) {
	    return x & 0x00000000ffffffffL;
	}
}
