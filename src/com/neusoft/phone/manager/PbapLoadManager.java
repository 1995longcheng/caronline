package com.neusoft.phone.manager;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.CallLog;
import android.provider.Settings;
import android.util.Log;

import com.neusoft.phone.listener.OnPbapSyncLinstener;
import com.neusoft.c3alfus.bluetooth.pbap.PbapManager;
import com.neusoft.c3alfus.bluetooth.pbap.PbapManagercb;
import com.neusoft.phone.model.PbapLoadInfo;
import com.neusoft.phone.utils.Const;

/**
 * Pbap load manager class.
 *
 * @author neusoft
 */
public class PbapLoadManager {
    private static final String TAG = "PbapLoadManager";
    // for fuction
    /** Load contacts max size.*/
    private static final int CONTACTS_TOTAL_MAX = -1;
    /** Load calllog max size each type.*/
    private static final int CALLLOG_EVERY_MAX = 120;
    /** Support load contacts photo or not.*/
    private static final boolean CONTACTS_PHOTO_SUPPORT = true;

    /**Event Pbap load status changed.*/
    private static final int EVENT_STATE = 0;
    /**Event get Pbap load info.*/
    private static final int EVENT_GET_LOADINFO = 1;
    /**Event update Pbap download progress.*/
    private static final int EVENT_UPDATE_PROGRESS = 2;

    /** PbapLoadManager object. {@link PbapLoadManager}*/
    private static PbapLoadManager mPbapLoadManager;
    /** Context.*/
    private static Context mContext;
    /** Pbap load info list. {@link PbapLoadInfo}*/
    private List<PbapLoadInfo> mPbapLoadList;
    /** Pbap manager. {@link PbapManager}*/
    private PbapManager mPbapManager;
    /** Device address.*/
    private String mDevice = "";
    /** Current storage type.*/
    private int mCurrentStorage;
    /** Pbap sync linstener. {@link OnPbapSyncLinstener}*/

    private OnPbapSyncLinstener mOnPbapSyncLinstener;

	private static boolean sInitDone = false;
	public static boolean hasInitDone() {
		return sInitDone;
	}

    private PbapLoadManager() {
        mPbapManager = PbapManager.getInstance(mContext);
        mPbapLoadList = new ArrayList<PbapLoadInfo>();
        mCurrentStorage = 0;
        if (null != mPbapManager) {
            mPbapManager.RegisterPbapManagerCallback(mPbapManagerCallback);
            if (!mPbapManager.setPbapDownloadNotify(5)) {
                Log.e(TAG, "set download notify failed");
            }
        }
        sInitDone = true;
    }

    /**
     * Get instance.
     *
     * @param context context
     * @return mPbapLoadManager {@link PbapLoadManager}
     */
    public static PbapLoadManager getInstance(Context context) {

        synchronized (PbapLoadManager.class) {
            mContext = context;
            if (null == mPbapLoadManager) {
                mPbapLoadManager = new PbapLoadManager();
            }
        }

        return mPbapLoadManager;
    }

    /**
     * Synchronize contacts.
     *
     * @param action PbapLoadInfo.ACTION_SIM or PbapLoadInfo.ACTION_MEMORY
     * @return true in loading contacts
     *         false not in loading
     */
    public boolean contactsSynchronize(int action) {
        if (doContactsSynchronize(action)) {
            sendPbapLoadBroadcast(Const.SYNC_CONTACT_START);
            return true;
        }
        return false;
    }

    /**
     * Synchronize calllog.
     *
     * @param action PbapLoadInfo.ACTION_INCOMING or PbapLoadInfo.ACTION_MISSED
     *               or PbapLoadInfo.ACTION_OUTGOING.
     * @return true in loading calllog
     *         false not in loading
     */
    public boolean callLogSynchronize(int action) {
        if (doCallLogSynchronize(action)) {
            sendPbapLoadBroadcast(Const.SYNC_CALLLOG_START);
            return true;
        }
        return false;
    }

    /**
     * Is calllog loading.
     *
     * @return true calllog is loading.
     *         false calllog is not loading
     */
    public boolean isCallLogDownLoading() {
        List<PbapLoadInfo> pbapLoadList = mPbapLoadList;
        for (PbapLoadInfo loadInfo : pbapLoadList) {
            if (PbapLoadInfo.TYPE_CALLLOG == loadInfo.mType) {
                return true;
            }
        }

        return false;
    }

    /**
     * Is contacts loading.
     *
     * @return true contacts is loading.
     *         false contacts is not loading
     */
    public boolean isContactsDownLoading() {
        List<PbapLoadInfo> pbapLoadList = mPbapLoadList;
        for (PbapLoadInfo loadInfo : pbapLoadList) {
            if (PbapLoadInfo.TYPE_CONTACTS == loadInfo.mType) {
                return true;
            }
        }

        return false;
    }

    /**
     * Stop synchronize all type.
     */
    public void stopAllSynchronize() {
        List<PbapLoadInfo> pbapLoadList = mPbapLoadList;
        for (PbapLoadInfo loadInfo : pbapLoadList) {
            String action = (PbapLoadInfo.TYPE_CONTACTS == loadInfo.mType)
                    ? Const.SYNC_CONTACT_STOP : Const.SYNC_CALLLOG_STOP;
            sendPbapLoadBroadcast(action);
        }

        pbapLoadList.clear();
        mPbapLoadList = pbapLoadList;

        if (PbapManager.STATE_LOADING == mPbapManager.getLoadState()) {
            mPbapManager.IntrruptPbaploadreq();
        }
    }

    /**
     * Do synchronize contacts.
     *
     * @param action PbapLoadInfo.ACTION_SIM or PbapLoadInfo.ACTION_MEMORY
     * @return true in loading contacts
     *         false not in loading
     */
    private boolean doContactsSynchronize(int action) {
        List<PbapLoadInfo> pbapLoadList = mPbapLoadList;

        PbapLoadInfo loadInfo;

        loadInfo = new PbapLoadInfo(mDevice, PbapLoadInfo.TYPE_CONTACTS,
                action, CONTACTS_PHOTO_SUPPORT, CONTACTS_TOTAL_MAX);

        if (!mPbapLoadList.isEmpty()) {
            mPbapLoadList.add(loadInfo);
            return true;
        }

        if (pbapLoadRequest(loadInfo)) {
            mPbapLoadList.add(loadInfo);
            return true;
        }

        return false;
    }

    /**
     * Do synchronize calllog.
     *
     * @param action PbapLoadInfo.ACTION_INCOMING or PbapLoadInfo.ACTION_MISSED
     *               or PbapLoadInfo.ACTION_OUTGOING.
     * @return true in loading calllog
     *         false not in loading
     */
    private boolean doCallLogSynchronize(int action) {
        List<PbapLoadInfo> pbapLoadList = mPbapLoadList;

        PbapLoadInfo loadInfo;

        loadInfo = new PbapLoadInfo(mDevice, PbapLoadInfo.TYPE_CALLLOG, action,
                false, CALLLOG_EVERY_MAX);

        if (!mPbapLoadList.isEmpty()) {
            mPbapLoadList.add(loadInfo);
            return true;
        }

        if (pbapLoadRequest(loadInfo)) {
            mPbapLoadList.add(loadInfo);
            return true;
        }

        return false;
    }

    /**
     * Request load Pbap.
     *
     * @param loadInfo {@link PbapLoadInfo}
     * @return boolean if hfp profile is connected and the device
     *              is Ready for downloading,return true.
     */
    private boolean pbapLoadRequest(PbapLoadInfo loadInfo) {
        Log.i(TAG, "loadInfo is " + mPbapManager.getLoadState());
        if (PbapManager.STATE_READY != mPbapManager.getLoadState()) {
            return true;
        }

        if (PbapLoadInfo.TYPE_CONTACTS == loadInfo.mType) {

            if (0x00 != (loadInfo.mAction & PbapLoadInfo.ACTION_SIM)) {
                boolean rlt = false;
                if (loadInfo.mReqCounts > 0) {
                    rlt = mPbapManager.Pbaploadreq(PbapManager.PBAP_SIM,
                            loadInfo.mIsPhotoNeed, 0, loadInfo.mReqCounts);
                }
                else {
                    rlt = mPbapManager.Pbaploadreq(PbapManager.PBAP_SIM,
                            loadInfo.mIsPhotoNeed);
                }

                if (rlt) {

                    mCurrentStorage = PbapManager.PBAP_SIM;
                    return true;
                }

                loadInfo.mAction = loadInfo.mAction
                        & (~PbapLoadInfo.ACTION_SIM);
                Log.i(TAG, "reqPbapDownload PBAP_SIM failed");
            }

            if (0x00 != (loadInfo.mAction & PbapLoadInfo.ACTION_MEMORY)) {
                boolean rlt = false;
                if (loadInfo.mReqCounts > 0) {
                    rlt = mPbapManager.Pbaploadreq(PbapManager.PBAP_PHONE,
                            loadInfo.mIsPhotoNeed, 0, loadInfo.mReqCounts);
                }
                else {
                    rlt = mPbapManager.Pbaploadreq(PbapManager.PBAP_PHONE,
                            loadInfo.mIsPhotoNeed);
                }

                if (rlt) {

                    mCurrentStorage = PbapManager.PBAP_PHONE;
                    return true;
                }
                loadInfo.mAction = loadInfo.mAction
                        & (~PbapLoadInfo.ACTION_MEMORY);
                Log.i(TAG, "reqPbapDownload PBAP_PHONE failed");
            }

            return false;
        }
        else {

            if (0x00 != (loadInfo.mAction & PbapLoadInfo.ACTION_INCOMING)) {
                boolean rlt = false;
                if (loadInfo.mReqCounts > 0) {
                    rlt = mPbapManager.Pbaploadreq(PbapManager.PBAP_INCOMING,
                            false, 0, loadInfo.mReqCounts);
                }
                else {
                    rlt = mPbapManager.Pbaploadreq(PbapManager.PBAP_INCOMING,
                            false);
                }
                if (rlt) {

                    mCurrentStorage = PbapManager.PBAP_INCOMING;
                    return true;
                }
                loadInfo.mAction = loadInfo.mAction
                        & (~PbapLoadInfo.ACTION_INCOMING);
                Log.i(TAG, "reqPbapDownload PBAP_INCOMING failed");
            }

            if (0x00 != (loadInfo.mAction & PbapLoadInfo.ACTION_OUTGOING)) {
                boolean rlt = false;
                if (loadInfo.mReqCounts > 0) {
                    rlt = mPbapManager.Pbaploadreq(PbapManager.PBAP_OUTGOING,
                            false, 0, loadInfo.mReqCounts);
                }
                else {
                    rlt = mPbapManager.Pbaploadreq(PbapManager.PBAP_OUTGOING,
                            false);
                }
                if (rlt) {

                    mCurrentStorage = PbapManager.PBAP_OUTGOING;
                    return true;
                }
                loadInfo.mAction = loadInfo.mAction
                        & (~PbapLoadInfo.ACTION_OUTGOING);
                Log.i(TAG, "reqPbapDownload PBAP_OUTGOING failed");
            }

            if (0x00 != (loadInfo.mAction & PbapLoadInfo.ACTION_MISSED)) {
                boolean rlt = false;
                if (loadInfo.mReqCounts > 0) {
                    rlt = mPbapManager.Pbaploadreq(PbapManager.PBAP_MISSED,
                            false, 0, loadInfo.mReqCounts);
                }
                else {
                    rlt = mPbapManager.Pbaploadreq(PbapManager.PBAP_MISSED,
                            false);
                }
                if (rlt) {

                    mCurrentStorage = PbapManager.PBAP_MISSED;
                    return true;
                }
                loadInfo.mAction = loadInfo.mAction
                        & (~PbapLoadInfo.ACTION_MISSED);
                Log.i(TAG, "reqPbapDownload PBAP_MISSED failed");
            }

            return false;
        }
    }

    /**
     * Bluetooth Pbap download callback.
     */
    private PbapManagercb mPbapManagerCallback = new PbapManagercb() {

        @Override
        public void PbapLoadStatusChanged(String arg0, int arg1, int arg2,
                int arg3, int arg4) {
            Message msg = mHandler.obtainMessage();
            msg.what = EVENT_STATE;
            Bundle bundle = new Bundle();
            bundle.putString("addr", arg0);
            bundle.putInt("prev", arg1);
            bundle.putInt("new", arg2);
            bundle.putInt("reason", arg3);
            bundle.putInt("counts", arg4);
            msg.setData(bundle);
            msg.sendToTarget();
        }

        /**
         *PbapManager.PBAP_SIM == 1;
         *PbapManager.PBAP_PHONE == 2;
         *PbapManager.PBAP_OUTGOING == 3;
         *PbapManager.PBAP_MISSED == 4;
         *PbapManager.PBAP_INCOMING == 5;
         */
        public void PbapDownloadNotifyProgress(int storage, int totalContacts, int downloadedContacts) {
        }
    };

    /**
     * Handler.
     */
    private Handler mHandler = new Handler() {

        public void handleMessage(android.os.Message msg) {
            switch (msg.what) {
                case EVENT_STATE:
                    Bundle bundle = msg.getData();
                    loadStateChanged(bundle.getString("addr"),
                            bundle.getInt("prev"), bundle.getInt("new"),
                            bundle.getInt("reason"), bundle.getInt("counts"));
                    break;
                case EVENT_GET_LOADINFO:
                    if (!mPbapLoadList.isEmpty()) {
                        PbapLoadInfo loadInfo = mPbapLoadList.get(0);
                        if (pbapLoadRequest(loadInfo)) {
                            return;
                        }

                        String action = (PbapLoadInfo.TYPE_CONTACTS
                                == loadInfo.mType) ? Const.SYNC_CONTACT_STOP
                                        : Const.SYNC_CALLLOG_STOP;
                        sendPbapLoadBroadcast(action);
                        mPbapLoadList.remove(0);

                        Message sendMsg = mHandler.obtainMessage();
                        sendMsg.what = EVENT_GET_LOADINFO;
                        sendMsg.sendToTarget();
                    }
                    break;
                default:
                    break;
            }
        }
    };

    /**
     * Change load state.
     *
     * @param address the device MAC address
     * @param prevState previous state
     * @param newState new state
     * @param reason change reason
     * @param counts change counts
     * PbapManager.STATE_INIT == 100;
     * PbapManager.STATE_READY/STATE_DOWNLOAD_COMPLETED == 110;
     * PbapManager.REASON_SUCCESS == 1;
     */
    private void loadStateChanged(String address, int prevState, int newState,
            int reason, int counts) {
        List<PbapLoadInfo> pbapLoadList = mPbapLoadList;
        boolean needRetry = false;
        Log.i(TAG, "prevState = " + prevState);
        Log.i(TAG, "newState = " + newState);
        Log.i(TAG, "reason = " + reason);
        Log.i(TAG, "counts = " + counts);
            if ((PbapManager.STATE_READY == newState)
                && (PbapManager.STATE_INIT != prevState)) {
            List<PbapLoadInfo> rmList = new ArrayList<PbapLoadInfo>();
            boolean rlt = (PbapManager.REASON_SUCCESS == reason) ? true : false;
            if (rlt) {
                for (PbapLoadInfo loadInfo : pbapLoadList) {
                    loadInfo.pbapCheckInfo(mCurrentStorage, counts);
                    if (loadInfo.mAction == 0) {
                        String action = (loadInfo.mType
                                == PbapLoadInfo.TYPE_CONTACTS)
                                ? Const.SYNC_CONTACT_STOP
                                        : Const.SYNC_CALLLOG_STOP;
                        sendPbapLoadBroadcast(action);
                        rmList.add(loadInfo);
                    }
                }

            } else {
                for (PbapLoadInfo loadInfo : pbapLoadList) {
                    if (loadInfo.checkInfoByStorage(mCurrentStorage)) {
                        if (loadInfo.checkTooManyRetries()) {
                            String action = (loadInfo.mType
                                    == PbapLoadInfo.TYPE_CONTACTS)
                                    ? Const.SYNC_CONTACT_STOP
                                            : Const.SYNC_CALLLOG_STOP;
                            sendPbapLoadBroadcast(action);
                            rmList.add(loadInfo);
                        }
                        else {
                            needRetry = true;
                        }
                    }
                }

            }

            for (PbapLoadInfo loadInfo : rmList) {
                pbapLoadList.remove(loadInfo);
            }
            rmList.clear();
        }
        mPbapLoadList = pbapLoadList;
        if (PbapManager.STATE_READY == newState) {
            Message msg = mHandler.obtainMessage(EVENT_GET_LOADINFO);
            // msg.sendToTarget();
            if (needRetry) {
                mHandler.sendMessageDelayed(msg, PbapLoadInfo.RETRIES_DELAY);
            }
            else {
                mHandler.sendMessage(msg);
            }
        }
    };

    /**
     * Send Pbap load broadcast.
     *
     * @param action action
     */
    private void sendPbapLoadBroadcast(String action) {

        if (mOnPbapSyncLinstener != null) {
            if (action.equals(Const.SYNC_CONTACT_STOP)) {
                mOnPbapSyncLinstener.onPhoneBookSyncEnd();
            }
            else if (action.equals(Const.SYNC_CONTACT_START)) {
                mOnPbapSyncLinstener.onPhoneBookSyncStart();
            }
            else if (action.equals(Const.SYNC_CALLLOG_STOP)) {
                mOnPbapSyncLinstener.onCallLogSyncEnd();
            }
            else if (action.equals(Const.SYNC_CALLLOG_START)) {
                mOnPbapSyncLinstener.onCallLogSyncStart();
            }
        }
        if (action.equals(Const.SYNC_CONTACT_STOP)) {
            setSettingPbProvider(Settings.Global.BLUETOOTH_CONTACTS_SYNC, 0);
        }
        else if (action.equals(Const.SYNC_CONTACT_START)) {
            setSettingPbProvider(Settings.Global.BLUETOOTH_CONTACTS_SYNC, 1);
        }
    }

    /**
     * Set setting Pbap provider.
     *
     * @param where name
     * @param value value
     */
    private void setSettingPbProvider(final String where, final int value) {
        new Thread(new Runnable() {

            @Override
            public void run() {
                Settings.Global.putInt(mContext.getApplicationContext()
                        .getContentResolver(), where, value);
                Log.e("GeminiPhoneApp", "setSettingPbProvider Sysc = "
                        + (value == 1 ? "Start" : "End"));
            }

        }).start();
    }

	public void setSyncListener(OnPbapSyncLinstener listen) {
		mOnPbapSyncLinstener = listen;
	}
	public void setHfpStatedChanged(int prevState, int newState) {
		if (android.bluetooth.BluetoothProfile.STATE_CONNECTED == prevState
			&& android.bluetooth.BluetoothProfile.STATE_CONNECTED != newState) {
			stopAllSynchronize();
		}
	}
}
