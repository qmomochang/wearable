package com.htc.gc.connectivity.v2.internal.component.wifi;

import java.lang.reflect.Method;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import com.htc.gc.connectivity.internal.common.CommonBase.GcWifiTransceiverErrorCode;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.DhcpInfo;
import android.net.NetworkInfo;
import android.net.NetworkInfo.DetailedState;
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



public class GcWifiTransceiver {

    private final static String TAG = "GcWifiTransceiver";
    private final static int CREATE_P2P_GROUP_RETRY_TIMES_DEFAULT = 5;
    private final static int REMOVE_P2P_GROUP_RETRY_TIMES_DEFAULT = 5;
    private final static int REQUEST_P2P_GROUP_INFO_RETRY_TIMES_DEFAULT = 20;
    
    private Context mContext;
    private WifiManager mWifiManager;
    private WifiP2pManager mWifiP2pManager;
    private ConnectivityManager mConnectivityManager;
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
        
        if (mConnectivityManager == null) {
        	
        	mConnectivityManager = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
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

	
	
	public String getGroupPassword() {
		
		return mDirectPassword;
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
					Log.i(TAG, "[MGCC] NETWORK_STATE_CHANGED_ACTION= "+networkInfo);
					DetailedState state = networkInfo.getDetailedState();
					///Log.i(TAG, "[MGCC] onReceive2: netstate=" + state);
					handleDetailedStateChanged(state);
				}
				//Log.i(TAG, "[MGCC] NETWORK_STATE_CHANGED_ACTION--");
			}
			else if (WifiManager.SUPPLICANT_STATE_CHANGED_ACTION.equals(action)) {
				///Log.i(TAG, "[MGCC] SUPPLICANT_STATE_CHANGED_ACTION++");
				SupplicantState state = intent.getParcelableExtra(WifiManager.EXTRA_NEW_STATE);
				Log.i(TAG, "[MGCC] SUPPLICANT_STATE_CHANGED_ACTION= "+state);
				///if (state != null)
					///Log.i(TAG, "[MGCC] onReceive: supplicant state=" + state);
				///Log.i(TAG, "[MGCC] onReceive: errorCode=" + errorCode);
				
				handleSupplicantStateChanged(state);
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
    	WifiInfo info = mWifiManager.getConnectionInfo();
    	
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
            	
            	listener.onWifiConnected(info);
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
	}
	
	private void handleSupplicantStateChanged(SupplicantState state) {
		
		final LinkedList<GcWifiTransceiverListener> listeners;
		
		switch(state) {
			case SCANNING:
	    		synchronized(mListeners){
	    			
	    			listeners = new LinkedList<GcWifiTransceiverListener>(mListeners);
	    		}
	    		
                for (GcWifiTransceiverListener listener : listeners) {
                	
                	listener.onWifiSupplicantScanning();
                }
                break;
			case COMPLETED:
	    		synchronized(mListeners){
	    			
	    			listeners = new LinkedList<GcWifiTransceiverListener>(mListeners);
	    		}
	    		
                for (GcWifiTransceiverListener listener : listeners) {
                	
                	listener.onWifiSupplicantCompleted();
                }
                break;
			case DISCONNECTED:
	    		synchronized(mListeners){
	    			
	    			listeners = new LinkedList<GcWifiTransceiverListener>(mListeners);
	    		}
	    		
                for (GcWifiTransceiverListener listener : listeners) {
                	
                	listener.onWifiSupplicantDisconnected();
                }
                break;				
            default:
		}
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
			}
		}
		
		int ID = mWifiManager.addNetwork(conf);
		Log.d(TAG, "[MGCC] ID="+ID);
		boolean ret = mWifiManager.enableNetwork(ID, true);
		Log.d(TAG, "[MGCC] enableNetwork="+ret);
		return true;
		
//		List<ScanResult> wifiAPList = mWifiManager.getScanResults();
//		for (ScanResult result : wifiAPList) {
//			Log.d(TAG, "[MGCC] SSID=" + result.SSID);
//			if (result.SSID != null && result.SSID.equals(SSID)) {
//				int ID = mWifiManager.addNetwork(conf);
//				Log.d(TAG, "[MGCC] ID="+ID);
//				boolean ret = mWifiManager.enableNetwork(ID, true);
//				Log.d(TAG, "[MGCC] enableNetwork="+ret);
//				return true;
//			} 	
//		}
//		Log.w(TAG, "[MGCC] target GC softAP not found!!");
//		return false;
	}
	
	public boolean disconnect() {
		Log.d(TAG, "[MGCC] disconnect");
		
		return mWifiManager.disconnect();
	}
	
	public boolean scanSoftAP() {
		Log.d(TAG, "[MGCC] scanSoftAP");
		return mWifiManager.startScan();
	}
	
	public void disableAllConfiguredNetworks() {
		Log.d(TAG, "[MGCC] disableAllConfiguredNetworks");
		List<WifiConfiguration> wifiList = mWifiManager.getConfiguredNetworks();
		if(wifiList != null) {
			for(WifiConfiguration configuration:wifiList) {
				boolean disableResult = mWifiManager.disableNetwork(configuration.networkId);
				Log.i(TAG, "[MGCC] disable network: "+configuration.SSID+", reuslt= "+disableResult);
			}
		}
	}
	
	public void enableAllConfiguredNetworks() {
		Log.d(TAG, "[MGCC] enableAllConfiguredNetworks");
		List<WifiConfiguration> wifiList = mWifiManager.getConfiguredNetworks();
		if(wifiList != null) {
			for(WifiConfiguration configuration:wifiList) {
				boolean enableResult = mWifiManager.enableNetwork(configuration.networkId, false);
				Log.i(TAG, "[MGCC] enable network: "+configuration.SSID+", reuslt= "+enableResult);
			}
		}
	}
	
	public boolean validateConnectedSSID(String targetSSID) {
		WifiInfo info = mWifiManager.getConnectionInfo();
		String SSID = info.getSSID();
		if (SSID != null && SSID.equalsIgnoreCase("\""+targetSSID+"\"")) {
			return true;
		} else {
			Log.e(TAG, "[MGCC] FATAL: validateConnectedSSID: target="+targetSSID+", connected=" + SSID);
			return false;			
		}
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
	
	public WifiInfo getWifiInfo() {
		return mWifiManager.getConnectionInfo();
	}
}
