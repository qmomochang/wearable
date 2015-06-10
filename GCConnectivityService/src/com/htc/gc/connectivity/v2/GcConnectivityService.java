package com.htc.gc.connectivity.v2;

import java.util.Calendar;

import com.htc.gc.connectivity.v2.interfaces.IGcConnectivityService;
import com.htc.gc.connectivity.v2.internal.common.GcConnectivityTask;
import com.htc.gc.connectivity.v2.internal.component.le.GcBleGattAttributes;
import com.htc.gc.connectivity.v2.internal.tasks.GcAutoBackupTask;
import com.htc.gc.connectivity.v2.internal.tasks.GcBleConnectTask;
import com.htc.gc.connectivity.v2.internal.tasks.GcCameraErrorTask;
import com.htc.gc.connectivity.v2.internal.tasks.GcGetAllFwVersionTask;
import com.htc.gc.connectivity.v2.internal.tasks.GcGetBleFWVersionTask;
import com.htc.gc.connectivity.v2.internal.tasks.GcGpsInfoTask;
import com.htc.gc.connectivity.v2.internal.tasks.GcHwStatusTask;
import com.htc.gc.connectivity.v2.internal.tasks.GcLongTermNotifyTask;
import com.htc.gc.connectivity.v2.internal.tasks.GcMetadataTask;
import com.htc.gc.connectivity.v2.internal.tasks.GcNameTask;
import com.htc.gc.connectivity.v2.internal.tasks.GcOperationTask;
import com.htc.gc.connectivity.v2.internal.tasks.GcPasswordTask;
import com.htc.gc.connectivity.v2.internal.tasks.GcSetDateTimeTask;
import com.htc.gc.connectivity.v2.internal.tasks.GcSoftAPConnectTask;
import com.htc.gc.connectivity.v2.internal.tasks.GcWifiDisconnectTask;
import com.htc.gc.connectivity.v2.internal.tasks.GcWifiGroupTask;
import com.htc.gc.connectivity.v2.internal.tasks.GcWifiStationConnectTask;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.os.Messenger;
import android.util.Log;



public class GcConnectivityService extends GcConnectivityServiceImpl implements IGcConnectivityService {

	private final static String TAG = "GcConnectivityService";

	
	
	public GcConnectivityService(Context context, Messenger messenger) {

		super(context, messenger);
	}

	

	@Override
	public boolean gcOpen() {

		Log.d(TAG, "[MGCC] gcOpen++");
		
		boolean ret = false;

		try {

			open();
			
			ret = true;
			
		} catch (Exception e) {

			Log.d(TAG, "[MGCC] gcOpen exception: " + e);
		}

		Log.d(TAG, "[MGCC] gcOpen--");

		return ret;
	}



	@Override
	public boolean gcClose() {

		Log.d(TAG, "[MGCC] gcClose++");
		
		boolean ret = false;

		try {

			close();
			
			ret = true;
			
		} catch (Exception e) {

			Log.d(TAG, "[MGCC] gcClose exception: " + e);
		}

		Log.d(TAG, "[MGCC] gcClose--");

		return ret;
	}

	
	
	@Override
	public boolean gcCreateWifiP2pGroup() {

		Log.d(TAG, "[MGCC] gcCreateWifiP2pGroup++");
		
		boolean ret = false;

		try {

			GcConnectivityTask task = new GcWifiGroupTask(mGcBleTransceiver, mGcWifiTransceiver, mMessenger, mExecutor, true, false);
			addTask(task);
			
			ret = true;

		} catch (Exception e) {

			Log.d(TAG, "[MGCC] gcCreateWifiP2pGroup exception: " + e);
		}

		Log.d(TAG, "[MGCC] gcCreateWifiP2pGroup--");
		
		return ret;
	}


	
	@Override
	public boolean gcRemoveWifiP2pGroup() {

		Log.d(TAG, "[MGCC] gcRemoveWifiP2pGroup++");
		
		boolean ret = false;

		try {

			GcConnectivityTask task = new GcWifiGroupTask(mGcBleTransceiver, mGcWifiTransceiver, mMessenger, mExecutor, false, false);
			addTask(task);
			
			ret = true;

		} catch (Exception e) {

			Log.d(TAG, "[MGCC] gcRemoveWifiP2pGroup exception: " + e);
		}

		Log.d(TAG, "[MGCC] gcRemoveWifiP2pGroup--");
		
		return ret;
	}
	
	
	
	@Override
	public boolean gcRemoveWifiP2pGroupForce() {

		Log.d(TAG, "[MGCC] gcRemoveWifiP2pGroupForce++");
		
		boolean ret = false;

		try {

			GcConnectivityTask task = new GcWifiGroupTask(mGcBleTransceiver, mGcWifiTransceiver, mMessenger, mExecutor, false, false, true);
			addTask(task);
			
			ret = true;

		} catch (Exception e) {

			Log.d(TAG, "[MGCC] gcRemoveWifiP2pGroupForce exception: " + e);
		}

		Log.d(TAG, "[MGCC] gcRemoveWifiP2pGroupForce--");
		
		return ret;
	}
	
	

	@Override
	public boolean gcBleConnect(BluetoothDevice device) {

		Log.d(TAG, "[MGCC] gcBleConnect++");
		
		boolean ret = false;

		try {

			GcConnectivityTask task = new GcBleConnectTask(mGcBleTransceiver, mGcWifiTransceiver, mMessenger, mExecutor, device, true);
			addTask(task);

			ret = true;
			
		} catch (Exception e) {

			Log.d(TAG, "[MGCC] gcBleConnect exception: " + e);
		}

		Log.d(TAG, "[MGCC] gcBleConnect--");
		
		return ret;
	}



	@Override
	public boolean gcBleDisconnect(BluetoothDevice device) {

		Log.d(TAG, "[MGCC] gcBleDisconnect++");
		
		boolean ret = false;

		try {
			
			GcConnectivityTask task = new GcBleConnectTask(mGcBleTransceiver, mGcWifiTransceiver, mMessenger, mExecutor, device, false);
			addTask(task);
			
			ret = true;
			
		} catch (Exception e) {

			Log.d(TAG, "[MGCC] gcBleDisconnect exception: " + e);
		}

		Log.d(TAG, "[MGCC] gcBleDisconnect--");
		
		return ret;
	}

	
	
	@Override
	public boolean gcBleDisconnectForce(BluetoothDevice device) {

		Log.d(TAG, "[MGCC] gcBleDisconnectForce++");
		
		boolean ret = false;

		try {
			
			GcConnectivityTask task = new GcBleConnectTask(mGcBleTransceiver, mGcWifiTransceiver, mMessenger, mExecutor, device, false, true);
			addTask(task);
			
			ret = true;
			
		} catch (Exception e) {

			Log.d(TAG, "[MGCC] gcBleDisconnectForce exception: " + e);
		}

		Log.d(TAG, "[MGCC] gcBleDisconnectForce--");
		
		return ret;
	}


	
	@Override
	public boolean gcWifiConnect(BluetoothDevice device) {

		Log.d(TAG, "[MGCC] gcWifiConnect++");
		
		boolean ret = false;

		try {

			///GcConnectivityTask taskT = new GcWifiGroupTask(mGcBleTransceiver, mGcWifiTransceiver, mMessenger, mExecutor, true, true);
			///addTask(taskT);
			
			///GcConnectivityTask task = new GcWifiConnectTask(mGcBleTransceiver, mGcWifiTransceiver, mMessenger, mExecutor, device);
			///addTask(task);

			GcConnectivityTask task = new GcWifiStationConnectTask(mGcBleTransceiver, mGcWifiTransceiver, mMessenger, mExecutor, device);
			addTask(task);

			ret = true;
			
		} catch (Exception e) {

			Log.d(TAG, "[MGCC] gcWifiConnect exception: " + e);
		}

		Log.d(TAG, "[MGCC] gcWifiConnect--");
		
		return ret;
	}



	@Override
	public boolean gcWifiDisconnect(BluetoothDevice device) {

		Log.d(TAG, "[MGCC] gcWifiDisconnect++");
		
		boolean ret = false;

		try {

			GcConnectivityTask task = new GcWifiDisconnectTask(mGcBleTransceiver, mGcWifiTransceiver, mMessenger, mExecutor, device);
			addTask(task);
			
			ret = true;
			
		} catch (Exception e) {

			Log.d(TAG, "[MGCC] gcWifiDisconnect exception: " + e);
		}

		Log.d(TAG, "[MGCC] gcWifiDisconnect--");
		
		return ret;
	}



	@Override
	public boolean gcSetPowerOnOff(BluetoothDevice device, Module module, SwitchOnOff onoff) {
		// TODO Auto-generated method stub
		return false;
	}



	@Override
	public boolean gcGetPowerOnOff(BluetoothDevice device, Module module) {
		// TODO Auto-generated method stub
		return false;
	}



	@Override
	public boolean gcGetHwStatus(BluetoothDevice device) {

		Log.d(TAG, "[MGCC] gcGetHwStatus++");
		
		boolean ret = false;

		try {

			GcConnectivityTask task = new GcHwStatusTask(mGcBleTransceiver, mGcWifiTransceiver, mMessenger, mExecutor, device, GcHwStatusTask.ACTION_GET_HW_STATUS);
			addTask(task);
			
			ret = true;
			
		} catch (Exception e) {

			Log.d(TAG, "[MGCC] gcGetHwStatus exception: " + e);
		}

		Log.d(TAG, "[MGCC] gcGetHwStatus--");
		
		return ret;
	}

	
	
	@Override
	public boolean gcSetHwStatusLTEvent(BluetoothDevice device) {

		Log.d(TAG, "[MGCC] gcSetHwStatusLTEvent++");
		
		boolean ret = false;

		try {

			GcConnectivityTask task = new GcHwStatusTask(mGcBleTransceiver, mGcWifiTransceiver, mMessenger, mExecutor, device, GcHwStatusTask.ACTION_SET_HW_STATUS_LTEVENT);
			addTask(task);
			
			registerLTEvent(device, GcBleGattAttributes.GC_HW_STATUS);
			registerLTEvent(device, GcBleGattAttributes.GC_BOOT_UP_READY);
			
			ret = true;
			
		} catch (Exception e) {

			Log.d(TAG, "[MGCC] gcSetHwStatusLTEvent exception: " + e);
		}

		Log.d(TAG, "[MGCC] gcSetHwStatusLTEvent--");
		
		return ret;
	}
	
	
	
	@Override
	public boolean gcClrHwStatusLTEvent(BluetoothDevice device) {

		Log.d(TAG, "[MGCC] gcClrHwStatusLTEvent++");
		
		boolean ret = false;

		try {

			GcConnectivityTask task = new GcHwStatusTask(mGcBleTransceiver, mGcWifiTransceiver, mMessenger, mExecutor, device, GcHwStatusTask.ACTION_CLR_HW_STATUS_LTEVENT);
			addTask(task);
			
			unregisterLTEvent(device, GcBleGattAttributes.GC_HW_STATUS);
			unregisterLTEvent(device, GcBleGattAttributes.GC_BOOT_UP_READY);
			
			ret = true;
			
		} catch (Exception e) {

			Log.d(TAG, "[MGCC] gcClrHwStatusLTEvent exception: " + e);
		}

		Log.d(TAG, "[MGCC] gcClrHwStatusLTEvent--");
		
		return ret;
	}

	
	
	@Override
	public boolean gcSetOperationLTEvent(BluetoothDevice device) {

		Log.d(TAG, "[MGCC] gcSetOperationLTEvent++");
		
		boolean ret = false;

		try {

			GcConnectivityTask task = new GcOperationTask(mGcBleTransceiver, mGcWifiTransceiver, mMessenger, mExecutor, device, GcOperationTask.ACTION_SET_OPERATION_LTEVENT, Operation.OPERATION_NONE);
			addTask(task);
			
			registerLTEvent(device, GcBleGattAttributes.GC_CAMERA_STATUS);
			
			ret = true;
			
		} catch (Exception e) {

			Log.d(TAG, "[MGCC] gcSetOperationLTEvent exception: " + e);
		}

		Log.d(TAG, "[MGCC] gcSetOperationLTEvent--");
		
		return ret;
	}



	@Override
	public boolean gcClrOperationLTEvent(BluetoothDevice device) {

		Log.d(TAG, "[MGCC] gcClrOperationLTEvent++");
		
		boolean ret = false;

		try {

			GcConnectivityTask task = new GcOperationTask(mGcBleTransceiver, mGcWifiTransceiver, mMessenger, mExecutor, device, GcOperationTask.ACTION_CLR_OPERATION_LTEVENT, Operation.OPERATION_NONE);
			addTask(task);
			
			unregisterLTEvent(device, GcBleGattAttributes.GC_CAMERA_STATUS);
			
			ret = true;
			
		} catch (Exception e) {

			Log.d(TAG, "[MGCC] gcClrOperationLTEvent exception: " + e);
		}

		Log.d(TAG, "[MGCC] gcClrOperationLTEvent--");
		
		return ret;
	}


	
	@Override
	public boolean gcSetOperation(BluetoothDevice device, Operation operation) {

		Log.d(TAG, "[MGCC] gcSetOperation++");
		
		boolean ret = false;

		try {

			GcConnectivityTask task = new GcOperationTask(mGcBleTransceiver, mGcWifiTransceiver, mMessenger, mExecutor, device, GcOperationTask.ACTION_SET_OPERATION, operation);
			addTask(task);
			
			ret = true;
			
		} catch (Exception e) {

			Log.d(TAG, "[MGCC] gcSetOperation exception: " + e);
		}

		Log.d(TAG, "[MGCC] gcSetOperation--");
		
		return ret;
	}



	@Override
	public boolean gcSetDateTime(BluetoothDevice device, Calendar calendar) {
		
		Log.d(TAG, "[MGCC] gcSetDateTime++");
		
		boolean ret = false;

		try {

			GcConnectivityTask task = new GcSetDateTimeTask(mGcBleTransceiver, mGcWifiTransceiver, mMessenger, mExecutor, device, calendar);
			addTask(task);
			
			ret = true;
			
		} catch (Exception e) {

			Log.d(TAG, "[MGCC] gcSetDateTime exception: " + e);
		}

		Log.d(TAG, "[MGCC] gcSetDateTime--");
		
		return ret;
	}



	@Override
	public boolean gcSetName(BluetoothDevice device, String name) {

		Log.d(TAG, "[MGCC] gcSetName++");
		
		boolean ret = false;

		try {

			GcConnectivityTask task = new GcNameTask(mGcBleTransceiver, mGcWifiTransceiver, mMessenger, mExecutor, device, GcNameTask.ACTION_SET_NAME, name);
			addTask(task);
			
			ret = true;
			
		} catch (Exception e) {

			Log.d(TAG, "[MGCC] gcSetName exception: " + e);
		}

		Log.d(TAG, "[MGCC] gcSetName--");
		
		return ret;
	}

	
	
	@Override
	public boolean gcGetName(BluetoothDevice device) {

		Log.d(TAG, "[MGCC] gcGetName++");
		
		boolean ret = false;

		try {

			GcConnectivityTask task = new GcNameTask(mGcBleTransceiver, mGcWifiTransceiver, mMessenger, mExecutor, device, GcNameTask.ACTION_GET_NAME, null);
			addTask(task);
			
			ret = true;
			
		} catch (Exception e) {

			Log.d(TAG, "[MGCC] gcGetName exception: " + e);
		}

		Log.d(TAG, "[MGCC] gcGetName--");
		
		return ret;
	}

	
	
	@Override
	public boolean gcSetGpsInfoLTEvent(BluetoothDevice device) {

		Log.d(TAG, "[MGCC] gcSetGpsInfoLTEvent++");
		
		boolean ret = false;

		try {

			GcConnectivityTask task = new GcGpsInfoTask(mGcBleTransceiver, mGcWifiTransceiver, mMessenger, mExecutor, device, GcGpsInfoTask.ACTION_SET_GPS_INFO_LTEVENT, null, 0, 0, 0);
			addTask(task);
			
			registerLTEvent(device, GcBleGattAttributes.GC_REQUEST_GPS_DATA);
			
			ret = true;
			
		} catch (Exception e) {

			Log.d(TAG, "[MGCC] gcSetGpsInfoLTEvent exception: " + e);
		}

		Log.d(TAG, "[MGCC] gcSetGpsInfoLTEvent--");
		
		return ret;
	}

	

	@Override
	public boolean gcClrGpsInfoLTEvent(BluetoothDevice device) {
		
		Log.d(TAG, "[MGCC] gcClrGpsInfoLTEvent++");
		
		boolean ret = false;

		try {

			GcConnectivityTask task = new GcGpsInfoTask(mGcBleTransceiver, mGcWifiTransceiver, mMessenger, mExecutor, device, GcGpsInfoTask.ACTION_CLR_GPS_INFO_LTEVENT, null, 0, 0, 0);
			addTask(task);
			
			unregisterLTEvent(device, GcBleGattAttributes.GC_REQUEST_GPS_DATA);
			
			ret = true;
			
		} catch (Exception e) {

			Log.d(TAG, "[MGCC] gcClrGpsInfoLTEvent exception: " + e);
		}

		Log.d(TAG, "[MGCC] gcClrGpsInfoLTEvent--");
		
		return ret;
	}

	

	@Override
	public boolean gcSetGpsInfo(BluetoothDevice device, Calendar calendar, double longitude, double latitude, double altitude) {

		Log.d(TAG, "[MGCC] gcSetGpsInfo++");
		
		boolean ret = false;

		try {

			GcConnectivityTask task = new GcGpsInfoTask(mGcBleTransceiver, mGcWifiTransceiver, mMessenger, mExecutor, device, GcGpsInfoTask.ACTION_SET_GPS_INFO, calendar, longitude, latitude, altitude);
			addTask(task);
			
			ret = true;
			
		} catch (Exception e) {

			Log.d(TAG, "[MGCC] gcSetGpsInfo exception: " + e);
		}

		Log.d(TAG, "[MGCC] gcSetGpsInfo--");
		
		return ret;
	}



	@Override
	public boolean gcGetBleFWVersion(BluetoothDevice device) {

		Log.d(TAG, "[MGCC] gcGetBleFWVersion++");

		boolean ret = false;

		try {

			GcConnectivityTask task = new GcGetBleFWVersionTask(mGcBleTransceiver, mGcWifiTransceiver, mMessenger, mExecutor, device);
			addTask(task);

			ret = true;
			
		} catch (Exception e) {

			Log.d(TAG, "[MGCC] gcGetBleFWVersion exception: " + e);
		}

		Log.d(TAG, "[MGCC] gcGetBleFWVersion--");
		
		return ret;
	}



	@Override
	public boolean gcVerifyPassword(BluetoothDevice device, String password) {

		Log.d(TAG, "[MGCC] gcVerifyPassword++");
		
		boolean ret = false;

		try {

			GcConnectivityTask task = new GcPasswordTask(mGcBleTransceiver, mGcWifiTransceiver, mMessenger, mExecutor, device, GcPasswordTask.ACTION_VERIFY_PASSWORD, password);
			addTask(task);
			
			ret = true;
			
		} catch (Exception e) {

			Log.d(TAG, "[MGCC] gcVerifyPassword exception: " + e);
		}

		Log.d(TAG, "[MGCC] gcVerifyPassword--");
		
		return ret;
	}



	@Override
	public boolean gcChangePassword(BluetoothDevice device, String password) {

		Log.d(TAG, "[MGCC] gcChangePassword++");
		
		boolean ret = false;

		try {

			GcConnectivityTask task = new GcPasswordTask(mGcBleTransceiver, mGcWifiTransceiver, mMessenger, mExecutor, device, GcPasswordTask.ACTION_CHANGE_PASSWORD, password);
			addTask(task);
			
			ret = true;
			
		} catch (Exception e) {

			Log.d(TAG, "[MGCC] gcChangePassword exception: " + e);
		}

		Log.d(TAG, "[MGCC] gcChangePassword--");
		
		return ret;
	}



	@Override
	public boolean gcSetMetadataLTEvent(BluetoothDevice device) {

		Log.d(TAG, "[MGCC] gcSetMetadataLTEvent++");
		
		boolean ret = false;

		try {

			GcConnectivityTask task = new GcMetadataTask(mGcBleTransceiver, mGcWifiTransceiver, mMessenger, mExecutor, device, GcMetadataTask.ACTION_SET_METADATA_LTEVENT);
			addTask(task);
			
			registerLTEvent(device, GcBleGattAttributes.GC_METADATA);
			
			ret = true;
			
		} catch (Exception e) {

			Log.d(TAG, "[MGCC] gcSetMetadataLTEvent exception: " + e);
		}

		Log.d(TAG, "[MGCC] gcSetMetadataLTEvent--");
		
		return ret;
	}



	@Override
	public boolean gcClrMetadataLTEvent(BluetoothDevice device) {

		Log.d(TAG, "[MGCC] gcClrMetadataLTEvent++");
		
		boolean ret = false;

		try {

			GcConnectivityTask task = new GcMetadataTask(mGcBleTransceiver, mGcWifiTransceiver, mMessenger, mExecutor, device, GcMetadataTask.ACTION_CLR_METADATA_LTEVENT);
			addTask(task);
			
			unregisterLTEvent(device, GcBleGattAttributes.GC_METADATA);
			
			ret = true;
			
		} catch (Exception e) {

			Log.d(TAG, "[MGCC] gcClrMetadataLTEvent exception: " + e);
		}

		Log.d(TAG, "[MGCC] gcClrMetadataLTEvent--");
		
		return ret;
	}



	@Override
	public boolean gcSetCameraErrorLTEvent(BluetoothDevice device) {

		Log.d(TAG, "[MGCC] gcSetCameraErrorLTEvent++");
		
		boolean ret = false;

		try {

			GcConnectivityTask task = new GcCameraErrorTask(mGcBleTransceiver, mGcWifiTransceiver, mMessenger, mExecutor, device, GcCameraErrorTask.ACTION_SET_CAMERA_ERROR_LTEVENT);
			addTask(task);
			
			registerLTEvent(device, GcBleGattAttributes.GC_CAMERA_ERROR);
			
			ret = true;
			
		} catch (Exception e) {

			Log.d(TAG, "[MGCC] gcSetCameraErrorLTEvent exception: " + e);
		}

		Log.d(TAG, "[MGCC] gcSetCameraErrorLTEvent--");
		
		return ret;
	}



	@Override
	public boolean gcClrCameraErrorLTEvent(BluetoothDevice device) {

		Log.d(TAG, "[MGCC] gcClrCameraErrorLTEvent++");
		
		boolean ret = false;

		try {

			GcConnectivityTask task = new GcCameraErrorTask(mGcBleTransceiver, mGcWifiTransceiver, mMessenger, mExecutor, device, GcCameraErrorTask.ACTION_CLR_CAMERA_ERROR_LTEVENT);
			addTask(task);
			
			unregisterLTEvent(device, GcBleGattAttributes.GC_CAMERA_ERROR);
			
			ret = true;
			
		} catch (Exception e) {

			Log.d(TAG, "[MGCC] gcClrCameraErrorLTEvent exception: " + e);
		}

		Log.d(TAG, "[MGCC] gcClrCameraErrorLTEvent--");
		
		return ret;
	}
	
	//tesla++
	@Override
	public boolean gcSoftAPConnect(BluetoothDevice device, String passwd) {

		Log.d(TAG, "[MGCC] gcSoftAPConnect++");
		
		boolean ret = false;

		try {

			GcConnectivityTask task = new GcSoftAPConnectTask(mGcBleTransceiver, mGcWifiTransceiver, mMessenger, mExecutor, device, passwd);
			addTask(task);
			
			ret = true;
			
		} catch (Exception e) {

			Log.d(TAG, "[MGCC] gcSoftAPConnect exception: " + e);
		}

		Log.d(TAG, "[MGCC] gcSoftAPConnect--");
		
		return ret;
	}
	//tesla--



	@Override
	public boolean gcSetAutoBackupLTEvent(BluetoothDevice device) {

		Log.d(TAG, "[MGCC] gcSetAutoBackupLTEvent++");
		
		boolean ret = false;

		try {

			GcConnectivityTask task = new GcAutoBackupTask(mGcBleTransceiver, mGcWifiTransceiver, mMessenger, mExecutor, device, GcAutoBackupTask.ACTION_SET_AUTO_BACKUP_LTEVENT, -1);
			addTask(task);
			
			registerLTEvent(device, GcBleGattAttributes.GC_AUTO_BACKUP_GENERAL_RESULT);
			
			ret = true;
			
		} catch (Exception e) {

			Log.d(TAG, "[MGCC] gcSetAutoBackupLTEvent exception: " + e);
		}

		Log.d(TAG, "[MGCC] gcSetAutoBackupLTEvent--");
		
		return ret;
	}



	@Override
	public boolean gcClrAutoBackupLTEvent(BluetoothDevice device) {

		Log.d(TAG, "[MGCC] gcClrAutoBackupLTEvent++");
		
		boolean ret = false;

		try {

			GcConnectivityTask task = new GcAutoBackupTask(mGcBleTransceiver, mGcWifiTransceiver, mMessenger, mExecutor, device, GcAutoBackupTask.ACTION_CLR_AUTO_BACKUP_LTEVENT, -1);
			addTask(task);
			
			unregisterLTEvent(device, GcBleGattAttributes.GC_AUTO_BACKUP_GENERAL_RESULT);
			
			ret = true;
			
		} catch (Exception e) {

			Log.d(TAG, "[MGCC] gcClrAutoBackupLTEvent exception: " + e);
		}

		Log.d(TAG, "[MGCC] gcClrAutoBackupLTEvent--");
		
		return ret;
	}



	@Override
	public boolean gcSetAutoBackupAP(BluetoothDevice device, WifiAP ap, String ssid, String passwd, byte security) {

		Log.d(TAG, "[MGCC] gcSetAutoBackupAP++");
		
		boolean ret = false;

		try {

			GcConnectivityTask task = new GcAutoBackupTask(mGcBleTransceiver, mGcWifiTransceiver, mMessenger, mExecutor, device, GcAutoBackupTask.ACTION_SET_AUTO_BACKUP_AP, ap, ssid, passwd, security);
			addTask(task);
			
			ret = true;
			
		} catch (Exception e) {

			Log.d(TAG, "[MGCC] gcSetAutoBackupAP exception: " + e);
		}

		Log.d(TAG, "[MGCC] gcSetAutoBackupAP--");
		
		return ret;
	}



	@Override
	public boolean gcClrAutoBackupAP(BluetoothDevice device, byte security, String ssid) {

		Log.d(TAG, "[MGCC] gcClrAutoBackupAP++");
		
		boolean ret = false;

		try {

			GcConnectivityTask task = new GcAutoBackupTask(mGcBleTransceiver, mGcWifiTransceiver, mMessenger, mExecutor, device, GcAutoBackupTask.ACTION_CLR_AUTO_BACKUP_AP, null, ssid, null, security);
			addTask(task);
			
			ret = true;
			
		} catch (Exception e) {

			Log.d(TAG, "[MGCC] gcClrAutoBackupAP exception: " + e);
		}

		Log.d(TAG, "[MGCC] gcClrAutoBackupAP--");
		
		return ret;
	}



	@Override
	public boolean gcSetLTNotify(BluetoothDevice device) {

		Log.d(TAG, "[MGCC] gcSetLTNotify++");
		
		boolean ret = false;

		try {

			GcConnectivityTask task = new GcLongTermNotifyTask(mGcBleTransceiver, mGcWifiTransceiver, mMessenger, mExecutor, device, GcLongTermNotifyTask.ACTION_SET_LTNOTIFY);
			addTask(task);
			
			ret = true;
			
		} catch (Exception e) {

			Log.d(TAG, "[MGCC] gcSetLTNotify exception: " + e);
		}

		Log.d(TAG, "[MGCC] gcSetLTNotify--");
		
		return ret;
	}

	
	
	@Override
	public boolean gcClrLTNotify(BluetoothDevice device) {

		Log.d(TAG, "[MGCC] gcClrLTNotify++");
		
		boolean ret = false;

		try {

			GcConnectivityTask task = new GcLongTermNotifyTask(mGcBleTransceiver, mGcWifiTransceiver, mMessenger, mExecutor, device, GcLongTermNotifyTask.ACTION_CLR_LTNOTIFY);
			addTask(task);
			
			ret = true;
			
		} catch (Exception e) {

			Log.d(TAG, "[MGCC] gcClrLTNotify exception: " + e);
		}

		Log.d(TAG, "[MGCC] gcClrLTNotify--");
		
		return ret;
	}



	@Override
	public boolean gcSetAutoBackupAPScan(BluetoothDevice device, int option) {

		Log.d(TAG, "[MGCC] gcSetAutoBackupAPScan++");
		
		boolean ret = false;

		try {

			GcConnectivityTask task = new GcAutoBackupTask(mGcBleTransceiver, mGcWifiTransceiver, mMessenger, mExecutor, device, GcAutoBackupTask.ACTION_SET_AUTO_BACKUP_AP_SCAN, option);
			addTask(task);

			registerLTEvent(device, GcBleGattAttributes.GC_AUTO_BACKUP_SCAN_RESULT);
			
			ret = true;
			
		} catch (Exception e) {

			Log.d(TAG, "[MGCC] gcSetAutoBackupAPScan exception: " + e);
		}

		Log.d(TAG, "[MGCC] gcSetAutoBackupAPScan--");
		
		return ret;
	}



	@Override
	public boolean gcSetAutoBackupProxy(BluetoothDevice device, int port, byte security, String ssid, String proxy) {

		Log.d(TAG, "[MGCC] gcSetAutoBackupProxy++");
		
		boolean ret = false;

		try {

			GcConnectivityTask task = new GcAutoBackupTask(mGcBleTransceiver, mGcWifiTransceiver, mMessenger, mExecutor, device, GcAutoBackupTask.ACTION_SET_AUTO_BACKUP_PROXY, ssid, port, proxy, security);
			addTask(task);
			
			ret = true;
			
		} catch (Exception e) {

			Log.d(TAG, "[MGCC] gcSetAutoBackupProxy exception: " + e);
		}

		Log.d(TAG, "[MGCC] gcSetAutoBackupProxy--");
		
		return ret;
	}



	@Override
	public boolean gcGetAutoBackupProxy(BluetoothDevice device, byte security, String ssid) {

		Log.d(TAG, "[MGCC] gcGetAutoBackupProxy++");
		
		boolean ret = false;

		try {

			GcConnectivityTask task = new GcAutoBackupTask(mGcBleTransceiver, mGcWifiTransceiver, mMessenger, mExecutor, device, GcAutoBackupTask.ACTION_GET_AUTO_BACKUP_PROXY, ssid, 0, null, security);
			addTask(task);
			
			ret = true;
			
		} catch (Exception e) {

			Log.d(TAG, "[MGCC] gcGetAutoBackupProxy exception: " + e);
		}

		Log.d(TAG, "[MGCC] gcGetAutoBackupProxy--");
		
		return ret;
	}



	@Override
	public boolean gcGetAllFwVersion(BluetoothDevice device) {
		
		Log.d(TAG, "[MGCC] gcGetAllFwVersion++");
		
		boolean ret = false;

		try {

			GcConnectivityTask task = new GcGetAllFwVersionTask(mGcBleTransceiver, mGcWifiTransceiver, mMessenger, mExecutor, device);
			addTask(task);
			
			ret = true;
			
		} catch (Exception e) {

			Log.d(TAG, "[MGCC] gcGetAllFwVersion exception: " + e);
		}

		Log.d(TAG, "[MGCC] gcGetAllFwVersion--");
		
		return ret;
	}
	
	
	
	@Override
	public boolean gcGetAutoBackupStatus(BluetoothDevice device) {

		Log.d(TAG, "[MGCC] gcGetAutoBackupStatus++");
		
		boolean ret = false;

		try {

			GcConnectivityTask task = new GcAutoBackupTask(mGcBleTransceiver, mGcWifiTransceiver, mMessenger, mExecutor, device, GcAutoBackupTask.ACTION_GET_AUTO_BACKUP_STATUS, 0);
			addTask(task);

			ret = true;
			
		} catch (Exception e) {

			Log.d(TAG, "[MGCC] gcGetAutoBackupStatus exception: " + e);
		}

		Log.d(TAG, "[MGCC] gcGetAutoBackupStatus--");
		
		return ret;
	}
}
