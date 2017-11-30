package com.neusoft.phone.model;

import java.util.Arrays;
import java.util.List;

/**
 * Contact info class.
 *
 * @author neusoft
 */
public class ContactInfo {

    /** Contact's name.*/
    public String name = "";
    /** Contact's id.*/
    private long id = -1;
    /** Contact's photo.*/
    private byte[] photo = null;

    /** Contact's name Pinyin.*/
    private String namePinyin = "";
    /** Contact's name sort.*/
    private String nameSort = ""; // pinyin+type
    /** Contact's name first letter.*/
    private String nameFirstLetter = "";

    /** Contact has phone number.*/
    private int hasPhoneNumber = 0;

    /** Contact's phone numbers.*/
    private List<ContactsPhones> phones = null;

    /** Sort key.*/
    private String sortKey = "";

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
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

    public String getSortKey() {
        return sortKey;
    }

    public void setSortKey(String sortKey) {
        this.sortKey = sortKey;
    }

    public List<ContactsPhones> getPhones() {
        return phones;
    }

    public void setPhones(List<ContactsPhones> phones) {
        this.phones = phones;
    }

    public int getHasPhoneNumber() {
        return hasPhoneNumber;
    }

    public void setHasPhoneNumber(int hasPhoneNumber) {
        this.hasPhoneNumber = hasPhoneNumber;
    }

    public void setNamePinyin(String pinyin) {
        this.namePinyin = pinyin;
    }

    public String getNamePinyin() {
        return namePinyin;
    }

    public void setNameSort(String sort) {
        this.nameSort = sort;
    }

    public String getNameSort() {
        return nameSort;
    }

    public void setNameFirstLetter(String firstLetter) {
        this.nameFirstLetter = firstLetter;
    }

    public String getNameFirstLetter() {
        return nameFirstLetter;
    }
}
