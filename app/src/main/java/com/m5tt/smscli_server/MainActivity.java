package com.m5tt.smscli_server;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity
{
    Button toggleServerButton;
    TextView statusTextView;

    private boolean bound = false;
    private MainService mainService;

    private ServiceConnection bindingConnection = new ServiceConnection()
    {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service)
        {
            MainService.LocalBinder binder = (MainService.LocalBinder) service;
            mainService = binder.getService();
        }

        @Override
        public void onServiceDisconnected(ComponentName name)
        {
            bound = false;
        }
    };

    private BroadcastReceiver statusReceiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            String status = intent.getStringExtra("status");
            statusTextView.setText(status);
        }
    };

    @Override
    protected void onStart()
    {
        super.onStart();

        this.bindService(new Intent(this, MainService.class), bindingConnection,
                Context.BIND_AUTO_CREATE);

        bound = true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Request permissions
        ActivityCompat.requestPermissions(
                this,
                new String[] { Manifest.permission.SEND_SMS},
                5
        );

        LocalBroadcastManager.getInstance(this).registerReceiver(
                statusReceiver, new IntentFilter("status-message"));

        toggleServerButton = (Button) findViewById(R.id.toggleServerButton);
        toggleServerButton.setText("Start Server");

        statusTextView = (TextView) findViewById(R.id.statusTextView);
        statusTextView.setText("Not running");
    }

    @Override
    protected void onResume()
    {
        super.onResume();

        if (mainService != null)
        {
            String status = mainService.getStatus();
            if (status != null && ! status.isEmpty())
                statusTextView.setText(status);
        }

        boolean startServer = mainService == null || ! mainService.isRunning();
        toggleServerButton.setText(startServer ? "Start Server" : "Stop Server");

    }

    @Override
    protected void onStop()
    {
        super.onStop();

        if (bound)
        {
            unbindService(bindingConnection);
            bound = false;
        }

    }

    public void onStartClick(View view)         // TODO: change name
    {
        Intent intent = new Intent(this, MainService.class);
        boolean startServer = mainService == null || ! mainService.isRunning();

        if (startServer)
        {
            if (! bound)
            {
                this.bindService(new Intent(this, MainService.class), bindingConnection,
                        Context.BIND_AUTO_CREATE);
                bound = true;
            }

            this.startService(intent);
            toggleServerButton.setText("Stop Server");

        }
        else
        {
            this.stopService(intent);

            if (bound)
            {
                unbindService(bindingConnection);
                bound = false;
            }

            toggleServerButton.setText("Start Server");
            statusTextView.setText("Not running");
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults)
    {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode)
        {
            case 5:
                if (!(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED))
                    toggleServerButton.setEnabled(false);
        }
    }

}