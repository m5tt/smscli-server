package com.m5tt.smscli_server;

import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;

public class SettingsFragment extends PreferenceFragment
{
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        // load preferences
        addPreferencesFromResource(R.xml.preferences);

        EditTextPreference editTextPreference = (EditTextPreference)
                getPreferenceScreen().findPreference(SettingsActivity.KEY_PREF_PORT);

        // Validate port number
        editTextPreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener()
        {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue)
            {
                int value = Integer.parseInt((String) newValue);

                if (value >= MainService.MIN_TCP_PORT && value <= MainService.MAX_TCP_PORT)
                    return true;
                else
                    return false;
            }
        });

        // Set cursor at end of text
        editTextPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener()
        {
            @Override
            public boolean onPreferenceClick(Preference preference)
            {
                EditTextPreference editTextPreference = (EditTextPreference) preference;
                editTextPreference.getEditText().setSelection(
                        editTextPreference.getText().length());

                return true;
            }
        });
    }
}
