package com.neusoft.phone.receiver;

import android.bluetooth.BluetoothHeadset;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.provider.Settings;
import android.util.Log;
import android.view.KeyEvent;

import com.neusoft.phone.manager.PbapLoadManager;
import com.neusoft.phone.utils.Const;
import com.neusoft.phone.utils.PhoneUtils;

/**
 * Broadcast receiver class.
 *
 * @author neusoft
 */
public class BootBroadcastReceiver extends BroadcastReceiver {

    private static final String TAG = "GeminiPhoneBootBroadcastReceiver";

	@Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)) {
            Log.i(TAG, "clear missed call data");
            //未接来电清零
            Settings.Global.putInt(context.getContentResolver(), "missed_call_count", 1);
        }
        if (intent.getAction().equals(
                BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED)) {
            Log.i(TAG, "ACTION_CONNECTION_STATE_CHANGED");
            int newState = intent.getIntExtra(BluetoothProfile.EXTRA_STATE, 0);
            int prevState = intent.getIntExtra(
                    BluetoothProfile.EXTRA_PREVIOUS_STATE, 0);
            PhoneUtils.getInstance(context).setHfpStateChanged(prevState, newState);
            if (PbapLoadManager.hasInitDone()) {
                PbapLoadManager.getInstance(context).setHfpStatedChanged(prevState, newState);
            }
//        } else if (intent.getAction().equals(Const.ACTION_HARDKEY_SHORTPRESS)){
//            int keyCode = intent.getIntExtra(Intent.EXTRA_C3_HARDKEY_KEYCODE, 0);
//            if (keyCode == KeyEvent.KEYCODE_CUSTOMIZED) {
//                //Intent openActivity = new Intent(context, MainActivity.class);
//                openActivity.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//                context.startActivity(openActivity);
//            }
//        } else if (intent.getAction().equals(Const.VRMMS_PHONE_BROADCAST)) {
//            String operator = intent.getStringExtra(Const.VRMMS_OPERATION_KEY);
//            Log.i("BootBroadcastReceiver", "Receive VR");
//            Log.i("BootBroadcastReceiver", "operator = " + operator);
//            Intent intentPhone = new Intent(context, MainActivity.class);
//            Intent broadIntent = new Intent("com.vrmms.intent.VRMMSMAIN");
//            long timer = intent.getLongExtra(Const.VRMMS_TIMER_KEY, 0);
//            if (operator.equals(Const.VRMMS_COMMAND_START)) {
//                intentPhone.putExtra(MainActivity.SELECTEDTAB_KEY, 0);
//                intentPhone.putExtra(Const.VRMMS_ACTION_KEY, 1);
//                intentPhone.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//                context.startActivity(intentPhone);
//                broadIntent.putExtra(Const.VRMMS_RESPONSE_KEY, 0);
//                broadIntent.putExtra(Const.VRMMS_TIMER_KEY, timer);
//            } else if (operator.equals(Const.VRMMS_COMMAND_PHONE_CALLLOG)) {
//                int index = intent
//                        .getIntExtra(Const.VRMMS_PARAM_PHONE_INDEX, 0);
//                Log.i("BootBroadcastReceiver", "index = " + index);
//                intentPhone.putExtra(MainActivity.SELECTEDTAB_KEY,
//                        (index - 1 < 0) ? 0 : (index -1));
//                intentPhone.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//                context.startActivity(intentPhone);
//                broadIntent.putExtra(Const.VRMMS_RESPONSE_KEY, 0);
//                broadIntent.putExtra(Const.VRMMS_TIMER_KEY, timer);
//            } else if (operator.equals(Const.VRMMS_COMMAND_PHONE_CALL)) {
//                String phoneNumber = intent
//                        .getStringExtra(Const.VRMMS_PARAM_PHONE_NUMBER);
//                Log.i("BootBroadcastReceiver", "phoneNumber = " + phoneNumber);
//                PhoneUtils.getInstance(context).dialTo("", phoneNumber);
//                // 广播action
//                broadIntent.putExtra(Const.VRMMS_RESPONSE_KEY, 0);
//                // 时间戳为接受到com.vrmms.intent.PHONE这个广播携带的时间戳
//                broadIntent.putExtra(Const.VRMMS_TIMER_KEY, timer);
//            }
//            context.sendBroadcast(broadIntent);
//
//        }
//        else if (intent.getAction().equals(Intent.ACTION_SCREEN_ON)) {
//            PhoneUtils.getInstance(context).processTransferToCar();
//        }
//        else if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
//            PhoneUtils.getInstance(context).processTransferToPhone();
//        } else if (intent.getAction().equals("action.gemini.start.app")) {
//            Log.i("BootBroadcastReceiver", "action.gemini.start.app");
//            if (("com.neusoft.phone").equals(intent.getExtra("extra.app.pkg")) &&
//                    ("com.neusoft.phone.InCallScreen").equals
//                    (intent.getExtra("extra.app.activity"))) {
//                Log.i("BootBroadcastReceiver", "getCurrentHfpInfo = " +
//                        PhoneUtils.getInstance(context).getCurrentHfpInfo());
//                if (PhoneApp.isCallStarted &&
//                        (PhoneUtils.getInstance(context).getCurrentHfpInfo() != null)) {
//                    Intent smartIntent = new Intent(context, com.neusoft.phone.InCallScreen.class);
//                    smartIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//                    context.startActivity(smartIntent);
//                } else {
//                    //never hope this works because that means a hfp error
//                    PhoneUtils.getInstance(context).finishCall();
//                }
//            }
        }
    }
}
