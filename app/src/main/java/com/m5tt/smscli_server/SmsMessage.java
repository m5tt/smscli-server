package com.m5tt.smscli_server;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;

import java.lang.reflect.Type;
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
 */

public class SmsMessage implements Comparable<SmsMessage>
{
    public enum SMS_MESSAGE_TYPE { INBOX, OUTBOX };

    private Time time;
    private String body;
    private String relatedContactId;
    private SMS_MESSAGE_TYPE smsMessageType;

    public static JsonDeserializer<Time> timeJsonDeserializer = new JsonDeserializer<Time>()
    {
        @Override
        public Time deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException
        {
            try
            {
                return new Time(new SimpleDateFormat("hh:mm:ss").parse(json.getAsString()).getTime());
            }
            catch (ParseException e)
            {
            }

            throw new JsonParseException("Unparseable time: \"" + json.getAsString()
                    + "\". Supported formats: " + "hh:mm:ss");
        }
    };


    /*
    public SmsMessage (Time time, String body, String relatedContactId, SMS_MESSAGE_TYPE smsMessageType)
    {
        this.time = time;
        this.body = body;
        this.relatedContactId = relatedContactId;
        this.smsMessageType = smsMessageType;
    }
    */

    public SmsMessage (String time, String body, String relatedContactId, SMS_MESSAGE_TYPE smsMessageType)
    {
        try
        {
            this.time = new Time(new SimpleDateFormat("hh:mm:ss").parse(time).getTime());
        }
        catch (ParseException e)
        {
        }

        this.body = body;
        this.relatedContactId = relatedContactId;
        this.smsMessageType = smsMessageType;
    }

    public SmsMessage (long time, String body, String relatedContactId, SMS_MESSAGE_TYPE smsMessageType)
    {
        try
        {
            this.time = new Time(time);
            this.time = new Time(new SimpleDateFormat("hh:mm:ss").parse(this.time.toString()).getTime());
        }
        catch (ParseException e)
        {
        }

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

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (!(o instanceof SmsMessage)) return false;

        SmsMessage that = (SmsMessage) o;

        if (time != null ? !time.equals(that.time) : that.time != null) return false;
        if (body != null ? !body.equals(that.body) : that.body != null) return false;
        if (relatedContactId != null ? !relatedContactId.equals(that.relatedContactId) : that.relatedContactId != null)
            return false;
        return smsMessageType == that.smsMessageType;

    }

    @Override
    public int hashCode()
    {
        int result = time != null ? time.hashCode() : 0;
        result = 31 * result + (body != null ? body.hashCode() : 0);
        result = 31 * result + (relatedContactId != null ? relatedContactId.hashCode() : 0);
        result = 31 * result + (smsMessageType != null ? smsMessageType.hashCode() : 0);
        return result;
    }
}