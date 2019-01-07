package com.app.androidkt.googlevisionapi;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void openVisionApiActivity(View view) {
        Intent intent = new Intent(this, VisionAPI.class);
        startActivity(intent);
    }

    public void openTextToSpeechActivity(View view) {
        Intent intent = new Intent(this, TextToSpeechActivity.class);
        startActivity(intent);
    }

    public void openCameraListenerActivity(View view) {
        Intent intent = new Intent(this, CameraListenerActivity.class);
        startActivity(intent);
    }

    public void openTakePictureAutoActivity(View view) {
        Intent intent = new Intent(this, TakePictureAuto.class);
        startActivity(intent);
    }

    public void openSettingsActivity(View view) {
        Intent intent = new Intent(this, SettingsActivity.class);
        startActivity(intent);
    }
}
