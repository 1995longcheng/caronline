package com.neusoft.phone.ui;

import android.app.Application;

import com.neusoft.phone.manager.CallLogManager;
import com.neusoft.phone.manager.ContactsManager;
import com.neusoft.phone.utils.PhoneUtils;

/**
 * PhoneApp.
 *
 * @author neusoft
 */
public class PhoneApp extends Application {
    /** Call time.*/
//    public static long mCallTimes[] = {-1, -1, -1, -1, -1};
	private PhoneUtils mPhoneUtils = null;

    @Override
    public void onCreate() {
        super.onCreate();
        mPhoneUtils  = PhoneUtils.getInstance(this);
        if(mPhoneUtils != null){
        	mPhoneUtils.setmMainActivityIntent("com.neusoft.phone.ui.PhoneActivity");
        	//mPhoneUtils.setmInCallScreenIntent("com.neusoft.phone.ui.InCallScreen");
        }
        ContactsManager.getInstance(this);
        CallLogManager.getInstance(this);
    }
}
