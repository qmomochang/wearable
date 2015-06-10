package com.htc.dummy.connectivity.v2;

import java.util.Calendar;

import com.htc.gc.connectivity.v2.interfaces.IGcConnectivityService;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.os.Messenger;

public class GcDummyConnectivityService extends GcDummyConnectivityServiceImp {
	
	public GcDummyConnectivityService(Context context, Messenger messenger) {
		super(context, messenger);
	}
	
	@Override
	public boolean gcOpen() {
		return true;
	}

	@Override
	public boolean gcClose() {
		return true;
	}

	@Override
	public boolean gcBleConnect(BluetoothDevice device) {
		processBleConnect();
		return true;
	}

	@Override
	public boolean gcBleDisconnect(BluetoothDevice device) {
		processBleDisconnect();
		return true;
	}
	
	@Override
	public boolean gcBleDisconnectForce(BluetoothDevice device) {
		return false;
	}

	@Override
	public boolean gcWifiConnect(BluetoothDevice device) {
		processWifiConnect();
		return true;
	}

	@Override
	public boolean gcWifiDisconnect(BluetoothDevice device) {
		processWifiDisconnect();
		return true;
	}

	@Override
	public boolean gcSetPowerOnOff(BluetoothDevice device, Module module,
			SwitchOnOff onoff) {
		return false;
	}

	@Override
	public boolean gcGetPowerOnOff(BluetoothDevice device, Module module) {
		return false;
	}

	@Override
	public boolean gcSetDateTime(BluetoothDevice device, Calendar calendar) {
		processCommandResponse(IGcConnectivityService.CB_SET_DATE_TIME_RESULT);
		return true;
	}

	@Override
	public boolean gcSetName(BluetoothDevice device, String name) {
		processCommandResponse(IGcConnectivityService.CB_SET_NAME_RESULT);
		return true;
	}

	@Override
	public boolean gcSetGpsInfo(BluetoothDevice device, Calendar calendar, double longitude, double latitude, double altitude) {
		processCommandResponse(IGcConnectivityService.CB_SET_GPS_INFO_RESULT);
		return true;
	}

	@Override
	public boolean gcGetBleFWVersion(BluetoothDevice device) {
		return true;
	}

	@Override
	public boolean gcVerifyPassword(BluetoothDevice device, String password) {
		processVerifyPassword();
		return true;
	}

	@Override
	public boolean gcChangePassword(BluetoothDevice device, String password) {
		processCommandResponse(IGcConnectivityService.CB_CHANGE_PASSWORD_RESULT);
		return true;
	}

	@Override
	public boolean gcGetHwStatus(BluetoothDevice device) {
		processGetHwStatus();
		return true;
	}

	@Override
	public boolean gcSetHwStatusLTEvent(BluetoothDevice device) {
		processCommandResponse(IGcConnectivityService.CB_SET_HW_STATUS_LTEVENT_RESULT);
		return true;
	}

	@Override
	public boolean gcClrHwStatusLTEvent(BluetoothDevice device) {
		processCommandResponse(IGcConnectivityService.CB_CLR_HW_STATUS_LTEVENT_RESULT);
		return true;
	}

	@Override
	public boolean gcSetGpsInfoLTEvent(BluetoothDevice device) {
		processCommandResponse(IGcConnectivityService.CB_SET_GPS_INFO_LTEVENT_RESULT);
		return true;
	}

	@Override
	public boolean gcClrGpsInfoLTEvent(BluetoothDevice device) {
		processCommandResponse(IGcConnectivityService.CB_CLR_GPS_INFO_LTEVENT_RESULT);
		return true;
	}

	@Override
	public boolean gcSetOperationLTEvent(BluetoothDevice device) {
		processCommandResponse(IGcConnectivityService.CB_SET_OPERATION_LTEVENT_RESULT);
		return true;
	}

	@Override
	public boolean gcClrOperationLTEvent(BluetoothDevice device) {
		processCommandResponse(IGcConnectivityService.CB_CLR_OPERATION_LTEVENT_RESULT);
		return true;
	}

	@Override
	public boolean gcSetOperation(BluetoothDevice device, Operation operation) {
		processOperationResponse(operation);
		return true;
	}

	@Override
	public boolean gcSetMetadataLTEvent(BluetoothDevice device) {
		processCommandResponse(IGcConnectivityService.CB_SET_METADATA_LTEVENT_RESULT);
		return true;
	}

	@Override
	public boolean gcClrMetadataLTEvent(BluetoothDevice device) {
		processCommandResponse(IGcConnectivityService.CB_CLR_METADATA_LTEVENT_RESULT);
		return true;
	}

	@Override
	public boolean gcSetCameraErrorLTEvent(BluetoothDevice device) {
		processCommandResponse(IGcConnectivityService.CB_SET_CAMERA_ERROR_LTEVENT_RESULT);
		return true;
	}

	@Override
	public boolean gcClrCameraErrorLTEvent(BluetoothDevice device) {
		processCommandResponse(IGcConnectivityService.CB_CLR_CAMERA_ERROR_LTEVENT_RESULT);
		return true;
	}

	@Override
	public boolean gcGetName(BluetoothDevice device) {
		processGetGcNameResponse();
		return true;
	}

	@Override
	public boolean gcSoftAPConnect(BluetoothDevice device, String passwd) {
		processWifiConnect();
		return true;
	}

	@Override
	public boolean gcSetAutoBackupLTEvent(BluetoothDevice device) {
		processCommandResponse(IGcConnectivityService.CB_SET_AUTO_BACKUP_LTEVENT_RESULT);
		return true;
	}

	@Override
	public boolean gcClrAutoBackupLTEvent(BluetoothDevice device) {
		processCommandResponse(IGcConnectivityService.CB_CLR_AUTO_BACKUP_LTEVENT_RESULT);
		return true;
	}

	@Override
	public boolean gcSetAutoBackupAP(BluetoothDevice device, WifiAP ap,
			String ssid, String passwd, byte security) {
		processSetAutoBackupAP();
		return true;
	}

	@Override
	public boolean gcClrAutoBackupAP(BluetoothDevice device, byte security, String ssid) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean gcSetLTNotify(BluetoothDevice device) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean gcClrLTNotify(BluetoothDevice device) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean gcSetAutoBackupAPScan(BluetoothDevice device, int option) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean gcSetAutoBackupProxy(BluetoothDevice device, int port, byte security,
			String ssid, String proxy) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean gcGetAutoBackupProxy(BluetoothDevice device, byte security,
			String ssid) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean gcCreateWifiP2pGroup() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean gcRemoveWifiP2pGroup() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean gcRemoveWifiP2pGroupForce() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean gcGetAllFwVersion(BluetoothDevice device) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean gcGetAutoBackupStatus(BluetoothDevice device) {
		// TODO Auto-generated method stub
		return false;
	}

}
