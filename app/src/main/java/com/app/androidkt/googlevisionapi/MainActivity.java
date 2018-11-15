package com.app.androidkt.googlevisionapi;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

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
}
