package com.app.androidkt.googlevisionapi;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewDebug;
import android.view.WindowManager;
import android.widget.Toast;

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

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import androidx.annotation.NonNull;
import androidx.core.view.GestureDetectorCompat;
import androidx.preference.PreferenceManager;

import static android.speech.tts.TextToSpeech.getMaxSpeechInputLength;
import static java.lang.Double.parseDouble;
import static java.lang.Float.parseFloat;
import static java.lang.Integer.getInteger;
import static java.lang.Integer.parseInt;
import static java.lang.Math.abs;
import static org.opencv.imgproc.Imgproc.CHAIN_APPROX_SIMPLE;
import static org.opencv.imgproc.Imgproc.RETR_EXTERNAL;
import static org.opencv.imgproc.Imgproc.rectangle;

public class CameraListenerActivity extends Activity implements CvCameraViewListener2, OnTouchListener, GestureDetector.OnGestureListener, GestureDetector.OnDoubleTapListener {
    private static final String TAG = "OCVSample::CameraListenerActivity";

    private CustomCameraView mOpenCvCameraView;
    private static final String CLOUD_VISION_API_KEY = "AIzaSyDxC9Btvkfi2SH_74NpRL-n5tmFtn-2J0Q";

    private static final String DEBUG_TAG = "Gestures";
    private GestureDetectorCompat mDetector;
    private ScreenIdentification screenIdentifier;
    private ScreenData currentScreenData;
    private ScreenTTSData screenDescriptions;

    TextToSpeech tts;

    // variables to control the quality and rate of the image passed to the api;
    int width = 640;
    int height = 480;
    int quality = 50;
    int sleep_time = 2000;
    double sigmaKernBefore = .9;
    double sigmaKernAfter = .3;

    float initialX, initialY;

    //defines the features we're using with the api
    private Feature feature;
    private String[] visionAPI = new String[]{"TEXT_DETECTION"};
    private String api = visionAPI[0];

    private BatchAnnotateImagesResponse responseFromApi;

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
                    mOpenCvCameraView.setOnTouchListener(CameraListenerActivity.this);
                    mDetector = new GestureDetectorCompat(CameraListenerActivity.this,CameraListenerActivity.this);
                    mDetector.setOnDoubleTapListener(CameraListenerActivity.this);
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

        //initialize text to speech engine
        tts = new TextToSpeech(CameraListenerActivity.this, new TextToSpeech.OnInitListener() {

            @Override
            public void onInit(int status) {
                // TODO Auto-generated method stub
                if(status == TextToSpeech.SUCCESS){
                    int result=tts.setLanguage(Locale.US);
                    if(result==TextToSpeech.LANG_MISSING_DATA ||
                            result==TextToSpeech.LANG_NOT_SUPPORTED){
                        Log.e("error", "This Language is not supported");
                    }
                }
                else
                    Log.e("error", "Initilization Failed!");
            }
        });
        tts.setLanguage(Locale.US);

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        String speechRateString = sharedPreferences.getString("SpeechRateValue", "1");

        float speechRate=parseFloat(speechRateString);
        tts.setSpeechRate(speechRate);

        screenIdentifier = new ScreenIdentification();
        screenDescriptions = new ScreenTTSData();
        // set up camera view
        mOpenCvCameraView = findViewById(R.id.java_surface_view);

        // set resolution for camera view
        mOpenCvCameraView.setMinimumHeight(height);
        mOpenCvCameraView.setMinimumWidth(width);
        mOpenCvCameraView.setMaxFrameSize(width, height);

        // make camera view visible and start processing frames
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);
    }

    @Override
    public void onPause()
    {
        super.onPause();
        // shut the text to speech up
        tts.speak(" ",TextToSpeech.QUEUE_FLUSH,null,null);
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
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        String speechRateString = sharedPreferences.getString("SpeechRateValue", "1");

        float speechRate=parseFloat(speechRateString);
        tts.setSpeechRate(speechRate);

    }

    public void onDestroy() {
        super.onDestroy();
        tts.speak(" ",TextToSpeech.QUEUE_FLUSH,null,null);

        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    // is called when camera view starts
    public void onCameraViewStarted(int width, int height) {
    }

    // is called when camera view stops
    public void onCameraViewStopped() {
        tts.speak(" ",TextToSpeech.QUEUE_FLUSH,null,null);

    }

    // this function is called for every frame and handles the processing of each frame
    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {

        // first convert the input frame to a matrix
        Mat frameMat = inputFrame.gray();

        // initialize a bitmap, convert the frame matrix into a bitmap, the perform pre-processing on the bitmap
        Bitmap frameBitmap = Bitmap.createBitmap(frameMat.cols(), frameMat.rows(), Bitmap.Config.ARGB_8888);
        Rect screenBoundingBox= processBitmap(frameMat);
        Utils.matToBitmap(frameMat, frameBitmap);

        // calls the cloudvision api on the processed bitmap
        callCloudVision(frameBitmap,feature,screenBoundingBox);

        Log.d("gestureTag", "IN SINGLE TAP");

        if(!calibrateScreen(screenBoundingBox))
        {
            return frameMat;
        }

        if(screenBoundingBox==null) {
            Log.d("TempTag1", "initializing without bounding box");
            currentScreenData = new ScreenData(responseFromApi);
        }
        else {
            Log.d("TempTag2", "initializing with bounding box");
            currentScreenData = new ScreenData(responseFromApi, screenBoundingBox);

        }

        if(screenIdentifier.identifyScreen(currentScreenData))
        {
            String curScreen =screenIdentifier.getCurrentScreen();
            sayScreenandActions(curScreen);
        }

        Utils.bitmapToMat(frameBitmap, frameMat);
        return frameMat;
    }

    // perform image pre-processing on bitmap
    private Rect processBitmap(Mat bitmapMatrix)
    {
        Rect screenBoundingBox = null;
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        Mat edges = new Mat();
        List<MatOfPoint> contours = new ArrayList<>();
        Imgproc.Canny(bitmapMatrix, edges, 100, 200);
        Imgproc.findContours(edges, contours, new Mat(), RETR_EXTERNAL, CHAIN_APPROX_SIMPLE);

        //finding the largest contour area in this case it should be the screen
        double maxArea = -1;
        int maxAreaIdx = -1;
        for (int idx = 0; idx < contours.size(); idx++) {
            Mat contour = contours.get(idx);
            double contourarea = Imgproc.contourArea(contour);
            if (contourarea > maxArea) {
                maxArea = contourarea;
                maxAreaIdx = idx;
            }
        }
        //find the boundingbox of the screen and draw it on the picture
        if (!contours.isEmpty()) {
            screenBoundingBox = (Imgproc.boundingRect(contours.get(maxAreaIdx)));
            rectangle(bitmapMatrix, new Point(screenBoundingBox.x, screenBoundingBox.y), new Point(screenBoundingBox.x + screenBoundingBox.width, screenBoundingBox.y + screenBoundingBox.height), new Scalar(100), 4);
        }
        boolean processImage = sharedPreferences.getBoolean("ImageProcessEnable", true);
        Log.i("Preferences", "processImage" + processImage);

        if (!processImage)
            return screenBoundingBox;


        String sigmaBeforeString = sharedPreferences.getString("GaussValueBefore", "0.7");
        String sigmaAfterString = sharedPreferences.getString("GaussValueAfter", "0.3");

        double sigmaBefore = parseDouble(sigmaBeforeString);
        double sigmaAfter = parseDouble(sigmaAfterString);

        Log.i("Preferences", "sigmaBefore" + sigmaBefore);
        Log.i("Preferences", "sigmaAfter" + sigmaAfter);

        int kernSize = (int) (2 * Math.ceil(2 * sigmaBefore) + 1);
        Imgproc.GaussianBlur(bitmapMatrix, bitmapMatrix, new org.opencv.core.Size(kernSize, kernSize), sigmaBefore);

        boolean convertToBW = sharedPreferences.getBoolean("ConvertToBW", true);
        Log.i("Preferences", "ConvertToBW" + convertToBW);

        if (!convertToBW)
            return screenBoundingBox;

        int rows=bitmapMatrix.rows();
        int cols=bitmapMatrix.cols();
        double []grayScaleHistogram=new double[256];
        for(int i=0;i<rows;i++){
            for(int j=0;j<cols;j++) {
                double[] value=bitmapMatrix.get(i,j);
                int pixelColourIntensity =(int)value[0];
                if(pixelColourIntensity>=100)
                    grayScaleHistogram[pixelColourIntensity]++;
            }
        }

        int []peaks=new int[255];
        findPeaks(grayScaleHistogram,peaks,50);
        Arrays.sort(peaks);
        int firstPeak=0;
        int secondPeak=0;

        if(peaks.length-1 >=0)
            firstPeak=peaks[peaks.length-1];
        if(peaks.length-2 >=0) {
            secondPeak = peaks[peaks.length - 2];
        }

        int difftomid=(abs(firstPeak-secondPeak)/2);
        int cutoff=(Math.min(firstPeak,secondPeak)+difftomid+difftomid/12);

        //manually set the values to 1/0 depending on the cutoff value
        for(int i=0;i<rows;i++){
            for(int j=0;j<cols;j++) {
                double[] value=bitmapMatrix.get(i,j);
                int test =(int)value[0];
                value[0]=255;
                if(test>=cutoff)
                    value[0]=255;
                else
                    value[0]=0;
                bitmapMatrix.put(i,j,value);
            }
        }


        kernSize = (int) (2 * Math.ceil(2 * sigmaAfter) + 1);
        Imgproc.GaussianBlur(bitmapMatrix, bitmapMatrix, new org.opencv.core.Size(kernSize, kernSize), sigmaAfter);


        return screenBoundingBox;
    }

    // calls the cloud vision api to perform ocr on the image
    public void callCloudVision(final Bitmap bitmap, final Feature feature, Rect boundingBox) {

        // get feature list (currently just text detection)
        final List<Feature> featureList = new ArrayList<>();
        featureList.add(feature);

        final List<AnnotateImageRequest> annotateImageRequests = new ArrayList<>();

        // setup and encode bitmap

        AnnotateImageRequest annotateImageReq = new AnnotateImageRequest();
        annotateImageReq.setFeatures(featureList);

        if(boundingBox!=null) {
            Bitmap resizedBitmap=Bitmap.createBitmap(bitmap, boundingBox.x,boundingBox.y,boundingBox.width,boundingBox.height);
            Log.i("boundingbox","Height:"+ Integer.toString(resizedBitmap.getHeight())+"width:"+Integer.toString(resizedBitmap.getWidth()));
            annotateImageReq.setImage(getImageEncodeImage(resizedBitmap));

        }else{
            annotateImageReq.setImage(getImageEncodeImage(bitmap));

        }

        annotateImageRequests.add(annotateImageReq);

        // call api, get a formatted response containing the text and text bounding boxes

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
                    responseFromApi = response;
                    convertResponseToString(response);
                    //return convertResponseToString(response);
                } catch (GoogleJsonResponseException e) {
                    Log.d(TAG, "failed to make API request because " + e.getContent());
                } catch (IOException e) {
                    Log.d(TAG, "failed to make API request because of other IOException " + e.getMessage());
                }
                //return "Cloud Vision API request failed. Check logs for details.";
            }

    //converts bitmap to JPEG for input into cloud vision
    @NonNull
    private Image getImageEncodeImage(Bitmap bitmap) {
        Image base64EncodedImage = new Image();
        // Convert the bitmap to a JPEG
        // Just in case it's a format that Android understands but Cloud Vision
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, byteArrayOutputStream);
        byte[] imageBytes = byteArrayOutputStream.toByteArray();

        // Base64 encode the JPEG
        base64EncodedImage.encodeContent(imageBytes);
        return base64EncodedImage;
    }

    // formats entire api response data into string
    // calls formatAnnotation(List<EntityAnnotation> entityAnnotation)
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
                //say the text here
//                saySomething(entity.getDescription());
            }
        } else {
            message = "Nothing Found";
        }
        return message;
    }

    // read a string out loud
    public void saySomething(String msg){
        if(msg.length()>getMaxSpeechInputLength()){

            msg = msg.substring(0,getMaxSpeechInputLength()-2);
        }

        tts.speak(msg,TextToSpeech.QUEUE_FLUSH,null,null);
    }

    public int findPeaks(double[] data, int[] peaks, int width) {
        int peakCount = 0;
        int maxp = 0;
        int mid = 0;
        int end = data.length;
        while (mid < end) {
            int i = mid - width;
            if (i < 0)
                i = 0;
            int stop = mid + width + 1;
            if (stop > data.length)
                stop = data.length;
            maxp = i;
            for (i++; i < stop; i++)
                if (data[i] > data[maxp])
                    maxp = i;
            if (maxp == mid) {
                int j;
                for (j = peakCount; j > 0; j--) {
                    if (data[maxp] <= data[peaks[j-1]])
                        break;
                    else if (j < peaks.length)
                        peaks[j] = peaks[j-1];
                }
                if (j != peaks.length)
                    peaks[j] = maxp;
                if (peakCount != peaks.length)
                    peakCount++;
            }
            mid++;
        }
        return peakCount;
    } // findPeaks()

    // reads any touch events
    @Override
    public boolean onTouch(View v, MotionEvent event) {
        // checks if the touch event matches any of the motion events events below
        if (this.mDetector.onTouchEvent(event)) {
            //Toast.makeText(this,"returning true", Toast.LENGTH_SHORT).show();
            return true;
        }
        return false;
    }

    // functions required for the onTouch event
    // all of them must be defined, but they do not all need to be used
    @Override
    public boolean onDown(MotionEvent event) {
        return true;
    }

    @Override
    public boolean onFling(MotionEvent event1, MotionEvent event2,
                           float velocityX, float velocityY) {
        return true;
    }

    @Override
    public void onLongPress(MotionEvent event) {
        //Toast.makeText(this,"onLongPress: " + event.toString(), Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean onScroll(MotionEvent event1, MotionEvent event2, float distanceX,
                            float distanceY) {
        if(distanceX + distanceY > 35)
        {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
        }
        //Toast.makeText(this, "Unknown Screen" , Toast.LENGTH_SHORT).show();
        return true;
    }

    @Override
    public void onShowPress(MotionEvent event) {
        //Toast.makeText(this,"onShowPress: " + event.toString(), Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean onSingleTapUp(MotionEvent event) {
        return true;
    }

    @Override
    public boolean onDoubleTap(MotionEvent event) {
        // output the bounding vertices captured by the Google Vision API
        String currentScreen = screenIdentifier.getCurrentScreen();
        getScreenActions(currentScreen);
        return true;
    }

    @Override
    public boolean onDoubleTapEvent(MotionEvent event) {
        return true;
    }

    @Override
    public boolean onSingleTapConfirmed(MotionEvent event) {
        // output the current screen of keyboard

        //convertResponseStringFromGesture(responseFromApi, "description");
        String currentScreen = screenIdentifier.getCurrentScreen();
        getScreenDescription(currentScreen);
        return true;
    }

    // takes in an option based on the specific touch events and outputs a response accordingly
    private String convertResponseStringFromGesture(BatchAnnotateImagesResponse response, String option) {

        AnnotateImageResponse imageResponses = response.getResponses().get(0);

        List<EntityAnnotation> entityAnnotations;

        String message = "";
        switch (api) {
            case "TEXT_DETECTION":
                entityAnnotations = imageResponses.getTextAnnotations();
                message = formatBoundingBoxAnnotation(entityAnnotations, option);
                break;
        }
        //Toast.makeText(this, message , Toast.LENGTH_SHORT).show();
        return message;
    }

    // formats individual element of api response data into string
    private String formatBoundingBoxAnnotation(List<EntityAnnotation> entityAnnotation, String option) {
        String message = "";

        if (entityAnnotation != null) {
            for (EntityAnnotation entity : entityAnnotation) {
                message = message + " Text:   " + entity.getDescription() + " Bounding box " + entity.getBoundingPoly();
                message += "\n";

                if (option == "description") {
                    //saySomething(entity.getDescription());
                } else if (option == "bounding") {
                    //saySomething(entity.getBoundingPoly().toString());
                }
            }
        } else {
            message = "Nothing Found";
        }
        return message;
    }

    private void getScreenDescription(String currentScreen){
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        String descriptionLevel = sharedPreferences.getString("DescriptionLevel", "Beginner");
        String description="";
        if(descriptionLevel.equals("Beginner")){
            description = screenDescriptions.getBeginnerDescription(currentScreen);
        }else {
            description = screenDescriptions.getAdvancedDescription(currentScreen);
        }
        saySomething(description);
        //Toast.makeText(this, description , Toast.LENGTH_SHORT).show();
    }

    private void getScreenActions(String currentScreen){
        String actions = screenDescriptions.getActions(currentScreen);
        saySomething(actions);
        //Toast.makeText(this, actions , Toast.LENGTH_SHORT).show();
    }

    private void sayScreenandActions(String currentScreen){
        String actions = screenDescriptions.getActions(currentScreen);
        String fullDescription = "You are currently on " + currentScreen + "screen\\." + actions;
        saySomething(fullDescription);
        //Toast.makeText(this, fullDescription , Toast.LENGTH_SHORT).show();
    }

    // returns true if the screen is correctly calibrated
    private boolean calibrateScreen(Rect boundary){
        //Log.d()
        return true;
    }
}


