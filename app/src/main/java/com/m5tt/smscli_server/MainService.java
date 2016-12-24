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
import java.util.Map;

public class MainService extends IntentService
{
    private Map<String, Contact> contactHash;
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
                android.telephony.SmsMessage[] receivedMessages = Telephony.Sms.Intents.getMessagesFromIntent(intent);

                // for each message from intent, parse to our own SmsMessage, write to client
                for (android.telephony.SmsMessage message : receivedMessages)
                {
                    // Look up contact
                    Log.d("smsReceiver", "HEY: " + message.getOriginatingAddress());
                    String phoneNumber = Util.formatPhoneNumber(message.getOriginatingAddress());
                    String relatedContactId = Util.getContactByPhoneNumber(contactHash, phoneNumber);

                    if (! contactHash.containsKey(relatedContactId))
                        Util.addNewContact(relatedContactId, contactHash);

                    // Build a SmsMessage and jsonify it
                    String smsMessageJson = Util.jsonifySmsMessage(new SmsMessage(
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
        contactHash = Util.buildContactHash(this);

        Log.d("onHandleIntent()", String.valueOf(Util.jsonifyContactHash(contactHash).getBytes().length));
        Log.d("onHandleIntent()", "Service started");

        startServer();

        Log.d("onHandleIntent", "Returning");

        unregisterReceiver(smsReceiver);
    }

    private void startServer()
    {
        // objects for broadcast receiver
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.provider.Telephony.SMS_RECEIVED");

        while (true)
        {
            try
            {
                ServerSocket serverSocket = new ServerSocket(PORT);
                Socket clientSocket = serverSocket.accept();

                Log.d("startSever", "Connected");

                inputStream = new DataInputStream(clientSocket.getInputStream());
                outputStream = new DataOutputStream(clientSocket.getOutputStream());

                // Initial data transfer: send jsonified contact list
                initialDataTransfer();

                // register our sms broadcast receiver on a separate thread
                HandlerThread handlerThread = new HandlerThread("SmsReceiver");
                handlerThread.start();
                registerReceiver(smsReceiver, filter, null, new Handler(handlerThread.getLooper()));

                // start client reading
                readClient();
            }
            catch (IOException e)
            {
                Log.d("startServer", "Oh what the fuck: ");
            }
        }
    }

    private void initialDataTransfer() throws IOException
    {
        String contactHashJson = Util.jsonifyContactHash(contactHash);
        Log.d("inital", contactHashJson);

        // initial data transfer
        // first write byte size so client knows how much to read
        outputStream.writeInt(contactHashJson.getBytes().length);

        /* now write json string, important we do it this way
         * instead of just writeUTF because it uses modified utf 8
         * which python cant handle
         */
        outputStream.write(contactHashJson.getBytes("UTF-8"));
    }

    private void readClient()
    {
        try
        {
            while (true)
            {
                int size = inputStream.readInt();
                byte[] data = new byte[size];
                inputStream.readFully(data);

                Type type = new TypeToken<SmsMessage>() {}.getType();
                SmsMessage smsMessage = new Gson().fromJson(new String(data, "UTF-8"), type);

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
            }
        }
        catch (IOException e)
        {
            return;
        }
    }
}
