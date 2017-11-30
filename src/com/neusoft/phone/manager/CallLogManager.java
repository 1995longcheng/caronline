package com.neusoft.phone.manager;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.provider.CallLog;
import android.provider.NeusoftContactsContract;
import android.text.TextUtils;

import com.neusoft.phone.model.CallLogInfo;
import com.neusoft.phone.model.ContactInfo;
import com.neusoft.phonedemo.R;

/**
 * Calllog manager class.
 *
 * @author neusoft
 */
public class CallLogManager {

    /** All call.*/
    public static final int TYPE_ALL = 0;
    /** Missed call.*/
    public static final int TYPE_MISSED = CallLog.Calls.MISSED_TYPE;
    /** Incoming call.*/
    public static final int TYPE_RECEIVED = CallLog.Calls.INCOMING_TYPE;
    /** Outgoning call.*/
    public static final int TYPE_DIALED = CallLog.Calls.OUTGOING_TYPE;
    //max item counts of each type
    private static final int MAX_COUNT = 100;

    /** Caller name column index.*/
    private static int mColumnIndexCallerName = -1;
    /** Calllog id column index.*/
    private static int mColumnIndexCalllogId = -1;
    /** Call number column index.*/
    private static int mColumnIndexCallNumber = -1;
    /** Calllog type column index.*/
    private static int mColumnIndexCalllogType = -1;
    /** Call time column index.*/
    private static int mColumnIndexCallTime = -1;
    /** Call duration column index.*/
    private static int mColumnIndexCallDuration = -1;

    /** Calllog info list.*/
    private List<CallLogInfo> mListCallLog;

    /** Context.*/
    private static Context mContext;

    /** Instance, calllog manager.*/
    private static CallLogManager mInstance;

    /** Search tag.*/
    private String mSearchTag;

    /**
     * Get instance.
     *
     * @param context context
     * @return instance {@link CallLogManager}
     */
    public static CallLogManager getInstance(Context context) {
        synchronized (CallLogManager.class) {
            if (mInstance == null) {
                mInstance = new CallLogManager();
                mContext = context;
            }
        }
        return mInstance;
    }

    /**
     * Constructor.
     */
    private CallLogManager() {
        mListCallLog = new ArrayList<CallLogInfo>();
    }

    /**
     * Get calllog list by type.
     *
     * @param callType call type
     * @return calllog info list
     */
    public List<CallLogInfo> getCallLogList(int callType) {
        return searchCallLogList(callType);
    }

    /**
     * Get calllog list by type and search tag.
     *
     * @param callType call type
     * @param where search tag
     * @return calllog info list
     */
    public List<CallLogInfo> getCallLogList(int callType, String where) {
        mSearchTag = where;
        return searchCallLogList(callType);
    }

    /**
     * Update calllog list.
     */
    public void updateCallLogList() {
        loadCallLogData(TYPE_ALL);
    }

    /**
     * Calllog list is empty or not.
     *
     * @return true calllog is empty.
     *         false calllog is not empty.
     */
    public boolean isNoData() {
        return (mListCallLog.isEmpty());
    }

    /**
     * Clean calllog.
     *
     * @param storage storage call type
     */
    public void cleanCallLog(int storage) {
        mListCallLog.clear();
        if (TYPE_ALL == storage) {
            mContext.getContentResolver().delete(
                    NeusoftContactsContract.NEUSOFT_CALLLOG_URI, null, null);
        }
        else {
            mContext.getContentResolver().delete(
                    NeusoftContactsContract.NEUSOFT_CALLLOG_URI,
                    "type = " + String.valueOf(storage), null);
        }
    }

    /**
     * Search calllog list by call type.
     *
     * @param callType calltype
     * @return calllog info list
     */
    private List<CallLogInfo> searchCallLogList(int callType) {
        List<CallLogInfo> listCallLog = mListCallLog;
        List<CallLogInfo> display = new ArrayList<CallLogInfo>();

        if (TYPE_ALL == callType) {
            searchAllCallLog(listCallLog, display);
        } else {
            for (int i = 0 ; i < listCallLog.size() ; i++) {
                CallLogInfo callLog = listCallLog.get(i);
                if (callType == callLog.getCallType() && display.size() < MAX_COUNT) {
                    display.add(callLog);
                }
            }
        }

        if (!TextUtils.isEmpty(mSearchTag)) {
            List<CallLogInfo> searchDisplay = new ArrayList<CallLogInfo>();
            for (int j = 0; j < display.size(); j++) {
                CallLogInfo callLogToSearch = display.get(j);
                if (callLogToSearch.getPhone() != null &&
                        callLogToSearch.getPhone().contains(mSearchTag)) {
                    searchDisplay.add(callLogToSearch);
                }
            }
            return searchDisplay;
        }

        return display;
    }

    private void searchAllCallLog(List<CallLogInfo> listCallLog, List<CallLogInfo> display) {
        int displayDialed = 0;
        int displayMissed = 0;
        int displayReceived = 0;
        for (int i = 0; i < listCallLog.size(); i++) {
            CallLogInfo callLog = listCallLog.get(i);
            if (TYPE_MISSED == callLog.getCallType() && displayMissed < MAX_COUNT) {
                display.add(callLog);
                displayMissed++;
            } else if (TYPE_RECEIVED == callLog.getCallType() && displayReceived < MAX_COUNT) {
                display.add(callLog);
                displayReceived++;
            } else if (TYPE_DIALED == callLog.getCallType() && displayDialed < MAX_COUNT) {
                display.add(callLog);
                displayDialed++;
            }
        }
    }

    /**
     * Load calllog data by call type.
     *
     * @param callType call type
     */
    private void loadCallLogData(int callType) {
        // loadCallLogData(callType,null);
        loadCallLogDataLocal(callType, null);
    }

    /**
     * Load calllog data from local.
     *
     * @param callType call type
     * @param where search tag
     */
    private void loadCallLogDataLocal(int callType, String where) {
        List<CallLogInfo> listCallLog = new ArrayList<CallLogInfo>();
        ContentResolver resolver = mContext.getContentResolver();
        String searchCase = null;
        switch (callType) {
            case TYPE_ALL:
                searchCase = null;
                break;
            case TYPE_MISSED:
                searchCase = NeusoftContactsContract.CallLogColumns.TYPE
                        + " = "
                        + String.valueOf(NeusoftContactsContract
                                .CallLogType.MISSED_TYPE);
                break;
            case TYPE_RECEIVED:
                searchCase = NeusoftContactsContract.CallLogColumns.TYPE
                        + " = "
                        + String.valueOf(NeusoftContactsContract
                                .CallLogType.INCOMING_TYPE);
                break;
            case TYPE_DIALED:
                searchCase = NeusoftContactsContract.CallLogColumns.TYPE
                        + " = "
                        + String.valueOf(NeusoftContactsContract
                                .CallLogType.OUTGOING_TYPE);
                break;
            default:
                searchCase = NeusoftContactsContract.CallLogColumns.TYPE + " = "
                        + String.valueOf(callType);
                break;
        }

        if (!TextUtils.isEmpty(where)) {
            if (searchCase == null) {
                searchCase = where;
            }
            else {
                searchCase = searchCase + " and " + where;
            }
        }

        Cursor cursor = resolver.query(
                NeusoftContactsContract.NEUSOFT_CALLLOG_URI, null, searchCase,
                null, NeusoftContactsContract.CallLogColumns.DATE + " DESC");

        while (cursor.moveToNext()) {

            if (mColumnIndexCallerName < 0) {
                mColumnIndexCallerName = cursor.getColumnIndex(
                        NeusoftContactsContract.CallLogColumns.DISPLAY_NAME);
            }

            if (mColumnIndexCalllogId < 0) {
                mColumnIndexCalllogId = cursor.getColumnIndex(
                        NeusoftContactsContract.CallLogColumns._ID);
            }

            if (mColumnIndexCallNumber < 0) {
                mColumnIndexCallNumber = cursor.getColumnIndex(
                        NeusoftContactsContract.CallLogColumns.NUMBER);
            }

            if (mColumnIndexCalllogType < 0) {
                mColumnIndexCalllogType = cursor.getColumnIndex(
                        NeusoftContactsContract.CallLogColumns.TYPE);
            }

            if (mColumnIndexCallTime < 0) {
                mColumnIndexCallTime = cursor.getColumnIndex(
                        NeusoftContactsContract.CallLogColumns.DATE);
            }

            String callerName = cursor.getString(mColumnIndexCallerName);

            long callLogId = cursor.getLong(mColumnIndexCalllogId);

            String callNumber = cursor.getString(mColumnIndexCallNumber);
            if(null != callNumber && callNumber.contains("-")){
            	callNumber = callNumber.replace("-","");
            }

            int callLogType = cursor.getInt(mColumnIndexCalllogType);

            long callTime = cursor.getLong(mColumnIndexCallTime);

            long contactId = -1;
            byte[] photo = null;

            CallLogInfo callLogInfo = new CallLogInfo();
            callLogInfo.setId(callLogId);
            callLogInfo.setName(formatName(callerName));
            callLogInfo.setCallType(callLogType);
            callLogInfo.setPhone(formatNumber(callNumber));
            callLogInfo.setCallTime(callTime);
            callLogInfo.setContactId(contactId);
            callLogInfo.setPhoto(photo);
            listCallLog.add(callLogInfo);
        }
        cursor.close();
        if (listCallLog.isEmpty()) {
            mListCallLog.clear();
        } else {
            mListCallLog = listCallLog;
        }
    }

    private String formatName(String name) {
        if (TextUtils.isEmpty(name)) {
            return name;
        }
        byte[] nameByte = null;
        try {
            nameByte = name.getBytes("UTF-8");
            if (nameByte.length > 31) {
                nameByte = Arrays.copyOfRange(nameByte, 0, 31);
                name = new String(nameByte,"UTF-8");
                if (name.contains("�")) {
                    name = name.replace("�", "");
                }
            }
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return name;
    }

    private String formatNumber(String number) {
        if (!TextUtils.isEmpty(number) && (number.length() > 31)) {
            number = number.substring(0, 31);
        }
        if (mContext != null && number.equals(mContext.getString
                (R.string.unknown_number_untranslate))) {
            return mContext.getString(R.string.unknown_number);
        }
        return number;
    }

    /**
     * Load calllog data.
     *
     * @param callType call type
     * @param where search tag
     */
    @SuppressWarnings("unused")
    private void loadCallLogData(int callType, String where) {
        List<CallLogInfo> listCallLog = new ArrayList<CallLogInfo>();
        ContentResolver resolver = mContext.getContentResolver();
        String searchCase = null;
        switch (callType) {
            case TYPE_ALL:
                searchCase = null;
                break;
            case TYPE_MISSED:
                searchCase = CallLog.Calls.TYPE + " = "
                        + String.valueOf(CallLog.Calls.MISSED_TYPE);
                break;
            case TYPE_RECEIVED:
                searchCase = CallLog.Calls.TYPE + " = "
                        + String.valueOf(CallLog.Calls.INCOMING_TYPE);
                break;
            case TYPE_DIALED:
                searchCase = CallLog.Calls.TYPE + " = "
                        + String.valueOf(CallLog.Calls.OUTGOING_TYPE);
                break;
            default:
                searchCase = CallLog.Calls.TYPE + " = "
                        + String.valueOf(callType);
                break;
        }

        if (!TextUtils.isEmpty(where)) {
            if (searchCase == null) {
                searchCase = where;
            }
            else {
                searchCase = searchCase + " and " + where;
            }
        }

        Cursor cursor = resolver.query(CallLog.Calls.CONTENT_URI, null,
                searchCase, null, CallLog.Calls.DATE + " DESC");

        while (cursor.moveToNext()) {

            // 取得数据列索引
            if (mColumnIndexCallerName < 0) {
                mColumnIndexCallerName = cursor
                        .getColumnIndex(CallLog.Calls.CACHED_NAME);
            }

            if (mColumnIndexCalllogId < 0) {
                mColumnIndexCalllogId = cursor
                        .getColumnIndex(CallLog.Calls._ID);
            }

            if (mColumnIndexCallNumber < 0) {
                mColumnIndexCallNumber = cursor
                        .getColumnIndex(CallLog.Calls.NUMBER);
            }

            if (mColumnIndexCalllogType < 0) {
                mColumnIndexCalllogType = cursor
                        .getColumnIndex(CallLog.Calls.TYPE);
            }

            if (mColumnIndexCallTime < 0) {
                mColumnIndexCallTime = cursor
                        .getColumnIndex(CallLog.Calls.DATE);
            }

            if (mColumnIndexCallDuration < 0) {
                mColumnIndexCallDuration = cursor
                        .getColumnIndex(CallLog.Calls.DURATION);
            }

            // 得到名字
            String callerName = cursor.getString(mColumnIndexCallerName);
            // 得到ID
            long callLogId = cursor.getLong(mColumnIndexCalllogId);
            // 得到电话号码
            String callNumber = cursor.getString(mColumnIndexCallNumber);

            int callLogType = cursor.getInt(mColumnIndexCalllogType);

            long callTime = cursor.getLong(mColumnIndexCallTime);

            // String duration = cursor.getString(mColumnIndex_CALL_DURATION);

            long contactId = -1;

            if (!TextUtils.isEmpty(callerName)
                    && !TextUtils.isEmpty(callNumber)) {

                ContactInfo contact = ContactsManager.queryContactByNameAndNum(
                        callerName, callNumber);
                if (null != contact) {
                    contactId = contact.getId();
                }
            }

            CallLogInfo callLogInfo = new CallLogInfo();
            callLogInfo.setId(callLogId);
            callLogInfo.setName(callerName);
            callLogInfo.setCallType(callLogType);
            callLogInfo.setPhone(callNumber);
            callLogInfo.setCallTime(callTime);
            callLogInfo.setContactId(contactId);
            listCallLog.add(callLogInfo);
        }
        cursor.close();
        mListCallLog = listCallLog;
    }
}
