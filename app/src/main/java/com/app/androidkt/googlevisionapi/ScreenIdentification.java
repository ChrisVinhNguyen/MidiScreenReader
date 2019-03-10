package com.app.androidkt.googlevisionapi;

import java.util.Iterator;
import java.util.List;
import java.io.InputStream;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

import java.io.IOException;
import java.util.Vector;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import com.app.androidkt.googlevisionapi.AppContext;
import com.google.api.services.vision.v1.model.Vertex;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;


public class ScreenIdentification {
    //will need to replace list with state machine here
    private List<ScreenData> screenSearchSet;
    private String currentScreen;

    public String getCurrentScreen() { return currentScreen; }

    public ScreenIdentification()
    {
        currentScreen = "initializing";
        try {
            loadScreenSearchSet();
        }
        catch(org.json.JSONException exception){
            // how you handle the exception
            Log.d("JsonTag", "Error loading json" );
        }
    }

    public void identifyScreen(ScreenData inputScreen)
    {
        Log.d("IdentificationTag", "in identify screen");
        for(ScreenData searchScreen: screenSearchSet)
        {
            Log.d("IdentificationTag", "Comparing screen" +  searchScreen.getName());
            if(searchScreen.compareScreen(inputScreen)){
                Log.d("IdentificationTag", "Screen identified:" +  searchScreen.getName());
                if(currentScreen != searchScreen.getName())
                {
                    currentScreen = searchScreen.getName();
                    Log.d("IdentificationTag", "Updating current screen to:" +  currentScreen);
                }
            }
        }
    }

    private void loadScreenSearchSet() throws JSONException {
        screenSearchSet = new Vector<>();
        JSONObject screensJson = new JSONObject(loadJSONFromAsset(AppContext.getAppContext()));
        JSONArray keys = screensJson.names();

        for(int i = 0; i < keys.length(); i++) {
            String key = keys.get(i).toString();
            JSONArray screenElementJson = screensJson.getJSONArray(key);

            List<String> screenText = new Vector<>();
            List<List<Vertex>> screenVertices = new Vector<>();

            for(int j = 0; j < screenElementJson.length(); j++)
            {
                JSONObject elementJson = screenElementJson.getJSONObject(j);
                String text = (String) elementJson.get("description");

                List<Vertex> vertices = getVertices(elementJson.getJSONObject("boundingPoly").getJSONArray("vertices"));
                screenText.add(text);
                screenVertices.add(vertices);
            }
            ScreenData newScreen = new ScreenData(screenText, screenVertices, key);
            screenSearchSet.add(newScreen);
        }
        Log.d("IdentificationTag", "done loading screen");
    }

    private List<Vertex> getVertices(JSONArray verticesJson) throws JSONException{
        List<Vertex> vertices = new Vector<>();
        for(int i = 0; i < verticesJson.length(); i++)
        {
            JSONObject vertexJson = verticesJson.getJSONObject(i);
            //Log.d("JsonTag", vertexJson.toString());
            int x = vertexJson.getInt("x");
            int y = vertexJson.getInt("y");

            Vertex vertex = new Vertex();
            vertex.setX(x);
            vertex.setY(y);
            vertices.add(vertex);
        }
        return  vertices;
    }

    private String loadJSONFromAsset(Context context) {
        String json = null;
        try {
            InputStream is = context.getAssets().open("screens.json");

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

