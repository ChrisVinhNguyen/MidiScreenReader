package com.app.androidkt.googlevisionapi;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.net.Uri;
import android.os.AsyncTask;
import android.speech.tts.TextToSpeech;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.imgproc.Imgproc;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.hardware.Camera.Size;

import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.vision.v1.Vision;
import com.google.api.services.vision.v1.VisionRequestInitializer;
import com.google.api.services.vision.v1.model.AnnotateImageRequest;
import com.google.api.services.vision.v1.model.AnnotateImageResponse;
import com.google.api.services.vision.v1.model.BatchAnnotateImagesRequest;
import com.google.api.services.vision.v1.model.BatchAnnotateImagesResponse;
import com.google.api.services.vision.v1.model.EntityAnnotation;
import com.google.api.services.vision.v1.model.Feature;
import com.google.api.services.vision.v1.model.Image;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;

import static android.speech.tts.TextToSpeech.getMaxSpeechInputLength;

public class CameraListenerActivity extends Activity implements CvCameraViewListener2 {
    private static final String TAG = "OCVSample::CameraListenerActivity";

    private CustomCameraView mOpenCvCameraView;
    private static final String CLOUD_VISION_API_KEY = "AIzaSyDxC9Btvkfi2SH_74NpRL-n5tmFtn-2J0Q";

    TextToSpeech tts;

    //defines the features we're using with the api
    private Feature feature;
    private String[] visionAPI = new String[]{"TEXT_DETECTION"};
    private String api = visionAPI[0];

    // initialization for open cv
    static {
        if (!OpenCVLoader.initDebug()) {
            Log.i("opencv", "OpenCV initialized failed");
        } else {
            Log.i("opencv", "OpenCV initialized success");
        }
    }

    // initialization for open cv
    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i(TAG, "OpenCV loaded successfully");
                    mOpenCvCameraView.enableView();
                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };


    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle  savedInstanceState) {
        Log.i(TAG, "called onCreate");
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.activity_camera_listener);

        // first, set up feature list for api (currently only looking for text response)
        feature = new Feature();
        feature.setType(visionAPI[0]);
        feature.setMaxResults(10);

        // set up camera view
        mOpenCvCameraView = (CustomCameraView) findViewById(R.id.java_surface_view);

        // set resolution for camera view
        mOpenCvCameraView.setMinimumHeight(240);
        mOpenCvCameraView.setMinimumWidth(320);
        mOpenCvCameraView.setMaxFrameSize(320, 240);

        // make camera view visible and start processing frames
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);
    }

    @Override
    public void onPause()
    {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public void onResume()
    {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    // is called when camera view starts
    public void onCameraViewStarted(int width, int height) {
    }

    // is called when camera view stops
    public void onCameraViewStopped() {
    }

    // this function is called for every frame and handles the processing of each frame
    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {

        // first convert the input frame to a matrix
        Mat frameMat = inputFrame.rgba();

        // initialize a bitmap, convert the frame matrix into a bitmap, the perform pre-processing on the bitmap
        Bitmap frameBitmap = Bitmap.createBitmap(frameMat.cols(), frameMat.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(frameMat, frameBitmap);
        frameBitmap = processBitmap(frameBitmap);

        // calls the cloudvision api on the processed bitmap
        callCloudVision(frameBitmap,feature);

        // sleep for 5 seconds, to wait a bit for the api response before sending the next frame in order to avoid sending too many frames
        // TODO: may need to update to wait for api response instead of waiting for 5 seconds every time, responses and new frames may become out of sync
        try
        {
            Thread.sleep(5000);
        }
        catch(InterruptedException ex)
        {
            Thread.currentThread().interrupt();
        }

        // convert bitmap back to matrix to return
        Utils.bitmapToMat(frameBitmap, frameMat);
        return frameMat;
    }

    // perform image pre-processing on bitmap
    private Bitmap processBitmap(Bitmap bitmap)
    {
        // TODO:: move image pre-processing code from VisionAPI activity to here
        return bitmap;
    }

    // calls the cloud vision api to perform ocr on the image
    public void callCloudVision(final Bitmap bitmap, final Feature feature) {

        // get feature list (currently just text detection)
        final List<Feature> featureList = new ArrayList<>();
        featureList.add(feature);

        final List<AnnotateImageRequest> annotateImageRequests = new ArrayList<>();

        // setup and encode bitmap
        AnnotateImageRequest annotateImageReq = new AnnotateImageRequest();
        annotateImageReq.setFeatures(featureList);
        annotateImageReq.setImage(getImageEncodeImage(bitmap));
        annotateImageRequests.add(annotateImageReq);

        // call api in background, get a formatted response containing the text and text bounding boxes
        new AsyncTask<Object, Void, String>() {
            @Override
            protected String doInBackground(Object... params) {
                try {

                    HttpTransport httpTransport = AndroidHttp.newCompatibleTransport();
                    JsonFactory jsonFactory = GsonFactory.getDefaultInstance();

                    VisionRequestInitializer requestInitializer = new VisionRequestInitializer(CLOUD_VISION_API_KEY);

                    Vision.Builder builder = new Vision.Builder(httpTransport, jsonFactory, null);
                    builder.setVisionRequestInitializer(requestInitializer);

                    Vision vision = builder.build();

                    BatchAnnotateImagesRequest batchAnnotateImagesRequest = new BatchAnnotateImagesRequest();
                    batchAnnotateImagesRequest.setRequests(annotateImageRequests);

                    Vision.Images.Annotate annotateRequest = vision.images().annotate(batchAnnotateImagesRequest);
                    annotateRequest.setDisableGZipContent(true);
                    BatchAnnotateImagesResponse response = annotateRequest.execute();
                    return convertResponseToString(response);
                } catch (GoogleJsonResponseException e) {
                    Log.d(TAG, "failed to make API request because " + e.getContent());
                } catch (IOException e) {
                    Log.d(TAG, "failed to make API request because of other IOException " + e.getMessage());
                }
                return "Cloud Vision API request failed. Check logs for details.";
            }
            // upon api response, display formatted response
            protected void onPostExecute(String result) {

                Context context = getApplicationContext();
                int duration = Toast.LENGTH_SHORT;

                Toast toast = Toast.makeText(context, result, duration);
                toast.show();

            }
        }.execute();

    }

    //converts bitmap to JPEG for input into cloud vision
    @NonNull
    private Image getImageEncodeImage(Bitmap bitmap) {
        Image base64EncodedImage = new Image();
        // Convert the bitmap to a JPEG
        // Just in case it's a format that Android understands but Cloud Vision
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream);
        byte[] imageBytes = byteArrayOutputStream.toByteArray();

        // Base64 encode the JPEG
        base64EncodedImage.encodeContent(imageBytes);
        return base64EncodedImage;
    }

    // formats entire api response data into string
    // calls formatAnootation(List<EntityAnnotation> entityAnnotation)
    private String convertResponseToString(BatchAnnotateImagesResponse response) {

        AnnotateImageResponse imageResponses = response.getResponses().get(0);

        List<EntityAnnotation> entityAnnotations;

        String message = "";
        switch (api) {
            case "TEXT_DETECTION":
                entityAnnotations = imageResponses.getTextAnnotations();
                message = formatAnnotation(entityAnnotations);
                break;
        }
        return message;
    }

    // formats individual element of api response data into string
    private String formatAnnotation(List<EntityAnnotation> entityAnnotation) {
        String message = "";

        if (entityAnnotation != null) {
            for (EntityAnnotation entity : entityAnnotation) {
                message = message + " Text:   " + entity.getDescription() + " Bounding box " + entity.getBoundingPoly();
                message += "\n";
            }
        } else {
            message = "Nothing Found";
        }
        return message;
    }

    // read a string out loung
    private void saySomething(String msg){
        if(msg.length()>getMaxSpeechInputLength()){

            msg = msg.substring(0,getMaxSpeechInputLength()-2);
        }

        tts.speak(msg,TextToSpeech.QUEUE_FLUSH,null,null);

    }
    
}

