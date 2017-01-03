package com.m5tt.smscli_server;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity
{
    Button toggleServerButton;
    TextView statusTextView;
    Resources resources;

    private boolean bound;
    private boolean serverRunning;

    private ServiceConnection bindingConnection = new ServiceConnection()
    {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service)
        {
            MainService.LocalBinder binder = (MainService.LocalBinder) service;
            MainService mainService = binder.getService();
            bound = true;

            // update ui
            statusTextView.setText(mainService.getStatus());
            toggleServerButton.setText(mainService.isRunning() ?
                    resources.getString(R.string.button_title_stop) :
                    resources.getString(R.string.button_title_start));

            serverRunning = mainService.isRunning();

            unbindService(this);
        }

        @Override
        public void onServiceDisconnected(ComponentName name)
        {
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
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        resources = getResources();

        /* This causes weird flashy crap - maybe need to check if have permission?
        // Request permissions
        ActivityCompat.requestPermissions(
                this,
                new String[] { Manifest.permission.SEND_SMS},
                5
        );
        */

        LocalBroadcastManager.getInstance(this).registerReceiver(
                statusReceiver, new IntentFilter("status-message"));

        toggleServerButton = (Button) findViewById(R.id.toggleServerButton);
        statusTextView = (TextView) findViewById(R.id.statusTextView);
        serverRunning = false;
    }

    @Override
    protected void onResume()
    {
        super.onResume();

        this.bindService(new Intent(this, MainService.class), bindingConnection,
                Context.BIND_AUTO_CREATE);
    }

    public void onStartClick(View view)         // TODO: change name
    {
        Intent intent = new Intent(this, MainService.class);

        if (! serverRunning)
        {
            this.startService(intent);
            serverRunning = true;
            toggleServerButton.setText(resources.getString(R.string.button_title_stop));
        }
        else
        {
            this.stopService(intent);
            serverRunning = false;

            toggleServerButton.setText(resources.getString(R.string.button_title_start));
            statusTextView.setText(resources.getString(R.string.status_stopped));
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