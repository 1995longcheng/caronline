package com.neusoft.phone.model;

import android.content.res.Resources;

import com.neusoft.phonedemo.R.string;

import java.util.Arrays;

/**
 * Calllog info class.
 *
 * @author neusoft
 */
public class CallLogInfo {

    /** Callong id.*/
    private long id = 0;
    /** Callong type.*/
    private int callType = 0;
    /** Callong phone number.*/
    private String phone = "";
    /** Callong call time.*/
    private long callTime = 0;
    /** Callong caller name.*/
    private String name = "";
    /** Callong contact id.*/
    private long contactId = -1;
    // private Bitmap photo = null;
    /** Callong contact photo.*/
    private byte[] photo = null;;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public int getCallType() {
        return callType;
    }

    public void setCallType(int callType) {
        this.callType = callType;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public long getCallTime() {
        return callTime;
    }

    public void setCallTime(long callTime) {
        this.callTime = callTime;
    }

    public String getName() {
        return name;
    }

    /**
     * Get contact's name.
     *
     * @param res Resources
     * @return contact's name
     */
    public String getName(Resources res) {
        String unKnown = res.getString(string.unknown);
        if (null == name) {
            return unKnown;
        }
        else if (name.isEmpty()) {
            return unKnown;
        }
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getContactId() {
        return contactId;
    }

    public void setContactId(long contactId) {
        this.contactId = contactId;
    }

    public byte[] getPhoto() {
        return photo;
    }

    /**
     * Set contact's photo.
     *
     * @param b byte[] original
     */
    public void setPhoto(byte[] b) {
        if (b == null || b.length <= 0) {
            return;
        }
        photo = Arrays.copyOfRange(b, 0, b.length);
    }
    // public Bitmap getPhoto() {
    // return photo;
    // }
    // public void setPhoto(Bitmap photo) {
    // this.photo = photo;
    // }
}
