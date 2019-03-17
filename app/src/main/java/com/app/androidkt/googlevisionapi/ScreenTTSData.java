package com.app.androidkt.googlevisionapi;

import android.content.Context;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;



/*
* setting for turing begining/advanced
* link to the gesture
* make sure its connected to the screen identify
* make settings page accessible
* passing text into texttospeech
* */





public class ScreenTTSData {
    private Map<String, Map<String, String>> screenDescriptions = new HashMap<>();

    public ScreenTTSData() {
        try {
            loadScreenDescriptions();
        }
        catch(org.json.JSONException exception){
            // how you handle the exception
            Log.d("JsonTag", "Error loading json" );
        }
    }

    private void loadScreenDescriptions() throws JSONException {
        JSONObject screensJson = new JSONObject(loadJSONFromAsset(AppContext.getAppContext()));
        JSONArray keys = screensJson.names();

        for(int i = 0; i < keys.length(); i++) {
            String key = keys.get(i).toString();

            JSONObject screenInfoJson = screensJson.getJSONObject(key);

            Map<String, String> screenInfo = new HashMap<>();

            String advancedDescription = (String) screenInfoJson.getJSONObject("Description").get("Advanced");
            String beginnerDescription = (String) screenInfoJson.getJSONObject("Description").get("Beginner");
            String actions = (String) screenInfoJson.get("Actions");

            screenInfo.put("advancedDescription", advancedDescription);
            screenInfo.put("beginnerDescription", beginnerDescription);
            screenInfo.put("actions", actions);

            screenDescriptions.put(key, screenInfo);
        }
    }

    public String getBeginnerDescription(String screenName) {
        Map<String,String> screen=screenDescriptions.get(screenName);
        if(screen==null){

            screen=screenDescriptions.get("Unknown");
            return "";
        }
        return screen.get("beginnerDescription");
    }

    public String getAdvancedDescription(String screenName) {
        Map<String,String> screen=screenDescriptions.get(screenName);
        if(screen==null){
            screen=screenDescriptions.get("Unknown");
            return "";

        }
        return screen.get("advancedDescription");
    }

    public String getActions(String screenName) {
        Map<String,String> screen=screenDescriptions.get(screenName);
        if(screen==null){
            screen=screenDescriptions.get("Unknown");
            return "";
        }
        return screen.get("actions");
    }

    private String loadJSONFromAsset(Context context) {
        String json = null;
        try {
            InputStream is = context.getAssets().open("screen_descriptions.json");

            int size = is.available();

            byte[] buffer = new byte[size];

            is.read(buffer);

            is.close();

            json = new String(buffer, "UTF-8");


        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
        return json;
    }
}
