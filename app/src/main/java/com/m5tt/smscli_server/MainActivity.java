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

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ActivityCompat.requestPermissions(
                this,
                new String[] { Manifest.permission.SEND_SMS},
                5
        );
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults)
    {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode)
        {
            case 5:
                if (!(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED))
                {
                    Button startServerButton = (Button) findViewById(R.id.startServerButton);
                    startServerButton.setEnabled(false);
                }
        }
    }

    public void onStartClick(View view)
    {
        // TODO: make toggle button, set labels and such

        Intent intent = new Intent(this, MainService.class);
        this.startService(intent);

    }
}