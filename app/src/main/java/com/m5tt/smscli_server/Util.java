package com.m5tt.smscli_server;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.provider.ContactsContract;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.i18n.phonenumbers.PhoneNumberUtil;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Locale;
import java.util.Map;

/**************************************************************************************************
 * Main data structure that program centers around
 * Static class to build a contact list:
 *      1) Gets all contacts with mobile numbers
 *      2) Populates the sms conversation the user has for each contact
 *
 * See Contact class for details on contact.
 *
 * We also have a CONVERSATION LENGTH MAX to prevent the whole data structure
 * from getting to big.
 *
 * This list is to be sent over to the client in the form of a JSON string
 *
 * TODO: Change this class name up or something, its used for more then just contactList building
 *
 */


public class Util
{
    private static final Uri CONTACT_URI = ContactsContract.Contacts.CONTENT_URI;
    private static final Uri CONTACT_PHONE_URI = ContactsContract.CommonDataKinds.Phone.CONTENT_URI;

    private static final String[] CONTACT_PROJECTION = {
            ContactsContract.Contacts._ID,
            ContactsContract.Contacts.DISPLAY_NAME
    };
    private static final String CONTACT_ORDER_KEY = ContactsContract.Contacts.DISPLAY_NAME;
    private static final String[] CONTACT_PHONE_PROJECTION = { ContactsContract.CommonDataKinds.Phone.NUMBER };

    public static Map<String, Contact> buildContactHash(Context context)
    {
        /** Gets the list of all contacts with mobile phone numbers **/

        ContentResolver contentResolver = context.getContentResolver();
        Map<String, Contact> contactHash = new Hashtable<>();

        Cursor contactCursor = contentResolver.query(
                CONTACT_URI,
                CONTACT_PROJECTION,
                ContactsContract.Contacts.HAS_PHONE_NUMBER + " = 1",
                null,
                CONTACT_ORDER_KEY
        );

        if (contactCursor.moveToFirst())
        {
            for (int i = 0; i < contactCursor.getCount(); i++)
            {
                // where clause to filter by mobile phone number
                // TODO: change to selection args
                String phoneWhereClause =
                        ContactsContract.CommonDataKinds.Phone.CONTACT_ID +
                                " = " + contactCursor.getString(
                                contactCursor.getColumnIndex(ContactsContract.Contacts._ID)) +
                                " and " + ContactsContract.CommonDataKinds.Phone.TYPE +
                                " = " + ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE;

                // now query the phone number for current contact
                Cursor phoneCursor = contentResolver.query(
                        CONTACT_PHONE_URI,
                        CONTACT_PHONE_PROJECTION,
                        phoneWhereClause,
                        null,
                        null
                );

                if (phoneCursor.moveToFirst())
                {
                    String id = contactCursor.getString(
                            contactCursor.getColumnIndex(ContactsContract.Contacts._ID)
                    );

                    String displayName = contactCursor.getString(
                            contactCursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)
                    );

                    String phone = phoneCursor.getString(
                            phoneCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                    );

                    phone = formatPhoneNumber(phone);

                    contactHash.put(
                            id, new Contact(id, displayName, phone, new ArrayList<SmsMessage>()));

                    phoneCursor.close();
                }

                contactCursor.moveToNext();
            }
        }

        contactCursor.close();
        return contactHash;
    }

    public static String getContactByPhoneNumber(Map<String, Contact> contactHash, String phoneNumber)
    {
        /* This could use PhoneLookup, but this is probably faster and definitely simpler given
            we already have the contact list
         */

        String relatedContactId = "";

        for (Contact contact : contactHash.values())
        {
            if (contact.getPhoneNumber().equals(phoneNumber))
            {
                relatedContactId = contact.getId();
                break;
            }
        }

        if (relatedContactId != "")
            return relatedContactId;
        else
            return phoneNumber;      // phone number is contact id
    }


    public static String jsonifyContactHash(Map<String, Contact> contactList)
    {
        Type type = new TypeToken<Map<String,Contact>>() {}.getType();
        return new Gson().toJson(contactList, type);
    }

    public static String jsonifySmsMessage(SmsMessage smsMessage)
    {
        Type type = new TypeToken<SmsMessage>() {}.getType();
        return new Gson().toJson(smsMessage, type);
    }

    public static SmsMessage parseSmsMessageJson(String smsMessageJson)
    {
        return null;
    }

    public static String formatPhoneNumber(String phoneNumber)
    {
        String parsedPhoneNumber = "";

        try
        {
            PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();
             parsedPhoneNumber = String.valueOf(
                     phoneUtil.parse(phoneNumber, Locale.getDefault()
                             .getCountry()).getNationalNumber());
        }
        catch (Exception e)
        {
        }

        return parsedPhoneNumber;

    }

    public static void addNewContact(String contactId, Map<String, Contact> contactHash)
    {
        /* For sms to/from numbers not in contact hash */

        contactHash.put(
                contactId,
                new Contact(contactId, contactId, contactId, new ArrayList<SmsMessage>())
        );

    }

    public static boolean isNetworkAvailable(Context context)
    {
        WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();

        return wifiManager.isWifiEnabled() && wifiInfo != null && wifiInfo.getNetworkId() != -1;
    }
}
