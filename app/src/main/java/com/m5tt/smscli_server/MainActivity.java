package com.m5tt.smscli_server;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

public class MainActivity extends AppCompatActivity
{
    Button toggleServerButton;
    boolean startServer = true;

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

        toggleServerButton = (Button) findViewById(R.id.toggleServerButton);
        toggleServerButton.setText("Start Server");
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults)
    {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode)
        {
            case 5:
                if (!(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED))
                    toggleServerButton.setEnabled(false);
        }
    }

    public void onStartClick(View view)
    {
        Intent intent = new Intent(this, MainService.class);
        toggleServerButton.setText(! startServer ? "Start Server" : "Stop Server");

        if (startServer)
            this.startService(intent);
        else
            this.stopService(intent);

        startServer = ! startServer;
    }
}