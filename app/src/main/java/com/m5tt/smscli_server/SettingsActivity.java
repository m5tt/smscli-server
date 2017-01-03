package com.m5tt.smscli_server;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

/**
 * Created by matt on 1/3/17.
 */

public class SettingsActivity extends AppCompatActivity
{
    public static final String KEY_PREF_PORT = "pref_port";

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        // display fragment as main content
        getFragmentManager()
                .beginTransaction()
                .replace(android.R.id.content, new SettingsFragment())
                .commit();
    }
}
