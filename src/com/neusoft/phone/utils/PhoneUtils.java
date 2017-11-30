/*
 * Copyright (C) 2006 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.neusoft.phone.utils;

import java.util.HashMap;
import java.util.List;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.media.AudioManager;
import android.media.AudioManager.OnAudioFocusChangeListener;
import android.net.Uri;
import android.os.AsyncResult;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.provider.CallLog.Calls;
import android.provider.Settings;
import android.telephony.PhoneNumberUtils;
import android.text.TextUtils;
import android.util.Log;

import com.neusoft.c3alfus.bluetooth.aidl.HfpPhoneNumberInfo;
import com.neusoft.c3alfus.bluetooth.phone.CallLogAsync;
import com.neusoft.c3alfus.bluetooth.phone.CallLogger;
import com.neusoft.c3alfus.bluetooth.phone.CallManager;
import com.neusoft.c3alfus.bluetooth.phone.CallState;
import com.neusoft.c3alfus.bluetooth.phone.PhoneManager;
import com.neusoft.c3alfus.bluetooth.phone.ValueInfo;
import com.neusoft.c3alfus.projectservice.IUsbProtectService;
import com.neusoft.phonedemo.R;
import com.neusoft.phone.listener.OnCallListener;
import com.neusoft.phone.listener.OnHFPChangListener;
import com.neusoft.phone.manager.ContactsManager;
import com.neusoft.phone.model.ContactInfo;
import com.neusoft.phone.model.PbapLoadInfo;
import com.neusoft.phone.service.PbapLoadService;

/**
 * Misc utilities for the Phone app.
 */
@SuppressLint("NewApi")
//手机相关类，打电话，获取手机联系人，手机信息，发短息等。
public class PhoneUtils {

    private static final int APP_CALL_NUMBER_UPDATE = 127;
    private static final int APP_CALL_AUTO_ANSWER = 129;
    private static final int APP_CALL_AUDIO_TRANSFER_CHANGED = 130;
    private static final int APP_HFP_STATE_CHANGED = 131;
    private static final int APP_NO_START_CALL = 132;
    private static final int APP_SIGNAL_STRENGTH_CHANGED = 133;
    private static final int APP_BATTREY_INDICATOR_CHANGED = 134;
    private static final int MSG_FINISH_WHEN_NOT_UPDATE = 1;

    private static final int NO_FINISH_CALL_DELAYED = 3000;
    private static final int NO_START_CALL_DELAYED = 3000; // 3s

    private final static String BROAD_START_CALL = "com.neusoft.phone.startcall";
    private final static String BROAD_FINISH_CALL = "com.neusoft.phone.finishcall";
    private final static String BROAD_UPDATE_CALL = "com.neusoft.phone.updatecall";

	private final static String WARNING_OFF = "com.neusoft.action.WARNING_OFF";
    private static final String PHONE_WARNING_STATUS = "phone_warning_status";
    private static final String PHONE_ACC_STATUS = "phone_acc_status";
    private static final String PHONE_POWER_STATUS = "phone_power_status";
    
    private static final int STATUS_ON = 1;
    private static final int STATUS_OFF = 0;

    private static final int NOTIFICATION_FLAG = 1;

    private static PhoneUtils INSTANCE;//操作系统中一系列的进程以及为这些进程所分配的内存块

    private static Context mContext;

    private OnCallListener mOnCallListener;

    private int mOnCallListenerCookie = -1;

    private OnHFPChangListener mOnHFPChangListener;

    private static final boolean DBG = true;

    private int mState = CallState.CALL_STATE_NONE;
    private int mPreviousTransfer = -1;
    private boolean mPowerOn = true;
    private boolean mAccOn = false;
    private boolean mIsLocalCall = false;
    private boolean mIsRinging;
    private boolean mhasNoFocus;

    private boolean conferenceShown;

    /** Noise suppression status as selected by user */
    private static boolean sIsNoiseSuppressionEnabled = true;
    
    /** Is call started. modify by hegf*/
    /** Call time.*/
    public static long mCallTimes[] = {-1, -1, -1, -1, -1};
    /** Is call started.*/
    public static boolean isCallStarted = false;
    
    private  String mInCallScreenIntent = null;

    public String getmInCallScreenIntent() {
		return mInCallScreenIntent;
	}

	public void setmInCallScreenIntent(String mInCallScreenIntent) {
		this.mInCallScreenIntent = mInCallScreenIntent;
	}
	
	private  String mMainActivityIntent = null;

	public String getmMainActivityIntent() {
		return mMainActivityIntent;
	}

	public void setmMainActivityIntent(String mMainActivityIntent) {
		this.mMainActivityIntent = mMainActivityIntent;
	}

	private CallManager mCM;
    private PhoneManager mPm;

    private CallLogger mCallLogger;

    private long protectDialTime = System.currentTimeMillis();

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case PhoneUtils.APP_CALL_NUMBER_UPDATE:
                Log.i("GeminiPhoneApp", "APP_CALL_NUMBER_UPDATE");
                HfpPhoneNumberInfo newInfo = (HfpPhoneNumberInfo) ((AsyncResult) msg.obj).result;
                if (newInfo == null) {
                    Log.i("GeminiPhoneApp", "HfpPhoneNumberInfo is null!!!");
                    if (getAudioTransfer() != CallManager.CALL_AUDIOTRANSFER_PHONE) {
                        processTransferToPhone();
                    }
                    return;
                }
                Log.i("GeminiPhoneApp", "APP_CALL_NUMBER_UPDATE, state is " + newInfo.getStatus());
                Log.i("GeminiPhoneApp", "APP_CALL_NUMBER_UPDATE, number is " + newInfo.getNumber());
                Log.i("GeminiPhoneApp", "APP_CALL_NUMBER_UPDATE, direction is " + newInfo.getDirection());

                if (newInfo.getDirection() == 0 && isLocalNumber(newInfo.getNumber())) {
                    //abandon local number
                    Log.i("GeminiPhoneApp", "abandon call from local number!");
                    if (getAudioTransfer() != CallManager.CALL_AUDIOTRANSFER_PHONE) {
                        processTransferToPhone();
                    }
                    mIsLocalCall = true;
                    return;
                } else {
                    mIsLocalCall =false;
                }

                if (newInfo.getStatus() != CallState.CALL_STATE_ACTIVE) {
                    //If state is not active, means this is different from conference call
                    conferenceShown = false;
                }

                if (analyseAllHfpInfo()) {
                    if (isCallStarted) {
                        if (mOnCallListener != null) {
                            mOnCallListener.onHangup();
                        }
                        finishCall();
                    }
                    return;
                }

                // Here number may be null, howerer we has no 'setNumber'
                // function
                if (TextUtils.isEmpty(newInfo.getNumber())) {
                    newInfo = new HfpPhoneNumberInfo(newInfo.getIndex(),
                            newInfo.getDirection(), newInfo.getStatus(),
                            newInfo.isMultiparty() ? 1 : 0,
                            mContext.getString(R.string.unknown_number));
                }
                if (!isCallStarted && newInfo.getStatus() !=
                        CallState.CALL_STATE_TERMINATED) {
                		
                   // Intent intent = new Intent(mContext, InCallScreen.class);
                	if(mInCallScreenIntent != null)
                	{
                		try {
                           	Intent intent = new Intent(mInCallScreenIntent);
    	                	intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    	                    intent.putExtra("oldCall", "normal");
    	                    mContext.startActivity(intent);
    	                    //request focus before InCallScreen has been shown
    	                    setAudioMode(true);
    	                    Log.e("GeminiPhoneApp",
    	                            "  APP_CALL_NUMBER_UPDATE  startActivity");
						} catch (Exception e) {
							// TODO: handle exception
							Log.e("GeminiPhoneApp", "Start InCallScreen Failed!");
						}	
                	}
                }
                if (mOnCallListener != null) {
                    mHandler.removeMessages(APP_NO_START_CALL);
                    mOnCallListener.onNumberChanged(newInfo);
                }

                if (newInfo.getStatus() == CallState.CALL_STATE_TERMINATED) {
                    mState = CallState.CALL_STATE_TERMINATED;
                    Ringer.init(mContext).stopRing();
                    mIsRinging = false;
                    int prevState = getLocalHfpInfoStateByIndex(newInfo
                            .getIndex());
                    Log.e("zhangwei", "getLocalHfpInfoStateByIndex prevState: "
                            + prevState);
                    removeMessages(MSG_FINISH_WHEN_NOT_UPDATE);
                    if (mCM.getHfpCallList() == null
                            || mCM.getHfpCallList().isEmpty()) {
                        if (mOnCallListener != null) {
                            mOnCallListener.onHangup();
                        }
                        Log.i("GeminiPhoneApp", "Hang up " + newInfo.getNumber());
                        finishCall(newInfo.getDirection() == 0 ? true : false,
                                prevState, newInfo.getNumber());
                    }
                    /* Here should only add call log */
                    else {
                        addCallLog(newInfo.getDirection() == 0 ? true : false,
                                prevState, newInfo.getNumber());
                    }
                } else if (newInfo.getStatus() == CallState.CALL_STATE_INCOMING) {
                    mState = CallState.CALL_STATE_INCOMING;
                    Log.i("MT_Ring", "getAudioTransfer() = " + getAudioTransfer());
                    Log.i("MT_Ring", "mhasNoFocus = " + mhasNoFocus);
                    Log.i("MT_Ring", "mPowerOn = " + mPowerOn);
                    Log.i("MT_Ring", "mAccOn = " + mAccOn);
                    if (getAudioTransfer() == 0 && !mIsRinging && !mhasNoFocus && mPowerOn &&
                            mAccOn && mPreviousTransfer == 0) {
                        Ringer.init(mContext).ring();
                        mIsRinging = true;
                    } else {
                        Ringer.init(mContext).stopRing();
                        mIsRinging = false;
                    }

                    //only can do this because of the transfer api was not perfect
                    if (mPreviousTransfer == -1) {
                        mPreviousTransfer = getAudioTransfer();
                    }
                } else if (newInfo.getStatus() == CallState.CALL_STATE_ACTIVE) {
                    mState = CallState.CALL_STATE_ACTIVE;
                    updateAudioTransfer();

                    //stop ringer
                    Ringer.init(mContext).stopRing();
                    mIsRinging = false;
                } else {
                    mState = newInfo.getStatus();
                    Ringer.init(mContext).stopRing();
                    mIsRinging = false;
                }
                updateCall();
                updateLocalHfpInfos(newInfo);
                break;
            case PhoneUtils.APP_CALL_AUDIO_TRANSFER_CHANGED:
                Log.i("GeminiPhoneApp", "APP_CALL_AUDIO_TRANSFER_CHANGED, transfer is " + getAudioTransfer());
                Log.i("GeminiPhoneApp", "APP_CALL_AUDIO_TRANSFER_CHANGED, mIsLocalCall is " + mIsLocalCall);
                Log.i("GeminiPhoneApp", "APP_CALL_AUDIO_TRANSFER_CHANGED, mState is " + mState);
                Log.i("GeminiPhoneApp", "APP_CALL_AUDIO_TRANSFER_CHANGED, mPowerOn is " + mPowerOn);
                Log.i("GeminiPhoneApp", "APP_CALL_AUDIO_TRANSFER_CHANGED, mAccOn is " + mAccOn);
                Log.i("GeminiPhoneApp", "APP_CALL_AUDIO_TRANSFER_CHANGED, mhasNoFocus is " + mhasNoFocus);

                if (mIsLocalCall && getAudioTransfer() != CallManager.CALL_AUDIOTRANSFER_PHONE) {
                    processTransferToPhone();
                }

                if (mhasNoFocus || !mPowerOn || !mAccOn) {
                    processTransferToPhone();
                }

                if (getAudioTransfer() == CallManager.CALL_AUDIOTRANSFER_CAR) {
                    //Always show the banner when transfer changes
                    conferenceShown = false;
                    analyseAllHfpInfo();
                }

                //only can do this because of the transfer api was not perfect
                if (mState == CallState.CALL_STATE_INCOMING && getAudioTransfer() == 1 &&
                        mPreviousTransfer != 1) {
                    mPreviousTransfer = 1;
                }
                if (mOnCallListener != null) {
                    mOnCallListener.onAudioTransferChanged();
                }
                break;
            case PhoneUtils.APP_CALL_AUTO_ANSWER:
                autoAnswer();
                break;
            case PhoneUtils.APP_HFP_STATE_CHANGED:
                Log.i("GeminiPhoneApp", "hfp state is " + (Boolean) ((AsyncResult) msg.obj).result);
                Settings.Global.putInt(mContext.getContentResolver(), "missed_call_count", 0);
                if (mOnHFPChangListener != null) {
                    mOnHFPChangListener
                            .onHFPStateChanged((Boolean) ((AsyncResult) msg.obj).result);
                }
                if (mOnCallListener != null) {
                    mOnCallListener
                            .onHfpStateChanged((Boolean) ((AsyncResult) msg.obj).result);
                }
                if ((Boolean) ((AsyncResult) msg.obj).result == false) {
                    Log.i("GeminiPhoneApp", "Hang up because hfp disconnected");
                    finishCall();
                    Intent hfpIntent = new Intent("com.android.btphone.disconnected");
                    mContext.sendBroadcast(hfpIntent);
                    ContactsManager.getInstance(mContext).cleanContacts();
                }
                break;
            case PhoneUtils.APP_NO_START_CALL:
                if (mCM.getHfpCallList() == null
                        || mCM.getHfpCallList().isEmpty()) {
                    if (mOnCallListener != null) {
                        mOnCallListener.onHangup();
                    }
                    Log.i("GeminiPhoneApp", "Hang up because not start");
                    finishCall();
                }
                break;
            case APP_SIGNAL_STRENGTH_CHANGED:
                ValueInfo signalInfo = (ValueInfo) ((AsyncResult) msg.obj).result;
                Log.i("GeminiPhoneApp", "signal strength changed, currentValue is "
                        + signalInfo.currentValue);
                Intent signalIntent = new Intent("com.android.btphone.signal.changed");
                signalIntent.putExtra("signal", signalInfo.currentValue);
                mContext.sendBroadcast(signalIntent);
                break;
            case APP_BATTREY_INDICATOR_CHANGED:
                ValueInfo batteryInfo = (ValueInfo) ((AsyncResult) msg.obj).result;
                Log.i("GeminiPhoneApp", "battery changed, currentValue is "
                        + batteryInfo.currentValue);
                Intent battreyIntent = new Intent("com.android.btphone.battery.changed");
                battreyIntent.putExtra("battery", batteryInfo.currentValue);
                mContext.sendBroadcast(battreyIntent);
                break;
            case MSG_FINISH_WHEN_NOT_UPDATE:
                HfpPhoneNumberInfo curHfpInfo = getCurrentHfpInfo();
                Log.i("GeminiPhoneApp", "curHfpInfo is " + curHfpInfo);
                if (curHfpInfo == null) {
                    if (mOnCallListener != null) {
                        //destory the incall ui
                        mOnCallListener.onHangup();
                    }
                    finishCall();
                } else {
                    sendMessageDelayed(obtainMessage(MSG_FINISH_WHEN_NOT_UPDATE),
                            NO_FINISH_CALL_DELAYED);
                }
                break;
            }
        }
    };

    /** This class is never instantiated. */
    private PhoneUtils(Context context) {
        mContext = context;
        mCM = CallManager.getInstance(context);
        mPm = PhoneManager.getInstance(context);
        mCallLogger = new CallLogger(context, new CallLogAsync());
        initCallManager(mHandler);
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_POWER_ON);
        filter.addAction(Intent.ACTION_POWER_OFF);
        filter.addAction(Intent.ACTION_ACC_ON);
        filter.addAction(Intent.ACTION_ACC_OFF);
        filter.addAction(WARNING_OFF);
        context.registerReceiver(mOnOffReceiver, filter);

        //update audio transfer in case of Phone died
        updateAudioTransfer();
    }

    public static PhoneUtils getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (PhoneUtils.class) {
                if (INSTANCE == null) {
                    INSTANCE = new PhoneUtils(context);
                }
            }
        }
        return INSTANCE;
    }

    public boolean isHfpConnect() {
        return mCM.isHfpConnected();
    }

    private void initCallManager(Handler mHandler) {
        mCM.registerForUpdateNumber(mHandler, APP_CALL_NUMBER_UPDATE, null);
        mCM.registerForAudioTransferChanged(mHandler,
                APP_CALL_AUDIO_TRANSFER_CHANGED, null);
        mCM.registerForHfpStateChanged(mHandler, APP_HFP_STATE_CHANGED, null);
        mCM.registerForSignalStrengthChanged(mHandler, APP_SIGNAL_STRENGTH_CHANGED, null);
        mCM.registerForBatteryIndicatorChanged(mHandler, APP_BATTREY_INDICATOR_CHANGED, null);

    }

    /**
     * Answer the currently-ringing call.
     *
     * @return true if we answered the call, or false if there wasn't actually a
     *         ringing incoming call, or some other error occurred.
     *
     * @see #answerAndEndHolding(CallManager, Call)
     * @see #answerAndEndActive(CallManager, Call)
     */
    public boolean answerCall(String number) {
        log("answerCall()...");
        boolean answered = false;

        answered = mCM.acceptCall(number, 0);
        if (answered) {
            // Always reset to "unmuted" for a freshly-answered call
            setMute(false);
        }
        return answered;
    }

    public boolean answerCallHold(String number) {
        log("answerCall()...");
        boolean answered = false;

        answered = mCM.acceptCall(number, 1);
        if (answered) {
            // Always reset to "unmuted" for a freshly-answered call
            setMute(false);
        }
        return answered;
    }

    public boolean hangupRingingCall(String number, String name) {
        if (DBG)
            log("hangup ringing call");
        boolean sucess = mCM.hangupRingingCall(number);
        if (sucess) {
            GeminiMultiWinUtils.showRejecteBanner(mContext, name);
        }
        return sucess;
    }

    /**
     * Trivial wrapper around Call.hangup(), except that we return a boolean
     * success code rather than throwing CallStateException on failure.
     *
     * @return true if the call was successfully hung up, or false if the call
     *         wasn't actually active.
     */
    public boolean hangup() {
        boolean hanged = mCM.hangupForegroundResumeBackground();
        Log.i("GeminiPhoneApp", "hang up result is " + hanged);
        // Add a msg into messagequeue to check if the call list is null
        // This msg should be cancel when the call list is null
        mHandler.removeMessages(MSG_FINISH_WHEN_NOT_UPDATE);
        mHandler.sendMessageDelayed(mHandler.obtainMessage(MSG_FINISH_WHEN_NOT_UPDATE),
                NO_FINISH_CALL_DELAYED);
        return hanged;
    }

    /**
     * Dial the number using the phone passed in.
     *
     * If the connection is establised, this method issues a sync call that may
     * block to query the caller info. TODO: Change the logic to use the async
     * query.
     *
     * @param context
     *            To perform the CallerInfo query.
     * @param phone
     *            the Phone object.
     * @param number
     *            to be dialed as requested by the user. This is NOT the phone
     *            number to connect to. It is used only to build the call card
     *            and to update the call log. See above for restrictions.
     * @param contactRef
     *            that triggered the call. Typically a 'tel:' uri but can also
     *            be a 'content://contacts' one.
     * @param isEmergencyCall
     *            indicates that whether or not this is an emergency call
     * @param gatewayUri
     *            Is the address used to setup the connection, null if not using
     *            a gateway
     *
     * @return either CALL_STATUS_DIALED or CALL_STATUS_FAILED
     */
    private boolean placeCall(Context context, String number, Uri contactRef) {
        boolean ret = false;
        String numberToDial = number;
        ret = mCM.dial(numberToDial);
        return ret;
    }

    /**
     * Given an Intent (which is presumably the ACTION_CALL intent that
     * initiated this outgoing call), figure out the actual phone number we
     * should dial.
     *
     * Note that the returned "number" may actually be a SIP address, if the
     * specified intent contains a sip: URI.
     *
     * This method is basically a wrapper around
     * PhoneUtils.getNumberFromIntent(), except it's also aware of the
     * EXTRA_ACTUAL_NUMBER_TO_DIAL extra. (That extra, if present, tells us the
     * exact string to pass down to the telephony layer. It's guaranteed to be
     * safe to dial: it's either a PSTN phone number with separators and keypad
     * letters stripped out, or a raw unencoded SIP address.)
     *
     * @return the phone number corresponding to the specified Intent, or null
     *         if the Intent has no action or if the intent's data is malformed
     *         or missing.
     *
     * @throws VoiceMailNumberMissingException
     *             if the intent contains a "voicemail" URI, but there's no
     *             voicemail number configured on the device.
     */
    public static String getInitialNumber(Intent intent) {

        String action = intent.getAction();
        if (TextUtils.isEmpty(action)) {
            return null;
        }

        return getNumberFromIntent(mContext, intent);
    }

    /**
     * Gets the phone number to be called from an intent. Requires a Context to
     * access the contacts database, and a Phone to access the voicemail number.
     *
     * <p>
     * If <code>phone</code> is <code>null</code>, the function will return
     * <code>null</code> for <code>voicemail:</code> URIs; if
     * <code>context</code> is <code>null</code>, the function will return
     * <code>null</code> for person/phone URIs.
     * </p>
     *
     * <p>
     * If the intent contains a <code>sip:</code> URI, the returned "number" is
     * actually the SIP address.
     * 
     * @param context
     *            a context to use (or
     * @param intent
     *            the intent
     *
     * @throws VoiceMailNumberMissingException
     *             if <code>intent</code> contains a <code>voicemail:</code>
     *             URI, but <code>phone</code> does not have a voicemail number
     *             set.
     *
     * @return the phone number (or SIP address) that would be called by the
     *         intent, or <code>null</code> if the number cannot be found.
     */
    private static String getNumberFromIntent(Context context, Intent intent) {

        // Otherwise, let PhoneNumberUtils.getNumberFromIntent() handle
        // the other cases (i.e. tel: and voicemail: and contact: URIs.)

        final String number = PhoneNumberUtils.getNumberFromIntent(intent,
                context);

        return number;
    }

    /**
     * Turns on/off speaker.
     *
     * @param context
     *            Context
     * @param flag
     *            True when speaker should be on. False otherwise.
     * @param store
     *            True when the settings should be stored in the device.
     */
    /* package */void turnOnSpeaker(Context context, boolean flag, boolean store) {
        if (DBG)
            log("turnOnSpeaker(flag=" + flag + ", store=" + store + ")...");

        AudioManager audioManager = (AudioManager) context
                .getSystemService(Context.AUDIO_SERVICE);
        audioManager.setSpeakerphoneOn(flag);

    }

    boolean isSpeakerOn(Context context) {
        AudioManager audioManager = (AudioManager) context
                .getSystemService(Context.AUDIO_SERVICE);
        return audioManager.isSpeakerphoneOn();
    }

    /**
     *
     * Mute / umute the foreground phone, which has the current foreground call
     *
     * All muting / unmuting from the in-call UI should go through this wrapper.
     *
     * Wrapper around Phone.setMute() and setMicrophoneMute(). It also updates
     * the connectionMuteTable and mute icon in the status bar.
     *
     */
    public void setMute(boolean muted) {

        AudioManager audioManager = (AudioManager) mContext
                .getSystemService(Context.AUDIO_SERVICE);
        audioManager.setMicrophoneMute(muted);
    }

    /**
     * Get the mute state of foreground phone, which has the current foreground
     * call
     */
    public boolean getMute() {
        AudioManager audioManager = (AudioManager) mContext
                .getSystemService(Context.AUDIO_SERVICE);
        return audioManager.isMicrophoneMute();
    }

    /**
     * Sets the audio mode per current phone state.
     */
    public void setAudioMode(boolean forceReq) {
        log("setAudioMode start");
        if (mContext == null)
            return;
        AudioManager audioManager = (AudioManager) mContext
                .getSystemService(Context.AUDIO_SERVICE);
        // 开始拨打电话
        if (isCallStarted || forceReq) {
            if (audioManager.requestAudioFocus(mOnAudioFocusChangeListener,
                    AudioManager.STREAM_VOICE_CALL,
                    AudioManager.AUDIOFOCUS_GAIN_TRANSIENT) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                setMute(false);
                if (!isSpeakerOn(mContext)) {
                    log("=== turnOnSpeaker ===");
                    turnOnSpeaker(mContext, true, true);
                }
                mhasNoFocus = false;
            } else {
                log("setAudioMode MODE_IN_CALL error");
                mhasNoFocus = true;
                processTransferToPhone();
            }
        } else {
            log("Restore AudioMode MODE_NORMAL");
            setMute(false);
            if (isSpeakerOn(mContext)) {
                log("=== turnOffSpeaker ===");
                turnOnSpeaker(mContext, false, true);
            }
            // 通话结束
            if (audioManager.abandonAudioFocus(mOnAudioFocusChangeListener) ==
                    AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            } else {
                log("Restore AudioMode error");
            }
        }
        log("setAudioMode end");
    }

    private OnAudioFocusChangeListener mOnAudioFocusChangeListener = new OnAudioFocusChangeListener() {
        @Override
        public void onAudioFocusChange(int focusChange) {
            if (focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) {
                Log.i("GeminiPhoneApp", "focus change: AUDIOFOCUS_LOSS_TRANSIENT");
                mhasNoFocus = true;
                processTransferToPhone();
            } else if (focusChange == AudioManager.AUDIOFOCUS_LOSS) {
                Log.i("GeminiPhoneApp", "focus change: AUDIOFOCUS_LOSS");
                mhasNoFocus = true;
                processTransferToPhone();
            } else if (focusChange == AudioManager.AUDIOFOCUS_GAIN) {
                Log.i("GeminiPhoneApp", "focus change: AUDIOFOCUS_GAIN");
                mhasNoFocus = false;
                if (mState != CallState.CALL_STATE_INCOMING &&
                        mState != CallState.CALL_STATE_NONE) {
                    updateAudioTransfer();
                }
            }
        }
    };

    public int getAudioTransfer() {
        return mCM.getAudioTransfer();
    }

    public void setAudioTransfer(int code) {
        boolean success = mPm.reqHfpAudioTransfer("", code);
        Log.i("GeminiPhoneApp", "setAudioTransfer " + code + " success = " + success);
    }

    public void sendDtmf(String c) {
        mCM.sendDtmf(c);
    }

    private void log(String msg) {
        Log.d("AppHfpTest", msg);
    }

    /**
     * Return Uri with an appropriate scheme, accepting Voicemail, SIP, and
     * usual phone call numbers.
     */
    private static Uri getCallUri(String number) {
        return Uri.fromParts("tel", number, null);
    }

    private boolean autoAnswer() {
        if (isCallStarted) {
            if (Settings.System.getInt(mContext.getContentResolver(),
                    "bt_auto_answer", 0) == 1) {
                answerCall("");
            }
        }
        return true;
    }

    public void setOnCallListener(OnCallListener listener, int cookie) {
        if (listener == null) {
            if (cookie == mOnCallListenerCookie) {
                mOnCallListener = null;
                mOnCallListenerCookie = -1;
            }

        } else {
            mOnCallListener = listener;

            mOnCallListenerCookie = cookie;
        }
    }

    public void setOnHfpListener(OnHFPChangListener listener) {
        mOnHFPChangListener = listener;
    }

    public boolean canDial() {
        return (!isCallStarted && (mCM.canDial() || mCM.canDialEmergency()));
    }

    public boolean placeCall(Intent intent) {

        Uri uri = intent.getData();
        if (uri == null) {
            throw new IllegalArgumentException("placeCall: intent had no data");
        }

        return placeCallInternal(intent);

    }

    /**
     * Actually make a call to whomever the intent tells us to.
     *
     * Note that there's no need to explicitly update (or refresh) the in-call
     * UI at any point in this method, since a fresh InCallScreen instance will
     * be launched automatically after we return (see placeCall() above.)
     *
     * @param intent
     *            the CALL intent describing whom to call
     * @return CallStatusCode.SUCCESS if we successfully initiated an outgoing
     *         call. If there was some kind of failure, return one of the other
     *         CallStatusCode codes indicating what went wrong.
     */
    private boolean placeCallInternal(Intent intent) {
        String number;

        number = PhoneUtils.getInitialNumber(intent);

        if (number == null) {
            return false;
        }

        Uri contactUri = intent.getData();

        return placeCall(mContext, number, contactUri);

    }

    private void addCallLog(boolean isOutGoing, int callState, String number) {
        if (!isOutGoing) {
            if (callState == CallState.CALL_STATE_INCOMING
                    || callState == CallState.CALL_STATE_WAITING) {
                addMissedCallLog(number, 0);
            } else
                addIncomimgCallLog(number, 0);
        } else {
            addOutgoingCallLog(number, 0);
        }
    }

    private void addIncomimgCallLog(String number, long duration) {
        if (number == null) {
            return;
        }
        mCallLogger.logCall(number, 0, Calls.INCOMING_TYPE,
                System.currentTimeMillis(), duration);
    }

    private void addOutgoingCallLog(String number, long duration) {
        if (number == null) {
            return;
        }
        mCallLogger.logCall(number, 0, Calls.OUTGOING_TYPE,
                System.currentTimeMillis(), duration);
    }

    private void addMissedCallLog(String number, long duration) {
        if (number == null) {
            return;
        }
        mCallLogger.logCall(number, 0, Calls.MISSED_TYPE,
                System.currentTimeMillis(), duration);

        Intent intent = new Intent(Const.BROAD_UPDATE_MISSCALL);
        intent.putExtra(Const.WIDGET_MISSCALL, Const.WIDGET_NOTICE_ASC);
        mContext.sendBroadcast(intent);

        int missedCallCount = Settings.Global.getInt(mContext.getContentResolver(), "missed_call_count", 0);
        Settings.Global.putInt(mContext.getContentResolver(), "missed_call_count", missedCallCount + 1);

        if(mMainActivityIntent != null)
        {
	        try {
	            //Intent intentMissed = new Intent(mContext, MainActivity.class);
	        	Intent intentMissed = new Intent(mMainActivityIntent);
	            //enter missed call
	            intentMissed.putExtra("showtabIndex", 1);
	            intentMissed.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
	            PendingIntent pendingIntent = PendingIntent.getActivity(mContext, 0, intentMissed, 0);
	
	            String contactInfo = null;
	            if (missedCallCount > 0) {
	                contactInfo = String.format(mContext.getString(R.string.missed_call_notification_text),
	                        missedCallCount + 1);
	            } else {
	                ContactInfo info = ContactsManager.queryContactByNum(number);
	                if (info != null) {
	                    contactInfo = info.getName();
	                }
	                if (!TextUtils.isEmpty(contactInfo)) {
	                    contactInfo = contactInfo + "   ";
	                }
	                contactInfo = contactInfo + number;
	            }
	
	            NotificationManager manager = (NotificationManager) mContext.getSystemService
	                    (Context.NOTIFICATION_SERVICE);
	            Notification notify = new Notification.Builder(mContext)
	                    .setSmallIcon(R.drawable.icon_missed_call_notification_m)
	                    .setTicker(mContext.getString(R.string.missed_call_notification_title))
	                    .setContentTitle(mContext.getString(R.string.missed_call_notification_title))
	                    .setContentText(contactInfo)
	                    .setContentIntent(pendingIntent).build();
	            notify.icon = R.drawable.icon_missed_call_notification_h;
	            notify.flags |= Notification.FLAG_AUTO_CANCEL;
	            manager.notify(NOTIFICATION_FLAG, notify);
	            mContext.getContentResolver().registerContentObserver(
	                    Settings.Global.getUriFor("missed_call_count"), false, mCalllogobserver);
			} catch (Exception e) {
				// TODO: handle exception
				Log.e("GeminiPhoneApp", "PendingIntent MainAcitity start Failed");
			}
        }else{
        	Log.e("GeminiPhoneApp", "mMainActivityIntent is null");
        }
        
 
    }

    private ContentObserver mCalllogobserver = new ContentObserver(null) {

        public void onChange(boolean selfChange) {
            int missedCallCount = Settings.Global.getInt(mContext.getContentResolver(),
                    "missed_call_count", 0);
            if (missedCallCount == 0) {
                NotificationManager manager = (NotificationManager) mContext.getSystemService
                        (Context.NOTIFICATION_SERVICE);
                manager.cancel(NOTIFICATION_FLAG);
            }
        }
    };

    public void dialTo(String name, String phoneNumber) {
        Intent intent = null;
        boolean res = false;
        boolean canDial = false;
        // zh.lu ST_Bug #45615 Start
        if (TextUtils.isEmpty(phoneNumber)) {
            noPhoneConnectedDialog(R.string.please_input_phone_number_text);
            return;
        }
        // zh.lu ST_Bug #45615 End
        if (null != phoneNumber) {
            if (phoneNumber.contains("-")) {
                phoneNumber = phoneNumber.replace("-", "");
            }

            if (phoneNumber.contains(" ")) {
                phoneNumber = phoneNumber.replace(" ", "");
            }
        }

        if (phoneNumber.length() < 3 || phoneNumber.length() > 24) {
            noPhoneConnectedDialog(R.string.can_not_make_call);
            return;
        }

        canDial = canDial();
        if (canDial && !isLocalNumber(phoneNumber)) {
            if (System.currentTimeMillis() - protectDialTime > 2000) {
                res = placeCall(mContext, phoneNumber, null);
                if (res) {
                    Message noStartCall = mHandler
                            .obtainMessage(APP_NO_START_CALL);
                    mHandler.sendMessageDelayed(noStartCall,
                            NO_START_CALL_DELAYED);
                    //intent = new Intent(mContext, InCallScreen.class);
                	if(mInCallScreenIntent != null)
                	{
                		try {
                           	intent = new Intent(mInCallScreenIntent);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            intent.setData(PhoneUtils.getCallUri(phoneNumber));
                            intent.setAction(Intent.ACTION_CALL);
                            intent.putExtra("isMT", "false");
                            mContext.startActivity(intent);
                            protectDialTime = System.currentTimeMillis();
						} catch (Exception e) {
							// TODO: handle exception
							Log.e("GeminiPhoneApp", "Start InCallScreen Failed!");
						}	
                	}
                }
            }
        } else {
            noPhoneConnectedDialog(R.string.can_not_make_call);
        }
    }

    private void noPhoneConnectedDialog(int src) {
        GeminiMultiWinUtils.showBanner(mContext,
                mContext.getResources().getString(src));
    }

    public void startCall(boolean isOutGoing) {
        //Make sure there is a activte call by getCurrentHfpInfo
        //Do not use mState because mState will be reinit if Phone has been died
        //Do not judge getCurrentHfpInfo while MO a call
        if (isCallStarted == false && (getCurrentHfpInfo() != null || isOutGoing)) {
            isCallStarted = true;
            IUsbProtectService xf6000yeService = IUsbProtectService.Stub
                    .asInterface(ServiceManager.getService("usbprotect"));
            try {
                xf6000yeService.ioctl_Service(0);
            } catch (RemoteException ex) {
                Log.e("PhoneUtil", "RemoteException when ioctl_Service");
            }
            setAudioMode(false);
            mContext.sendBroadcast(new Intent(BROAD_START_CALL));
            Log.i("GeminiPhoneApp", "send start call broadcast");
            if (!isOutGoing) {
                mHandler.sendEmptyMessageDelayed(APP_CALL_AUTO_ANSWER, 5000);

            }
        }
    }

    public void finishCall(boolean isOutGoing, int callState, String number) {
        isCallStarted = false;
        if (mIsRinging) {
            Ringer.init(mContext).stopRing();
            mIsRinging = false;
        }
        //PhoneApp.isCallStarted was already false
        setAudioMode(false);

        //reset call state
        mState = CallState.CALL_STATE_NONE;

        mPreviousTransfer = -1;
        mContext.sendBroadcast(new Intent(BROAD_FINISH_CALL));
        Log.i("GeminiPhoneApp", "send stop call broadcast");
        for (int i = 0; i < 5; i++) {
            mCallTimes[i] = 0;
        }
        addCallLog(isOutGoing, callState, number);
        IUsbProtectService xf6000yeService = IUsbProtectService.Stub
                .asInterface(ServiceManager.getService("usbprotect"));
        try {
            xf6000yeService.ioctl_Service(1);
        } catch (RemoteException ex) {
            Log.e("PhoneUtil", "RemoteException when ioctl_Service");
        }
    }

    public void finishCall() {
    	if(isCallStarted == true){
            mContext.sendBroadcast(new Intent(BROAD_FINISH_CALL));
    	}
        isCallStarted = false;
        if (mIsRinging) {
            Ringer.init(mContext).stopRing();
            mIsRinging = false;
        }
        //PhoneApp.isCallStarted was already false
        setAudioMode(false);

        //reset call state
        mState = CallState.CALL_STATE_NONE;

        Log.i("GeminiPhoneApp", "send stop call broadcast");
        for (int i = 0; i < 5; i++) {
            mCallTimes[i] = 0;
        }
        IUsbProtectService xf6000yeService = IUsbProtectService.Stub
                .asInterface(ServiceManager.getService("usbprotect"));
        try {
            xf6000yeService.ioctl_Service(1);
        } catch (RemoteException ex) {
            Log.e("PhoneUtil", "RemoteException when ioctl_Service");
        }
    }

    private void updateCall() {
        if (isCallStarted) {
            mContext.sendBroadcast(new Intent(BROAD_UPDATE_CALL));
        }
    }

    public void processTransferToPhone() {
        Log.i("GeminiPhoneApp", "processTransferToPhone, getAudioTransfer = " + getAudioTransfer());
        if (getAudioTransfer() == CallManager.CALL_AUDIOTRANSFER_CAR) {
            setAudioTransfer(0);
        }
    }

    public void processTransferToCar() {
        Log.i("GeminiPhoneApp", "processTransferToCar, getAudioTransfer = " + getAudioTransfer());
        if (getAudioTransfer() == CallManager.CALL_AUDIOTRANSFER_PHONE) {
            setAudioTransfer(1);
        }
    }

    public void setHfpStateChanged(int prevState, int newState) {
        Log.i("GeminiPhoneApp", "setHfpStateChanged, prevState = "
                + prevState + ", newState = " + newState);

        if (BluetoothProfile.STATE_CONNECTED == newState
                && prevState < BluetoothProfile.STATE_CONNECTED) {

            Intent intent = new Intent(mContext, PbapLoadService.class);

            //no need to judge last device and current device
            mContext.startService(intent.putExtra(
                    PbapLoadService.LOAD_TYPE, PbapLoadInfo.TYPE_CONTACTS).putExtra(
                            PbapLoadService.LOAD_CONTAINS,
                            PbapLoadInfo.ACTION_MEMORY | PbapLoadInfo.ACTION_SIM));

            mContext.startService(intent.putExtra(PbapLoadService.LOAD_TYPE,
                    PbapLoadInfo.TYPE_CALLLOG).putExtra(
                    PbapLoadService.LOAD_CONTAINS,
                    PbapLoadInfo.ACTION_INCOMING | PbapLoadInfo.ACTION_MISSED
                            | PbapLoadInfo.ACTION_OUTGOING));
        }
    }

    public HfpPhoneNumberInfo getCurrentHfpInfo() {
        List<HfpPhoneNumberInfo> infoList = mCM.getHfpCallList();
        if (infoList != null && !infoList.isEmpty()) {
            if (infoList.size() == 1) {
                return infoList.get(0);
            } else {
                for (HfpPhoneNumberInfo info : infoList) {
                    if (info.getStatus() != CallState.CALL_STATE_NONE &&
                            info.getStatus() != CallState.CALL_STATE_HELD_BY_RESPONSE_AND_HOLD &&
                            info.getStatus() != CallState.CALL_STATE_TERMINATED) {
                        return info;
                    }
                }

            }
        }
        return null;
    }

    public List<HfpPhoneNumberInfo> getAllHfpInfo() {
        return mCM.getHfpCallList();
    }

    private boolean analyseAllHfpInfo() {
        List<HfpPhoneNumberInfo> infos = getAllHfpInfo();
        String operator = mCM.getNetworkOperator();
        int activeNum = 0;
        int aliveNum = 0;
        if (infos == null) {
            return false;
        }
        for (HfpPhoneNumberInfo info : infos) {
            if (info.getStatus() == CallState.CALL_STATE_ACTIVE) {
                activeNum++;
            }
            if (info.getStatus() != CallState.CALL_STATE_NONE &&
                    info.getStatus() != CallState.CALL_STATE_TERMINATED) {
                aliveNum++;
            }
        }
        if (activeNum >= 2) {
            Log.i("GeminiPhoneApp", "Do not support conference calls!  conferenceShown is "
                    + conferenceShown);
            if (!conferenceShown) {
                GeminiMultiWinUtils.showBanner(mContext, mContext.getResources().getString
                        (R.string.do_not_support_conference_call));
                conferenceShown = true;
            }
            //transfer to phone since do not support conference calls
            processTransferToPhone();
            return true;
        }
        if (isCDMA(operator)) {
            if (aliveNum >= 2) {
                Log.i("GeminiPhoneApp", "Do not support multi CDMA calls!");
                GeminiMultiWinUtils.showBanner(mContext, mContext.getResources().getString
                        (R.string.do_not_support_cdma_call));
                //transfer to phone since do not support multi cdma calls
                processTransferToPhone();
                return true;
            }
        }
        return false;
    }

    private boolean isCDMA(String operator) {
        if (TextUtils.isEmpty(operator)) {
            return false;
        }
        operator = operator.toLowerCase().replace(" ", "");
        if (operator.contains(mContext.getResources().getString(R.string.cdma_operator_name_01)) ||
                operator.contains(mContext.getResources().getString
                (R.string.cdma_operator_name_02)) || operator.contains(mContext.getResources()
                .getString(R.string.cdma_operator_name_03)) || operator.contains(mContext
                .getResources().getString(R.string.cdma_operator_name_04)) || operator.contains
                (mContext.getResources().getString(R.string.cdma_operator_name_05))) {
            return true;
        }
        return false;
    }

    private HashMap<Integer, HfpPhoneNumberInfo> mLocalHfpInfos = new HashMap<Integer, HfpPhoneNumberInfo>();

    private void updateLocalHfpInfos(HfpPhoneNumberInfo other) {
        Log.e("zhangwei", "updateLocalHfpInfos mLocalHfpInfos 1: "
                + mLocalHfpInfos);
        if (other == null)
            return;
        if (other.getStatus() == CallState.CALL_STATE_TERMINATED
                || other.getStatus() == CallState.CALL_STATE_NONE) {
            mLocalHfpInfos.remove(new Integer(other.getIndex()));
        } else {
            mLocalHfpInfos.put(new Integer(other.getIndex()), other);
        }
        Log.e("zhangwei", "updateLocalHfpInfos mLocalHfpInfos 2: "
                + mLocalHfpInfos);
    }
    private int getLocalHfpInfoStateByIndex(int index) {
        HfpPhoneNumberInfo info = mLocalHfpInfos.get(new Integer(index));
        if (info != null) {
            return info.getStatus();
        }
        return CallState.CALL_STATE_NONE;
    }
    private void updateAudioTransfer() {
        int warningStatus = Settings.Global.getInt(mContext.getContentResolver(),
                PHONE_WARNING_STATUS, 0);
        int accStatus = Settings.Global.getInt(mContext.getContentResolver(),
                PHONE_ACC_STATUS, 0);
        int powerStatus = Settings.Global.getInt(mContext.getContentResolver(),
                PHONE_POWER_STATUS, 0);
        Log.i("GeminiPhoneApp", "updateAudioTransfer, warningStatus = " + warningStatus);
        Log.i("GeminiPhoneApp", "updateAudioTransfer, accStatus = " + accStatus);
        Log.i("GeminiPhoneApp", "updateAudioTransfer, powerStatus = " + powerStatus);
        mPowerOn = powerStatus == 1;
        mAccOn = (accStatus == 1) && (warningStatus == 1);
        processAudioTransfer();
    }

    private void updateAudioTransferAcc(int accStatus) {
        int warningStatus = Settings.Global.getInt(mContext.getContentResolver(),
                PHONE_WARNING_STATUS, 0);
        int powerStatus = Settings.Global.getInt(mContext.getContentResolver(),
                PHONE_POWER_STATUS, 0);
        mPowerOn = powerStatus == 1;
        mAccOn = (accStatus == 1) && (warningStatus == 1);
    }

    private void updateAudioTransferPower(int powerStatus) {
        int warningStatus = Settings.Global.getInt(mContext.getContentResolver(),
                PHONE_WARNING_STATUS, 0);
        int accStatus = Settings.Global.getInt(mContext.getContentResolver(),
                PHONE_ACC_STATUS, 0);
        mPowerOn = powerStatus == 1;
        mAccOn = (accStatus == 1) && (warningStatus == 1);
    }

    private void updateAudioTransferWarning(int warningStatus) {
        int accStatus = Settings.Global.getInt(mContext.getContentResolver(),
                PHONE_ACC_STATUS, 0);
        int powerStatus = Settings.Global.getInt(mContext.getContentResolver(),
                PHONE_POWER_STATUS, 0);
        mPowerOn = powerStatus == 1;
        mAccOn = (accStatus == 1) && (warningStatus == 1);
    }

    private void processAudioTransfer() {
        if (mState != CallState.CALL_STATE_INCOMING && mState != CallState.CALL_STATE_NONE) {
            if (mPowerOn && mAccOn && !mhasNoFocus) {
                Log.i("GeminiPhoneApp", "updateAudioTransfer, transfer to car");
                processTransferToCar();
            } else {
                Log.i("GeminiPhoneApp", "updateAudioTransfer, transfer to phone");
                processTransferToPhone();
            }
        }
    }

    private boolean isLocalNumber(String num) {
        String localNum = mCM.getSubscriberNumber();
        Log.i("GeminiPhoneApp", "isLocalNumber, localNum = " + localNum);
        localNum = formatNumber(localNum);
        num = formatNumber(num);
        if (TextUtils.isEmpty(num) || localNum.equals(num)) {
            return true;
        }
        return false;
    }

    private String formatNumber (String number) {
        if (number == null) {
            return null;
        }
        if (number.startsWith("+86")) {
            number = number.substring(3, number.length());
        }
        if (number.startsWith("86")) {
            number = number.substring(2, number.length());
        }
        int len = number.length();
        StringBuilder ret = new StringBuilder(len);
        for (int i = 0; i < len; i++) {
            char c = number.charAt(i);
            if (c >= '0' && c <= '9') {
                ret.append(c);
            }
        }
        return ret.toString();
    }

    private BroadcastReceiver mOnOffReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Intent.ACTION_POWER_OFF)) {
                Log.i("GeminiPhoneApp", "power off");
                updateAudioTransferPower(STATUS_OFF);
                processAudioTransfer();
            } else if (intent.getAction().equals(Intent.ACTION_POWER_ON)) {
                Log.i("GeminiPhoneApp", "power on");
                updateAudioTransferPower(STATUS_ON);
                if (mState != CallState.CALL_STATE_INCOMING &&
                        mState != CallState.CALL_STATE_NONE) {
                    processAudioTransfer();
                }
            } else if (intent.getAction().equals(Intent.ACTION_ACC_OFF)) {
                Log.i("GeminiPhoneApp", "acc off");
                updateAudioTransferAcc(STATUS_OFF);
                processAudioTransfer();
            } else if (intent.getAction().equals(Intent.ACTION_ACC_ON)) {
                Log.i("GeminiPhoneApp", "acc on");
                updateAudioTransferAcc(STATUS_ON);
                if (mState != CallState.CALL_STATE_INCOMING &&
                        mState != CallState.CALL_STATE_NONE) {
                    processAudioTransfer();
                }
            } else if (intent.getAction().equals(WARNING_OFF)) {
                Log.i("GeminiPhoneApp", "warning agree");
                updateAudioTransferWarning(STATUS_ON);
                if (mState != CallState.CALL_STATE_INCOMING &&
                        mState != CallState.CALL_STATE_NONE) {
                    processAudioTransfer();
                }
            }
            Log.i("GeminiPhoneApp", "power state is " + mPowerOn);
            Log.i("GeminiPhoneApp", "acc state is " + mAccOn);
        }
    };
}
