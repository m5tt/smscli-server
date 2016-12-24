package com.m5tt.smscli_server;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.provider.Telephony;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.i18n.phonenumbers.PhoneNumberUtil;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
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


public class ContactListBuilder
{
    private static final Uri CONTACT_URI = ContactsContract.Contacts.CONTENT_URI;
    private static final Uri CONTACT_PHONE_URI = ContactsContract.CommonDataKinds.Phone.CONTENT_URI;

    private static final String[] CONTACT_PROJECTION = {
            ContactsContract.Contacts._ID,
            ContactsContract.Contacts.DISPLAY_NAME
    };
    private static final String CONTACT_ORDER_KEY = ContactsContract.Contacts.DISPLAY_NAME;
    private static final String[] CONTACT_PHONE_PROJECTION = { ContactsContract.CommonDataKinds.Phone.NUMBER };

    private static final Uri SMS_URI = Telephony.Sms.CONTENT_URI;
    private static final String[] SMS_PROJECTION = {
            Telephony.TextBasedSmsColumns.ADDRESS,
            Telephony.TextBasedSmsColumns.PERSON,
            Telephony.TextBasedSmsColumns.DATE,
            Telephony.TextBasedSmsColumns.BODY,
            Telephony.TextBasedSmsColumns.TYPE
    };

    // TODO: Move this to a xml file that the user can set
    private static final int CONVERSATION_LENGTH_MAX = 100;

    public static List<Contact> buildContactList(Context context)
    {
        ContentResolver contentResolver = context.getContentResolver();

        List<Contact> contactList = getContacts(contentResolver);
        populateConversations(contentResolver, contactList);

        return contactList;
    }

    private static List<Contact> getContacts(ContentResolver contentResolver)
    {
        /** Gets the list of all contacts with mobile phone numbers **/

        List<Contact> contactList = new ArrayList<>();
        Cursor contactCursor = contentResolver.query(
                CONTACT_URI,
                CONTACT_PROJECTION,
                ContactsContract.Contacts.HAS_PHONE_NUMBER + " = 1",
                null,
                CONTACT_ORDER_KEY
        );

        Log.d("getContacts()", String.valueOf(contactCursor.getCount()));

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

                    contactList.add(new Contact(id, displayName, phone));
                    phoneCursor.close();
                }

                contactCursor.moveToNext();
            }
        }

        contactCursor.close();
        return contactList;
    }

    private static void populateConversations(ContentResolver contentResolver, List<Contact> contactList)
    {
        // First get all sms ever sent and received
        // TODO: ignore drafts
        Cursor smsCursor = contentResolver.query(
                SMS_URI,
                SMS_PROJECTION,
                null,
                null,
                null
        );

        Log.d("populate", "About to call build all");

        // Now convert to list of smsMessage objects
        List<SmsMessage> allSms = buildAllSms(contactList, smsCursor);
        smsCursor.close();
        smsCursor = null;

        // Group sms messages by related contact id
        Map<String, List<SmsMessage>> allSmsGrouped = new HashMap<>();
        for (SmsMessage smsMessage : allSms)
        {
            String relatedContactId = smsMessage.getRelatedContactId();
            if (allSmsGrouped.containsKey(relatedContactId))
            {
                if (allSmsGrouped.get(relatedContactId).size() <= CONVERSATION_LENGTH_MAX)
                {
                    allSmsGrouped.get(relatedContactId).add(smsMessage);
                }
            }
            else
            {
                allSmsGrouped.put(relatedContactId, new ArrayList<>(Arrays.asList(smsMessage)));
            }
        }

        allSms = null;

        Log.d("populate", String.valueOf(allSmsGrouped.size()));

        // TODO: make ContactList a dictionary
        // finally assign conversations to there association contact

        for (Contact contact : contactList)
        {
            if (allSmsGrouped.containsKey(contact.getId()))
                contact.setConversation(allSmsGrouped.get(contact.getId()));
            else
                contact.setConversation(new ArrayList<SmsMessage>());
        }

        // for sms messages without a known contact - could do this in one line with java 8
        List<String> unknownContactIds = new ArrayList<>();

        for (String contactId : allSmsGrouped.keySet())
        {
            boolean unknown = false;
            for (Contact contact : contactList)
            {
                if (contactId.equals(contact.getId()))
                {
                    unknown = true;
                    break;
                }
            }

            if (! unknown)
                unknownContactIds.add(contactId);
        }

        for (String contactId : unknownContactIds)
        {
            contactList.add(new Contact(
                    contactId,
                    contactId,
                    contactId,
                    allSmsGrouped.get(contactId)
            ));
        }
    }

    private static List<SmsMessage> buildAllSms(List<Contact> contactList, Cursor smsCursor)
    {
        /** Convert sms result set into a list of SmsMessage objects **/

        List<SmsMessage> allSms = new ArrayList<>();

        Log.d("buildAllSms", "here");

        while (smsCursor.moveToNext())
        {
            String phoneNumber = smsCursor.getString(smsCursor.getColumnIndex(Telephony.Sms.ADDRESS));
            phoneNumber = formatPhoneNumber(phoneNumber);
            String relatedContactId = getContactByPhoneNumber(contactList, phoneNumber);

            SmsMessage.SMS_MESSAGE_TYPE smsMessageType;
            if (smsCursor.getString(smsCursor.getColumnIndex(Telephony.Sms.TYPE)).equals(String.valueOf(Telephony.Sms.MESSAGE_TYPE_INBOX)))
                smsMessageType = SmsMessage.SMS_MESSAGE_TYPE.INBOX;
            else
                smsMessageType = SmsMessage.SMS_MESSAGE_TYPE.OUTBOX;

            allSms.add(new SmsMessage(
                    smsCursor.getString(smsCursor.getColumnIndex(Telephony.Sms.DATE)),
                    smsCursor.getString(smsCursor.getColumnIndex(Telephony.Sms.BODY)),
                    relatedContactId,
                    smsMessageType
            ));
        }

        return allSms;
    }

    // TODO: maybe these dont belong here

    public static String getContactByPhoneNumber(List<Contact> contactList, String phoneNumber)
    {
        /* This could use PhoneLookup, but this is probably faster and definitely simpler given
            we already have the contact list
         */

        String relatedContactId = "";

        for (Contact contact : contactList)
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


    public static String jsonifyContactList(List<Contact> contactList)
    {
        Type type = new TypeToken<List<Contact>>() {}.getType();
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
}
