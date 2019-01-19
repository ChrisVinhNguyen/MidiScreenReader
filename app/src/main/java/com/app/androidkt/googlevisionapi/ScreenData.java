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

    public List<ScreenElement> elementList;

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

}
