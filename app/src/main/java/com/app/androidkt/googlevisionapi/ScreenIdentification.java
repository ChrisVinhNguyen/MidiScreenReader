package com.app.androidkt.googlevisionapi;

import java.util.List;
import java.io.InputStream;
import android.content.Context;
import java.io.IOException;

public class ScreenIdentification {
    //will need to replace list with state machine here
    private List<ScreenData> screenSearchSet;

    public String currentScreen;

    public String loadJSONFromAsset(Context context) {
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

