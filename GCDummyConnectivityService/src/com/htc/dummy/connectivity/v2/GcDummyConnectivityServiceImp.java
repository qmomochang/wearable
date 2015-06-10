package com.htc.dummy.connectivity.v2;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.text.format.Formatter;

import com.htc.gc.connectivity.v2.interfaces.IGcConnectivityService;

public abstract class GcDummyConnectivityServiceImp implements IGcConnectivityService {
	private final Context mCtx;
	private final Messenger mMessenger;
	
	private final Handler mHandler = new Handler();
	
	//private boolean mIsScanning = false;
	
	public GcDummyConnectivityServiceImp(Context context, Messenger messenger) {
		mCtx = context;
		mMessenger = messenger;
	}

	protected void processCommandResponse(final int callbackID) {
		mHandler.post(new Runnable() {

			@Override
			public void run() {
				Message message = Message.obtain();
				message.what = callbackID;
				Bundle bundle = new Bundle();
				bundle.putSerializable(IGcConnectivityService.PARAM_RESULT, Result.RESULT_SUCCESS);
				message.setData(bundle);
				try {
					mMessenger.send(message);
				} catch (RemoteException e) {
					e.printStackTrace();
				}
			}
		});		
	}
	
	protected void processBleConnect() {
		mHandler.post(new Runnable() {

			@Override
			public void run() {
				Message message = Message.obtain();
				message.what = IGcConnectivityService.CB_BLE_CONNECT_RESULT;
				Bundle bundle = new Bundle();
				bundle.putSerializable(IGcConnectivityService.PARAM_RESULT, IGcConnectivityService.Result.RESULT_SUCCESS);
				message.setData(bundle);
				try {
					mMessenger.send(message);
				} catch (RemoteException e) {
					e.printStackTrace();
				}
			}
			
		});
	}
	
	protected void processBleDisconnect() {
		mHandler.post(new Runnable() {

			@Override
			public void run() {
				Message message = Message.obtain();
				message.what = IGcConnectivityService.CB_BLE_DISCONNECT_RESULT;
				Bundle bundle = new Bundle();
				bundle.putSerializable(IGcConnectivityService.PARAM_RESULT, IGcConnectivityService.Result.RESULT_SUCCESS);
				message.setData(bundle);
				try {
					mMessenger.send(message);
				} catch (RemoteException e) {
					e.printStackTrace();
				}
			}
			
		});
	}
	
	protected void processWifiConnect() {
		WifiManager mgr = (WifiManager)mCtx.getSystemService(Context.WIFI_SERVICE);
		final String ip = Formatter.formatIpAddress(mgr.getDhcpInfo().gateway);
		
		mHandler.post(new Runnable() {

			@Override
			public void run() {
				Message message = Message.obtain();
				message.what = IGcConnectivityService.CB_WIFI_CONNECT_RESULT;
				Bundle bundle = new Bundle();
				bundle.putSerializable(IGcConnectivityService.PARAM_RESULT, IGcConnectivityService.Result.RESULT_SUCCESS);
				bundle.putString(IGcConnectivityService.PARAM_DEVICE_IP_ADDRESS, ip);
				message.setData(bundle);
				try {
					mMessenger.send(message);
				} catch (RemoteException e) {
					e.printStackTrace();
				}
			}
			
		});
	}
	
	protected void processWifiDisconnect() {
		mHandler.post(new Runnable() {

			@Override
			public void run() {
				Message message = Message.obtain();
				message.what = IGcConnectivityService.CB_WIFI_DISCONNECT_RESULT;
				Bundle bundle = new Bundle();
				bundle.putSerializable(IGcConnectivityService.PARAM_RESULT, IGcConnectivityService.Result.RESULT_SUCCESS);
				message.setData(bundle);
				try {
					mMessenger.send(message);
				} catch (RemoteException e) {
					e.printStackTrace();
				}
			}
			
		});
	}
	
	protected void processVerifyPassword() {
		mHandler.post(new Runnable() {

			@Override
			public void run() {
				Message message = Message.obtain();
				message.what = IGcConnectivityService.CB_VERIFY_PASSWORD_RESULT;
				Bundle bundle = new Bundle();
				bundle.putSerializable(IGcConnectivityService.PARAM_RESULT, IGcConnectivityService.Result.RESULT_SUCCESS);
				bundle.putSerializable(IGcConnectivityService.PARAM_VERIFY_PASSWORD_STATUS, VerifyPasswordStatus.VPSTATUS_CHANGED_AND_CORRECT);
				message.setData(bundle);
				try {
					mMessenger.send(message);
				} catch (RemoteException e) {
					e.printStackTrace();
				}
			}
			
		});		
	}
	
	protected void processGetHwStatus() {
		mHandler.post(new Runnable() {

			@Override
			public void run() {
				Message message = Message.obtain();
				message.what = IGcConnectivityService.CB_VERIFY_PASSWORD_RESULT;
				Bundle bundle = new Bundle();
				bundle.putSerializable(IGcConnectivityService.PARAM_RESULT, IGcConnectivityService.Result.RESULT_SUCCESS);
				bundle.putInt(IGcConnectivityService.PARAM_BATTERY_LEVEL, 999);
				bundle.putInt(IGcConnectivityService.PARAM_USB_STORAGE, 999);
				message.setData(bundle);
				try {
					mMessenger.send(message);
				} catch (RemoteException e) {
					e.printStackTrace();
				}
			}
			
		});		
	}
	
	protected void processOperationResponse(final Operation operation) {
		mHandler.post(new Runnable() {

			@Override
			public void run() {
				Message message = Message.obtain();
				message.what = IGcConnectivityService.CB_SET_OPERATION_RESULT;
				Bundle bundle = new Bundle();
				bundle.putSerializable(IGcConnectivityService.PARAM_RESULT, IGcConnectivityService.Result.RESULT_SUCCESS);
				bundle.putSerializable(IGcConnectivityService.PARAM_OPERATION, operation);
				bundle.putInt(IGcConnectivityService.PARAM_OPERATION_ERROR_CODE, 0x0);
				message.setData(bundle);
				try {
					mMessenger.send(message);
				} catch (RemoteException e) {
					e.printStackTrace();
				}
			}
			
		});		
	}
	
	protected void processGetGcNameResponse() {
		mHandler.post(new Runnable() {

			@Override
			public void run() {
				Message message = Message.obtain();
				message.what = IGcConnectivityService.CB_GET_NAME_RESULT;
				Bundle bundle = new Bundle();
				bundle.putSerializable(IGcConnectivityService.PARAM_RESULT, IGcConnectivityService.Result.RESULT_SUCCESS);
				bundle.putString(IGcConnectivityService.PARAM_GC_NAME, "Dummy");
				message.setData(bundle);
				try {
					mMessenger.send(message);
				} catch (RemoteException e) {
					e.printStackTrace();
				}
			}
			
		});		
	}
	
	protected void processSetAutoBackupAP() {
		mHandler.post(new Runnable() {

			@Override
			public void run() {
				Message message = Message.obtain();
				message.what = IGcConnectivityService.CB_SET_AUTO_BACKUP_AP_RESULT;
				Bundle bundle = new Bundle();
				bundle.putSerializable(IGcConnectivityService.PARAM_RESULT, IGcConnectivityService.Result.RESULT_SUCCESS);
				bundle.putInt(IGcConnectivityService.PARAM_WIFI_ERROR_CODE, 0x0);
				message.setData(bundle);
				try {
					mMessenger.send(message);
				} catch (RemoteException e) {
					e.printStackTrace();
				}
			}
			
		});		
	}
	
}
