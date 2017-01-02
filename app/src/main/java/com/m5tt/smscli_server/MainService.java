package com.m5tt.smscli_server;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.database.Cursor;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.provider.Telephony;
import android.telephony.SmsManager;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.reflect.Type;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.Time;
import java.util.Map;

import static com.m5tt.smscli_server.Util.getContactByPhoneNumber;

public class MainService extends Service
{
    // TODO: Move to xml config so user can set
    private static final int PORT = 55900;
    private static final int ONGOING_NOTIFICATION_ID = 1;

    private Map<String, Contact> contactHash;
    private DataInputStream inputStream;
    private DataOutputStream outputStream;
    private HandlerThread handlerThreadReceiver;
    private HandlerThread handlerThreadObserver;

    private BroadcastReceiver smsReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            if (intent.getAction().equals("android.provider.Telephony.SMS_RECEIVED"))
            {
                android.telephony.SmsMessage[] receivedMessages = Telephony.Sms.Intents.getMessagesFromIntent(intent);

                // for each message from intent, parse to our own SmsMessage, write to client
                for (android.telephony.SmsMessage message : receivedMessages)
                {
                    // Look up contact
                    String phoneNumber = Util.formatPhoneNumber(message.getOriginatingAddress());
                    String relatedContactId = getContactByPhoneNumber(contactHash, phoneNumber);

                    if (! contactHash.containsKey(relatedContactId))
                        Util.addNewContact(relatedContactId, contactHash);

                    // Build a SmsMessage and jsonify it
                    SmsMessage smsMessage = new SmsMessage(
                            message.getTimestampMillis(),
                            message.getMessageBody(),
                            relatedContactId,
                            SmsMessage.SMS_MESSAGE_TYPE.INBOX
                    );

                    contactHash.get(relatedContactId).addToConversation(smsMessage);
                    writeClient(Util.jsonifySmsMessage(smsMessage));
                }
            }
        }
    };

    class SentSmsObserver extends ContentObserver
    {
        private final String[] SMS_PROJECTION = {
                Telephony.TextBasedSmsColumns.ADDRESS,
                Telephony.TextBasedSmsColumns.PERSON,
                Telephony.TextBasedSmsColumns.DATE,
                Telephony.TextBasedSmsColumns.BODY,
                Telephony.TextBasedSmsColumns.TYPE
        };

        private final String SENT_SMS_TYPE = "2";
        private ContentResolver resolver;
        private SmsMessage prevSmsMessage;

        public SentSmsObserver(Handler handler, ContentResolver resolver)
        {
            super(handler);
            this.resolver = resolver;
        }

        @Override
        public boolean deliverSelfNotifications()
        {
            return true;
        }

        private boolean smsInList(String contactId, SmsMessage sms)
        {
            /* Check if sms is in list already. Use this instead
             * of contains and equals() because we want to exclude
             * time property. sms time in system database is never
             * going to be the same as time in our contact hash
             *
             * Consequence of this is, multiple messages with same body,
             * wont be sent over
             */

            for (SmsMessage curSms : contactHash.get(contactId).getConversation())
                if (curSms.getBody().equals(sms.getBody())
                    && curSms.getRelatedContactId().equals(sms.getRelatedContactId())
                    && curSms.getSmsMessageType().equals(sms.getSmsMessageType()))
                    return true;

            return false;
        }
        @Override
        public void onChange(boolean selfChange)
        {
            super.onChange(selfChange);
            Cursor cursor = this.resolver.query(
                    Telephony.Sms.Sent.CONTENT_URI,
                    this.SMS_PROJECTION,
                    null,
                    null,
                    null
            );

            if (cursor.moveToNext() && cursor.getString(cursor.getColumnIndex(Telephony.Sms.TYPE)).equals(SENT_SMS_TYPE))
            {
                String address = Util.formatPhoneNumber(
                        cursor.getString(cursor.getColumnIndex(Telephony.Sms.ADDRESS)));
                String relatedContactId = Util.getContactByPhoneNumber(contactHash, address);

                SmsMessage smsMessage = new SmsMessage(
                        cursor.getLong(cursor.getColumnIndex(Telephony.Sms.DATE)),
                        cursor.getString(cursor.getColumnIndex(Telephony.Sms.BODY)),
                        relatedContactId,
                        SmsMessage.SMS_MESSAGE_TYPE.OUTBOX
                );

                if (this.prevSmsMessage == null || ! smsMessage.equals(this.prevSmsMessage))
                {
                    if (! contactHash.containsKey(relatedContactId))
                        Util.addNewContact(relatedContactId, contactHash);

                    if (! smsInList(smsMessage.getRelatedContactId(), smsMessage))
                    {
                        contactHash.get(relatedContactId).addToConversation(smsMessage);
                        this.prevSmsMessage = smsMessage;

                        writeClient(Util.jsonifySmsMessage(smsMessage));
                    }
                }
            }

            cursor.close();
        }
    }

    @Override
    public void onCreate()
    {
        super.onCreate();
        startForeground(ONGOING_NOTIFICATION_ID, buildNotification("Starting"));
    }

    public int onStartCommand(Intent intent, int flags, int startId)
    {
        Log.d("onHandleIntent()", "Service started");

        Thread mainThread = new Thread() {
            public void run()
            {
                startServer();
            }
        };

        mainThread.start();
        return Service.START_NOT_STICKY;
    }

    public IBinder onBind(Intent intent)
    {
        return null;
    }

    public Notification buildNotification(String status)
    {
        PendingIntent contentIntent = PendingIntent.getActivity(
                this, 0, new Intent(this, MainActivity.class), 0);

        return new Notification.Builder(this)
                .setContentTitle("Title")
                .setContentText(status)
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentIntent(contentIntent)
                .build();
    }

    public void updateNotification(String status)
    {
        Notification notification = buildNotification(status);
        NotificationManager notificationManager  =
                (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);

        notificationManager.notify(ONGOING_NOTIFICATION_ID, notification);
    }

    private void startServer()
    {
        IntentFilter smsFilter = new IntentFilter();
        IntentFilter networkFilter = new IntentFilter();
        smsFilter.addAction("android.provider.Telephony.SMS_RECEIVED");
        networkFilter.addAction("android.net.conn.CONNECTIVITY_CHANGE");

        ContentObserver sentSmsObserver = null;
        ServerSocket serverSocket = null;

        while (true)
        {
            try
            {
                serverSocket = new ServerSocket(PORT);
                Log.d("startSever", "Blocking");
                updateNotification("Waiting for connection");   // TODO: put strings in layout
                Socket clientSocket = serverSocket.accept();

                contactHash = Util.buildContactHash(this);

                Log.d("startSever", "Connected");
                updateNotification("Connected");

                inputStream = new DataInputStream(clientSocket.getInputStream());
                outputStream = new DataOutputStream(clientSocket.getOutputStream());

                handlerThreadReceiver = new HandlerThread("SmsReceiver");
                handlerThreadReceiver.start();
                this.registerReceiver(smsReceiver,
                        smsFilter, null, new Handler(handlerThreadReceiver.getLooper()));

                handlerThreadObserver = new HandlerThread("SentSmsObserver");
                handlerThreadObserver.start();
                sentSmsObserver = new SentSmsObserver(
                        new Handler(handlerThreadObserver.getLooper()), this.getContentResolver());
                this.getContentResolver().registerContentObserver(
                        Telephony.Sms.CONTENT_URI, true, sentSmsObserver
                );

                writeClient(Util.jsonifyContactHash(contactHash));
                readClient();
            }
            catch (IOException e)
            {
                Log.d("startServer", "Lost connection");
                updateNotification("Lost connection");
            }
            finally
            {
                try
                {
                    unregisterReceiver(smsReceiver);
                    this.getContentResolver().unregisterContentObserver(sentSmsObserver);
                    serverSocket.close();
                }
                catch (IOException e)
                {
                    Log.d("startServer", String.valueOf(e.getStackTrace()));
                }
            }
        }
    }

    private void readClient() throws IOException
    {
        while (! Thread.currentThread().isInterrupted())
        {
            int size = inputStream.readInt();
            byte[] data = new byte[size];
            inputStream.readFully(data);

            Gson gson = new GsonBuilder()
                    .registerTypeAdapter(Time.class, SmsMessage.timeJsonDeserializer).create();
            Type type = new TypeToken<SmsMessage>() {}.getType();
            SmsMessage smsMessage = gson.fromJson(new String(data, "UTF-8"), type);

            if (! contactHash.containsKey(smsMessage.getRelatedContactId()))
                Util.addNewContact(smsMessage.getRelatedContactId(), contactHash);

            Contact contact = contactHash.get(smsMessage.getRelatedContactId());

            SmsManager.getDefault().sendTextMessage(
                    contact.getPhoneNumber(),
                    null,
                    smsMessage.getBody(),
                    null,
                    null
            );

            contact.addToConversation(smsMessage);
        }
    }


    public synchronized void writeClient(String jsonMessage)
    {
        /* Make this synchronized cause ContentObserver and
         * BroadCastReceiver could happen at the same time
         */

        try
        {
            // Now write the json to the client
            outputStream.writeInt(jsonMessage.getBytes().length);
            outputStream.write(jsonMessage.getBytes("UTF-8"));
            outputStream.flush();
        }
        catch (IOException e)
        {
            // not a big deal
            Log.d("writeClient", "Write failed");
        }
    }

}
