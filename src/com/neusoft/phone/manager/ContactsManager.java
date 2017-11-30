package com.neusoft.phone.manager;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.text.Collator;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.Locale;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.text.TextUtils;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.NeusoftContactsContract;
import android.util.Log;

import com.neusoft.phone.model.ContactInfo;
import com.neusoft.phone.model.ContactsPhones;
import com.neusoft.phonedemo.R;
import com.neusoft.phone.utils.HanziToPinyin;

/**
 * Contacts manager class.
 *
 * @author neusoft
 */
public class ContactsManager {

    /** One contact has 5 numbers at most.*/
    public static final int PHONES_SIZE = 5;
    // for pinyin sort
    //zh.lu ST_Bug #54402 Start
    //Priority Modified:
    //PRIORITY_OTHER>PRIORITY_NUMBER>PRIORITY_LETTER>PRIORITY_PINYIN
    /** Priority number.*/
    private static final String PRIORITY_LETTER = "α*";
    /** Priority letter.*/
    private static final String PRIORITY_OTHER = "!";
    /** Priority pinyin.*/
    private static final String PRIORITY_NUMBER = "#";
    /** Priority other.*/
    private static final String PRIORITY_PINYIN = "β*";
    /** Priority null.*/
    private static final String PRIORITY_NULL = "γ*";
    //zh.lu ST_Bug #54402 End

    /**Context.*/
    private static Context mContext = null;
    /** Contact name column index.*/
    private static int mColumnIndexName = -1;
    /** Contact id column index.*/
    private static int mColumnIndexContactId = -1;
    /** Contact has phone number column index.*/
    private static int mColumnIndexHasNumberPhone = -1;
    /** Contact sort key primary column index.*/
    private static int mColumnIndexSortKeyPrimary = -1;

    /** Instance, contacts manager.*/
    private static ContactsManager mInstance;

    /** Contacts list. {@link ContactInfo}*/
    private static List<ContactInfo> mListContacts;
    /** Search contacts tag.*/
    private String mSearchTag;

    /** Is update thread stop.*/
    private static boolean mIsUpdateThreadStop = false;
    /** Synchronized lock.*/
    private Object mLock = new Object();

    /**
     * Constructor.
     */
    private ContactsManager() {
        mListContacts = new ArrayList<ContactInfo>();
    }

    /**
     * Get instance.
     *
     * @param context context
     * @return instance {@link ContactsManager}
     */
    public static ContactsManager getInstance(Context context) {
        synchronized (ContactsManager.class) {
            if (mInstance == null) {
                mInstance = new ContactsManager();
                mContext = context;
            }
        }
        return mInstance;
    }

    public List<ContactInfo> getContactsList() {
        return searchContactsList();
    }

    /**
     * Get contacts list by search tag.
     *
     * @param where search tag
     * @return contacts info list
     */
    public List<ContactInfo> getContactsList(String where) {
        mSearchTag = where;
        return searchContactsList();
    }

    /**
     * Sync update contacts list.
     */
    public synchronized void updateContactsList() {
        mListContacts.clear();
        loadPhoneContactsLocal(null, null);
    }

    /**
     * Update contacts list for number.
     */
    public synchronized void updateContactsListForNum() {

        new Thread(new Runnable() {
            @Override
            public void run() {
                synchronized (mLock) {
                    int size = mListContacts.size();
                    if (size > 0) {
                        mIsUpdateThreadStop = false;
                    }
                    int i = 0;
                    while (i < size && !mIsUpdateThreadStop) {
                        ContactInfo info = null;
                        try {
                            info = mListContacts.get(i);
                        }
                        catch (Exception e) {
                            break;
                        }
                        if (info == null) {
                            break;
                        }
                        i++;
                        List<ContactsPhones> diaplayList
                                = new ArrayList<ContactsPhones>();
                        if (0 != info.getHasPhoneNumber()) {
                            List<ContactsPhones> phones
                                    = queryContactsPhoneByContactId(info
                                            .getId());
                            int phonesCount = phones.size();

                            phonesCount = (phonesCount > PHONES_SIZE)
                                    ? PHONES_SIZE : phonesCount;
                            for (int loop = 0; loop < phonesCount; loop++) {
                                diaplayList.add(phones.get(loop));
                            }
                        }
                        info.setPhones(diaplayList);
                    }
                }
            }
        }).start();
    }

    /**
     * Get contacts info list by id. 
     *
     * @param contactId contact id
     * @return contacts info list
     */
    public ContactInfo queryListContactInfoById(long contactId) {
        List<ContactInfo> listContacts = mListContacts;
        if (contactId < 0) {
            return null;
        }

        if (null == listContacts) {
            return null;
        }

        if (listContacts.isEmpty()) {
            return null;
        }

        for (ContactInfo contact : listContacts) {

            if (contactId == contact.getId()) {
                return contact;
            }
        }

        return null;
    }

    /**
     * check list is null or not
     *
     * @return true contacts list is empty
     *         false contacts list is not empty
     */
    public boolean isNoData() {
        return (mListContacts.isEmpty());
    }

    /**
     * clean Contacts
     *
     * @param contactId
     * @return
     */
    public void cleanContacts() {
        mIsUpdateThreadStop = true;
        ContentResolver cr = mContext.getContentResolver();
        cr.delete(NeusoftContactsContract.NEUSOFT_CONTACT_URI, null, null);
        synchronized (mLock) {
            mListContacts.clear();
        }
    }

    /**
     * Search contacts list.
     *
     * @return contacts info list.
     */
    private synchronized List<ContactInfo> searchContactsList() {
        List<ContactInfo> listContacts = mListContacts;
        List<ContactInfo> display = new ArrayList<ContactInfo>();
        if (null == listContacts) {
            return display;
        }

        try {
            for (ContactInfo contact : listContacts) {
                if ((null == mSearchTag) || (0 == mSearchTag.length())) {
                    display.add(contact);
                    continue;
                }

                if (null != contact.getName()) {
                    if (contact.getName().contains(mSearchTag)) {
                        display.add(contact);
                        continue;
                    }
                }

                if (!TextUtils.isEmpty(contact.getNameFirstLetter())) {
                    if (contact.getNameFirstLetter().startsWith(mSearchTag.toUpperCase())) {
                        display.add(contact);
                        continue;
                    }
                }

                if ((!TextUtils.isEmpty(contact.getNameFirstLetter())) &&
                        (!TextUtils.isEmpty(contact.getNamePinyin()))) {
                    if (contact.getNamePinyin().startsWith(mSearchTag.toUpperCase())) {
                        display.add(contact);
                        continue;
                    }
                }

                if (0 != contact.getHasPhoneNumber()) {
                    if (contact.getPhones() != null) {
                        for (ContactsPhones phones : contact.getPhones()) {
                            if (phones.getNumber().replace("-", "")
                                    .contains(mSearchTag)) {
                                display.add(contact);
                                break;
                            }
                        }
                    }
                    continue;
                }
            }
        } catch (ConcurrentModificationException e){
            Log.e("ContactsManager", "ConcurrentModificationException is " + e);
        }
        return display;
    }

    /**
     * Load phone contacts from local.
     *
     * @param where search tag
     * @param args selectionArgs
     */
    private void loadPhoneContactsLocal(String where, String[] args) {
        List<ContactInfo> listContacts = new ArrayList<ContactInfo>();
        ContentResolver resolver = mContext.getContentResolver();
        Cursor cursor = resolver.query(
                NeusoftContactsContract.NEUSOFT_CONTACT_URI, null, where, args,
                null);
        HanziToPinyin pinyinInstance = HanziToPinyin.getInstance();

        if (cursor == null) {
            return;
        }
        int columnIndexId = cursor.getColumnIndex(
                NeusoftContactsContract.ContactColumns._ID);
        int columnIndexName = cursor.getColumnIndex(
                NeusoftContactsContract.ContactColumns.DISPLAY_NAME);
        int columnIndexPhone1 = cursor.getColumnIndex(
                NeusoftContactsContract.ContactColumns.PHONE1);
        int columnIndexPhone2 = cursor.getColumnIndex(
                NeusoftContactsContract.ContactColumns.PHONE2);
        int columnIndexPhone3 = cursor.getColumnIndex(
                NeusoftContactsContract.ContactColumns.PHONE3);
        int columnIndexPhone4 = cursor.getColumnIndex(
                NeusoftContactsContract.ContactColumns.PHONE4);
        int columnIndexPhone5 = cursor.getColumnIndex(
                NeusoftContactsContract.ContactColumns.PHONE5);
        int columnIndexPhoto = cursor.getColumnIndex(
                NeusoftContactsContract.ContactColumns.PHOTO);
        long id;
        String name;
        String phone1;
        String phone2;
        String phone3;
        String phone4;
        String phone5;
        byte[] photo;
        int hasPhoneNumber = 0;
        ContactsPhones phone;
        List<ContactsPhones> phoneList;
        while (cursor.moveToNext()) {
            id = cursor.getLong(columnIndexId);
            name = cursor.getString(columnIndexName);
            phone1 = cursor.getString(columnIndexPhone1);
            phone2 = cursor.getString(columnIndexPhone2);
            phone3 = cursor.getString(columnIndexPhone3);
            phone4 = cursor.getString(columnIndexPhone4);
            phone5 = cursor.getString(columnIndexPhone5);
            photo = cursor.getBlob(columnIndexPhoto);
            phoneList = new ArrayList<ContactsPhones>();
            if (phone1 != null) {
                phone = new ContactsPhones();
                phone.setNumber(formatNumber(phone1));
                phoneList.add(phone);
            }
            if (phone2 != null) {
                phone = new ContactsPhones();
                phone.setNumber(formatNumber(phone2));
                phoneList.add(phone);
            }
            if (phone3 != null) {
                phone = new ContactsPhones();
                phone.setNumber(formatNumber(phone3));
                phoneList.add(phone);
            }
            if (phone4 != null) {
                phone = new ContactsPhones();
                phone.setNumber(formatNumber(phone4));
                phoneList.add(phone);
            }
            if (phone5 != null) {
                phone = new ContactsPhones();
                phone.setNumber(formatNumber(phone5));
                phoneList.add(phone);
            }

            ContactInfo contactInfo = new ContactInfo();
            if (phoneList.size() > 0) {
                hasPhoneNumber = 1;
                contactInfo.setPhones(phoneList);
            }
            else {
                hasPhoneNumber = 0;
            }
            contactInfo.setId(id);
            contactInfo.setName(formatName(name));
            contactInfo.setHasPhoneNumber(hasPhoneNumber);
            contactInfo.setPhoto(photo);

            String pinyin = "";
            String sort = "";
            String firstLet = "";
            if (null == name) {
                sort = PRIORITY_NULL;
            }
            else if (0 == name.length()) {
                sort = PRIORITY_NULL;
            }
            else {
                List<HanziToPinyin.Token> pinyinList = pinyinInstance
                        .get(name);
                int count = pinyinList.size();
                pinyin = "";
                sort = "";
                firstLet = "";
                for (int i = 0; i < count; i++) {
                    pinyin = pinyin + pinyinList.get(i).target;
                    sort = sort + pinyinList.get(i).target
                            + getSortPriority(pinyinList.get(i).type)
                            + pinyinList.get(i).source;
                    if (0 != pinyinList.get(i).target.length()) {
                        firstLet = firstLet
                                + pinyinList.get(i).target.charAt(0);
                    }
                }
            }
            contactInfo.setNamePinyin(pinyin);
            contactInfo.setNameFirstLetter(firstLet);
            contactInfo.setNameSort(sort);

            if ((hasPhoneNumber == 1) || !TextUtils.isEmpty(contactInfo.getName())) {
                listContacts.add(contactInfo);
            }
        }

        ComparatorContacts comparator = new ComparatorContacts();
        Collections.sort(listContacts, comparator);

        cursor.close();
        if (listContacts.size() > 2000) {
            mListContacts = listContacts.subList(0, 2000);
        } else {
            mListContacts = listContacts;
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
     * Load phone contacts list.
     *
     * @param where search tag
     * @param args selectionArgs
     */
    @SuppressWarnings("unused")
    private void loadPhoneContacts(String where, String[] args) {
        List<ContactInfo> listContacts = new ArrayList<ContactInfo>();
        ContentResolver resolver = mContext.getContentResolver();
        Cursor cursor = resolver.query(ContactsContract.Contacts.CONTENT_URI,
                null, where, args, null);

        HanziToPinyin pinyinInstance = HanziToPinyin.getInstance();

        while (cursor.moveToNext()) {

            if (mColumnIndexName < 0) {
                mColumnIndexName = cursor.getColumnIndex(
                        ContactsContract.Contacts.DISPLAY_NAME);
            }

            if (mColumnIndexContactId < 0) {
                mColumnIndexContactId = cursor.getColumnIndex(
                        ContactsContract.Contacts._ID);
            }

            if (mColumnIndexHasNumberPhone < 0) {
                mColumnIndexHasNumberPhone = cursor.getColumnIndex(
                        ContactsContract.Contacts.HAS_PHONE_NUMBER);
            }

            if (mColumnIndexSortKeyPrimary < 0) {
                mColumnIndexSortKeyPrimary = cursor.getColumnIndex(
                        ContactsContract.Contacts.SORT_KEY_PRIMARY);
            }

            // 取得名字
            String name = cursor.getString(mColumnIndexName);
            // 取得ContactID
            long contactId = cursor.getLong(mColumnIndexContactId);
            // 取得是否有电话号码
            int hasPhoneNumber = cursor.getInt(mColumnIndexHasNumberPhone);
            // 取得sortKey
            String sortKey = cursor.getString(mColumnIndexSortKeyPrimary);

            ContactInfo contactInfo = new ContactInfo();
            contactInfo.setName(name);
            contactInfo.setId(contactId);
            contactInfo.setHasPhoneNumber(hasPhoneNumber);
            contactInfo.setSortKey(sortKey);
            String pinyin = "";
            String sort = "";
            String firstLet = "";
            if (null == name) {
                sort = PRIORITY_NULL;
            }
            else if (0 == name.length()) {
                sort = PRIORITY_NULL;
            }
            else {

                List<HanziToPinyin.Token> pinyinList = pinyinInstance
                        .get(name);
                int count = pinyinList.size();
                pinyin = "";
                sort = "";
                firstLet = "";
                for (int i = 0; i < count; i++) {
                    pinyin = pinyin + pinyinList.get(i).target;
                    sort = sort + pinyinList.get(i).target
                            + getSortPriority(pinyinList.get(i).type)
                            + pinyinList.get(i).source;
                    if (0 != pinyinList.get(i).target.length()) {
                        firstLet = firstLet
                                + pinyinList.get(i).target.charAt(0);
                    }
                }
            }
            contactInfo.setNamePinyin(pinyin);
            contactInfo.setNameFirstLetter(firstLet);
            contactInfo.setNameSort(sort);
            /*
             * List<ContactsPhones> diaplayList = new
             * ArrayList<ContactsPhones>(); if(0!=hasPhoneNumber){
             *
             * List<ContactsPhones> phones =
             * queryContactsPhoneByContactId(contactInfo.getId()); int
             * phonesCount = phones.size();
             *
             * phonesCount = (phonesCount>PHONES_SIZE)?PHONES_SIZE:phonesCount;
             * for(int loop=0;loop<phonesCount;loop++){
             * diaplayList.add(phones.get(loop)); } }
             * contactInfo.setPhones(diaplayList);
             */

            listContacts.add(contactInfo);

        }

        ComparatorContacts comparator = new ComparatorContacts();
        Collections.sort(listContacts, comparator);

        cursor.close();
        mListContacts = listContacts;
    }

    /**
     * Get sort priority.
     *
     * @param type HanziToPinyin.Token
     * @return sort priority
     */
    private String getSortPriority(int type) {

        if ((HanziToPinyin.Token.FULL_NUM == type)
                || (HanziToPinyin.Token.HALF_NUM == type)) {
            return PRIORITY_NUMBER;
        }
        else if ((HanziToPinyin.Token.FULL_BIG_LET == type)
                || (HanziToPinyin.Token.FULL_SMALL_LET == type)
                || (HanziToPinyin.Token.HALF_BIG_LET == type)
                || (HanziToPinyin.Token.HALF_SMALL_LET == type)) {
            return PRIORITY_LETTER;
        }
        else if (HanziToPinyin.Token.PINYIN == type) {
            return PRIORITY_PINYIN;
        }

        return PRIORITY_OTHER;
    }

    /**
     * Contacts comparator.
     *
     * @author neusoft
     */
    private class ComparatorContacts implements Comparator<ContactInfo> {
        Collator collator = Collator.getInstance(Locale.CHINA);
        @Override
        public int compare(ContactInfo lhs, ContactInfo rhs) {
            String lSort = lhs.getNameSort();
            String rSort = rhs.getNameSort();
            if (TextUtils.isEmpty(lSort) && TextUtils.isEmpty(rSort)) {
                return 0;
            }

            if (TextUtils.isEmpty(lSort)) {
                return -1;
            }

            if (TextUtils.isEmpty(rSort)) {
                return 1;
            }

            return collator.compare(lSort, rSort);
        }
    }

    /**
     * Query contact photo by contact id.
     *
     * @param contactId contact id
     * @return contact photo
     */
    public static Bitmap queryContactPhoto(long contactId) {
        if (contactId <= 0) {
            return null;
        }
        ContentResolver resolver = mContext.getContentResolver();
        Uri uri = ContentUris.withAppendedId(
                ContactsContract.Contacts.CONTENT_URI, Long.valueOf(contactId));
        InputStream input = ContactsContract.Contacts
                .openContactPhotoInputStream(resolver, uri);

        return BitmapFactory.decodeStream(input);
    }

    /**
     * Query contact photo by contact info
     *
     * @param info contact info
     * @return contact photo
     */
    public static Bitmap queryContactPhotoLocal(ContactInfo info) {
        byte[] photo = info.getPhoto();
        if (photo == null) {
            return null;
        }
        return BitmapFactory.decodeStream(new ByteArrayInputStream(photo));
    }

    /**
     * Query contact by phone number.
     *
     * @param context context
     * @param phoneNumber phone number
     * @return contacts info list
     */
    public static List<ContactInfo> queryContactByPhone(Context context,
            String phoneNumber) {

        List<ContactInfo> result = new ArrayList<ContactInfo>();

        ContentResolver resolver = context.getContentResolver();

        android.net.Uri uri = android.net.Uri
                .parse("content://com.android.contacts/data/phones/filter/"
                        + phoneNumber);

        Cursor cursor = resolver.query(uri,
                new String[] {Phone.CONTACT_ID, Phone.DISPLAY_NAME},
                null, null, null);

        while (cursor.moveToNext()) {

            ContactInfo contactInfo = new ContactInfo();

            contactInfo.setId(cursor.getLong(0));

            contactInfo.setName(cursor.getString(1));

            result.add(contactInfo);
        }
        cursor.close();

        return result;
    }

    /**
     * Query contact phone by contacts id.
     *
     * @param contactId contact id
     * @return contact phones list
     */
    public static List<ContactsPhones> queryContactsPhoneByContactId(
            long contactId) {

        List<ContactsPhones> listNumber = new ArrayList<ContactsPhones>();
        ContentResolver resolver = mContext.getContentResolver();
        Cursor phoneCursor = resolver.query(Phone.CONTENT_URI, null,
                Phone.CONTACT_ID + "=" + contactId, null, null);
        while (phoneCursor.moveToNext()) {
            ContactsPhones phone = new ContactsPhones();
            phone.setNumber(phoneCursor.getString(phoneCursor
                    .getColumnIndex(Phone.NUMBER)));
            phone.setType(phoneCursor.getInt(phoneCursor
                    .getColumnIndex(Phone.TYPE)));
            listNumber.add(phone);
        }
        phoneCursor.close();

        return listNumber;
    }

    /**
     * Query contact info by contact id.
     *
     * @param contactId contact id
     * @return contact info
     */
    public static ContactInfo queryContactInfoById(long contactId) {
        ContactInfo contactInfo = null;
        ContentResolver resolver = mContext.getContentResolver();
        Cursor contactCursor = resolver.query(Phone.CONTENT_URI, null,
                Phone.CONTACT_ID + "=" + contactId, null, null);

        int columnIndexName = -1;
        int columnIndexPhoto = -1;
        while (contactCursor.moveToNext()) {

            if (columnIndexName < 0) {
                columnIndexName = contactCursor
                        .getColumnIndex(Phone.DISPLAY_NAME);
            }
            if (columnIndexPhoto < 0) {
                columnIndexPhoto = contactCursor.getColumnIndex(Phone.PHOTO_ID);
            }

            contactInfo = new ContactInfo();
            contactInfo.setId((long) contactId);

            String contactName = contactCursor.getString(columnIndexName);
            contactInfo.setName(contactName);

        }
        contactCursor.close();
        return contactInfo;
    }

    /**
     * Search contact id by contact name.
     *
     * @param name contact name
     * @return contact id list
     */
    public List<Long> searchContactIdByName(String name) {

        ContentResolver resolver = mContext.getContentResolver();
        List<Long> contactIds = new ArrayList<Long>();
        Cursor cursor = resolver.query(ContactsContract.Contacts.CONTENT_URI,
                new String[] { ContactsContract.Contacts._ID },
                ContactsContract.Contacts.DISPLAY_NAME + "=?",
                new String[] { name }, null);

        int columnIndex = -1;
        while (cursor.moveToNext()) {
            if (columnIndex < 0) {
                columnIndex = cursor
                        .getColumnIndex(ContactsContract.Contacts._ID);
            }
            long contactId = cursor.getLong(columnIndex);
            contactIds.add(contactId);
        }
        cursor.close();
        return contactIds;

    }

    /**
     * Query contact info by name and number.
     *
     * @param name contact name
     * @param number phone number
     * @return contact info
     */
    public static ContactInfo queryContactByNameAndNum(String name,
            String number) {

        if (null == name) {
            return null;
        }

        if (name.isEmpty()) {
            return null;
        }

        if (null == number) {
            return null;
        }

        if (number.startsWith("+86")) {
            number = number.replaceFirst("\\+86", "");
        }

        if (number.isEmpty()) {
            return null;
        }

        ContactInfo contact = new ContactInfo();
        ContentResolver resolver = mContext.getContentResolver();
        Cursor cursor = resolver.query(ContactsContract.Contacts.CONTENT_URI,
                null, ContactsContract.Contacts.HAS_PHONE_NUMBER + ">0 AND "
                        + ContactsContract.Contacts.DISPLAY_NAME + "=?",
                new String[] { name }, null);

        while (cursor.moveToNext()) {
            long id = cursor.getLong(cursor
                    .getColumnIndex(ContactsContract.Contacts._ID));
            List<ContactsPhones> phones = queryContactsPhoneByContactId(id);
            for (ContactsPhones phone : phones) {
                String searchNum = phone.getNumber();
                if (null != searchNum) {

                    if (searchNum.startsWith("+86")) {
                        searchNum = searchNum.replaceFirst("\\+86", "");
                    }

                    if (searchNum.replace("-", "").equals(
                            number.replace("-", ""))) {
                        contact.setHasPhoneNumber(cursor.getInt(cursor
                                .getColumnIndex(ContactsContract.Contacts
                                        .HAS_PHONE_NUMBER)));
                        contact.setId(id);
                        contact.setName(name);
                        contact.setPhones(phones);
                        cursor.close();
                        return contact;
                    }
                }
            }
        }

        cursor.close();

        return null;
    }

    /**
     * Query contact by phone number.
     *
     * @param phoneNumber phone number
     * @return contact info
     */
    public static ContactInfo queryContactByNum(String phoneNumber) {
        String number1 = "";
        String number2 = "";
        if (null == phoneNumber) {
            return null;
        }

        if (phoneNumber.isEmpty()) {
            return null;
        }

        if (phoneNumber.startsWith("+86")) {
            number1 = phoneNumber.replaceFirst("\\+86", "");
        }
        else {
            number1 = "+86" + phoneNumber;
        }
        if (phoneNumber.contains(" ")) {
            number2 = phoneNumber.replace(" ", "-");
        }
        else if (phoneNumber.contains("-")) {
            number2 = phoneNumber.replace("-", "");
        }
        else {
            number2 = formatPhoneNumber(phoneNumber);
        }

        ContentResolver resolver = mContext.getContentResolver();
        String or = " OR ";
        String where = NeusoftContactsContract.ContactColumns.PHONE1 + "=?"
                + or + NeusoftContactsContract.ContactColumns.PHONE2 + "=?"
                + or + NeusoftContactsContract.ContactColumns.PHONE3 + "=?"
                + or + NeusoftContactsContract.ContactColumns.PHONE4 + "=?"
                + or + NeusoftContactsContract.ContactColumns.PHONE5 + "=?"
                + or + NeusoftContactsContract.ContactColumns.PHONE1 + "=?"
                + or + NeusoftContactsContract.ContactColumns.PHONE2 + "=?"
                + or + NeusoftContactsContract.ContactColumns.PHONE3 + "=?"
                + or + NeusoftContactsContract.ContactColumns.PHONE4 + "=?"
                + or + NeusoftContactsContract.ContactColumns.PHONE5 + "=?"
                + or + NeusoftContactsContract.ContactColumns.PHONE1 + "=?"
                + or + NeusoftContactsContract.ContactColumns.PHONE2 + "=?"
                + or + NeusoftContactsContract.ContactColumns.PHONE3 + "=?"
                + or + NeusoftContactsContract.ContactColumns.PHONE4 + "=?"
                + or + NeusoftContactsContract.ContactColumns.PHONE5 + "=?";
        String[] selectionArgs 
                = new String[] {phoneNumber, phoneNumber, phoneNumber,
                                phoneNumber, phoneNumber, number2, number2,
                                number2, number2, number2, number1, number1,
                                number1, number1, number1};
        Cursor cursor = resolver.query(
                NeusoftContactsContract.NEUSOFT_CONTACT_URI, null, where,
                selectionArgs, null);
        if (cursor == null) {
            return null;
        }
        int indexId = cursor.getColumnIndex(
                NeusoftContactsContract.ContactColumns._ID);
        int displayName = cursor.getColumnIndex(
                NeusoftContactsContract.ContactColumns.DISPLAY_NAME);
        int indexPhoto = cursor.getColumnIndex(
                NeusoftContactsContract.ContactColumns.PHOTO);

        long id;
        String name;
        byte[] photo = null;
        ContactInfo contact = new ContactInfo();
        while (cursor.moveToNext()) {
            id = cursor.getLong(indexId);
            name = cursor.getString(displayName);
            photo = cursor.getBlob(indexPhoto);

            contact.setId(id);
            contact.setPhoto(photo);
            contact.setName(name);
            cursor.close();
            return contact;
        }

        cursor.close();

        return new ContactInfo();
    }

    /**
     * Format phone number.
     *
     * @param input phone number
     * @return formated phone number
     */
    private static String formatPhoneNumber(String input) {

        if (input.startsWith("1")) {
            if (input.length() == 1) {
                return input;
            }
            else if (input.length() > 1 && input.length() < 5) {
                return input.substring(0, 1) + "-"
                        + input.substring(1, input.length());
            }
            else if (input.length() >= 5 && input.length() < 8) {
                return input.substring(0, 1) + "-" + input.substring(1, 4)
                        + "-" + input.substring(4, input.length());
            }
            else if (input.length() >= 8) {
                return input.substring(0, 1) + "-" + input.substring(1, 4)
                        + "-" + input.substring(4, 7) + "-"
                        + input.substring(7, input.length());
            }
        }
        else {
            if (input.length() <= 3) {
                return input;
            }
            else if (input.length() > 3 && input.length() < 7) {
                return input.substring(0, 3) + "-"
                        + input.substring(3, input.length());
            }
            else if (input.length() >= 7) {
                return input.substring(0, 3) + "-" + input.substring(3, 6)
                        + "-" + input.substring(6, input.length());
            }
        }
        return input;
    }
}
