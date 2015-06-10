package com.htc.gc.connectivity.v3.internal.component.wifi;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import com.htc.gc.connectivity.internal.common.CommonBase.GcWifiTransceiverErrorCode;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.NetworkInfo;
import android.net.DhcpInfo;
import android.net.NetworkInfo.DetailedState;
import android.net.wifi.ScanResult;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.ActionListener;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.net.wifi.p2p.WifiP2pManager.GroupInfoListener;
import android.os.Handler;
import android.os.Looper;
import android.os.Parcelable;
import android.text.format.Formatter;
import android.util.Log;
import android.util.SparseArray;



public class GcWifiTransceiver {

    private final static String TAG = "GcWifiTransceiver";
    private final static int CREATE_P2P_GROUP_RETRY_TIMES_DEFAULT = 5;
    private final static int REMOVE_P2P_GROUP_RETRY_TIMES_DEFAULT = 5;
    private final static int REQUEST_P2P_GROUP_INFO_RETRY_TIMES_DEFAULT = 20;
    
    private Context mContext;
    private WifiManager mWifiManager;
    private WifiP2pManager mWifiP2pManager;
    private Channel mChannel;
    private String mDirectNetworkName;
    private String mDirectPassword;
    private BroadcastReceiver mBroadcastReceiver = null;
    private LinkedList<GcWifiTransceiverListener> mListeners = new LinkedList<GcWifiTransceiverListener>();
    private Handler mHandler;
    private WifiP2pGroupState mP2pGroupState = WifiP2pGroupState.STATE_P2P_GROUP_REMOVED;
    private int mCreateP2pGroupRetryTimes = 0;
    private int mRequestP2pGroupInfoRetryTimes = 0;
    private int mRemoveP2pGroupRetryTimes = 0;
    private int mWifiP2pGroupCreatingStepCnt = 0;
    private int mWifiP2pGroupRemovingStepCnt = 0;
    private SparseArray<Byte> tranChannel24G = new SparseArray<Byte>();
    private SparseArray<Byte> tranChannel5G  = new SparseArray<Byte>();
    private byte mBandSelect = 0; //0: 2.4G, 1: 5G
    private byte mWifiChannel = 0;
    private byte[] mStaticIP = new byte[4];
    private AtomicBoolean mCanSkipRemovingP2pGroup = new AtomicBoolean(false);

    
	public enum WifiP2pGroupState {

		STATE_P2P_GROUP_REMOVED,
		STATE_P2P_GROUP_REMOVING,
		STATE_P2P_GROUP_CREATED,
		STATE_P2P_GROUP_CREATING,
	}

    
    
    public GcWifiTransceiver(Context context) throws Exception {

    	Log.d(TAG, "[MGCC] onCreate()");
    	
    	mContext = context;
    	
		mHandler = new Handler(Looper.getMainLooper());
		
		if (!init()) {
			
			throw new Exception("GcWifiTransceiver init fail!!");
		}
    }
    

    
    public boolean init() {

    	init_ChannelTable();

		if (mWifiManager == null) {
			
			mWifiManager = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
		}
		
		///if (!setWifiEnable()) {
			
			///Log.d(TAG, "[MGCC] Unable to set Wifi enable.");
			///return false;
		///}

        if (mWifiP2pManager == null) {
        	
        	mWifiP2pManager = (WifiP2pManager) mContext.getSystemService(Context.WIFI_P2P_SERVICE);
        	
            if (mWifiP2pManager != null) {
            	
            	mChannel = mWifiP2pManager.initialize(mContext, mContext.getMainLooper(), null);

            	///removeGroup();
            	
            } else {

            	Log.d(TAG, "[MGCC] Unable to initialize WifiP2pManager.");
                return false;
            }
        }

        if (mBroadcastReceiver == null) {

            mBroadcastReceiver = new GcWifiBroadcastReceiver(mWifiP2pManager);
            mContext.registerReceiver(mBroadcastReceiver, makeWifiIntentFilter());
        }
        
        return true;
	}
	
	
	
	public void deInit() {

		if(mBroadcastReceiver != null) {

			mContext.unregisterReceiver(mBroadcastReceiver);
		}
	}

	
	
    public void registerListener(GcWifiTransceiverListener listener) {
    	
    	synchronized(mListeners) {

			mListeners.add(listener);
		}
    	
    	Log.d(TAG, "[MGCC] After registerListener mListeners.size() = " + mListeners.size());
    }

    
    
    public void unregisterListener(GcWifiTransceiverListener listener) {
    	
    	synchronized(mListeners) {

			mListeners.remove(listener);
    	}

    	Log.d(TAG, "[MGCC] After unregisterListener mListeners.size() = " + mListeners.size());
    }
	

    
	public boolean createGroup() {

		mCreateP2pGroupRetryTimes = CREATE_P2P_GROUP_RETRY_TIMES_DEFAULT;
		
		return createWifiP2pGroup();
	}

	
	
	public boolean removeGroup() {

		mRemoveP2pGroupRetryTimes = REMOVE_P2P_GROUP_RETRY_TIMES_DEFAULT;
		
		return removeWifiP2pGroup();
	}
    
    
    
	private boolean createWifiP2pGroup() {
		
		Log.d(TAG, "[MGCC] createWifiP2pGroup");

		if (!setWifiEnable()) {
			return false;
		}
		
		// cannot skip removing p2p group once it tries to create p2p group
		mCanSkipRemovingP2pGroup.set(false);
		
		Log.d(TAG, "[MGCC] mCreateP2pGroupRetryTimes = " + mCreateP2pGroupRetryTimes);
		if (mCreateP2pGroupRetryTimes > 0) {
			
			mCreateP2pGroupRetryTimes--;
			
		} else {
			
			return false;
		}

		if (!getP2pGroupState().equals(WifiP2pGroupState.STATE_P2P_GROUP_CREATING)) {
			
			setP2pGroupState(WifiP2pGroupState.STATE_P2P_GROUP_CREATING);
		}

		mWifiP2pManager.requestGroupInfo(mChannel, new GroupInfoListener() {

			@Override
			public void onGroupInfoAvailable(WifiP2pGroup group) {

				if ((group != null) && (group.getNetworkName().contains("DIRECT"))) {
					
					Log.d(TAG, "[MGCC] createWifiP2pGroup group is already exist");

					Log.d(TAG, "[MGCC] THE NETWORK NAME: "+ group.getNetworkName());
					Log.d(TAG, "[MGCC] DIRECT PASSWORD: " + group.getPassphrase());

					Log.d(TAG, "[MGCC] wifip2pGroup speed up+");
					prepareStaticIP(group);
					prepareWifiChannel(group);
					Log.d(TAG, "[MGCC] wifip2pGroup speed up-");
					
					setGroupInfo(group.getNetworkName(), group.getPassphrase());
					
					setP2pGroupState(WifiP2pGroupState.STATE_P2P_GROUP_CREATED);
					
            		final LinkedList<GcWifiTransceiverListener> listeners;
            		synchronized(mListeners){

            			listeners = new LinkedList<GcWifiTransceiverListener>(mListeners);
            		}
            		
                    for (GcWifiTransceiverListener listener : listeners) {
                    	
                    	listener.onWifiDirectGroupCreated();
                    }

				} else {
					
					if (!getP2pGroupState().equals(WifiP2pGroupState.STATE_P2P_GROUP_CREATING)) {
					
						setP2pGroupState(WifiP2pGroupState.STATE_P2P_GROUP_CREATING);
					}
					
					mWifiP2pGroupCreatingStepCnt = 0;
					
					mWifiP2pManager.createGroup(mChannel, new ActionListener() {

						@Override
						public void onSuccess() {

							Log.d(TAG, "[MGCC] createWifiP2pGroup onSuccess");

							mRequestP2pGroupInfoRetryTimes = REQUEST_P2P_GROUP_INFO_RETRY_TIMES_DEFAULT;
							
							mWifiP2pGroupCreatingStepCnt++;
							
							Log.d(TAG, "[MGCC] mWifiP2pGroupCreatingStepCnt = " + mWifiP2pGroupCreatingStepCnt);
							if (mWifiP2pGroupCreatingStepCnt == 2) {
								
								requestGroupInfo();
							}
						}

						@Override
						public void onFailure(int reason) {
							
							Log.d(TAG, "[MGCC] createWifiP2pGroup onFailure reason = " + reason);
							
							if (reason == WifiP2pManager.BUSY) {

								mHandler.postDelayed(new Runnable() {

									@Override
									public void run() {

										if (!createWifiP2pGroup()) {
											
						            		final LinkedList<GcWifiTransceiverListener> listeners;
						            		synchronized(mListeners){

						            			listeners = new LinkedList<GcWifiTransceiverListener>(mListeners);
						            		}
						            		
						                    for (GcWifiTransceiverListener listener : listeners) {
						                    	
						                    	listener.onError(GcWifiTransceiverErrorCode.ERROR_WIFI_P2P_GROUP);
						                    }
										}
									}

								}, 500);

							} else {
								
			            		final LinkedList<GcWifiTransceiverListener> listeners;
			            		synchronized(mListeners){

			            			listeners = new LinkedList<GcWifiTransceiverListener>(mListeners);
			            		}
			            		
			                    for (GcWifiTransceiverListener listener : listeners) {
			                    	
			                    	listener.onError(GcWifiTransceiverErrorCode.ERROR_WIFI_P2P_GROUP);
			                    }
							}
						}
					});
				}
			}
		});
		
		return true;
	}
    
	
	
	private boolean removeWifiP2pGroup() {
		
		Log.d(TAG, "[MGCC] removeWifiP2pGroup");
		
		if (!setWifiEnable()) {
			return false;
		}
		
		if (mCanSkipRemovingP2pGroup.get()) {
			Log.d(TAG, "[MGCC] can skip removing p2p group");
			
			mHandler.postDelayed(new Runnable(){

				@Override
				public void run() {
					Log.d(TAG, "[MGCC] skip removing p2p group, invoke callback directly");
					
					setP2pGroupState(WifiP2pGroupState.STATE_P2P_GROUP_REMOVED);
					
					final LinkedList<GcWifiTransceiverListener> listeners;
	        		synchronized(mListeners){

	        			listeners = new LinkedList<GcWifiTransceiverListener>(mListeners);
	        		}
	        		
	                for (GcWifiTransceiverListener listener : listeners) {
	                	
	                	listener.onWifiDirectGroupRemoved();
	                }
				}
			}, 0);
			
			return true;
		}

		Log.d(TAG, "[MGCC] mRemoveP2pGroupRetryTimes = " + mRemoveP2pGroupRetryTimes);
		if (mRemoveP2pGroupRetryTimes > 0) {
			
			mRemoveP2pGroupRetryTimes--;
			
		} else {
			
			return false;
		}
		
		if (!getP2pGroupState().equals(WifiP2pGroupState.STATE_P2P_GROUP_REMOVING)) {
			
			setP2pGroupState(WifiP2pGroupState.STATE_P2P_GROUP_REMOVING);
		}

		mWifiP2pManager.requestGroupInfo(mChannel, new GroupInfoListener() {

			@Override
			public void onGroupInfoAvailable(WifiP2pGroup group) {

				if ((group != null) && (group.getNetworkName().contains("DIRECT"))) {
					
					if (!getP2pGroupState().equals(WifiP2pGroupState.STATE_P2P_GROUP_REMOVING)) {
						
						setP2pGroupState(WifiP2pGroupState.STATE_P2P_GROUP_REMOVING);
					}

					mWifiP2pGroupRemovingStepCnt = 0;
					
					mWifiP2pManager.removeGroup(mChannel, new ActionListener() {

						@Override
						public void onSuccess() {
							
							Log.d(TAG, "[MGCC] removeWifiP2pGroup onSuccess");

							mWifiP2pGroupRemovingStepCnt++;
							
							Log.d(TAG, "[MGCC] mWifiP2pGroupRemovingStepCnt = " + mWifiP2pGroupRemovingStepCnt);
							if (mWifiP2pGroupRemovingStepCnt == 2) {
								
								setP2pGroupState(WifiP2pGroupState.STATE_P2P_GROUP_REMOVED);
								
				        		final LinkedList<GcWifiTransceiverListener> listeners;
				        		synchronized(mListeners){

				        			listeners = new LinkedList<GcWifiTransceiverListener>(mListeners);
				        		}
				        		
				                for (GcWifiTransceiverListener listener : listeners) {
				                	
				                	listener.onWifiDirectGroupRemoved();
				                }
				                
				                mWifiP2pGroupRemovingStepCnt = 0;
				                mCanSkipRemovingP2pGroup.set(true);
							}
						}

						@Override
						public void onFailure(int reason) {
							
							Log.d(TAG, "[MGCC] removeWifiP2pGroup onFailure reason = " + reason);
							
							if (reason == WifiP2pManager.BUSY) {

								mHandler.postDelayed(new Runnable() {

									@Override
									public void run() {

										if (!removeWifiP2pGroup()) {
											
						            		final LinkedList<GcWifiTransceiverListener> listeners;
						            		synchronized(mListeners){

						            			listeners = new LinkedList<GcWifiTransceiverListener>(mListeners);
						            		}
						            		
						                    for (GcWifiTransceiverListener listener : listeners) {
						                    	
						                    	listener.onError(GcWifiTransceiverErrorCode.ERROR_WIFI_P2P_GROUP);
						                    }
										}
									}

								}, 500);

							} else {
								
								setP2pGroupState(WifiP2pGroupState.STATE_P2P_GROUP_REMOVED);
								
				        		final LinkedList<GcWifiTransceiverListener> listeners;
				        		synchronized(mListeners){

				        			listeners = new LinkedList<GcWifiTransceiverListener>(mListeners);
				        		}
				        		
				                for (GcWifiTransceiverListener listener : listeners) {
				                	
				                	listener.onError(GcWifiTransceiverErrorCode.ERROR_WIFI_P2P_GROUP);
				                }
							}
						}
					});
					
				} else {
					
					Log.d(TAG, "[MGCC] removeWifiP2pGroup group is null.");
					setP2pGroupState(WifiP2pGroupState.STATE_P2P_GROUP_REMOVED);
					
	        		final LinkedList<GcWifiTransceiverListener> listeners;
	        		synchronized(mListeners){

	        			listeners = new LinkedList<GcWifiTransceiverListener>(mListeners);
	        		}
	        		
	                for (GcWifiTransceiverListener listener : listeners) {
	                	
	                	listener.onWifiDirectGroupRemoved();
	                }
	                
	                mCanSkipRemovingP2pGroup.set(true);
				}
			}
		});
		
		return true;
	}
	
	
	
	private void requestGroupInfo() {

		Log.d(TAG, "[MGCC] Run requestGroupInfo on inner thread");

		class InnerThread implements Runnable {

			@Override
			public void run() {

				Log.d(TAG, "[MGCC] Requesting group info...");
				
				mWifiP2pManager.requestGroupInfo(mChannel, new GroupInfoListener() {

					@Override
					public void onGroupInfoAvailable(WifiP2pGroup group) {

						Log.d(TAG, "[MGCC] onGroupInfoAvailable group = " + group);
						
						if (group != null) {

							Log.d(TAG, "[MGCC] THE NETWORK NAME: "+ group.getNetworkName());
							Log.d(TAG, "[MGCC] DIRECT PASSWORD: " + group.getPassphrase());

							Log.d(TAG, "[MGCC] wifip2pGroup speed up+");
							prepareStaticIP(group);
							prepareWifiChannel(group);
							Log.d(TAG, "[MGCC] wifip2pGroup speed up-");							
							
							setGroupInfo(group.getNetworkName(), group.getPassphrase());
							
							setP2pGroupState(WifiP2pGroupState.STATE_P2P_GROUP_CREATED);
							mWifiP2pGroupCreatingStepCnt = 0;

                    		final LinkedList<GcWifiTransceiverListener> listeners;
                    		synchronized(mListeners){

                    			listeners = new LinkedList<GcWifiTransceiverListener>(mListeners);
                    		}
                    		
                            for (GcWifiTransceiverListener listener : listeners) {
                            	
                            	listener.onWifiDirectGroupCreated();
                            }
					        
						} else {
							
							requestGroupInfo();
						}
						
					}
				});
			}
		}

		long delayTime = 100;
		Log.d(TAG, "[MGCC] mRequestP2pGroupInfoRetryTimes = " + mRequestP2pGroupInfoRetryTimes);
		if (mRequestP2pGroupInfoRetryTimes > 0) {

			if (mRequestP2pGroupInfoRetryTimes == REQUEST_P2P_GROUP_INFO_RETRY_TIMES_DEFAULT) {
				
				delayTime = 0;
			}

			mRequestP2pGroupInfoRetryTimes--;
			
		} else {
			
    		final LinkedList<GcWifiTransceiverListener> listeners;
    		synchronized(mListeners){

    			listeners = new LinkedList<GcWifiTransceiverListener>(mListeners);
    		}
    		
            for (GcWifiTransceiverListener listener : listeners) {
            	
            	listener.onError(GcWifiTransceiverErrorCode.ERROR_WIFI_P2P_GROUP);
            }
            
            return;
		}

		mHandler.postDelayed(new Runnable() {

			@Override
			public void run() {

				Thread innerThread = new Thread(new InnerThread());
				innerThread.start();
			}

		}, delayTime);
	}
	
	
	
	private void setGroupInfo(String name, String password) {
		
		mDirectNetworkName = name;
		mDirectPassword = password;
	}
	
	
	
	private boolean setWifiEnable() {
		
		boolean ret = false;
		
		if (mWifiManager != null) {
			
	        if (mWifiManager.isWifiEnabled()) {

	        	ret = true;

	        } else {

	        	Log.d(TAG, "[MGCC] WiFi is DISABLED. Please enable it.");

	        	/// Do NOT turn WiFi automatically.
	        	///if (mWifiManager.setWifiEnabled(true)) {
	        		
	        		///ret = true;
	        	///}
	        }
		}
		
		return ret;
	}
	
	
	
    synchronized public WifiP2pGroupState getP2pGroupState() {
    	
    	return mP2pGroupState;
    }

    
    
    synchronized private void setP2pGroupState(WifiP2pGroupState state) {
    	
    	Log.d(TAG, "[MGCC] setP2pGroupState: " + mP2pGroupState + " --> " + state);
    	mP2pGroupState = state;
    }

	
    
	public String getGroupName() {
		
		return mDirectNetworkName;
	}

	public byte[] getStaticIP()
	{
		return mStaticIP;
	}
	
	
	public String getGroupPassword() {
		
		return mDirectPassword;
	}

	public byte getWifiChannel()
	{
		return mWifiChannel;
	}
	
	public byte getBandSelect()
	{
		return mBandSelect;
	}
	
	public static IntentFilter makeWifiIntentFilter() {

		final IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
		intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
		intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
		intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
		//wifi on/off
		intentFilter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
		//connected/disconnected/etc.
		intentFilter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
		//wpa_supplicant failure
		intentFilter.addAction(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION);
		intentFilter.addAction(WifiManager.SUPPLICANT_CONNECTION_CHANGE_ACTION);
		//wifi scan result available^M
		intentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
		return intentFilter;
	}
	
	
	
	public class GcWifiBroadcastReceiver extends BroadcastReceiver {

	    private WifiP2pManager mWifiP2pManager;

	    
	    
	    public GcWifiBroadcastReceiver(WifiP2pManager manager) {

	    	super();
	        this.mWifiP2pManager = manager;
	    }

	    
   
	    @Override
	    public void onReceive(final Context context, final Intent intent) {

	        String action = intent.getAction();

	    	Log.d(TAG, "[MGCC] onReceive action = " + action);

	        if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)) {

	            int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);

	            Log.d(TAG, "[MGCC] P2P state = " + state);

	            if (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED) {

	            	
	            	
	            } else if (state == WifiP2pManager.WIFI_P2P_STATE_DISABLED) {

	            	if (getP2pGroupState().equals(WifiP2pGroupState.STATE_P2P_GROUP_CREATED)) {
	            		
		            	setP2pGroupState(WifiP2pGroupState.STATE_P2P_GROUP_REMOVED);
		            	
		        		final LinkedList<GcWifiTransceiverListener> listeners;
		        		synchronized(mListeners){

		        			listeners = new LinkedList<GcWifiTransceiverListener>(mListeners);
		        		}
		        		
		                for (GcWifiTransceiverListener listener : listeners) {
		                	
		                	listener.onWifiP2pDisabled();
		                }
	            	}
	            }

	        } else if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {


	        } else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {

	            if (mWifiP2pManager != null) {

	            	///NetworkInfo networkInfo = (NetworkInfo) intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);
	            	WifiP2pInfo wifiP2pInfo = (WifiP2pInfo) intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_INFO);
	            	WifiP2pGroup wifiP2pGroup = (WifiP2pGroup) intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_GROUP);
	            	
	            	Log.d(TAG, "[MGCC] wifip2pInfo = " + wifiP2pInfo);
	            	if (wifiP2pInfo != null) {

	            		boolean isGroupFormed = wifiP2pInfo.groupFormed;
	            		///boolean isGroupOwner = wifiP2pInfo.isGroupOwner;
	            		
	            		if (!isGroupFormed) {
	            			
	            			if (getP2pGroupState().equals(WifiP2pGroupState.STATE_P2P_GROUP_CREATED)) {

		            			setP2pGroupState(WifiP2pGroupState.STATE_P2P_GROUP_REMOVED);

		    	        		final LinkedList<GcWifiTransceiverListener> listeners;
		    	        		synchronized(mListeners){

		    	        			listeners = new LinkedList<GcWifiTransceiverListener>(mListeners);
		    	        		}
		    	        		
		    	                for (GcWifiTransceiverListener listener : listeners) {
		    	                	
		    	                	listener.onWifiP2pDisabled();
		    	                }

	            			} else if (getP2pGroupState().equals(WifiP2pGroupState.STATE_P2P_GROUP_REMOVING)) {
	            				
								mWifiP2pGroupRemovingStepCnt++;
								
								Log.d(TAG, "[MGCC] mWifiP2pGroupRemovingStepCnt = " + mWifiP2pGroupRemovingStepCnt);
								if (mWifiP2pGroupRemovingStepCnt == 2) {
									
									setP2pGroupState(WifiP2pGroupState.STATE_P2P_GROUP_REMOVED);
									
					        		final LinkedList<GcWifiTransceiverListener> listeners;
					        		synchronized(mListeners){

					        			listeners = new LinkedList<GcWifiTransceiverListener>(mListeners);
					        		}
					        		
					                for (GcWifiTransceiverListener listener : listeners) {
					                	
					                	listener.onWifiDirectGroupRemoved();
					                }
					                
					                mWifiP2pGroupRemovingStepCnt = 0;
					                mCanSkipRemovingP2pGroup.set(true);
								}
	            			}

	            		} else {
	            			
	            			if (getP2pGroupState().equals(WifiP2pGroupState.STATE_P2P_GROUP_CREATING)) {
	            				
								mWifiP2pGroupCreatingStepCnt++;
								
								Log.d(TAG, "[MGCC] mWifiP2pGroupCreatingStepCnt = " + mWifiP2pGroupCreatingStepCnt);
								if (mWifiP2pGroupCreatingStepCnt == 2) {
									
									requestGroupInfo();
								}
	            			}
	            		}
	            	}
	            	
            		if (wifiP2pGroup != null) {
            			
    		        	for (WifiP2pDevice p2pDevice : wifiP2pGroup.getClientList()) {

    		        		Log.d(TAG, "[MGCC] P2pGroup client MAC address = " + p2pDevice.deviceAddress);
    		        	}
            		}
	            }

	        } else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)) {

	        	
	        }
			/**************************************************************/
			//WIFI softAP mode
			else if (WifiManager.NETWORK_STATE_CHANGED_ACTION.equals(action)) {
				///Log.i(TAG, "[MGCC] NETWORK_STATE_CHANGED_ACTION++");
				Parcelable parcelableExtra = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
				if (null != parcelableExtra) {
					NetworkInfo networkInfo = (NetworkInfo) parcelableExtra;
					DetailedState state = networkInfo.getDetailedState();
					///Log.i(TAG, "[MGCC] onReceive2: netstate=" + state);
					handleDetailedStateChanged(state);
				}
				//Log.i(TAG, "[MGCC] NETWORK_STATE_CHANGED_ACTION--");
			}
			else if (WifiManager.SUPPLICANT_STATE_CHANGED_ACTION.equals(action)) {
				///Log.i(TAG, "[MGCC] SUPPLICANT_STATE_CHANGED_ACTION++");
				SupplicantState state = intent.getParcelableExtra(WifiManager.EXTRA_NEW_STATE);
				int errorCode = intent.getIntExtra(WifiManager.EXTRA_SUPPLICANT_ERROR, WIFIMGR_ERROR_NONE);
				///if (state != null)
					///Log.i(TAG, "[MGCC] onReceive: supplicant state=" + state);
				///Log.i(TAG, "[MGCC] onReceive: errorCode=" + errorCode);
				
				///Log.i(TAG, "[MGCC] SUPPLICANT_STATE_CHANGED_ACTION--");
			}
			else if (WifiManager.SCAN_RESULTS_AVAILABLE_ACTION.equals(action)) {
				
        		final LinkedList<GcWifiTransceiverListener> listeners;
        		synchronized(mListeners){

        			listeners = new LinkedList<GcWifiTransceiverListener>(mListeners);
        		}
        		
                for (GcWifiTransceiverListener listener : listeners) {
                	
                	listener.onWifiScanResultAvailable();
                }
			}
		}
	}
	
	private void handleDetailedStateChanged(DetailedState state) {
		
		final LinkedList<GcWifiTransceiverListener> listeners;
		
		switch(state) {
			case IDLE:
			case SCANNING:
			case CONNECTING:
				break;
			case AUTHENTICATING:
				break;
			case CONNECTED:
        		synchronized(mListeners){

        			listeners = new LinkedList<GcWifiTransceiverListener>(mListeners);
        		}
        		
                for (GcWifiTransceiverListener listener : listeners) {
                	
                	listener.onWifiConnected();
                }
				
				break;
			case DISCONNECTING:
				break;
			case DISCONNECTED:
        		synchronized(mListeners){

        			listeners = new LinkedList<GcWifiTransceiverListener>(mListeners);
        		}
        		
                for (GcWifiTransceiverListener listener : listeners) {
                	
                	listener.onWifiDisconnected();
                }
				break;
			case FAILED:
				break;
			case BLOCKED:
				break;
			case OBTAINING_IPADDR:
				break;
		}
//		for (GcWifiTransceiverListener lis : mListeners)
//			lis.onWifiConnected();
	}
	
	private void handleSupplicantStateChanged(SupplicantState state) {
		switch(state) {
			case SCANNING:
			case AUTHENTICATING:
				break;
			case FOUR_WAY_HANDSHAKE:
			case DISCONNECTED:
        		final LinkedList<GcWifiTransceiverListener> listeners;
        		synchronized(mListeners){

        			listeners = new LinkedList<GcWifiTransceiverListener>(mListeners);
        		}
        		
                for (GcWifiTransceiverListener listener : listeners) {
                	
                	listener.onWifiDisconnected();
                }
				break;
		}
//		for (GcWifiTransceiverListener lis : mListeners)
//			lis.onWifiConnected();
	}
	
	public boolean connectToWPA2(String SSID, String passwd) {		
		Log.d(TAG, "[MGCC] connectToWPA2 SSID=" + SSID + ", passwd=" + passwd);
		WifiConfiguration conf = new WifiConfiguration();
		conf.SSID = "\"" + SSID + "\"";
		Log.d(TAG, "[MGCC] SSID= " + conf.SSID);
		conf.preSharedKey = "\"" + passwd + "\"";
		List<WifiConfiguration> list = mWifiManager.getConfiguredNetworks();
		for( WifiConfiguration i : list ) {
			if(i.SSID != null && i.SSID.equals("\"" + SSID + "\"")) {
				Log.d(TAG, "[MGCC] remove old config: " + i.toString());
				mWifiManager.removeNetwork(i.networkId);
				break;
			}
		}
		List<ScanResult> wifiAPList = mWifiManager.getScanResults();
		for (ScanResult result : wifiAPList) {
			Log.d(TAG, "[MGCC] SSID=" + result.SSID);
			if (result.SSID != null && result.SSID.equals(SSID)) {
				mWifiManager.disconnect();
				int ID = mWifiManager.addNetwork(conf);
				Log.d(TAG, "[MGCC] ID="+ID);
				boolean ret = mWifiManager.enableNetwork(ID, true);
				Log.d(TAG, "[MGCC] enableNetwork="+ret);
				ret = mWifiManager.reconnect();
				Log.d(TAG, "[MGCC] reconnect="+ret);
				return true;
			}
		}
		Log.w(TAG, "[MGCC] target GC softAP not found!!");
		return false;
	}

	public void scanSoftAP() {
		Log.d(TAG, "[MGCC] scanSoftAP");
		mWifiManager.startScan();
	}
	
	public boolean validateConnectedSSID(String targetSSID) {
		WifiInfo info = mWifiManager.getConnectionInfo();
		String SSID = info.getSSID();
		if (SSID != null &&
			SSID.equalsIgnoreCase("\""+targetSSID+"\""))
			return true;
		Log.e(TAG, "FATAL: validateConnectedSSID: target=" 
			+ targetSSID+ ", connected=" + SSID);
		return false;
	}

	public String getDhcpServerIP() {
		//in softAP mode, our DHCP server should be GC
		//btw, the IP format in Android DhcpInfo seems to be always IPv4,
		//so "formatIpAddres" should always work well here
		DhcpInfo info = mWifiManager.getDhcpInfo();
		if (info == null)
			return null;
		return Formatter.formatIpAddress(info.serverAddress);
	}

	//workaround: wifimgr doesn't declare any constant for error free case,
	//only ERROR_AUTHENTICATING(0x1) is defined
	private static final int WIFIMGR_ERROR_NONE = 0;

	public boolean isDualBandSupported() {
		try {
			Method m = WifiManager.class.getMethod("isDualBandSupported", new Class[]{});
			if (m != null && m.getReturnType() == Boolean.TYPE) {
				return (Boolean)m.invoke(mWifiManager, null);
			}
		} catch (Exception e) {
			Log.e(TAG, e.getMessage());
			Log.e(TAG, "MINOR: failed to call isDualBandSupported, assume 2.4GHz only");
		}
		return false;
	}
	
	private Object reflectGetMethod( Object aninstance, String methodName) 
			throws SecurityException, NoSuchMethodException, IllegalArgumentException, 
			IllegalAccessException, InvocationTargetException {
		Method m = null;
		Object ret = null;
		Class<?> aclass;// = null;
		
		try {
			aclass = aninstance.getClass();
			m = aclass.getDeclaredMethod(methodName, new Class<?>[]{});
			m.setAccessible(true);
			ret = m.invoke(aninstance, new Object[] {});
			Log.d(TAG, "[MGCC] ret:" + ret.toString());
		} catch (NoSuchMethodException e) {
			Log.d(TAG, "[MGCC] no such method, " + methodName);
		}
		return ret;
	}

	private void prepareStaticIP(WifiP2pGroup group) {
		Log.d(TAG, "[MGCC] prepareStaticIP+");
		Object objIP = null;

        try {
        	objIP = reflectGetMethod(group, "getP2pStaticIPv4");
        } catch (Exception e){
        	e.printStackTrace();
        }

        if (objIP != null) {
        	Log.d(TAG, "[MGCC] StaticIP=" + objIP.toString());
        	InetAddress ip;
			try {
				ip = InetAddress.getByName(objIP.toString());
				mStaticIP = ip.getAddress();
			} catch (UnknownHostException e) {
				e.printStackTrace();
			}

        	//Log.d(TAG, "[MGCC] mStaticIP=" + mStaticIP.toString());
        } else {
        	Log.d(TAG, "[MGCC] Get static IP fail");
        	mStaticIP = new byte[4];
        }
        Log.d(TAG, "[MGCC] prepareStaticIP-");
	}

	private void prepareCountry(WifiP2pGroup group) {
		Log.d(TAG, "[MGCC] prepareCountryCode+");
		Object objCountry = null;

        try {
        	objCountry = reflectGetMethod(group, "getCountryCode");
        } catch (Exception e) {
        	e.printStackTrace();
        }

        if (objCountry != null) {
        	Log.d(TAG, "[MGCC] Country=" + objCountry.toString());
        } else {
        	Log.d(TAG, "[MGCC] Get country fail");
        }
        Log.d(TAG, "[MGCC] prepareCountryCode-");
	}

	private void prepareWifiChannel(WifiP2pGroup group) {
		Log.d(TAG, "[MGCC] prepareChannel+");
		Object objFreq = null;

        try {
        	objFreq = reflectGetMethod(group, "getOptFreq");
        } catch (Exception e) {
        	e.printStackTrace();
        }

        if (objFreq != null) {
        		
        	if (tranChannel24G.indexOfKey((Integer)objFreq) >= 0) {
        		mBandSelect = 0;
        		mWifiChannel = tranChannel24G.get((Integer)objFreq);
        	} else if (tranChannel5G.indexOfKey((Integer)objFreq) >= 0) {
        		mBandSelect = 1;
        		mWifiChannel = tranChannel5G.get((Integer)objFreq);
        	} else {
        		mBandSelect = 0;
        		mWifiChannel = 0;
        		Log.d(TAG, "[MGCC] Freq analyzing error!");
        	}
        	Log.d(TAG, "[MGCC] Freq=" + objFreq.toString() + " Chan:" + mWifiChannel);
        } else {
        	Log.d(TAG, "[MGCC] Get channel fail");
        	mBandSelect = 0;
    		mWifiChannel = 0;
        }

		Log.d(TAG, "[MGCC] prepareChannel-");
	}

    private void init_ChannelTable() {
    	Log.d(TAG, "[MGCC] initial frequecy table");
    	tranChannel24G.put(2412,(byte)0x01);
    	tranChannel24G.put(2417,(byte)0x02);
    	tranChannel24G.put(2422,(byte)0x03);
    	tranChannel24G.put(2427,(byte)0x04);
    	tranChannel24G.put(2432,(byte)0x05);
    	tranChannel24G.put(2437,(byte)0x06);
    	tranChannel24G.put(2442,(byte)0x07);
    	tranChannel24G.put(2447,(byte)0x08);
    	tranChannel24G.put(2452,(byte)0x09);
    	tranChannel24G.put(2457,(byte)0x0A);
    	tranChannel24G.put(2462,(byte)0x0B);
    	tranChannel24G.put(2467,(byte)0x0C);
    	tranChannel24G.put(2472,(byte)0x0D);
    	tranChannel24G.put(2484,(byte)0x0E);

    	tranChannel5G.put(5035, (byte)7);
    	tranChannel5G.put(5040, (byte)8);
    	tranChannel5G.put(5045, (byte)9);
    	tranChannel5G.put(5055, (byte)11);
    	tranChannel5G.put(5060, (byte)12);
    	tranChannel5G.put(5080, (byte)16);
    	tranChannel5G.put(5170, (byte)34);
    	tranChannel5G.put(5180, (byte)36);
    	tranChannel5G.put(5190, (byte)38);
    	tranChannel5G.put(5200, (byte)40);
    	tranChannel5G.put(5210, (byte)42);
    	tranChannel5G.put(5220, (byte)44);
    	tranChannel5G.put(5230, (byte)46);
    	tranChannel5G.put(5240, (byte)48);
    	tranChannel5G.put(5260, (byte)52);
    	tranChannel5G.put(5280, (byte)56);
    	tranChannel5G.put(5300, (byte)60);
    	tranChannel5G.put(5320, (byte)64);
    	tranChannel5G.put(5500, (byte)100);
    	tranChannel5G.put(5520, (byte)104);
    	tranChannel5G.put(5540, (byte)108);
    	tranChannel5G.put(5560, (byte)112);
    	tranChannel5G.put(5580, (byte)116);
    	tranChannel5G.put(5600, (byte)120);
    	tranChannel5G.put(5620, (byte)124);
    	tranChannel5G.put(5640, (byte)128);
    	tranChannel5G.put(5660, (byte)132);
    	tranChannel5G.put(5680, (byte)136);
    	tranChannel5G.put(5700, (byte)140);
    	tranChannel5G.put(5745, (byte)149);
    	tranChannel5G.put(5765, (byte)153);
    	tranChannel5G.put(5785, (byte)157);
    	tranChannel5G.put(5805, (byte)161);
    	tranChannel5G.put(5825, (byte)165);
    	tranChannel5G.put(4915, (byte)183);
    	tranChannel5G.put(4920, (byte)184);
    	tranChannel5G.put(4925, (byte)185);
    	tranChannel5G.put(4935, (byte)187);
    	tranChannel5G.put(4940, (byte)188);
    	tranChannel5G.put(4945, (byte)189);
    	tranChannel5G.put(4960, (byte)192);
    	tranChannel5G.put(4980, (byte)196);
    }

}
