package com.htc.gc.connectivity.v2.internal.tasks;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.TimeUnit;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.os.Bundle;
import android.os.Message;
import android.os.Messenger;
import android.util.Log;

import com.htc.gc.connectivity.interfaces.IGcConnectivityServiceBase.LongTermEvent;
import com.htc.gc.connectivity.interfaces.IGcConnectivityServiceBase.PlugIO;
import com.htc.gc.connectivity.interfaces.IGcConnectivityServiceBase.SwitchOnOff;
import com.htc.gc.connectivity.internal.common.CommonBase.GcBleTransceiverErrorCode;
import com.htc.gc.connectivity.internal.common.CommonBase.GcWifiTransceiverErrorCode;
import com.htc.gc.connectivity.v2.IGcConnectivityServiceListener;
import com.htc.gc.connectivity.v2.interfaces.IGcConnectivityService;
import com.htc.gc.connectivity.v2.interfaces.IGcConnectivityService.OperationEvent;
import com.htc.gc.connectivity.v2.internal.common.GcConnectivityTask;
import com.htc.gc.connectivity.v2.internal.common.LongCommandCollector;
import com.htc.gc.connectivity.v2.internal.component.le.GcBleGattAttributeUtil;
import com.htc.gc.connectivity.v2.internal.component.le.GcBleGattAttributes;
import com.htc.gc.connectivity.v2.internal.component.le.GcBleTransceiver;
import com.htc.gc.connectivity.v2.internal.component.le.GcBleTransceiverListener;
import com.htc.gc.connectivity.v2.internal.component.wifi.GcWifiTransceiver;
import com.htc.gc.connectivity.v2.internal.component.wifi.GcWifiTransceiverListener;



public class GcLongTermEventTask extends GcConnectivityTask {

	private final static String TAG = "GcLongTermEventTask";
	
	private boolean mEnable;
	private final PriorityBlockingQueue<Notification> mNotificationQueue = new PriorityBlockingQueue<Notification>();
	private final HashMap<BluetoothDevice, ArrayList<String>> mNotificationMap = new HashMap<BluetoothDevice, ArrayList<String>>();
	private final HashMap<BluetoothDevice, ArrayList<LongCommandCollector>> mLongCommandCollectorMap = new HashMap<BluetoothDevice, ArrayList<LongCommandCollector>>();
	private final IGcConnectivityServiceListener mGcConnectivityServiceListener;
	
	
	
	private GcBleTransceiverListener mGcBleTransceiverListener = new GcBleTransceiverListener() {
		
		@Override
		public void onDisconnectedFromGattServer(BluetoothDevice device) {
			
			Log.d(TAG, "[MGCC] onDisconnectedFromGattServer device = " + device);

			Notification notification = new Notification(device, GcBleTransceiverErrorCode.ERROR_DISCONNECTED_FROM_GATT_SERVER);
			addNotification(notification);
		}
		
		
		
		@Override
		public void onNotificationReceive(BluetoothDevice device, BluetoothGattCharacteristic characteristic) {
			
			Log.d(TAG, "[MGCC] onNotificationReceive!!");
			
			BluetoothGattCharacteristic tempCharacteristic = new BluetoothGattCharacteristic(characteristic.getUuid(), characteristic.getProperties(), characteristic.getPermissions());
			tempCharacteristic.setValue(characteristic.getValue());
			
			Notification notification = new Notification(device, tempCharacteristic);
			addNotification(notification);
		}
		
		
		
		@Override
		public void onError(BluetoothDevice device, BluetoothGattCharacteristic characteristic, GcBleTransceiverErrorCode errorCode) {
			
			Log.d(TAG, "[MGCC] onError!!");
			
			Notification notification = new Notification(device, errorCode);
			addNotification(notification);
		}
	};
	
	
	
	private GcWifiTransceiverListener mGcWifiTransceiverListener = new GcWifiTransceiverListener() {
		
		@Override
		public void onWifiP2pDisabled() {

			Log.d(TAG, "[MGCC] onWifiP2pDisabled!!");
			Notification notification = new Notification(null, GcWifiTransceiverErrorCode.ERROR_WIFI_P2P_DISABLED);
			addNotification(notification);
		}
	};
	
	
	
	public GcLongTermEventTask(GcBleTransceiver gcBleTransceiver, GcWifiTransceiver gcWifiTransceiver, Messenger messenger, ExecutorService executor, IGcConnectivityServiceListener listener) {

		super(gcBleTransceiver, gcWifiTransceiver, messenger, executor);
		
		mGcConnectivityServiceListener = listener;
		
		setEnable(true);
	}

	
	
	@Override
	public void execute() throws Exception {
		
		super.execute();

		mGcBleTransceiver.registerListener(mGcBleTransceiverListener);
		mGcWifiTransceiver.registerListener(mGcWifiTransceiverListener);
		
		Log.d(TAG, "[MGCC] mEnable = " + mEnable);
		
		while (mEnable) {
			
			Log.d(TAG, "[MGCC] mNotificationQueue.size() = " + mNotificationQueue.size());
			Notification notification = mNotificationQueue.poll(Long.MAX_VALUE, TimeUnit.SECONDS);
			processNotification(notification);
		}
		
		mGcBleTransceiver.unregisterListener(mGcBleTransceiverListener);
		mGcWifiTransceiver.unregisterListener(mGcWifiTransceiverListener);
	}
	
	
	
	@Override
	public void error(Exception e) {

	}
	
	
	
	public void setEnable(boolean enable) {
		
		mEnable = enable;
	}
	
	
	
	public void registerUuid(BluetoothDevice device, String uuidString) {
		
		ArrayList<String> uuidStringList = mNotificationMap.get(device);
		ArrayList<LongCommandCollector> collectorList = mLongCommandCollectorMap.get(device);
		
		if (uuidStringList != null) {

			for (int cnt = 0; cnt < uuidStringList.size(); cnt++) {
				
				if (uuidStringList.get(cnt).equals(uuidString)) {

					return;
				}
			}
			
			uuidStringList.add(uuidString);
			
		} else {
			
			uuidStringList = new ArrayList<String>();
			mNotificationMap.put(device, uuidStringList);
			
			uuidStringList.add(uuidString);
		}
		
		if (GcBleGattAttributes.isLongFormat(uuidString)) {
			
			if (collectorList != null) {

				for (int cnt = 0; cnt < collectorList.size(); cnt++) {
					
					if (collectorList.get(cnt).getUuid().equals(uuidString)) {

						return;
					}
				}
				
				collectorList.add(new LongCommandCollector(device, uuidString));

			} else {
				
				collectorList = new ArrayList<LongCommandCollector>();
				mLongCommandCollectorMap.put(device, collectorList);

				collectorList.add(new LongCommandCollector(device, uuidString));
			}
		}
	}

	
	
	public void unregisterUuid(BluetoothDevice device, String uuidString) {
		
		ArrayList<String> uuidStringList = mNotificationMap.get(device);
		ArrayList<LongCommandCollector> collectorList = mLongCommandCollectorMap.get(device);
		
		if (uuidStringList != null) {
			
			for (int cnt = 0; cnt < uuidStringList.size(); cnt++) {
				
				if (uuidStringList.get(cnt).equals(uuidString)) {

					uuidStringList.remove(cnt);
					
				}
			}
		}
		
		if (GcBleGattAttributes.isLongFormat(uuidString)) {
			
			if (collectorList != null) {
				
				for (int cnt = 0; cnt < collectorList.size(); cnt++) {
					
					if (collectorList.get(cnt).getUuid().equals(uuidString)) {

						collectorList.remove(cnt);
					}
				}
			}
		}
	}

	
	
	public boolean checkUuid(BluetoothDevice device, String uuidString) {
		
		boolean ret = false;
		ArrayList<String> uuidStringList = mNotificationMap.get(device);
		
		if (uuidStringList != null) {
			
			for (int cnt = 0; cnt < uuidStringList.size(); cnt++) {
				
				if (uuidStringList.get(cnt).equals(uuidString)) {

					ret = true;
				}
			}
		}
		
		return ret;
	}
	
	
	
	public LongCommandCollector getCollector(BluetoothDevice device, String uuidString) {
		
		LongCommandCollector ret = null;
		
		ArrayList<LongCommandCollector> collectorList = mLongCommandCollectorMap.get(device);
		
		if (GcBleGattAttributes.isLongFormat(uuidString)) {

			if (collectorList != null) {
				
				for (int cnt = 0; cnt < collectorList.size(); cnt++) {
					
					if (collectorList.get(cnt).getUuid().equals(uuidString)) {

						ret = collectorList.get(cnt);
					}
				}
			}
		}
		
		return ret;
	}
	
	
	
	private synchronized void addNotification(Notification notification) {

		Log.d(TAG, "[MGCC] addNotification " + notification);

		if (notification != null) {

			mNotificationQueue.add(notification);
		}
	}
	
	
	
	private void processNotification(Notification notification) {
		
		Log.d(TAG, "[MGCC] processNotification mDevice = " + notification.mDevice + ", object = " + notification.mObject);
		
		if ((notification != null) && (notification.mObject instanceof GcBleTransceiverErrorCode)) {
			
			GcBleTransceiverErrorCode errorCode = (GcBleTransceiverErrorCode) notification.mObject;
			
			if (errorCode.equals(GcBleTransceiverErrorCode.ERROR_DISCONNECTED_FROM_GATT_SERVER)) {
				
				try {

					mGcConnectivityServiceListener.onError(881);
					
					Message outMsg = Message.obtain();
					outMsg.what = IGcConnectivityService.CB_LONG_TERM_EVENT_RESULT;
					Bundle outData = new Bundle();
					outData.putSerializable(IGcConnectivityService.PARAM_LONG_TERM_EVENT, LongTermEvent.LTEVENT_DISCONNECTED_FROM_GATT_SERVER);
					outData.putParcelable(IGcConnectivityService.PARAM_BLUETOOTH_DEVICE, notification.mDevice);
					outMsg.setData(outData);

					mMessenger.send(outMsg);
					
				} catch (Exception e) {

					e.printStackTrace();
				}
			}

		} else if ((notification != null) && (notification.mObject instanceof GcWifiTransceiverErrorCode)) {
			
			GcWifiTransceiverErrorCode errorCode = (GcWifiTransceiverErrorCode) notification.mObject;
			
			if (errorCode.equals(GcWifiTransceiverErrorCode.ERROR_WIFI_P2P_DISABLED)) {
				
				try {

					///mGcConnectivityServiceListener.onError(881);

					Message outMsg = Message.obtain();
					outMsg.what = IGcConnectivityService.CB_LONG_TERM_EVENT_RESULT;
					Bundle outData = new Bundle();
					outData.putSerializable(IGcConnectivityService.PARAM_LONG_TERM_EVENT, LongTermEvent.LTEVENT_WIFI_DISCONNECTED);
					outMsg.setData(outData);

					mMessenger.send(outMsg);
					
				} catch (Exception e) {

					e.printStackTrace();
				}
			}		
			
		} else if ((notification != null) && (notification.mObject instanceof BluetoothGattCharacteristic)) {

			BluetoothGattCharacteristic characteristic = (BluetoothGattCharacteristic) notification.mObject;
			
			if (checkUuid(notification.mDevice, characteristic.getUuid().toString())) {
				
				try {

					if (characteristic.getUuid().toString().equals(GcBleGattAttributes.GC_HW_STATUS)) {
						
						int level = GcBleGattAttributeUtil.getHwStatus(characteristic, 0, false);
						int usbStorage = GcBleGattAttributeUtil.getHwStatus(characteristic, 1, false);
						int adapterPlugin = GcBleGattAttributeUtil.getHwStatus(characteristic, 2, false);
						
						processHwStatus(notification.mDevice, level, usbStorage, adapterPlugin, -1);

					} else if (characteristic.getUuid().toString().equals(GcBleGattAttributes.GC_BOOT_UP_READY)) {

						if (characteristic.getValue().length > 0) {
							
							int value = (int)characteristic.getValue()[0];
							
							processHwStatus(notification.mDevice, -1, -1, -1, value);
						}

					} else if (characteristic.getUuid().toString().equals(GcBleGattAttributes.GC_REQUEST_GPS_DATA)) {

						boolean onoff = GcBleGattAttributeUtil.getRequestGpsInfoSwitch(characteristic);
						
						Message outMsg = Message.obtain();
						outMsg.what = IGcConnectivityService.CB_LONG_TERM_EVENT_RESULT;
						Bundle outData = new Bundle();
						outData.putSerializable(IGcConnectivityService.PARAM_LONG_TERM_EVENT, LongTermEvent.LTEVENT_REQUEST_GPS_INFO);
						outData.putParcelable(IGcConnectivityService.PARAM_BLUETOOTH_DEVICE, notification.mDevice);

						if (onoff) {
							
							outData.putSerializable(IGcConnectivityService.PARAM_REQUEST_GPS_INFO_SWITCH, SwitchOnOff.SWITCH_ON);
							
						} else {

							outData.putSerializable(IGcConnectivityService.PARAM_REQUEST_GPS_INFO_SWITCH, SwitchOnOff.SWITCH_OFF);
						}
						
						outMsg.setData(outData);

						mMessenger.send(outMsg);

					} else if (characteristic.getUuid().toString().equals(GcBleGattAttributes.GC_METADATA)) {						
						
						LongCommandCollector collector = getCollector(notification.mDevice, characteristic.getUuid().toString());
						
						Log.d(TAG, "[MGCC] collector = " + collector);
						
						if (collector != null) {
							
							if (collector.update(notification.mDevice, characteristic)) {
								
								byte[] value = collector.get();
								String str = "";
								for (int i = 0; i < value.length; i++) {

									str = str + String.format("%02xh ", value[i]);
								}
								Log.d(TAG, "[MGCC] Received = " + str);
								
								processMetadata(notification.mDevice, value);
								
								collector.reset();
							}
						}

					} else if (characteristic.getUuid().toString().equals(GcBleGattAttributes.GC_CAMERA_STATUS)) {						

						processCameraStatus(notification.mDevice, characteristic.getValue());

					} else if (characteristic.getUuid().toString().equals(GcBleGattAttributes.GC_CAMERA_ERROR)) {						

						processCameraError(notification.mDevice, characteristic.getValue());

					} else if (characteristic.getUuid().toString().equals(GcBleGattAttributes.GC_AUTO_BACKUP_GENERAL_RESULT)) {						

						processAutoBackupError(notification.mDevice, characteristic.getValue());

					} else if (characteristic.getUuid().toString().equals(GcBleGattAttributes.GC_AUTO_BACKUP_SCAN_RESULT)) {

						LongCommandCollector collector = getCollector(notification.mDevice, characteristic.getUuid().toString());
						
						Log.d(TAG, "[MGCC] collector = " + collector);
						
						if (collector != null) {
							
							if (collector.update(notification.mDevice, characteristic)) {
								
								byte[] value = collector.get();
								String str = "";
								for (int i = 0; i < value.length; i++) {

									str = str + String.format("%02xh ", value[i]);
								}
								Log.d(TAG, "[MGCC] Received = " + str);
						
								processAPScanResult(notification.mDevice, value);
								
								collector.reset();
							}
						}
						
					} else {
						
					}
					
				} catch (Exception e) {
				
					e.printStackTrace();
				}
			}
			
		}
	}
	
	
	
	private void processHwStatus(BluetoothDevice device, int level, int usbStorage, int adapterPlugin, int gcPower) {
	
		try {
			
			Message outMsg = Message.obtain();
			outMsg.what = IGcConnectivityService.CB_LONG_TERM_EVENT_RESULT;
			Bundle outData = new Bundle();
			outData.putSerializable(IGcConnectivityService.PARAM_LONG_TERM_EVENT, LongTermEvent.LTEVENT_HW_STATUS);
			outData.putParcelable(IGcConnectivityService.PARAM_BLUETOOTH_DEVICE, device);
			
			if (level >= 0) {
				
				outData.putSerializable(IGcConnectivityService.PARAM_BATTERY_LEVEL, (Integer) level);
			}

			if (usbStorage == 0) {
				
				outData.putSerializable(IGcConnectivityService.PARAM_USB_STORAGE, PlugIO.PLUG_OUT);
				
			} else if (usbStorage == 1) {
				
				outData.putSerializable(IGcConnectivityService.PARAM_USB_STORAGE, PlugIO.PLUG_IN);
			}

			if (adapterPlugin == 0) {
				
				outData.putSerializable(IGcConnectivityService.PARAM_ADAPTER_PLUGIN, PlugIO.PLUG_OUT);
				
			} else if (adapterPlugin == 1) {
				
				outData.putSerializable(IGcConnectivityService.PARAM_ADAPTER_PLUGIN, PlugIO.PLUG_IN);
			}

			if (gcPower == 0) {
				
				outData.putSerializable(IGcConnectivityService.PARAM_GC_POWER, SwitchOnOff.SWITCH_OFF);
				
			} else if (gcPower == 1) {
				
				outData.putSerializable(IGcConnectivityService.PARAM_GC_POWER, SwitchOnOff.SWITCH_ON);
			}
			
			outMsg.setData(outData);

			mMessenger.send(outMsg);
			
		} catch (Exception e) {

			e.printStackTrace();
		}
		
	}
	
	
	
	private void processMetadata(BluetoothDevice device, byte[] metadataArray) {
		
		int fileId;
		String folderName = "";
		String fileName = "";
		int fileType;
		Calendar fileCreateTime = Calendar.getInstance();
		int fileSize;
		int videoDuration;
		
		try {

			if (metadataArray.length < 42) {
				
				return;
			}

			fileId = GcBleGattAttributeUtil.byteArrayToInt(metadataArray, 0);
			
			for (int cnt = 4; cnt < 13; cnt++) {
				
				if (metadataArray[cnt] != 0x00) {
					
					folderName = folderName + String.format("%c", metadataArray[cnt]);
					
				} else {
					
					break;
				}
			}
			
			if (folderName.length() <= 0) {
				
				return;
			}
			
			for (int cnt = 13; cnt < 26; cnt++) {
				
				if (metadataArray[cnt] != 0x00) {
					
					fileName = fileName + String.format("%c", metadataArray[cnt]);
					
				} else {
					
					break;
				}
			}
			
			if (fileName.length() <= 0) {
				
				return;
			}

			fileType = metadataArray[26];
			
			int year = (metadataArray[27] & 0xff) | ((metadataArray[28] & 0xff) << 8);
			int month = metadataArray[29];
			int date = metadataArray[30];
			int hour = metadataArray[31];
			int minute = metadataArray[32];
			int second = metadataArray[33];

			Log.d(TAG, "[MGCC] year = " + year + ", month = " + month + ", date = " + date + ", hour = " + hour + ", minute = " + minute + ", second = " + second);

			if ((year < 1970) ||
				(month < 0) || (month > 11) ||
				(date < 1) || (date > 31) ||
				(hour < 0) || (hour > 23) ||
				(minute < 0) ||	(minute > 59) ||
				(second < 0) || (second > 59)) {
				
				return;
			}
			
			fileCreateTime.clear();
			fileCreateTime.set(year, month, date, hour, minute, second);
			
			fileSize = GcBleGattAttributeUtil.byteArrayToInt(metadataArray, 34);

			if (fileSize < 0) {
				
				return;
			}
			
			videoDuration = GcBleGattAttributeUtil.byteArrayToInt(metadataArray, 38);

			if (videoDuration < 0) {
				
				return;
			}
			
			Message outMsg = Message.obtain();
			outMsg.what = IGcConnectivityService.CB_LONG_TERM_EVENT_RESULT;
			Bundle outData = new Bundle();
			outData.putSerializable(IGcConnectivityService.PARAM_LONG_TERM_EVENT, LongTermEvent.LTEVENT_METADATA);
			outData.putParcelable(IGcConnectivityService.PARAM_BLUETOOTH_DEVICE, device);
			outData.putInt(IGcConnectivityService.PARAM_FILE_ID, fileId);
			outData.putString(IGcConnectivityService.PARAM_FOLDER_NAME, folderName);
			outData.putString(IGcConnectivityService.PARAM_FILE_NAME, fileName);
			outData.putInt(IGcConnectivityService.PARAM_FILE_TYPE, fileType);
			outData.putSerializable(IGcConnectivityService.PARAM_FILE_CREATE_TIME, fileCreateTime);
			outData.putInt(IGcConnectivityService.PARAM_FILE_SIZE, fileSize);
			outData.putInt(IGcConnectivityService.PARAM_VIDEO_DURATION, videoDuration);
			
			outMsg.setData(outData);
			
			mMessenger.send(outMsg);

		} catch (Exception e) {

			e.printStackTrace();
		}
	}
	
	
	
	private void processCameraStatus(BluetoothDevice device, byte[] statusArray) {
		
		OperationEvent opEvent;
		int eventType;
		int fileType;
		int readyBit;
		int imageRemainCount;
		int videoRemainSecond;
		int timelapseRemainCount;
		int timelapseTotalCount;
		int slowmotionRemainSecond;
		int timelapseCurrentCount;
		
		try {

			Message outMsg = Message.obtain();
			outMsg.what = IGcConnectivityService.CB_LONG_TERM_EVENT_RESULT;
			Bundle outData = new Bundle();
			outData.putSerializable(IGcConnectivityService.PARAM_LONG_TERM_EVENT, LongTermEvent.LTEVENT_CAMERA_STATUS);
			outData.putParcelable(IGcConnectivityService.PARAM_BLUETOOTH_DEVICE, device);

			eventType = statusArray[0];
			
			if ((eventType == 0x01) || (eventType == 0x03)) {

				opEvent = (eventType == 0x01) ? OperationEvent.OPEVENT_START_CAPTURING : OperationEvent.OPEVENT_START_RECORDING;
				fileType = statusArray[1];

				outData.putSerializable(IGcConnectivityService.PARAM_OPERATION_EVENT, opEvent);
				outData.putInt(IGcConnectivityService.PARAM_FILE_TYPE, fileType);
				
			} else if ((eventType == 0x02) || (eventType == 0x04)) {

				opEvent = (eventType == 0x02) ? OperationEvent.OPEVENT_COMPLETE_CAPTURING : OperationEvent.OPEVENT_COMPLETE_RECORDING;
				fileType = statusArray[1];
				readyBit = statusArray[2];
				imageRemainCount = GcBleGattAttributeUtil.byteArrayToInt(statusArray, 3);
				videoRemainSecond = GcBleGattAttributeUtil.byteArrayToInt(statusArray, 7);
				timelapseRemainCount = GcBleGattAttributeUtil.byteArrayToInt(statusArray, 11);
				slowmotionRemainSecond = GcBleGattAttributeUtil.byteArrayToInt(statusArray, 15);

				outData.putSerializable(IGcConnectivityService.PARAM_OPERATION_EVENT, opEvent);
				outData.putInt(IGcConnectivityService.PARAM_FILE_TYPE, fileType);
				outData.putInt(IGcConnectivityService.PARAM_READY_BIT, readyBit);
				outData.putInt(IGcConnectivityService.PARAM_IMAGE_REMAIN_COUNT, imageRemainCount);
				outData.putInt(IGcConnectivityService.PARAM_VIDEO_REMAIN_SECOND, videoRemainSecond);
				outData.putInt(IGcConnectivityService.PARAM_TIME_LAPSE_REMAIN_COUNT, timelapseRemainCount);
				outData.putInt(IGcConnectivityService.PARAM_SLOW_MOTION_REMAIN_SECOND, slowmotionRemainSecond);
				
			} else if (eventType == 0x05) {

				opEvent = OperationEvent.OPEVENT_TIME_LAPSE_CAPTURE_ONE;
				timelapseCurrentCount = GcBleGattAttributeUtil.byteArrayToInt(statusArray, 1);
				timelapseRemainCount = GcBleGattAttributeUtil.byteArrayToInt(statusArray, 5);
				timelapseTotalCount = GcBleGattAttributeUtil.byteArrayToInt(statusArray, 9);
				
				outData.putSerializable(IGcConnectivityService.PARAM_OPERATION_EVENT, opEvent);
				outData.putInt(IGcConnectivityService.PARAM_TIME_LAPSE_CURRENT_COUNT, timelapseCurrentCount);
				outData.putInt(IGcConnectivityService.PARAM_TIME_LAPSE_REMAIN_COUNT, timelapseRemainCount);
				outData.putInt(IGcConnectivityService.PARAM_TIME_LAPSE_TOTAL_COUNT, timelapseTotalCount);

			} else {
				
				return;
			}
			
			outMsg.setData(outData);
			
			mMessenger.send(outMsg);
			
		} catch (Exception e) {

			e.printStackTrace();
		}
	}
	
	
	
	private void processCameraError(BluetoothDevice device, byte[] errorArray) {
		
		Log.d(TAG, "[MGCC] processCameraError statusArray.length = " + errorArray.length);
		
		int errorIndex;
		int errorCode;
		
		try {

			Message outMsg = Message.obtain();
			outMsg.what = IGcConnectivityService.CB_LONG_TERM_EVENT_RESULT;
			Bundle outData = new Bundle();
			outData.putSerializable(IGcConnectivityService.PARAM_LONG_TERM_EVENT, LongTermEvent.LTEVENT_CAMERA_ERROR);
			outData.putParcelable(IGcConnectivityService.PARAM_BLUETOOTH_DEVICE, device);

			errorIndex = GcBleGattAttributeUtil.byteArrayToInt(errorArray, 0);
			errorCode = GcBleGattAttributeUtil.byteArrayToInt(errorArray, 4);

			outData.putInt(IGcConnectivityService.PARAM_CAMERA_ERROR_INDEX, errorIndex);
			outData.putInt(IGcConnectivityService.PARAM_CAMERA_ERROR_CODE, errorCode);

			outMsg.setData(outData);
			
			mMessenger.send(outMsg);
			
		} catch (Exception e) {

			e.printStackTrace();
		}
	}

	
	
	private void processAutoBackupError(BluetoothDevice device, byte[] abErrorArray) {
		
		try {

			int type = abErrorArray[0];
			
			if (type == 0) {

				/// APP error code
				Message outMsg = Message.obtain();
				outMsg.what = IGcConnectivityService.CB_LONG_TERM_EVENT_RESULT;
				Bundle outData = new Bundle();
				
				int errorCode = (int) GcBleGattAttributeUtil.byteArrayToShort(abErrorArray, 1);
				
				if (errorCode == 1) {
					
					SwitchOnOff onoff = SwitchOnOff.SWITCH_ON;

					outData.putSerializable(IGcConnectivityService.PARAM_LONG_TERM_EVENT, LongTermEvent.LTEVENT_HOTSPOT_CONTROL);
					outData.putParcelable(IGcConnectivityService.PARAM_BLUETOOTH_DEVICE, device);
					outData.putSerializable(IGcConnectivityService.PARAM_SWITCH_ON_OFF, onoff);
					
				} else if (errorCode == 2) {
					
					SwitchOnOff onoff = SwitchOnOff.SWITCH_OFF;

					outData.putSerializable(IGcConnectivityService.PARAM_LONG_TERM_EVENT, LongTermEvent.LTEVENT_HOTSPOT_CONTROL);
					outData.putParcelable(IGcConnectivityService.PARAM_BLUETOOTH_DEVICE, device);
					outData.putSerializable(IGcConnectivityService.PARAM_SWITCH_ON_OFF, onoff);

				} else {

					outData.putSerializable(IGcConnectivityService.PARAM_LONG_TERM_EVENT, LongTermEvent.LTEVENT_AUTO_BACKUP_ERROR);
					outData.putParcelable(IGcConnectivityService.PARAM_BLUETOOTH_DEVICE, device);
					outData.putInt(IGcConnectivityService.PARAM_AUTO_BACKUP_ERROR_TYPE, type);
					outData.putInt(IGcConnectivityService.PARAM_AUTO_BACKUP_ERROR_CODE, errorCode);
				}

				outMsg.setData(outData);
				mMessenger.send(outMsg);

			} else if (type == 1) {
				
				/// Progress
				byte [] generalResultArray = abErrorArray;
				int errorCode = generalResultArray[1];
				
				if (errorCode == 0x00) {
					
					int remainFileCount = GcBleGattAttributeUtil.byteArrayToInt(generalResultArray, 3);
					int totalFileCount = GcBleGattAttributeUtil.byteArrayToInt(generalResultArray, 7);

					Log.d(TAG, "[MGCC] remainFileCount = " + remainFileCount);
					Log.d(TAG, "[MGCC] totalFileCount = " + totalFileCount);
					
					Message outMsg = Message.obtain();
					outMsg.what = IGcConnectivityService.CB_LONG_TERM_EVENT_RESULT;
					Bundle outData = new Bundle();
					outData.putSerializable(IGcConnectivityService.PARAM_LONG_TERM_EVENT, LongTermEvent.LTEVENT_AUTO_BACKUP_PROGRESS);
					outData.putParcelable(IGcConnectivityService.PARAM_BLUETOOTH_DEVICE, device);
					outData.putInt(IGcConnectivityService.PARAM_REMAIN_FILE_COUNT, remainFileCount);
					outData.putInt(IGcConnectivityService.PARAM_TOTAL_FILE_COUNT, totalFileCount);
					
					outMsg.setData(outData);
					mMessenger.send(outMsg);
				}
				
			} else if (type == 2) {
				
				/// CS error code
				Message outMsg = Message.obtain();
				outMsg.what = IGcConnectivityService.CB_LONG_TERM_EVENT_RESULT;
				Bundle outData = new Bundle();
				
				int errorCode = (int) GcBleGattAttributeUtil.byteArrayToShort(abErrorArray, 1);
				
				outData.putSerializable(IGcConnectivityService.PARAM_LONG_TERM_EVENT, LongTermEvent.LTEVENT_AUTO_BACKUP_ERROR);
				outData.putParcelable(IGcConnectivityService.PARAM_BLUETOOTH_DEVICE, device);
				outData.putInt(IGcConnectivityService.PARAM_AUTO_BACKUP_ERROR_TYPE, type);
				outData.putInt(IGcConnectivityService.PARAM_AUTO_BACKUP_ERROR_CODE, errorCode);

				outMsg.setData(outData);
				mMessenger.send(outMsg);
			}

		} catch (Exception e) {

			e.printStackTrace();
		}
	}
	
	
	
	private void processAPScanResult(BluetoothDevice device, byte[] scanResultArray) {
		
		int endOfScanList;
		int indexOfScanList;
		short rssi;
		int security;
		int authorization;
		String apSsid;
		
		try {

			endOfScanList = ((scanResultArray[0] & 0x80) == 0x80) ? 1 : 0;
			indexOfScanList = scanResultArray[0] & 0x7f;
			rssi = GcBleGattAttributeUtil.byteArrayToShort(scanResultArray, 1);
			security = scanResultArray[3];
			authorization = scanResultArray[4];
			apSsid = GcBleGattAttributeUtil.byteArrayToString(scanResultArray, 6, scanResultArray[5]);

			Log.d(TAG, "[MGCC] endOfScanList = " + endOfScanList);
			Log.d(TAG, "[MGCC] indexOfScanList = " + indexOfScanList);
			Log.d(TAG, "[MGCC] rssi = " + rssi);
			Log.d(TAG, "[MGCC] security = " + security);
			Log.d(TAG, "[MGCC] authorization = " + authorization);
			Log.d(TAG, "[MGCC] apSsid = " + apSsid);

			Message outMsg = Message.obtain();
			outMsg.what = IGcConnectivityService.CB_LONG_TERM_EVENT_RESULT;
			Bundle outData = new Bundle();
			outData.putSerializable(IGcConnectivityService.PARAM_LONG_TERM_EVENT, LongTermEvent.LTEVENT_AUTO_BACKUP_AP_SCAN_RESULT);
			outData.putParcelable(IGcConnectivityService.PARAM_BLUETOOTH_DEVICE, device);

			outData.putInt(IGcConnectivityService.PARAM_AP_END_OF_SCAN_LIST, endOfScanList);
			outData.putInt(IGcConnectivityService.PARAM_AP_INDEX_OF_SCAN_LIST, indexOfScanList);
			outData.putShort(IGcConnectivityService.PARAM_AP_RSSI, rssi);
			outData.putInt(IGcConnectivityService.PARAM_AP_SECURITY, security);
			outData.putInt(IGcConnectivityService.PARAM_AP_AUTHORIZATION, authorization);
			outData.putString(IGcConnectivityService.PARAM_AP_SSID, apSsid);

			outMsg.setData(outData);
			
			mMessenger.send(outMsg);
			
		} catch (Exception e) {

			e.printStackTrace();
		}
	}
	
	
	
	private class Notification implements Comparable<Notification> {
		
		public final BluetoothDevice mDevice;
		public final Object mObject;
		
		public Notification(BluetoothDevice device, Object object) {
			
			mDevice = device;
			mObject = object;
		}

		
		
		@Override
		public int compareTo(Notification another) {

			return 0;
		}
	}
}
