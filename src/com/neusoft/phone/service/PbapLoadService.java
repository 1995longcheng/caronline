package com.neusoft.phone.service;

import android.app.Service;
import android.bluetooth.BluetoothProfile;
import android.content.Intent;
import android.os.IBinder;
import android.provider.Settings;

import com.neusoft.phone.manager.CallLogManager;
import com.neusoft.phone.manager.ContactsManager;
import com.neusoft.phone.manager.PbapLoadManager;
import com.neusoft.phone.model.PbapLoadInfo;
import com.neusoft.phone.utils.Const;
import android.util.Log;

/**
 * Pbap load service class.
 *
 * @author neusoft
 */
public class PbapLoadService extends Service {
    /** Pbap load manager. {@link PbapLoadManager}*/
    private PbapLoadManager mPbapLoadManager;
    /** Pbap load type.*/
    public static final String LOAD_TYPE = "type";
    /** Pbap load contains.*/
    public static final String LOAD_CONTAINS = "contains";

    /**
     * Constructor.
     */
    public PbapLoadService() {

    }

    @Override
    public void onCreate() {
        Log.i("PbapLoadService", "onCreate");
        super.onCreate();
        mPbapLoadManager = PbapLoadManager.getInstance(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (null != intent) {
            int type = intent
                    .getIntExtra(LOAD_TYPE, PbapLoadInfo.TYPE_CONTACTS);
            if (PbapLoadInfo.TYPE_CONTACTS == type) {
                int action = intent.getIntExtra(LOAD_CONTAINS,
                        PbapLoadInfo.ACTION_SIM | PbapLoadInfo.ACTION_MEMORY);
                if (!mPbapLoadManager.isContactsDownLoading()) {
                    ContactsManager.getInstance(this).cleanContacts();
                    if (!mPbapLoadManager.contactsSynchronize(action)) {
                        Log.e("GeminiPhoneApp", " ==== onStartCommand ==");
                        Settings.Global.putInt(this.getApplicationContext()
                                .getContentResolver(),
                                Settings.Global.BLUETOOTH_CONTACTS_SYNC, 0);
                        Log.e("GeminiPhoneApp", "setSettingPbProvider Sysc Start");
                    }
                }
            }
            else {
                int action = intent.getIntExtra(LOAD_CONTAINS,
                        PbapLoadInfo.ACTION_INCOMING
                                | PbapLoadInfo.ACTION_MISSED
                                | PbapLoadInfo.ACTION_OUTGOING);
                if (!mPbapLoadManager.isCallLogDownLoading()) {
                    CallLogManager.getInstance(this).cleanCallLog(
                            CallLogManager.TYPE_ALL);

                    mPbapLoadManager.callLogSynchronize(action);
                }
            }
        }

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        Log.i("PbapLoadService", "onDestroy");
        super.onDestroy();
    }
}
