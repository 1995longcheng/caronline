package com.neusoft.phone.model;

/**
 * Contact phone numbers class.
 *
 * @author neusoft
 *
 */
public class ContactsPhones {

    /** Contact phone number.*/
    private String number;
    /** Contact phone type.*/
    private int type = -1;

    public void setNumber(String number) {

        this.number = number;
    }

    public void setType(int type) {

        this.type = type;
    }

    public int getType() {
        return type;
    }

    public String getNumber() {
        return number;
    }
}
