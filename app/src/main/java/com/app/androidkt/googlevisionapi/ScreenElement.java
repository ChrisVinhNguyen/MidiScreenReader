package com.app.androidkt.googlevisionapi;

import android.util.Log;

import com.google.api.services.vision.v1.model.Vertex;

import org.opencv.core.Rect;

import java.util.List;

public class ScreenElement {

    private String text;
    private List<Vertex> vertices;

    // constructor for relative vertices with bounding box
    public ScreenElement(String textData, List<Vertex> verticesData, Rect screenBoundingBox)
    {
        text = textData;
        //Log.d("ScreenElementTag", "bounding box: " + screenBoundingBox);
        vertices = verticesData;


    }

    // constructor without bounding box
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
