package com.m5tt.smscli_server;

import android.app.IntentService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.HandlerThread;
import android.provider.Telephony;
import android.telephony.SmsManager;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.reflect.Type;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Collections;
import java.util.List;

public class MainService extends IntentService
{
    private List<Contact> contactList;
    private DataInputStream inputStream;
    private DataOutputStream outputStream;

    // TODO: Move to xml config so user can set
    private static final int PORT = 55900;

    private BroadcastReceiver smsReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            if (intent.getAction().equals("android.provider.Telephony.SMS_RECEIVED"))
            {
                Log.d("SmsListener onReceive()", "caught sms");
                android.telephony.SmsMessage[] receivedMessages = Telephony.Sms.Intents.getMessagesFromIntent(intent);

                // for each message from intent, parse to our own SmsMessage, write to client
                for (android.telephony.SmsMessage message : receivedMessages)
                {
                    // Look up contact
                    Log.d("smsReceiver", "HEY: " + message.getOriginatingAddress());
                    String phoneNumber = ContactListBuilder.formatPhoneNumber(message.getOriginatingAddress());
                    String relatedContactId = ContactListBuilder.getContactByPhoneNumber(contactList, phoneNumber);

                    // Build a SmsMessage and jsonify it
                    String smsMessageJson = ContactListBuilder.jsonifySmsMessage(new SmsMessage(
                            String.valueOf(message.getTimestampMillis()),
                            message.getMessageBody(),
                            relatedContactId,
                            SmsMessage.SMS_MESSAGE_TYPE.INBOX
                    ));

                    try
                    {
                        // Now write the json to the client
                        outputStream.writeInt(smsMessageJson.getBytes().length);
                        outputStream.write(smsMessageJson.getBytes("UTF-8"));
                    }
                    catch (IOException e)
                    {
                    }
                }
            }
        }
    };


    public MainService()
    {
        super("Main Service");
    }

    @Override
    protected void onHandleIntent(Intent intent)
    {
        Log.d("here", "getting contact list");

        // will be added to by different threads so this should make it okay...
        List<Contact> tempContactList = ContactListBuilder.buildContactList(this);
        contactList = Collections.synchronizedList(tempContactList);  // this is the context

        Log.d("fuck", contactList.toString());

        Log.d("onHandleIntent()", String.valueOf(ContactListBuilder.jsonifyContactList(contactList).getBytes().length));
        Log.d("onHandleIntent()", "Service started");

        startServer();

        Log.d("onHandleIntent", "Returning");

        unregisterReceiver(smsReceiver);
    }

    private void startServer()
    {
        try
        {
            ServerSocket serverSocket = new ServerSocket(PORT);
            Socket clientSocket = serverSocket.accept();

            Log.d("startSever", "Woo connected");

            inputStream = new DataInputStream(clientSocket.getInputStream());
            outputStream = new DataOutputStream(clientSocket.getOutputStream());

            // Initial data transfer: send jsonified contact list
            initialDataTransfer();

            // register our sms broadcast receiver on a separate thread
            IntentFilter filter = new IntentFilter();
            filter.addAction("android.provider.Telephony.SMS_RECEIVED");

            HandlerThread handlerThread = new HandlerThread("SmsReceiver");
            handlerThread.start();

            registerReceiver(smsReceiver, filter, null, new Handler(handlerThread.getLooper()));

            // start client reading
            readClient();

        }
        catch (IOException e)
        {
            Log.d("startServer", "Oh what the fuck: ", e);
        }
    }

    private void initialDataTransfer() throws IOException
    {
        //TODO: fix this
        Log.d("initialDataTransfer()", "Started initial data data transfer");
        String contactListJson = ContactListBuilder.jsonifyContactList(contactList);

        // first write byte size so client knows how much to read
        outputStream.writeInt(contactListJson.getBytes().length);

        /* now write json string, important we do it this way
         * instead of just writeUTF because it uses modified utf 8
         * which python cant handle
         */
        outputStream.write(contactListJson.getBytes("UTF-8"));
    }


    private void readClient()
    {
        /** Our main thread loop **/

        Log.d("clientListener", "client reading started");

        try
        {
            while (true)
            {
                int size = inputStream.readInt();
                byte[] data = new byte[size];
                inputStream.readFully(data);

                // TODO: make a JSONHelper class
                Type type = new TypeToken<SmsMessage>() {}.getType();
                SmsMessage smsMessage = new Gson().fromJson(new String(data, "UTF-8"), type);

                Log.d("readClient() loop", smsMessage.getBody());

                SmsManager.getDefault().sendTextMessage(
                        smsMessage.getRelatedContactId(),
                        null,
                        smsMessage.getBody(),
                        null,
                        null
                );

                // add to data structure
            }
        }
        catch (IOException e)
        {
        }
    }
}
