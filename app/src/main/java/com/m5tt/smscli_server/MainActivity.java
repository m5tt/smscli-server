package com.m5tt.smscli_server;

import android.Manifest;
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
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SwitchCompat;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.CompoundButton;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity
{
    private static final int PERMISSION_ALL = 1;
    private static final String[] PERMISSIONS = {
            Manifest.permission.READ_SMS,
            Manifest.permission.SEND_SMS,
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.READ_CONTACTS
    };

    SwitchCompat serverSwitch;
    TextView statusTextView;
    Resources resources;

    private ServiceConnection bindingConnection = new ServiceConnection()
    {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service)
        {
            MainService.LocalBinder binder = (MainService.LocalBinder) service;
            MainService mainService = binder.getService();

            // set state of switch according to switch of server
            serverSwitch.setChecked(mainService.isRunning());

            // set server switch text
            serverSwitch.setText(serverSwitch.isChecked() ?
                    resources.getString(R.string.switch_title_on) :
                    resources.getString(R.string.switch_title_off));

            // set status
            statusTextView.setText(mainService.getStatus());

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
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

        // set up toolbar as our app bar
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.showOverflowMenu();

        // set some globals
        serverSwitch = (SwitchCompat) findViewById(R.id.serverSwitch);
        statusTextView = (TextView) findViewById(R.id.statusTextView);
        resources = getResources();

        // register our status receiver for status updates
        LocalBroadcastManager.getInstance(this).registerReceiver(
                statusReceiver, new IntentFilter("status-message"));

        // set handler for serverSwitch - switch is initialized by onResume()
        serverSwitch.setOnCheckedChangeListener(
                new SwitchCompat.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
                    {
                         Intent intent = new Intent(MainActivity.this, MainService.class);

                        if (isChecked)
                        {
                            MainActivity.this.startService(intent);
                            serverSwitch.setText(resources.getString(R.string.switch_title_on));
                        }
                        else
                        {
                            MainActivity.this.stopService(intent);

                            serverSwitch.setText(resources.getString(R.string.switch_title_off));
                            statusTextView.setText(resources.getString(R.string.status_stopped));
                        }
                    }
                });

        serverSwitch.setEnabled(true);
        requestPermissions();
    }

    @Override
    protected void onResume()
    {
        super.onResume();

        // update status in ui
        this.bindService(new Intent(this, MainService.class), bindingConnection,
                Context.BIND_AUTO_CREATE);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        getMenuInflater().inflate(R.menu.options_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId())
        {
            case R.id.item_settings:
                this.startActivity(new Intent(this, SettingsActivity.class));
            default:    // not recognized
                return super.onOptionsItemSelected(item);
        }
    }

    private void requestPermissions()
    {
        if (! Util.hasPermissions(this, PERMISSIONS))
            ActivityCompat.requestPermissions(this, PERMISSIONS, PERMISSION_ALL);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults)
    {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode)
        {
            case PERMISSION_ALL:
                if (grantResults.length <= 0 || grantResults[0] != PackageManager.PERMISSION_GRANTED)
                    serverSwitch.setEnabled(false);
                break;
        }
    }

}