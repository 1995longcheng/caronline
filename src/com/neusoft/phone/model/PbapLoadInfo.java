package com.neusoft.phone.model;

import com.neusoft.c3alfus.bluetooth.pbap.PbapManager;

/**
 * Pbap load info class.
 *
 * @author neusoft
 */
public class PbapLoadInfo {

    /** Type:contacts.*/
    public static final int TYPE_CONTACTS = 0;
    /** Type:calllog.*/
    public static final int TYPE_CALLLOG = 1;

    /** Action:SIM contacts.*/
    public static final int ACTION_SIM = 0x01;
    /** Action:memory contacts.*/
    public static final int ACTION_MEMORY = 0x02;

    /** Action:incoming call.*/
    public static final int ACTION_INCOMING = 0x04;
    /** Action:outgoing call.*/
    public static final int ACTION_OUTGOING = 0x08;
    /** Action:missed call.*/
    public static final int ACTION_MISSED = 0x010;

    /** Retries delay.*/
    public static final int RETRIES_DELAY = 2500; // in ms
    /** Max number of retries.*/
    public static final int MAX_RETRIES = 3;

    /** Device address.*/
    private String mAddress;

    /** Action.*/
    public int mAction;

    /** Need load contact photo or not.*/
    public boolean mIsPhotoNeed;

    /** Type.*/
    public int mType;

    /** Max request counts.*/
    public int mReqCounts;

    /** Number of retries.*/
    public int mRetries = 0;

    /**
     * Constructor.
     *
     * @param address device address
     * @param type TYPE_CONTACTS = 0;
     *             TYPE_CALLLOG = 1
     * @param action ACTION_SIM = 0x01;
     *               ACTION_MEMORY = 0x02;
     *               ACTION_INCOMING = 0x04;
     *               ACTION_OUTGOING = 0x08;
     *               ACTION_MISSED = 0x010;
     * @param isPhotoNeed need load contact photo or not
     * @param max max request counts
     */
    public PbapLoadInfo(String address, int type, int action,
            boolean isPhotoNeed, int max) {
        mAddress = address;
        mType = type;
        mAction = action;
        mIsPhotoNeed = isPhotoNeed;
        mReqCounts = max;
    }

    /**
     * Check Pbap load info.
     *
     * @param storage PbapManager.PBAP_SIM;
     *                PbapManager.PBAP_PHONE;
     *                PbapManager.PBAP_INCOMING;
     *                PbapManager.PBAP_OUTGOING;
     *                PbapManager.PBAP_MISSED.
     * @param counts counts
     * @return true check Pbap info OK
     *         false check Pbap info NG
     */
    public boolean pbapCheckInfo(int storage, int counts) {
        if (TYPE_CONTACTS == mType) {
            if (mReqCounts > 0) {
                if (counts >= mReqCounts) {
                    mAction = 0;
                    return true;
                }
                else {
                    mReqCounts = mReqCounts - counts;
                }
            }

            if (PbapManager.PBAP_SIM == storage) {
                mAction = (mAction & ~ACTION_SIM);
                return true;
            }

            if (PbapManager.PBAP_PHONE == storage) {
                mAction = (mAction & ~ACTION_MEMORY);
                return true;
            }
        }
        else {

            if (PbapManager.PBAP_INCOMING == storage) {
                mAction = (mAction & ~ACTION_INCOMING);
                return true;
            }

            if (PbapManager.PBAP_OUTGOING == storage) {
                mAction = (mAction & ~ACTION_OUTGOING);
                return true;
            }

            if (PbapManager.PBAP_MISSED == storage) {
                mAction = (mAction & ~ACTION_MISSED);
                return true;
            }
        }
        return false;
    }

    /**
     * Check whether the number of retries exceeds MAX_RETRIES or not.
     *
     * @return true the number of retries exceeds MAX_RETRIES
     *         false the number of retries less than MAX_RETRIES
     */
    public boolean checkTooManyRetries() {
        if (++mRetries >= MAX_RETRIES) {
            return true;
        }
        return false;
    }

    /**
     * Check Pbap load info by storage type.
     *
     * @param storage PbapManager.PBAP_SIM;
     *                PbapManager.PBAP_PHONE;
     *                PbapManager.PBAP_INCOMING;
     *                PbapManager.PBAP_OUTGOING;
     *                PbapManager.PBAP_MISSED
     * @return true check OK
     *         false check NG
     */
    public boolean checkInfoByStorage(int storage) {
        if ((mType == TYPE_CONTACTS && (storage == PbapManager.PBAP_SIM
                || storage == PbapManager.PBAP_PHONE))
                || (mType == TYPE_CALLLOG
                && (storage == PbapManager.PBAP_INCOMING
                || storage == PbapManager.PBAP_OUTGOING
                || storage == PbapManager.PBAP_MISSED))) {
            return true;
        }
        return false;
    }
}
