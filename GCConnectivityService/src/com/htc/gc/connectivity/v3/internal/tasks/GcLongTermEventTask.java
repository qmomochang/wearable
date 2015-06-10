package com.htc.gc.connectivity.v3.internal.tasks;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.TimeUnit;

import com.htc.gc.connectivity.interfaces.IGcConnectivityServiceBase.LongTermEvent;
import com.htc.gc.connectivity.interfaces.IGcConnectivityServiceBase.PlugIO;
import com.htc.gc.connectivity.interfaces.IGcConnectivityServiceBase.SwitchOnOff;
import com.htc.gc.connectivity.internal.common.CommonBase.GcBleTransceiverErrorCode;
import com.htc.gc.connectivity.internal.common.CommonBase.GcWifiTransceiverErrorCode;
import com.htc.gc.connectivity.v3.IGcConnectivityServiceListener;
import com.htc.gc.connectivity.v3.interfaces.IGcConnectivityService;
import com.htc.gc.connectivity.v3.interfaces.IGcConnectivityService.MCUBatteryLevel;
import com.htc.gc.connectivity.v3.interfaces.IGcConnectivityService.OperationEvent;
import com.htc.gc.connectivity.v3.internal.common.GcConnectivityTask;
import com.htc.gc.connectivity.v3.internal.common.LongCommandCollector;
import com.htc.gc.connectivity.v3.internal.common.ReceiveSMSCollector;
import com.htc.gc.connectivity.v3.internal.component.le.GcBleGattAttributeUtil;
import com.htc.gc.connectivity.v3.internal.component.le.GcBleGattAttributes;
import com.htc.gc.connectivity.v3.internal.component.le.GcBleTransceiver;
import com.htc.gc.connectivity.v3.internal.component.le.GcBleTransceiverListener;
import com.htc.gc.connectivity.v3.internal.component.wifi.GcWifiTransceiver;
import com.htc.gc.connectivity.v3.internal.component.wifi.GcWifiTransceiverListener;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.os.Bundle;
import android.os.Message;
import android.os.Messenger;
import android.util.Log;



public class GcLongTermEventTask extends GcConnectivityTask {

	private final static String TAG = "GcLongTermEventTask";
	
	private boolean mEnable;
	private final PriorityBlockingQueue<Notification> mNotificationQueue = new PriorityBlockingQueue<Notification>();
	private final HashMap<BluetoothDevice, ArrayList<GcBleGattAttributes.GcV2CommandEnum>> mNotificationMap = new HashMap<BluetoothDevice, ArrayList<GcBleGattAttributes.GcV2CommandEnum>>();
	private final HashMap<BluetoothDevice, ArrayList<LongCommandCollector>> mLongCommandCollectorMap = new HashMap<BluetoothDevice, ArrayList<LongCommandCollector>>();
	private final IGcConnectivityServiceListener mGcConnectivityServiceListener;
	private final ReceiveSMSCollector mReceiveSMSCollector = new ReceiveSMSCollector();
	
	
	
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
	
	
	
	public void registerUuid(BluetoothDevice device, GcBleGattAttributes.GcV2CommandEnum commandID) {
		
		ArrayList<GcBleGattAttributes.GcV2CommandEnum> uuidCommandList = mNotificationMap.get(device);
		ArrayList<LongCommandCollector> collectorList = mLongCommandCollectorMap.get(device);
		
		if (uuidCommandList != null) {

			for (int cnt = 0; cnt < uuidCommandList.size(); cnt++) {
				
				if (uuidCommandList.get(cnt) == commandID) {

					return;
				}
			}
			
			uuidCommandList.add(commandID);
			
		} else {
			
			uuidCommandList = new ArrayList<GcBleGattAttributes.GcV2CommandEnum>();
			mNotificationMap.put(device, uuidCommandList);
			
			uuidCommandList.add(commandID);
		}
		
		if (GcBleGattAttributes.isLongFormat(commandID)) {
			
			if (collectorList != null) {

				for (int cnt = 0; cnt < collectorList.size(); cnt++) {
					
					if (collectorList.get(cnt).getUuid() == commandID) {

						return;
					}
				}
				
				collectorList.add(new LongCommandCollector(device, commandID));

			} else {
				
				collectorList = new ArrayList<LongCommandCollector>();
				mLongCommandCollectorMap.put(device, collectorList);

				collectorList.add(new LongCommandCollector(device, commandID));
			}
		}
	}

	
	
	public void unregisterUuid(BluetoothDevice device, GcBleGattAttributes.GcV2CommandEnum commandID) {
		
		ArrayList<GcBleGattAttributes.GcV2CommandEnum> uuidCommandList = mNotificationMap.get(device);
		ArrayList<LongCommandCollector> collectorList = mLongCommandCollectorMap.get(device);
		
		if (uuidCommandList != null) {
			
			for (int cnt = 0; cnt < uuidCommandList.size(); cnt++) {
				
				if (uuidCommandList.get(cnt) == commandID) {

					uuidCommandList.remove(cnt);
					
				}
			}
		}
		
		if (GcBleGattAttributes.isLongFormat(commandID)) {
			
			if (collectorList != null) {
				
				for (int cnt = 0; cnt < collectorList.size(); cnt++) {
					
					if (collectorList.get(cnt).getUuid() == commandID) {

						collectorList.remove(cnt);
					}
				}
			}
		}
	}

	
	
	public boolean checkUuid(BluetoothDevice device, GcBleGattAttributes.GcV2CommandEnum commandID) {
		
		boolean ret = false;
		ArrayList<GcBleGattAttributes.GcV2CommandEnum> uuidCommandList = mNotificationMap.get(device);
		
		if (uuidCommandList != null) {
			
			for (int cnt = 0; cnt < uuidCommandList.size(); cnt++) {
				
				if (uuidCommandList.get(cnt) == commandID) {

					ret = true;
				}
			}
		}
		
		return ret;
	}
	
	
	
	public LongCommandCollector getCollector(BluetoothDevice device, GcBleGattAttributes.GcV2CommandEnum commandID) {
		
		LongCommandCollector ret = null;
		
		ArrayList<LongCommandCollector> collectorList = mLongCommandCollectorMap.get(device);
		
		if (GcBleGattAttributes.isLongFormat(commandID)) {

			if (collectorList != null) {
				
				for (int cnt = 0; cnt < collectorList.size(); cnt++) {
					
					if (collectorList.get(cnt).getUuid() == commandID) {

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
			Log.d(TAG, "Chararteristic:" + characteristic.getValue());
			GcBleGattAttributes.GcV2CommandEnum command = GcBleGattAttributes.GcV2CommandEnum.findCommandID(characteristic.getValue()[0]);
			if (checkUuid(notification.mDevice, command)) {
				
				try {

					if (command == GcBleGattAttributes.GcV2CommandEnum.HWSTATUS_EVENT) {
						
						int level = GcBleGattAttributeUtil.getHwStatus_BatteryLevel(characteristic);
						int usbStorage  = GcBleGattAttributeUtil.getHwStatus_USBStatus(characteristic);
						int adapterPlugin  = GcBleGattAttributeUtil.getHwStatus_AdapterStatus(characteristic);
						
						processHwStatus(notification.mDevice, level, usbStorage, adapterPlugin, -1);

					} else if (command == GcBleGattAttributes.GcV2CommandEnum.POWER_ON_STATUS_EVENT) {

						if (characteristic.getValue().length > 0) {
							
							int value = (int)characteristic.getValue()[0];
							
							processHwStatus(notification.mDevice, -1, -1, -1, value);
						}

					} else if (command == GcBleGattAttributes.GcV2CommandEnum.REQUEST_GSP_DATE_EVENT) {

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

					} else if (command == GcBleGattAttributes.GcV2CommandEnum.GET_METADATA_EVENT) {
						
						LongCommandCollector collector = getCollector(notification.mDevice, GcBleGattAttributes.GcV2CommandEnum.GET_METADATA_EVENT);
						
						Log.d(TAG, "[MGCC] collector = " + collector);
						
						if (collector != null) {
							
							if (collector.update(notification.mDevice, characteristic)) {
								
								byte[] value = collector.get();
								String str = "";
								for (int i = 0; i < value.length; i++) {

									str = str + String.format("%02xh ", value[i]);
								}
								Log.d(TAG, "[MGCC] get meta data event, received = " + str);
								
								processMetadata(notification.mDevice, value);
								
								collector.reset();
							}
						}

					} else if (command == GcBleGattAttributes.GcV2CommandEnum.OPERATION_STATUS_EVENT) {

						processCameraStatus(notification.mDevice, characteristic.getValue());

					} else if (command == GcBleGattAttributes.GcV2CommandEnum.CAMERA_ERROR_EVENT) {

						processCameraError(notification.mDevice, characteristic.getValue());

					} /*else if (characteristic.getUuid().toString().equals(GcBleGattAttributes.GC_AUTO_BACKUP_GENERAL_RESULT)) {						

						processAutoBackupError(notification.mDevice, characteristic.getValue());

					} */else if (command == GcBleGattAttributes.GcV2CommandEnum.WIFI_SCAN_RESULT_EVENT) {
						
						LongCommandCollector collector = getCollector(notification.mDevice, GcBleGattAttributes.GcV2CommandEnum.WIFI_SCAN_RESULT_EVENT);
						
						Log.d(TAG, "[MGCC] collector = " + collector);
						
						if (collector != null) {
							
							if (collector.update(notification.mDevice, characteristic)) {
								
								byte[] value = collector.get();
								String str = "";
								for (int i = 0; i < value.length; i++) {

									str = str + String.format("%02xh ", value[i]);
								}
								Log.d(TAG, "[MGCC] wifi scan result event, received = " + str);
						
								processAPScanResult(notification.mDevice, value);
								
								collector.reset();
							}
						}
						
					} else if (command == GcBleGattAttributes.GcV2CommandEnum.GENERAL_PURPOSE_NOTIFY_EVENT) {
						
						LongCommandCollector collector = getCollector(notification.mDevice, GcBleGattAttributes.GcV2CommandEnum.GENERAL_PURPOSE_NOTIFY_EVENT);
						
						Log.d(TAG, "[MGCC] collector = " + collector);
						
						if (collector != null) {
							
							if (collector.update(notification.mDevice, characteristic)) {
								
								byte[] value = collector.get();
								String str = "";
								for (int i = 0; i < value.length; i++) {

									str = str + String.format("%02xh ", value[i]);
								}
								Log.d(TAG, "[MGCC] general purpose notify event, received = " + str);
								
								processGeneralPurposeNotify(notification.mDevice, value);
								
								collector.reset();
							}
						}
						
					} else if (command == GcBleGattAttributes.GcV2CommandEnum.GET_CAMERA_MODE_REQUEST_EVENT) {
						
						processCameraMode(notification.mDevice, characteristic.getValue());
						
					} else if (command == GcBleGattAttributes.GcV2CommandEnum.LTE_CAMPING_STATUS_REQUEST_EVENT) {
						
						processLTECampingStatus(notification.mDevice, characteristic.getValue());
						
					} else {
						
						Log.d(TAG, "[MGCC] unknown command " + command);
						
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
			
			if (level >= 1) {
				
				outData.putSerializable(IGcConnectivityService.PARAM_BATTERY_LEVEL, MCUBatteryLevel.findLevel(level));
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
	
	private final int METADATA_NO_GPS_INFO_LENGTH = 42;
	private final int METADATA_FOLDERNAME_START_INDEX = 4;
	private final int METADATA_FOLDERNAME_END_INDEX = 12;
	private final int METADATA_FILENAME_START_INDEX = 13;
	private final int METADATA_FILENAME_END_INDEX = 25;
	
	private void processMetadata(BluetoothDevice device, byte[] metadataArray) {
		
		int fileId;
		String folderName = "";
		String fileName = "";
		int fileType;
		Calendar fileCreateTime = Calendar.getInstance();
		int fileSize;
		int videoDuration;
		
		try {

			if (metadataArray.length < METADATA_NO_GPS_INFO_LENGTH) {
				
				return;
			}

			fileId = GcBleGattAttributeUtil.byteArrayToInt(metadataArray, 0);
			
			for (int cnt = METADATA_FOLDERNAME_START_INDEX; cnt <= METADATA_FOLDERNAME_END_INDEX; cnt++) {
				
				if (metadataArray[cnt] != 0x00) {
					
					folderName = folderName + String.format("%c", metadataArray[cnt]);
					
				} else {
					
					break;
				}
			}
			
			if (folderName.length() <= 0) {
				
				return;
			}
			
			for (int cnt = METADATA_FILENAME_START_INDEX; cnt <= METADATA_FILENAME_END_INDEX; cnt++) {
				
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

			eventType = statusArray[1];
			
			if ((eventType == 0x01) || (eventType == 0x03)) {

				opEvent = (eventType == 0x01) ? OperationEvent.OPEVENT_START_CAPTURING : OperationEvent.OPEVENT_START_RECORDING;
				fileType = statusArray[2];

				outData.putSerializable(IGcConnectivityService.PARAM_OPERATION_EVENT, opEvent);
				outData.putInt(IGcConnectivityService.PARAM_FILE_TYPE, fileType);
				
			} else if ((eventType == 0x02) || (eventType == 0x05)) {

				opEvent = (eventType == 0x02) ? OperationEvent.OPEVENT_COMPLETE_CAPTURING : OperationEvent.OPEVENT_COMPLETE_RECORDING;
				fileType = statusArray[2];
				readyBit = statusArray[3];
				imageRemainCount = GcBleGattAttributeUtil.byteArrayToInt(statusArray, 4);
				videoRemainSecond = GcBleGattAttributeUtil.byteArrayToInt(statusArray, 8);
				timelapseRemainCount = GcBleGattAttributeUtil.byteArrayToInt(statusArray, 12);
				slowmotionRemainSecond = GcBleGattAttributeUtil.byteArrayToInt(statusArray, 16);

				outData.putSerializable(IGcConnectivityService.PARAM_OPERATION_EVENT, opEvent);
				outData.putInt(IGcConnectivityService.PARAM_FILE_TYPE, fileType);
				outData.putInt(IGcConnectivityService.PARAM_READY_BIT, readyBit);
				outData.putInt(IGcConnectivityService.PARAM_IMAGE_REMAIN_COUNT, imageRemainCount);
				outData.putInt(IGcConnectivityService.PARAM_VIDEO_REMAIN_SECOND, videoRemainSecond);
				outData.putInt(IGcConnectivityService.PARAM_TIME_LAPSE_REMAIN_COUNT, timelapseRemainCount);
				outData.putInt(IGcConnectivityService.PARAM_SLOW_MOTION_REMAIN_SECOND, slowmotionRemainSecond);

			} else if (eventType == 0x07) {

				opEvent = OperationEvent.OPEVENT_TIME_LAPSE_CAPTURE_ONE;
				timelapseCurrentCount = GcBleGattAttributeUtil.byteArrayToInt(statusArray, 2);
				timelapseRemainCount = GcBleGattAttributeUtil.byteArrayToInt(statusArray, 6);
				timelapseTotalCount = GcBleGattAttributeUtil.byteArrayToInt(statusArray, 10);
				
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
		
		try {

			if (scanResultArray.length == 1 && scanResultArray[0] == (byte)0x80) {
				Log.d(TAG, "[MGCC] cannot find any wifi ap");
				
				Message outMsg = Message.obtain();
				outMsg.what = IGcConnectivityService.CB_LONG_TERM_EVENT_RESULT;
				Bundle outData = new Bundle();
				outData.putSerializable(IGcConnectivityService.PARAM_LONG_TERM_EVENT, LongTermEvent.LTEVENT_AUTO_BACKUP_AP_SCAN_RESULT);
				outData.putParcelable(IGcConnectivityService.PARAM_BLUETOOTH_DEVICE, device);

				outData.putBoolean(IGcConnectivityService.PARAM_AP_ANY_SCAN_RESULT, false);

				outMsg.setData(outData);
				
				mMessenger.send(outMsg);
			} else {
				int endOfScanList = ((scanResultArray[0] & 0x80) == 0x80) ? 1 : 0;
				int indexOfScanList = scanResultArray[0] & 0x7f;
				short rssi = GcBleGattAttributeUtil.byteArrayToShort(scanResultArray, 1);
				int security = scanResultArray[3];
				int authorization = scanResultArray[4];
				String apSsid = GcBleGattAttributeUtil.byteArrayToString(scanResultArray, 6, scanResultArray[5]);

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

				outData.putBoolean(IGcConnectivityService.PARAM_AP_ANY_SCAN_RESULT, true);
				outData.putInt(IGcConnectivityService.PARAM_AP_END_OF_SCAN_LIST, endOfScanList);
				outData.putInt(IGcConnectivityService.PARAM_AP_INDEX_OF_SCAN_LIST, indexOfScanList);
				outData.putShort(IGcConnectivityService.PARAM_AP_RSSI, rssi);
				outData.putInt(IGcConnectivityService.PARAM_AP_SECURITY, security);
				outData.putInt(IGcConnectivityService.PARAM_AP_AUTHORIZATION, authorization);
				outData.putString(IGcConnectivityService.PARAM_AP_SSID, apSsid);

				outMsg.setData(outData);
				
				mMessenger.send(outMsg);
			}
		} catch (Exception e) {

			e.printStackTrace();
		}
	}
	
	
	
	private void processGeneralPurposeNotify(BluetoothDevice device, byte[] dataArray) {
		
		GcGeneralPurposeCommandTask.MessageNotifyData messageNotifyData = GcGeneralPurposeCommandTask.parseMessageNotifyData(dataArray);
		final byte appId = messageNotifyData.appId;
		final String messageType = messageNotifyData.messageType;
		final String message = messageNotifyData.message;
		final byte[] messageRawData = messageNotifyData.messageRawData;
		Log.d(TAG, "[MGCC] got general purpose notify: app ID=" + appId + ", message type=" + messageType + ", message=" + message);
		
		try {
			switch (appId) {
			case GcGeneralPurposeCommandTask.APP_ID_AUTOBACKUP:
				if (messageType.equals("user")) {
					Message outMsg = Message.obtain();
					outMsg.what = IGcConnectivityService.CB_LONG_TERM_EVENT_RESULT;
					Bundle outData = new Bundle();
					outData.putSerializable(IGcConnectivityService.PARAM_LONG_TERM_EVENT, LongTermEvent.LTEVENT_AUTO_BACKUP_ERROR2);

					outData.putString(IGcConnectivityService.PARAM_AUTO_BACKUP_ERROR2_MESSAGE, message);

					outMsg.setData(outData);
					
					mMessenger.send(outMsg);
				} else {
					Log.d(TAG, "[MGCC] unknown message type:" + messageType);
				}
				break;
			case GcGeneralPurposeCommandTask.APP_ID_BROADCAST:
				switch (messageType.getBytes()[0]) {
				case (byte)0x01: { // error code 
					// messageRawData[0]  : error code
					// messageRawData[1]  : ,
					// messageRawData[2~] : timestamp
					byte errorCode = messageRawData[0];
					String errorTimestamp = null;
					try {
						errorTimestamp = new String(messageRawData, 2, messageRawData.length - 2);
					} catch (IndexOutOfBoundsException e) {
						Log.e(TAG, "[MGCC] cannot get error timestamp", e);
					}
					
					Message outMsg = Message.obtain();
					outMsg.what = IGcConnectivityService.CB_LONG_TERM_EVENT_RESULT;
					Bundle outData = new Bundle();
					outData.putSerializable(IGcConnectivityService.PARAM_LONG_TERM_EVENT, LongTermEvent.LTEVENT_BROADCAST_ERROR);
	
					outData.putByte(IGcConnectivityService.PARAM_BROADCAST_ERROR_CODE, errorCode);
					outData.putString(IGcConnectivityService.PARAM_BROADCAST_ERROR_TIMESTAMP, errorTimestamp);
	
					outMsg.setData(outData);
					
					mMessenger.send(outMsg);
				} break;
				case (byte)0x02: { // video url
					Message outMsg = Message.obtain();
					outMsg.what = IGcConnectivityService.CB_LONG_TERM_EVENT_RESULT;
					Bundle outData = new Bundle();
					outData.putSerializable(IGcConnectivityService.PARAM_LONG_TERM_EVENT, LongTermEvent.LTEVENT_BROADCAST_VIDEO_URL_RECEIVED);
	
					outData.putString(IGcConnectivityService.PARAM_BROADCAST_VIDEO_URL, message);
	
					outMsg.setData(outData);
					
					mMessenger.send(outMsg);
				} break;
				case (byte)0x03: { // live event status
					switch (messageRawData[0]) {
					case ((byte)0x1): {
						Message outMsg = Message.obtain();outMsg.what = IGcConnectivityService.CB_LONG_TERM_EVENT_RESULT;
						Bundle outData = new Bundle();
						outData.putSerializable(IGcConnectivityService.PARAM_LONG_TERM_EVENT, LongTermEvent.LTEVENT_BROADCAST_LIVE_BEGIN);
						
						outMsg.setData(outData);
						
						mMessenger.send(outMsg);
					} break;
					case ((byte)0x2): {
						Message outMsg = Message.obtain();
						outMsg.what = IGcConnectivityService.CB_LONG_TERM_EVENT_RESULT;
						Bundle outData = new Bundle();
						outData.putSerializable(IGcConnectivityService.PARAM_LONG_TERM_EVENT, LongTermEvent.LTEVENT_BROADCAST_LIVE_END);
						
						outMsg.setData(outData);
						
						mMessenger.send(outMsg);
					} break;
					default:
						Log.d(TAG, "[MGCC] unknown live event status:0x" + Integer.toHexString(messageRawData[0]));
						break;
					}
				} break;
				default:
					Log.d(TAG, "[MGCC] unknown message type:0x" + Integer.toHexString(messageType.getBytes()[0]));
					break;
				}
				break;
			case GcGeneralPurposeCommandTask.APP_ID_SMS:
				switch (messageType.getBytes()[0]) {
				case (byte) 0x01:
					mReceiveSMSCollector.collect(messageRawData);
					if (mReceiveSMSCollector.isComplete()) {
						String dateTime = mReceiveSMSCollector.getDateTime();
						String phoneNumber = mReceiveSMSCollector.getPhoneNumber();
						String messageContent = mReceiveSMSCollector.getMessageContent();
						mReceiveSMSCollector.reset();
						
						Message outMsg = Message.obtain();
						outMsg.what = IGcConnectivityService.CB_LONG_TERM_EVENT_RESULT;
						Bundle outData = new Bundle();
						outData.putSerializable(IGcConnectivityService.PARAM_LONG_TERM_EVENT, LongTermEvent.LTEVENT_SMS_RECEIVED);
						
						outData.putString(IGcConnectivityService.PARAM_SMS_DATE_TIME, dateTime);
						outData.putString(IGcConnectivityService.PARAM_SMS_PHONE_NUMBER, phoneNumber);
						outData.putString(IGcConnectivityService.PARAM_SMS_MESSAGE_CONTENT, messageContent);
						
						outMsg.setData(outData);
						
						mMessenger.send(outMsg);
					}
					break;
				default :
					Log.d(TAG, "[MGCC] unknown message type:0x" + Integer.toHexString(messageType.getBytes()[0]));
					break;
				}
				break;
			default:
				Log.d(TAG, "[MGCC] unknown app id " + appId + " for general purpose notify");
				break;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
		
	
	private void processCameraMode(BluetoothDevice device, byte[] dataArray) {
		try {

			Message outMsg = Message.obtain();
			outMsg.what = IGcConnectivityService.CB_LONG_TERM_EVENT_RESULT;
			Bundle outData = new Bundle();
			outData.putSerializable(IGcConnectivityService.PARAM_LONG_TERM_EVENT, LongTermEvent.LTEVENT_CAMERA_MODE);
			outData.putParcelable(IGcConnectivityService.PARAM_BLUETOOTH_DEVICE, device);
			outData.putSerializable(IGcConnectivityService.PARAM_CAMERA_MODE, IGcConnectivityService.CameraMode.findMode(dataArray[1]));
			
			outMsg.setData(outData);
			
			mMessenger.send(outMsg);
			
		} catch (Exception e) {

			e.printStackTrace();
		}
	}
	
	
	
	private void processLTECampingStatus(BluetoothDevice device, byte[] dataArray) {
		try {

			Message outMsg = Message.obtain();
			outMsg.what = IGcConnectivityService.CB_LONG_TERM_EVENT_RESULT;
			Bundle outData = new Bundle();
			outData.putSerializable(IGcConnectivityService.PARAM_LONG_TERM_EVENT, LongTermEvent.LTEVENT_LTE_CAMPING_STATUS);
			outData.putParcelable(IGcConnectivityService.PARAM_BLUETOOTH_DEVICE, device);
			outData.putSerializable(IGcConnectivityService.PARAM_LTE_CAMPING_STATUS, IGcConnectivityService.LTECampingStatus.findStatus(dataArray[1]));
			
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
