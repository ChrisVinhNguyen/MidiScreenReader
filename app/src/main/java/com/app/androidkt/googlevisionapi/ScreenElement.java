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
        vertices = verticesData;
        for (Vertex v:verticesData){
            Integer xVal=v.getX();
            Integer yVal=v.getY();
            v.setX(xVal==null?0:(xVal*100)/screenBoundingBox.width);
            v.setY(yVal==null?0:(yVal*100)/screenBoundingBox.height);
        }
        this.printScreenElement();

    }

    // constructor without bounding box
    public ScreenElement(String textData, List<Vertex> verticesData)
    {
        text = textData;
        vertices = verticesData;
        this.printScreenElement();

    }

    public String getText(){ return text; }
    public List<Vertex> getVertices(){ return vertices; }

    public void printScreenElement()
    {
        Log.d("PrintScreenTag", "text: " + text + "  |  Bounding box: " + vertices );
    }

}
