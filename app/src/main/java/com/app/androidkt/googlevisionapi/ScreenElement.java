package com.app.androidkt.googlevisionapi;

import android.util.Log;

import com.google.api.services.vision.v1.model.Vertex;

import java.util.List;

public class ScreenElement {

    private String text;
    private List<Vertex> vertices;

    // constructor
    public ScreenElement(String textData, List<Vertex> verticesData)
    {
        text = textData;
        vertices = verticesData;

        //this.printScreenElement();
    }

    public String getText(){ return text; }
    public List<Vertex> getVertices(){ return vertices; }

    public void printScreenElement()
    {
        Log.d("ScreenElementTag", "text: " + text + "  |  Bounding box: " + vertices );
    }

}
