package com.m5tt.smscli_server;

import android.os.Bundle;
import android.preference.PreferenceFragment;

/**
 * Created by matt on 1/3/17.
 */

public class SettingsFragment extends PreferenceFragment
{
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        // load preferences
        addPreferencesFromResource(R.xml.preferences);
    }
}
