package com.app.androidkt.googlevisionapi;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.icu.text.SimpleDateFormat;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;

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

import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import butterknife.BindView;
import butterknife.ButterKnife;

import static android.speech.tts.TextToSpeech.getMaxSpeechInputLength;

public class VisionAPI extends AppCompatActivity implements AdapterView.OnItemSelectedListener {


    private static final String TAG = "MainActivity";
    private static final int RECORD_REQUEST_CODE = 101;
    private static final int CAMERA_REQUEST_CODE = 102;

    private static final String CLOUD_VISION_API_KEY = "AIzaSyDxC9Btvkfi2SH_74NpRL-n5tmFtn-2J0Q";

    TextToSpeech tts;

    @BindView(R.id.takePicture)
    Button takePicture;

    @BindView(R.id.visionSpeechButton)
    Button visionSpeechButton;

    @BindView(R.id.imageProgress)
    ProgressBar imageUploadProgress;

    @BindView(R.id.imageView)
    ImageView imageView;

    @BindView(R.id.spinnerVisionAPI)
    Spinner spinnerVisionAPI;

    @BindView(R.id.visionAPIData)
    TextView visionAPIData;
    private Feature feature;
    private Bitmap bitmap;
    Uri image;
    String mCameraFileName;
    private Mat bitmapMatrix;
    private String[] visionAPI = new String[]{"TEXT_DETECTION"};

    private String api = visionAPI[0];

    static {
        if (!OpenCVLoader.initDebug()) {
            Log.i("opencv", "OpenCV initialized failed");
        } else {
            Log.i("opencv", "OpenCV initialized success");
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vision_api);
        ButterKnife.bind(this);
        feature = new Feature();
        feature.setType(visionAPI[0]);
        feature.setMaxResults(10);

        spinnerVisionAPI.setOnItemSelectedListener(this);
        ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, visionAPI);
        dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerVisionAPI.setAdapter(dataAdapter);

        takePicture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                takePictureFromCamera();
            }
        });

        visionSpeechButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                final TextView ettext = (TextView) findViewById(R.id.visionAPIData);
                String message = ettext.getText().toString().trim();
                saySomething(message);
            }

        });

        tts = new TextToSpeech(VisionAPI.this, new TextToSpeech.OnInitListener() {

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
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (checkPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            takePicture.setVisibility(View.VISIBLE);
        } else {
            takePicture.setVisibility(View.INVISIBLE);
            makeRequest(Manifest.permission.CAMERA);
        }
    }

    private int checkPermission(String permission) {
        return ContextCompat.checkSelfPermission(this, permission);
    }

    private void makeRequest(String permission) {
        ActivityCompat.requestPermissions(this, new String[]{permission}, RECORD_REQUEST_CODE);
    }

    public void takePictureFromCamera() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        // Ensure that there's a camera activity to handle the intent
        if (intent.resolveActivity(getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                // Error occurred while creating the File

            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this,
                        "com.app.androidkt.googlevisionapi",
                        photoFile);
                intent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(intent, CAMERA_REQUEST_CODE);
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode,
                                    Intent data) {
        if (requestCode == CAMERA_REQUEST_CODE && resultCode == RESULT_OK) {


            // Get the dimensions of the bitmap
            BitmapFactory.Options bmOptions = new BitmapFactory.Options();
            bmOptions.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(mCameraFileName, bmOptions);
            int photoW = bmOptions.outWidth;
            int photoH = bmOptions.outHeight;
            // Get the dimensions of the View
            // int targetW = photoW/2;
            //int targetH = photoH/2;
            // Determine how much to scale down the image
            int scaleFactor = 2;//Math.min(photoW/targetW, photoH/targetH);

            // Decode the image file into a Bitmap sized to fill the View
            bmOptions.inJustDecodeBounds = false;
            bmOptions.inSampleSize = scaleFactor;

            bitmap = BitmapFactory.decodeFile(mCameraFileName, bmOptions);

            //String colourspacename = bitmap.getColorSpace().getName();
            //String configbitmap = bitmap.getConfig().toString();
            bitmapMatrix = new Mat();
            bitmap = convertToGrayscale(bitmap);
            Utils.bitmapToMat(bitmap, bitmapMatrix);
            double sigmaKern = .9;
            int kernSize = (int) (2 * Math.ceil(2 * sigmaKern) + 1);
            Imgproc.GaussianBlur(bitmapMatrix, bitmapMatrix, new Size(kernSize, kernSize), sigmaKern);

            Utils.matToBitmap(bitmapMatrix, bitmap);
            bitmapMatrix.release();
            imageView.setImageBitmap(bitmap);
            callCloudVision(bitmap, feature);
        }
    }


    private void callCloudVision(final Bitmap bitmap, final Feature feature) {
        imageUploadProgress.setVisibility(View.VISIBLE);
        final List<Feature> featureList = new ArrayList<>();
        featureList.add(feature);

        final List<AnnotateImageRequest> annotateImageRequests = new ArrayList<>();

        AnnotateImageRequest annotateImageReq = new AnnotateImageRequest();
        annotateImageReq.setFeatures(featureList);
        annotateImageReq.setImage(getImageEncodeImage(bitmap));
        annotateImageRequests.add(annotateImageReq);


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

            protected void onPostExecute(String result) {
                visionAPIData.setText(result);
                imageUploadProgress.setVisibility(View.INVISIBLE);
            }
        }.execute();
//        bitmap.recycle();
    }

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

    @Override
    public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
        api = (String) adapterView.getItemAtPosition(i);
        feature.setType(api);
        if (bitmap != null)
            callCloudVision(bitmap, feature);
    }

    @Override
    public void onNothingSelected(AdapterView<?> adapterView) {

    }

    public Bitmap convertToGrayscale(Bitmap bmpOriginal) {
        int width, height;
        height = bmpOriginal.getHeight();
        width = bmpOriginal.getWidth();

        Bitmap bmpGrayscale = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(bmpGrayscale);
        Paint paint = new Paint();
        ColorMatrix cm = new ColorMatrix();
        cm.setSaturation(0);
        ColorMatrixColorFilter f = new ColorMatrixColorFilter(cm);
        paint.setColorFilter(f);
        c.drawBitmap(bmpOriginal, 0, 0, paint);
        bmpOriginal.recycle();
        return bmpGrayscale;
    }

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        mCameraFileName = image.getAbsolutePath();
        return image;
    }

    private void saySomething(String msg){
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



}
