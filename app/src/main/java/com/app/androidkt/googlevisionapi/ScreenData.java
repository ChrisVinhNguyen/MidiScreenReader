package com.app.androidkt.googlevisionapi;


import android.util.Log;
import android.widget.Toast;

import com.google.api.services.vision.v1.model.AnnotateImageResponse;
import com.google.api.services.vision.v1.model.BatchAnnotateImagesResponse;
import com.google.api.services.vision.v1.model.EntityAnnotation;
import com.google.api.services.vision.v1.model.Vertex;

import java.util.List;
import java.util.Vector;

public class ScreenData {

    private List<ScreenElement> elementList;

    public ScreenData( BatchAnnotateImagesResponse response)
    {
        elementList = new Vector<>();

        AnnotateImageResponse imageResponses = response.getResponses().get(0);

        List<EntityAnnotation> entityAnnotations;
        entityAnnotations = imageResponses.getTextAnnotations();

        if (entityAnnotations != null) {
            for (EntityAnnotation entity : entityAnnotations) {
                String text = entity.getDescription();
                List<Vertex> vertices = entity.getBoundingPoly().getVertices();

                ScreenElement element = new ScreenElement(text, vertices);
                elementList.add(element);

            }
        }
    }

    public boolean compareScreen(ScreenData inputScreen)
    {
        boolean sameScreen = false;
        int numElements = this.elementList.size();
        int correctElements = 0;
        float vertexTreshold = 50;

        for(ScreenElement element : this.elementList)
        {
            // min number for viable levenshtein distance
            int textThreshold = element.getText().length()/2;

            for(ScreenElement inputElement : inputScreen.elementList)
            {
                //if()
            }
        }

        if (correctElements == numElements)
        {
            sameScreen = true;
        }
        return  sameScreen;
    }

    // computes the minimum number of single-character edits required to change one word into the other. Strings do not have to be the same length
    public static int levenshteinDistance(String a, String b) {
        a = a.toLowerCase();
        b = b.toLowerCase();
        // i == 0
        int [] costs = new int [b.length() + 1];
        for (int j = 0; j < costs.length; j++)
            costs[j] = j;
        for (int i = 1; i <= a.length(); i++) {
            // j == 0; nw = lev(i - 1, j)
            costs[0] = i;
            int nw = i - 1;
            for (int j = 1; j <= b.length(); j++) {
                int cj = Math.min(1 + Math.min(costs[j], costs[j - 1]), a.charAt(i - 1) == b.charAt(j - 1) ? nw : nw + 1);
                nw = costs[j];
                costs[j] = cj;
            }
        }
        return costs[b.length()];
    }

}
