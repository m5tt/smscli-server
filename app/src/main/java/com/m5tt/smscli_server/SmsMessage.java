package com.m5tt.smscli_server;

import java.sql.Time;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/*******************************************************
 * Represents a sms message,
 * holds the time **received**, the body
 * related contact and its message type
 *
 * related contact is id of the contact that this
 * sms message is either from or being sent too.
 * This is also implied by the fact that
 * the sms message is in a conversation that will be
 * a field of the contact object
 *
 * type can be either be received or sent.
 *
 * TODO: Change type up so sms client can see if a message has failed, pending.., dont do drafs
 */

public class SmsMessage implements Comparable<SmsMessage>
{
    public enum SMS_MESSAGE_TYPE { INBOX, OUTBOX };

    private Time time;
    private String body;
    private String relatedContactId;
    private SMS_MESSAGE_TYPE smsMessageType;

    public SmsMessage (Time time, String body, String relatedContactId, SMS_MESSAGE_TYPE smsMessageType)
    {
        this.time = time;
        this.body = body;
        this.relatedContactId = relatedContactId;
        this.smsMessageType = smsMessageType;
    }

    public SmsMessage (String time, String body, String relatedContactId, SMS_MESSAGE_TYPE smsMessageType)
    {
        try
        {
            this.time = new Time(new SimpleDateFormat("hh:mm:ss").parse(time).getTime());
        }
        catch (ParseException e) {}

        this.body = body;
        this.relatedContactId = relatedContactId;
        this.smsMessageType = smsMessageType;
    }

    public Date getTime()
    {
        return time;
    }

    public String getBody()
    {
        return body;
    }

    public String getRelatedContactId()
    {
        return relatedContactId;
    }

    public SMS_MESSAGE_TYPE getSmsMessageType()
    {
        return smsMessageType;
    }

    public void setTime(Time time)
    {
        this.time = time;
    }

    public void setBody(String body)
    {
        this.body = body;
    }

    public void setRelatedContactId(String relatedContactId)
    {
        this.relatedContactId = relatedContactId;
    }

    public void setSmsMessageType(SMS_MESSAGE_TYPE smsMessageType)
    {
        this.smsMessageType = smsMessageType;
    }

    public int compareTo(SmsMessage o)
    {
        return this.getTime().compareTo(o.getTime());
    }

    /*
    public static Date parseTimeStamp(String date)
    {
        // parse sms DATE into a full time
        Long time = Long.parseLong(date);
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(time);
        return calendar.getTime();
    }
    */

}