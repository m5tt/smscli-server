package com.m5tt.smscli_server;

import java.util.List;

/***********************************************************************
 * Represents a Contact, only holds a few necessary
 * properties.
 *
 * Also holds a SmsMessage array conversation. This is the entire
 * sms conversation between the user and a contact. Holds both the
 * messages from the contact as well as the messages sent by the user
 * Ordered by date, recent to oldest.*
 */

public class Contact
{
    private String id;
    private String displayName;
    private String phoneNumber;
    private List<SmsMessage> conversation;

    public Contact(String id, String displayName, String phoneNumber)
    {
        this.id = id;
        this.displayName = displayName;
        this.phoneNumber = phoneNumber;
    }

    public Contact(String id, String displayName, String phoneNumber, List<SmsMessage> conversation)
    {
        this.id = id;
        this.displayName = displayName;
        this.phoneNumber = phoneNumber;
        this.conversation = conversation;
    }

    public String getId()
    {
        return this.id;
    }

    public String getDisplayName()
    {
        return this.displayName;
    }

    public String getPhoneNumber()
    {
        return phoneNumber;
    }

    public List<SmsMessage> getConversation()
    {
        return conversation;
    }

    public void setId(String id)
    {
        this.id = id;
    }

    public void setDisplayName(String displayName)
    {
        this.displayName = displayName;
    }

    public void setPhoneNumber(String phoneNumber)
    {
        this.phoneNumber = phoneNumber;
    }

    public void setConversation(List<SmsMessage> conversation)
    {
        this.conversation = conversation;
    }

    public void addToConversation(SmsMessage smsMessage)
    {
        this.conversation.add(smsMessage);
    }
}
