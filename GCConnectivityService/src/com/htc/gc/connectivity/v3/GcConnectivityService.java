package com.htc.gc.connectivity.v3;

import java.util.Calendar;
import java.util.List;

import com.htc.gc.connectivity.v3.interfaces.IGcConnectivityService;
import com.htc.gc.connectivity.v3.internal.common.GcConnectivityTask;
import com.htc.gc.connectivity.v3.internal.component.le.GcBleGattAttributes;
import com.htc.gc.connectivity.v3.internal.tasks.GcAutoBackupTask;
import com.htc.gc.connectivity.v3.internal.tasks.GcBleConnectTask;
import com.htc.gc.connectivity.v3.internal.tasks.GcCameraErrorTask;
import com.htc.gc.connectivity.v3.internal.tasks.GcCameraModeNotifyTask;
import com.htc.gc.connectivity.v3.internal.tasks.GcGetBroadcastSMSContentTask;
import com.htc.gc.connectivity.v3.internal.tasks.GcGetBroadcastUserNameTask;
import com.htc.gc.connectivity.v3.internal.tasks.GcSetBroadcastSMSContentTask;
import com.htc.gc.connectivity.v3.internal.tasks.GcUnlockSimPinTask;
import com.htc.gc.connectivity.v3.internal.tasks.GcFirmwareUpdateTask;
import com.htc.gc.connectivity.v3.internal.tasks.GcFlightModeTask;
import com.htc.gc.connectivity.v3.internal.tasks.GcGeneralPurposeCommandNotifyTask;
import com.htc.gc.connectivity.v3.internal.tasks.GcGetAllFwVersionTask;
import com.htc.gc.connectivity.v3.internal.tasks.GcGetAutoBackupAccountTask;
import com.htc.gc.connectivity.v3.internal.tasks.GcGetAutoBackupIsAvailableTask;
import com.htc.gc.connectivity.v3.internal.tasks.GcGetAutoBackupPreferenceTask;
import com.htc.gc.connectivity.v3.internal.tasks.GcGetAutoBackupStatusTask;
import com.htc.gc.connectivity.v3.internal.tasks.GcGetBleFWVersionTask;
import com.htc.gc.connectivity.v3.internal.tasks.GcGetBroadcastErrorListTask;
import com.htc.gc.connectivity.v3.internal.tasks.GcGetBroadcastInvitationListTask;
import com.htc.gc.connectivity.v3.internal.tasks.GcGetBroadcastPlatformTask;
import com.htc.gc.connectivity.v3.internal.tasks.GcGetBroadcastPrivacyTask;
import com.htc.gc.connectivity.v3.internal.tasks.GcGetBroadcastSettingTask;
import com.htc.gc.connectivity.v3.internal.tasks.GcGetBroadcastStatusTask;
import com.htc.gc.connectivity.v3.internal.tasks.GcGetBroadcastVideoUrlTask;
import com.htc.gc.connectivity.v3.internal.tasks.GcGetCameraModeTask;
import com.htc.gc.connectivity.v3.internal.tasks.GcGetModemStatusTask;
import com.htc.gc.connectivity.v3.internal.tasks.GcGpsInfoTask;
import com.htc.gc.connectivity.v3.internal.tasks.GcHwStatusTask;
import com.htc.gc.connectivity.v3.internal.tasks.GcLTECampingStatusTask;
import com.htc.gc.connectivity.v3.internal.tasks.GcLongTermNotifyTask;
import com.htc.gc.connectivity.v3.internal.tasks.GcMetadataTask;
import com.htc.gc.connectivity.v3.internal.tasks.GcNameTask;
import com.htc.gc.connectivity.v3.internal.tasks.GcOperationTask;
import com.htc.gc.connectivity.v3.internal.tasks.GcPasswordTask;
import com.htc.gc.connectivity.v3.internal.tasks.GcSetBroadcastUserNameTask;
import com.htc.gc.connectivity.v3.internal.tasks.GcSetCameraModeTask;
import com.htc.gc.connectivity.v3.internal.tasks.GcSetAutoBackupAccountTask;
import com.htc.gc.connectivity.v3.internal.tasks.GcSetAutoBackupPreferenceTask;
import com.htc.gc.connectivity.v3.internal.tasks.GcSetAutoBackupProviderTokenTask;
import com.htc.gc.connectivity.v3.internal.tasks.GcSetAutoSleepTimerOffsetTask;
import com.htc.gc.connectivity.v3.internal.tasks.GcSetBroadcastInvitationListTask;
import com.htc.gc.connectivity.v3.internal.tasks.GcSetBroadcastPlatformTask;
import com.htc.gc.connectivity.v3.internal.tasks.GcSetBroadcastPrivacyTask;
import com.htc.gc.connectivity.v3.internal.tasks.GcSetBroadcastSettingTask;
import com.htc.gc.connectivity.v3.internal.tasks.GcSetDateTimeTask;
import com.htc.gc.connectivity.v3.internal.tasks.GcSimHwStatusTask;
import com.htc.gc.connectivity.v3.internal.tasks.GcSoftAPConnectTask;
import com.htc.gc.connectivity.v3.internal.tasks.GcWifiDisconnectTask;
import com.htc.gc.connectivity.v3.internal.tasks.GcWifiGroupTask;
import com.htc.gc.connectivity.v3.internal.tasks.GcWifiStationConnectTask;
import com.htc.gc.connectivity.v3.internal.tasks.GcBootTask;

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
	public boolean gcBootUp(BluetoothDevice device)
	{
		Log.d(TAG, "[MGCC] gcBootUp++");
		boolean ret = false;

		try {

			GcConnectivityTask task = new GcBootTask(mGcBleTransceiver, mGcWifiTransceiver, mMessenger, mExecutor, device);
			addTask(task);
			ret = true;
		} catch (Exception e) {
			Log.d(TAG, "[MGCC] gcBootUp exception: " + e);
		}

		Log.d(TAG, "[MGCC] gcBootUp--");

		return ret;
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
			
			registerLTEvent(device, GcBleGattAttributes.GcV2CommandEnum.HWSTATUS_EVENT);//GcBleGattAttributes.GC_HW_STATUS);
			registerLTEvent(device, GcBleGattAttributes.GcV2CommandEnum.POWER_ON_STATUS_EVENT);//GcBleGattAttributes.GC_BOOT_UP_READY);
			
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
			
			unregisterLTEvent(device, GcBleGattAttributes.GcV2CommandEnum.HWSTATUS_EVENT);//GcBleGattAttributes.GC_HW_STATUS);
			unregisterLTEvent(device, GcBleGattAttributes.GcV2CommandEnum.POWER_ON_STATUS_EVENT);//GcBleGattAttributes.GC_BOOT_UP_READY);
			
			ret = true;
			
		} catch (Exception e) {

			Log.d(TAG, "[MGCC] gcClrHwStatusLTEvent exception: " + e);
		}

		Log.d(TAG, "[MGCC] gcClrHwStatusLTEvent--");
		
		return ret;
	}
	
	
	
	@Override
	public boolean gcGetSimHwStatus(BluetoothDevice device) {
		Log.d(TAG, "[MGCC] gcGetSimHwStatus++");
		
		boolean ret = false;

		try {

			GcConnectivityTask task = new GcSimHwStatusTask(mGcBleTransceiver, mGcWifiTransceiver, mMessenger, mExecutor, device, GcSimHwStatusTask.ACTION_GET_STATUS);
			addTask(task);
			
			ret = true;
			
		} catch (Exception e) {

			Log.d(TAG, "[MGCC] gcGetSimHwStatus exception: " + e);
		}

		Log.d(TAG, "[MGCC] gcGetSimHwStatus--");
		
		return ret;
	}

	
	
	@Override
	public boolean gcSetOperationLTEvent(BluetoothDevice device) {

		Log.d(TAG, "[MGCC] gcSetOperationLTEvent++");
		
		boolean ret = false;

		try {

			GcConnectivityTask task = new GcOperationTask(mGcBleTransceiver, mGcWifiTransceiver, mMessenger, mExecutor, device, GcOperationTask.ACTION_SET_OPERATION_LTEVENT, Operation.OPERATION_NONE);
			addTask(task);
			
			registerLTEvent(device, GcBleGattAttributes.GcV2CommandEnum.OPERATION_STATUS_EVENT);//GcBleGattAttributes.GC_CAMERA_STATUS);
			
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
			
			unregisterLTEvent(device, GcBleGattAttributes.GcV2CommandEnum.OPERATION_STATUS_EVENT);//GcBleGattAttributes.GC_CAMERA_STATUS);
			
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
			
			registerLTEvent(device, GcBleGattAttributes.GcV2CommandEnum.REQUEST_GSP_DATE_EVENT);//GcBleGattAttributes.GC_REQUEST_GPS_DATA);
			
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
			
			unregisterLTEvent(device, GcBleGattAttributes.GcV2CommandEnum.REQUEST_GSP_DATE_EVENT);//GcBleGattAttributes.GC_REQUEST_GPS_DATA);
			
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
	public boolean gcSetCameraModeLTEvent(BluetoothDevice device) {
		Log.d(TAG, "[MGCC] gcSetCameraModeLTEvent++");
		
		boolean ret = false;

		try {

			GcConnectivityTask task = new GcCameraModeNotifyTask(mGcBleTransceiver, mGcWifiTransceiver, mMessenger, mExecutor, device, GcCameraModeNotifyTask.ACTION_SET_LTEVENT);
			addTask(task);
			
			registerLTEvent(device, GcBleGattAttributes.GcV2CommandEnum.GET_CAMERA_MODE_REQUEST_EVENT);
			
			ret = true;
			
		} catch (Exception e) {

			Log.d(TAG, "[MGCC] gcSetCameraModeLTEvent exception: " + e);
		}

		Log.d(TAG, "[MGCC] gcSetCameraModeLTEvent--");
		
		return ret;
	}
	
	
	
	@Override
	public boolean gcClrCameraModeLTEvent(BluetoothDevice device) {
		Log.d(TAG, "[MGCC] gcClrCameraModeLTEvent++");
		
		boolean ret = false;

		try {

			GcConnectivityTask task = new GcCameraModeNotifyTask(mGcBleTransceiver, mGcWifiTransceiver, mMessenger, mExecutor, device, GcCameraModeNotifyTask.ACTION_CLR_LTEVENT);
			addTask(task);
			
			unregisterLTEvent(device, GcBleGattAttributes.GcV2CommandEnum.GET_CAMERA_MODE_REQUEST_EVENT);
			
			ret = true;
			
		} catch (Exception e) {

			Log.d(TAG, "[MGCC] gcClrCameraModeLTEvent exception: " + e);
		}

		Log.d(TAG, "[MGCC] gcClrCameraModeLTEvent--");
		
		return ret;
	}
	
	
	
	@Override
	public boolean gcSetCameraMode(BluetoothDevice device, CameraMode mode) {
		Log.d(TAG, "[MGCC] gcSetCameraMode++");
		
		boolean ret = false;

		try {

			GcConnectivityTask task = new GcSetCameraModeTask(mGcBleTransceiver, mGcWifiTransceiver, mMessenger, mExecutor, device, mode);
			addTask(task);
			
			ret = true;
			
		} catch (Exception e) {

			Log.d(TAG, "[MGCC] gcSetCameraMode exception: " + e);
		}

		Log.d(TAG, "[MGCC] gcSetCameraMode--");
		
		return ret;
	}
	
	
	
	@Override
	public boolean gcGetCameraMode(BluetoothDevice device) {
		Log.d(TAG, "[MGCC] gcGetCameraMode++");
		
		boolean ret = false;

		try {

			GcConnectivityTask task = new GcGetCameraModeTask(mGcBleTransceiver, mGcWifiTransceiver, mMessenger, mExecutor, device);
			addTask(task);
			
			ret = true;
			
		} catch (Exception e) {

			Log.d(TAG, "[MGCC] gcGetCameraMode exception: " + e);
		}

		Log.d(TAG, "[MGCC] gcGetCameraMode--");
		
		return ret;
	}



	@Override
	public boolean gcSetMetadataLTEvent(BluetoothDevice device) {

		Log.d(TAG, "[MGCC] gcSetMetadataLTEvent++");
		
		boolean ret = false;

		try {

			GcConnectivityTask task = new GcMetadataTask(mGcBleTransceiver, mGcWifiTransceiver, mMessenger, mExecutor, device, GcMetadataTask.ACTION_SET_METADATA_LTEVENT);
			addTask(task);
			
			registerLTEvent(device, GcBleGattAttributes.GcV2CommandEnum.GET_METADATA_EVENT);//GcBleGattAttributes.GC_METADATA);
			
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
			
			unregisterLTEvent(device, GcBleGattAttributes.GcV2CommandEnum.GET_METADATA_EVENT);//GcBleGattAttributes.GC_METADATA);
			
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
			
			registerLTEvent(device, GcBleGattAttributes.GcV2CommandEnum.CAMERA_ERROR_EVENT);//GcBleGattAttributes.GC_CAMERA_ERROR);
			
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
			
			unregisterLTEvent(device, GcBleGattAttributes.GcV2CommandEnum.CAMERA_ERROR_EVENT);//GcBleGattAttributes.GC_CAMERA_ERROR);
			
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
			
			registerLTEvent(device, GcBleGattAttributes.GcV2CommandEnum.WIFI_SCAN_RESULT_EVENT);
			
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
			
			unregisterLTEvent(device, GcBleGattAttributes.GcV2CommandEnum.WIFI_SCAN_RESULT_EVENT);
			
			ret = true;
			
		} catch (Exception e) {

			Log.d(TAG, "[MGCC] gcClrAutoBackupLTEvent exception: " + e);
		}

		Log.d(TAG, "[MGCC] gcClrAutoBackupLTEvent--");
		
		return ret;
	}



	@Override
	public boolean gcSetAutoBackupAP(BluetoothDevice device, String ssid, String passwd, byte security) {

		Log.d(TAG, "[MGCC] gcSetAutoBackupAP++");
		
		boolean ret = false;

		try {

			GcConnectivityTask task = new GcAutoBackupTask(mGcBleTransceiver, mGcWifiTransceiver, mMessenger, mExecutor, device, GcAutoBackupTask.ACTION_SET_AUTO_BACKUP_AP, ssid, passwd, security);
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

			GcConnectivityTask task = new GcAutoBackupTask(mGcBleTransceiver, mGcWifiTransceiver, mMessenger, mExecutor, device, GcAutoBackupTask.ACTION_CLR_AUTO_BACKUP_AP, ssid, null, security);
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
	public boolean gcSetAutoBackupAPScan(BluetoothDevice device, int startorstop, int option) {

		Log.d(TAG, "[MGCC] gcSetAutoBackupAPScan++");
		
		boolean ret = false;

		try {
			GcConnectivityTask task;
			if (startorstop == 0)
				task = new GcAutoBackupTask(mGcBleTransceiver, mGcWifiTransceiver, mMessenger, mExecutor, device, GcAutoBackupTask.ACTION_SET_AUTO_BACKUP_AP_SCAN_START, option);
			else
				task = new GcAutoBackupTask(mGcBleTransceiver, mGcWifiTransceiver, mMessenger, mExecutor, device, GcAutoBackupTask.ACTION_SET_AUTO_BACKUP_AP_SCAN_STOP, option);
			addTask(task);

			//registerLTEvent(device, GcBleGattAttributes.GC_AUTO_BACKUP_SCAN_RESULT);
			
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

			GcConnectivityTask task = new GcGetAutoBackupStatusTask(mGcBleTransceiver, mGcWifiTransceiver, mMessenger, mExecutor, device);
			addTask(task);

			ret = true;
			
		} catch (Exception e) {

			Log.d(TAG, "[MGCC] gcGetAutoBackupStatus exception: " + e);
		}

		Log.d(TAG, "[MGCC] gcGetAutoBackupStatus--");
		
		return ret;
	}
	
	@Override
	public boolean gcGetAutoBackupIsAvailable(BluetoothDevice device) {
		Log.d(TAG, "[MGCC] gcGetAutoBackupIsAvailable++");
		
		boolean ret = false;

		try {

			GcConnectivityTask task = new GcGetAutoBackupIsAvailableTask(mGcBleTransceiver, mGcWifiTransceiver, mMessenger, mExecutor, device);
			addTask(task);

			ret = true;
			
		} catch (Exception e) {

			Log.d(TAG, "[MGCC] gcGetAutoBackupIsAvailable exception: " + e);
		}

		Log.d(TAG, "[MGCC] gcGetAutoBackupIsAvailable--");
		
		return ret;
	}
	
	@Override
	public boolean gcSetAutoBackupAccount(BluetoothDevice device, String name) {
		Log.d(TAG, "[MGCC] gcSetAutoBackupAccount++");
		
		boolean ret = false;

		try {

			GcConnectivityTask task = new GcSetAutoBackupAccountTask(mGcBleTransceiver, mGcWifiTransceiver, mMessenger, mExecutor, device, name);
			addTask(task);

			ret = true;
			
		} catch (Exception e) {

			Log.d(TAG, "[MGCC] gcSetAutoBackupAccount exception: " + e);
		}

		Log.d(TAG, "[MGCC] gcSetAutoBackupAccount--");
		
		return ret;
	}
	
	@Override
	public boolean gcGetAutoBackupAccount(BluetoothDevice device) {
		Log.d(TAG, "[MGCC] gcGetAutoBackupAccount++");
		
		boolean ret = false;

		try {

			GcConnectivityTask task = new GcGetAutoBackupAccountTask(mGcBleTransceiver, mGcWifiTransceiver, mMessenger, mExecutor, device);
			addTask(task);

			ret = true;
			
		} catch (Exception e) {

			Log.d(TAG, "[MGCC] gcGetAutoBackupAccount exception: " + e);
		}

		Log.d(TAG, "[MGCC] gcGetAutoBackupAccount--");
		
		return ret;
	}
	
	@Override
	public boolean gcSetAutoBackupPreference(BluetoothDevice device, boolean enableBackup, boolean deleteAfterBackup, boolean backupWithoutAC) {
		Log.d(TAG, "[MGCC] gcSetAutoBackupPreference++");
		
		boolean ret = false;

		try {

			GcConnectivityTask task = new GcSetAutoBackupPreferenceTask(mGcBleTransceiver, mGcWifiTransceiver, mMessenger, mExecutor, device, enableBackup, deleteAfterBackup, backupWithoutAC);
			addTask(task);

			ret = true;
			
		} catch (Exception e) {

			Log.d(TAG, "[MGCC] gcSetAutoBackupPreference exception: " + e);
		}

		Log.d(TAG, "[MGCC] gcSetAutoBackupPreference--");
		
		return ret;
	}
	
	@Override
	public boolean gcGetAutoBackupPreference(BluetoothDevice device) {
		Log.d(TAG, "[MGCC] gcGetAutoBackupPreference++");
		
		boolean ret = false;

		try {

			GcConnectivityTask task = new GcGetAutoBackupPreferenceTask(mGcBleTransceiver, mGcWifiTransceiver, mMessenger, mExecutor, device);
			addTask(task);

			ret = true;
			
		} catch (Exception e) {

			Log.d(TAG, "[MGCC] gcGetAutoBackupPreference exception: " + e);
		}

		Log.d(TAG, "[MGCC] gcGetAutoBackupPreference--");
		
		return ret;
	}

	@Override
	public boolean gcSetFlightMode(BluetoothDevice device) {
		Log.d(TAG, "[MGCC] gcSetFlightMode++");

		boolean ret = false;
		try {
			GcConnectivityTask task = new GcFlightModeTask(mGcBleTransceiver, mGcWifiTransceiver, mMessenger, mExecutor, device);
			addTask(task);
			ret = true;
		} catch (Exception e) {
			Log.d(TAG, "[MGCC] gcSetFlightMode exception: " + e);
		}

		Log.d(TAG, "[MGCC] gcSetFlightMode--");
		return ret;
	}

    @Override
    public boolean gcTriggerFWUpdate(BluetoothDevice device,
                    boolean update_rtos, boolean update_modem, boolean update_mcu, String firmwareVersion) {
            Log.d(TAG, "[MGCC] gcTriggerFWUpdate++");

            boolean ret = false;
            try {
                    GcConnectivityTask task = new GcFirmwareUpdateTask(mGcBleTransceiver, mGcWifiTransceiver, mMessenger, mExecutor, device, update_rtos, update_modem, update_mcu, firmwareVersion);
                    addTask(task);
                    ret = true;
            } catch (Exception e) {
                    Log.d(TAG, "[MGCC] gcTriggerFWUpdate exception: " + e);
            }

            Log.d(TAG, "[MGCC] gcTriggerFWUpdate--");
            return ret;
    }

	@Override
	public boolean gcSetAutoSleepTimerOffset(BluetoothDevice device, int offset_sec) {
		Log.d(TAG, "[MGCC] gcSetAutoSleepTimerOffset++");

		boolean ret = false;
		try {
			GcConnectivityTask task = new GcSetAutoSleepTimerOffsetTask(mGcBleTransceiver, mGcWifiTransceiver, mMessenger, mExecutor, device, offset_sec);
			addTask(task);
			ret = true;
		} catch (Exception e) {
			Log.d(TAG, "[MGCC] gcSetAutoSleepTimerOffset exception: " + e);
		}

		Log.d(TAG, "[MGCC] gcSetAutoSleepTimerOffset--");
		return ret;
	}

	@Override
	public boolean gcSetAutoBackupToken(BluetoothDevice device,BackupProviderIdIndex pidx, BackupTokenType type, String token) {
		Log.d(TAG, "[MGCC] gcSetAutoBackupToken++");

		boolean ret = false;
		try {
			GcConnectivityTask task = new GcSetAutoBackupProviderTokenTask(mGcBleTransceiver, mGcWifiTransceiver, mMessenger, mExecutor, device, pidx, type, token);
			addTask(task);
			ret = true;
		} catch (Exception e) {
			Log.d(TAG, "[MGCC] gcSetAutoBackupToken exception: " + e);
		}

		Log.d(TAG, "[MGCC] gcSetAutoBackupToken--");
		return ret;
	}
	
	@Override
	public boolean gcSetBroadcastSetting(BluetoothDevice device, BroadcastSetting setting) {
		Log.d(TAG, "[MGCC] gcSetBroadcastSetting++");
		
		boolean ret=  false;
		try {
			GcConnectivityTask task = new GcSetBroadcastSettingTask(mGcBleTransceiver, mGcWifiTransceiver, mMessenger, mExecutor, device, setting);
			addTask(task);
			ret = true;
		} catch (Exception e) {
			Log.d(TAG, "[MGCC] gcSetBroadcastSetting exception: " + e);
		}
		
		Log.d(TAG, "[MGCC] gcSetBroadcastSetting--");
		return ret;
	}
	
	@Override
	public boolean gcGetBroadcastSetting(BluetoothDevice device) {
		Log.d(TAG, "[MGCC] gcGetBroadcastSetting++");
		
		boolean ret=  false;
		try {
			GcConnectivityTask task = new GcGetBroadcastSettingTask(mGcBleTransceiver, mGcWifiTransceiver, mMessenger, mExecutor, device);
			addTask(task);
			ret = true;
		} catch (Exception e) {
			Log.d(TAG, "[MGCC] gcGetBroadcastSetting exception: " + e);
		}
		
		Log.d(TAG, "[MGCC] gcGetBroadcastSetting--");
		return ret;
	}
	
	@Override
	public boolean gcSetBroadcastPlatform(BluetoothDevice device, BroadcastPlatform platform, BroadcastTokenType tokenType, String token) {
		Log.d(TAG, "[MGCC] gcSetBroadcastPlatform++");
		
		boolean ret=  false;
		try {
			GcConnectivityTask task = new GcSetBroadcastPlatformTask(mGcBleTransceiver, mGcWifiTransceiver, mMessenger, mExecutor, device, platform, tokenType, token);
			addTask(task);
			ret = true;
		} catch (Exception e) {
			Log.d(TAG, "[MGCC] gcSetBroadcastPlatform exception: " + e);
		}
		
		Log.d(TAG, "[MGCC] gcSetBroadcastPlatform--");
		return ret;
	}
	
	@Override
	public boolean gcSetBroadcastInvitationList(BluetoothDevice device, List<String> invitationList) {
		Log.d(TAG, "[MGCC] gcSetBroadcastInvitationList++");
		
		boolean ret=  false;
		try {
			GcConnectivityTask task = new GcSetBroadcastInvitationListTask(mGcBleTransceiver, mGcWifiTransceiver, mMessenger, mExecutor, device, invitationList);
			addTask(task);
			ret = true;
		} catch (Exception e) {
			Log.d(TAG, "[MGCC] gcSetBroadcastInvitationList exception: " + e);
		}
		
		Log.d(TAG, "[MGCC] gcSetBroadcastInvitationList--");
		return ret;
	}
	
	@Override
	public boolean gcSetBroadcastPrivacy(BluetoothDevice device, BroadcastPrivacy privacy) {
		Log.d(TAG, "[MGCC] gcSetBroadcastPrivacy++");
		
		boolean ret=  false;
		try {
			GcConnectivityTask task = new GcSetBroadcastPrivacyTask(mGcBleTransceiver, mGcWifiTransceiver, mMessenger, mExecutor, device, privacy);
			addTask(task);
			ret = true;
		} catch (Exception e) {
			Log.d(TAG, "[MGCC] gcSetBroadcastPrivacy exception: " + e);
		}
		
		Log.d(TAG, "[MGCC] gcSetBroadcastPrivacy--");
		return ret;
	}
	
	@Override
	public boolean gcGetBroadcastStatus(BluetoothDevice device) {
		Log.d(TAG, "[MGCC] gcGetBroadcastStatus++");
		
		boolean ret=  false;
		try {
			GcConnectivityTask task = new GcGetBroadcastStatusTask(mGcBleTransceiver, mGcWifiTransceiver, mMessenger, mExecutor, device);
			addTask(task);
			ret = true;
		} catch (Exception e) {
			Log.d(TAG, "[MGCC] gcGetBroadcastStatus exception: " + e);
		}
		
		Log.d(TAG, "[MGCC] gcGetBroadcastStatus--");
		return ret;
	}
	
	@Override
	public boolean gcGetBroadcastInvitationList(BluetoothDevice device) {
		Log.d(TAG, "[MGCC] gcGetBroadcastInvitationList++");
		
		boolean ret=  false;
		try {
			GcConnectivityTask task = new GcGetBroadcastInvitationListTask(mGcBleTransceiver, mGcWifiTransceiver, mMessenger, mExecutor, device);
			addTask(task);
			ret = true;
		} catch (Exception e) {
			Log.d(TAG, "[MGCC] gcGetBroadcastInvitationList exception: " + e);
		}
		
		Log.d(TAG, "[MGCC] gcGetBroadcastInvitationList--");
		return ret;
	}
	
	@Override
	public boolean gcGetBroadcastPrivacy(BluetoothDevice device) {
		Log.d(TAG, "[MGCC] gcGetBroadcastPrivacy++");
		
		boolean ret=  false;
		try {
			GcConnectivityTask task = new GcGetBroadcastPrivacyTask(mGcBleTransceiver, mGcWifiTransceiver, mMessenger, mExecutor, device);
			addTask(task);
			ret = true;
		} catch (Exception e) {
			Log.d(TAG, "[MGCC] gcGetBroadcastPrivacy exception: " + e);
		}
		
		Log.d(TAG, "[MGCC] gcGetBroadcastPrivacy--");
		return ret;
	}
	
	@Override
	public boolean gcGetBroadcastPlatform(BluetoothDevice device) {
		Log.d(TAG, "[MGCC] gcGetBroadcastPlatform++");
		
		boolean ret=  false;
		try {
			GcConnectivityTask task = new GcGetBroadcastPlatformTask(mGcBleTransceiver, mGcWifiTransceiver, mMessenger, mExecutor, device);
			addTask(task);
			ret = true;
		} catch (Exception e) {
			Log.d(TAG, "[MGCC] gcGetBroadcastPlatform exception: " + e);
		}
		
		Log.d(TAG, "[MGCC] gcGetBroadcastPlatform--");
		return ret;
	}
	
	@Override
	public boolean gcGetBroadcastVideoUrl(BluetoothDevice device) {
		Log.d(TAG, "[MGCC] gcGetBroadcastVideoUrl++");
		
		boolean ret=  false;
		try {
			GcConnectivityTask task = new GcGetBroadcastVideoUrlTask(mGcBleTransceiver, mGcWifiTransceiver, mMessenger, mExecutor, device);
			addTask(task);
			ret = true;
		} catch (Exception e) {
			Log.d(TAG, "[MGCC] gcGetBroadcastVideoUrl exception: " + e);
		}
		
		Log.d(TAG, "[MGCC] gcGetBroadcastVideoUrl--");
		return ret;
	}
	
	@Override
	public boolean gcGetBroadcastErrorList(BluetoothDevice device) {
		Log.d(TAG, "[MGCC] gcGetBroadcastErrorList++");
		
		boolean ret=  false;
		try {
			GcConnectivityTask task = new GcGetBroadcastErrorListTask(mGcBleTransceiver, mGcWifiTransceiver, mMessenger, mExecutor, device);
			addTask(task);
			ret = true;
		} catch (Exception e) {
			Log.d(TAG, "[MGCC] gcGetBroadcastErrorList exception: " + e);
		}
		
		Log.d(TAG, "[MGCC] gcGetBroadcastErrorList--");
		return ret;
	}
	
	@Override
	public boolean gcSetBroadcastUserName(BluetoothDevice device, String userName) {
		Log.d(TAG, "[MGCC] gcSetBroadcastUserName++");
		
		boolean ret=  false;
		try {
			GcConnectivityTask task = new GcSetBroadcastUserNameTask(mGcBleTransceiver, mGcWifiTransceiver, mMessenger, mExecutor, device, userName);
			addTask(task);
			ret = true;
		} catch (Exception e) {
			Log.d(TAG, "[MGCC] gcSetBroadcastUserName exception: " + e);
		}
		
		Log.d(TAG, "[MGCC] gcSetBroadcastUserName--");
		return ret;
	}
	
	@Override
	public boolean gcSetBroadcastSMSContent(BluetoothDevice device, String smsContent) {
		Log.d(TAG, "[MGCC] gcSetBroadcastSMSContent++");
		
		boolean ret=  false;
		try {
			GcConnectivityTask task = new GcSetBroadcastSMSContentTask(mGcBleTransceiver, mGcWifiTransceiver, mMessenger, mExecutor, device, smsContent);
			addTask(task);
			ret = true;
		} catch (Exception e) {
			Log.d(TAG, "[MGCC] gcSetBroadcastSMSContent exception: " + e);
		}
		
		Log.d(TAG, "[MGCC] gcSetBroadcastSMSContent--");
		return ret;
	}
	
	@Override
	public boolean gcGetBroadcastUserName(BluetoothDevice device) {
		Log.d(TAG, "[MGCC] gcGetBroadcastUserName++");
		
		boolean ret=  false;
		try {
			GcConnectivityTask task = new GcGetBroadcastUserNameTask(mGcBleTransceiver, mGcWifiTransceiver, mMessenger, mExecutor, device);
			addTask(task);
			ret = true;
		} catch (Exception e) {
			Log.d(TAG, "[MGCC] gcGetBroadcastUserName exception: " + e);
		}
		
		Log.d(TAG, "[MGCC] gcGetBroadcastUserName--");
		return ret;
	}
	
	@Override
	public boolean gcGetBroadcastSMSContent(BluetoothDevice device) {
		Log.d(TAG, "[MGCC] gcGetBroadcastSMSContent++");
		
		boolean ret=  false;
		try {
			GcConnectivityTask task = new GcGetBroadcastSMSContentTask(mGcBleTransceiver, mGcWifiTransceiver, mMessenger, mExecutor, device);
			addTask(task);
			ret = true;
		} catch (Exception e) {
			Log.d(TAG, "[MGCC] gcGetBroadcastSMSContent exception: " + e);
		}
		
		Log.d(TAG, "[MGCC] gcGetBroadcastSMSContent--");
		return ret;
	}
	
	@Override
	public boolean gcSetGeneralPurposeCommandLTNotify(BluetoothDevice device) {
		Log.d(TAG, "[MGCC] gcSetGeneralPurposeCommandLTNotify++");
		
		boolean ret=  false;
		try {
			GcConnectivityTask task = new GcGeneralPurposeCommandNotifyTask(mGcBleTransceiver, mGcWifiTransceiver, mMessenger, mExecutor, device, GcGeneralPurposeCommandNotifyTask.ACTION_SET_LTEVENT);
			addTask(task);
			
			registerLTEvent(device, GcBleGattAttributes.GcV2CommandEnum.GENERAL_PURPOSE_NOTIFY_EVENT);
			
			ret = true;
		} catch (Exception e) {
			Log.d(TAG, "[MGCC] gcSetGeneralPurposeCommandLTNotify exception: " + e);
		}
		
		Log.d(TAG, "[MGCC] gcSetGeneralPurposeCommandLTNotify--");
		return ret;
	}
	
	@Override
	public boolean gcClrGeneralPurposeCommandLTNotify(BluetoothDevice device) {
		Log.d(TAG, "[MGCC] gcClrGeneralPurposeCommandLTNotify++");
		
		boolean ret=  false;
		try {
			GcConnectivityTask task = new GcGeneralPurposeCommandNotifyTask(mGcBleTransceiver, mGcWifiTransceiver, mMessenger, mExecutor, device, GcGeneralPurposeCommandNotifyTask.ACTION_CLR_LTEVENT);
			addTask(task);
			
			unregisterLTEvent(device, GcBleGattAttributes.GcV2CommandEnum.GENERAL_PURPOSE_NOTIFY_EVENT);
			
			ret = true;
		} catch (Exception e) {
			Log.d(TAG, "[MGCC] gcClrGeneralPurposeCommandLTNotify exception: " + e);
		}
		
		Log.d(TAG, "[MGCC] gcClrGeneralPurposeCommandLTNotify--");
		return ret;
	}
	
	@Override
	public boolean gcGetLTECampingStatus(BluetoothDevice device) {
		Log.d(TAG, "[MGCC] gcGetLTECampingStatus++");
		
		boolean ret=  false;
		try {
			GcConnectivityTask task = new GcLTECampingStatusTask(mGcBleTransceiver, mGcWifiTransceiver, mMessenger, mExecutor, device, GcLTECampingStatusTask.ACTION_GET);
			addTask(task);
			ret = true;
		} catch (Exception e) {
			Log.d(TAG, "[MGCC] gcGetLTECampingStatus exception: " + e);
		}
		
		Log.d(TAG, "[MGCC] gcGetLTECampingStatus--");
		return ret;
	}
	
	@Override
	public boolean gcSetLTECampingStatusLTEvent(BluetoothDevice device) {
		Log.d(TAG, "[MGCC] gcSetLTECampingStatusLTEvent++");
		
		boolean ret=  false;
		try {
			GcConnectivityTask task = new GcLTECampingStatusTask(mGcBleTransceiver, mGcWifiTransceiver, mMessenger, mExecutor, device, GcLTECampingStatusTask.ACTION_SET_LTEVENT);
			addTask(task);
			
			registerLTEvent(device, GcBleGattAttributes.GcV2CommandEnum.LTE_CAMPING_STATUS_REQUEST_EVENT);
			
			ret = true;
		} catch (Exception e) {
			Log.d(TAG, "[MGCC] gcSetLTECampingStatusLTEvent exception: " + e);
		}
		
		Log.d(TAG, "[MGCC] gcSetLTECampingStatusLTEvent--");
		return ret;
	}
	
	@Override
	public boolean gcClrLTECampingStatusLTEvent(BluetoothDevice device) {
		Log.d(TAG, "[MGCC] gcClrLTECampingStatusLTEvent++");
		
		boolean ret=  false;
		try {
			GcConnectivityTask task = new GcLTECampingStatusTask(mGcBleTransceiver, mGcWifiTransceiver, mMessenger, mExecutor, device, GcLTECampingStatusTask.ACTION_CLR_LTEVENT);
			addTask(task);
			
			unregisterLTEvent(device, GcBleGattAttributes.GcV2CommandEnum.LTE_CAMPING_STATUS_REQUEST_EVENT);
			
			ret = true;
		} catch (Exception e) {
			Log.d(TAG, "[MGCC] gcClrLTECampingStatusLTEvent exception: " + e);
		}
		
		Log.d(TAG, "[MGCC] gcClrLTECampingStatusLTEvent--");
		return ret;
	}
	
	@Override
	public boolean gcGetModemStatus(BluetoothDevice device) {
		Log.d(TAG, "[MGCC] gcGetModemStatus++");
		
		boolean ret=  false;
		try {
			GcConnectivityTask task = new GcGetModemStatusTask(mGcBleTransceiver, mGcWifiTransceiver, mMessenger, mExecutor, device);
			addTask(task);
			ret = true;
		} catch (Exception e) {
			Log.d(TAG, "[MGCC] gcGetModemStatus exception: " + e);
		}
		
		Log.d(TAG, "[MGCC] gcGetModemStatus--");
		return ret;
	}
	
	@Override
	public boolean gcUnlockSimPin(BluetoothDevice device, String pinCode) {
		Log.d(TAG, "[MGCC] gcUnlockSimPin++");
		
		boolean ret=  false;
		try {
			GcConnectivityTask task = new GcUnlockSimPinTask(mGcBleTransceiver, mGcWifiTransceiver, mMessenger, mExecutor, device, pinCode);
			addTask(task);
			ret = true;
		} catch (Exception e) {
			Log.d(TAG, "[MGCC] gcUnlockSimPin exception: " + e);
		}
		
		Log.d(TAG, "[MGCC] gcUnlockSimPin--");
		return ret;
	}
}
