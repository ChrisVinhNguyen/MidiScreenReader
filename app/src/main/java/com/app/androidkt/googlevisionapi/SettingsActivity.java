package com.app.androidkt.googlevisionapi;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

public class SettingsActivity extends AppCompatActivity {

    /*implements
    PreferenceFragmentCompat.OnPreferenceStartFragmentCallback {*/
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.settings_activity_page, new MySettingsFragment())
                .commit();

    }

    public void openCameraListenerActivity(View view) {
        Intent intent = new Intent(this, CameraListenerActivity.class);
        startActivity(intent);
    }
/*
    @Override
    public boolean onPreferenceStartFragment(PreferenceFragmentCompat caller, Preference pref) {
        // Instantiate the new Fragment
        final Bundle args = pref.getExtras();
        final Fragment fragment = getSupportFragmentManager().getFragmentFactory().instantiate(
                getClassLoader(),
                pref.getFragment(),
                args);
        fragment.setArguments(args);
        fragment.setTargetFragment(caller, 0);
        // Replace the existing Fragment with the new Fragment
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.settings_activity_page, fragment)
                .addToBackStack(null)
                .commit();
        return true;
    }
*/
}
