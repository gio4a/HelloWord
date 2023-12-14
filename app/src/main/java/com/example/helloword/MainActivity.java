package com.example.helloword;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AlertDialog;
import androidx.lifecycle.LifecycleOwner;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Color;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Build;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.example.helloword.ml.Model1116;
import com.example.helloword.ml.ModelliteMobilenet1207;
import com.example.helloword.ml.ModelliteMobilenetNonq1212;
import com.google.common.util.concurrent.ListenableFuture;

import org.tensorflow.lite.DataType;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.Executor;


public class MainActivity extends AppCompatActivity implements View.OnClickListener, ImageAnalysis.Analyzer {

    private ListenableFuture<ProcessCameraProvider> provider;
    private static final int CAMERA_PERMISSION_REQUEST_CODE = 200;
    private static final int AUDIO_PERMISSION_REQUEST_CODE = 300;
    private static final int DIM_WIN = 200;
    private ImageAnalysis imageAn;
    private Button bin_button, trash_button, mic_button;
    private PreviewView pview;
    private TextView tv;
    private ImageCapture imageCapt;
    private SoundPool soundPool;
    Model1116 modelResNet1116;
    ModelliteMobilenet1207 modelliteMobilenet1207;
    ModelliteMobilenetNonq1212 modelliteMobilenetNonq1212;
    private int[] sound=new int[6];
    private int our_width = 512;
    private int our_height = 384;
    private String [] labels = {"Cartone", "Vetro", "Metallo", "Carta", "Plastica", "Indifferenziato"};

    private ColorUtils colorUtils = new ColorUtils();

    private SpeechRecognizer speechRecognizer;

    @Override
    protected void onDestroy() {
        super.onDestroy();
        modelResNet1116.close();
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (! checkCameraPermission())
            requestCameraPermission();

        if (! checkAudioPermission())
            requestAudioPermission();

        try {
            modelResNet1116 = Model1116.newInstance(getApplicationContext());
            //modelliteMobilenet1207 = ModelliteMobilenet1207.newInstance(getApplicationContext());
            //modelliteMobilenetNonq1212 = ModelliteMobilenetNonq1212.newInstance(getApplicationContext());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        bin_button = findViewById(R.id.bin_bt);
        trash_button = findViewById(R.id.trash_bt);
        mic_button = findViewById(R.id.mic_button);
        tv = findViewById(R.id.textView);
        pview = findViewById(R.id.previewView);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

            AudioAttributes audioAttributes = new AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .build();

            soundPool = new SoundPool.Builder()
                    .setAudioAttributes(audioAttributes)
                    .build();

        } else
            soundPool = new SoundPool(1, AudioManager.STREAM_MUSIC, 0);

        sound[3] = soundPool.load(this, R.raw.carta, 1);
        sound[0] = soundPool.load(this, R.raw.cartone, 1);
        sound[4] = soundPool.load(this, R.raw.plastica, 1);
        sound[1] = soundPool.load(this, R.raw.vetro, 1);
        sound[2] = soundPool.load(this, R.raw.metallo, 1);
        sound[5] = soundPool.load(this, R.raw.indifferenziata, 1);

        bin_button.setOnClickListener(this);
        trash_button.setOnClickListener(this);


        provider = ProcessCameraProvider.getInstance(this);
        provider.addListener( () ->
        {
            try{
                ProcessCameraProvider cameraProvider = provider.get();
                startCamera(cameraProvider);
            } catch (Exception e)
            {
                e.printStackTrace();
            }
        }, getExecutor());

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        final Intent speechRecognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());

        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override
            public void onReadyForSpeech(Bundle bundle) {
                tv.setText("Parla!");
            }

            @Override
            public void onBeginningOfSpeech() {
               //tv.setText("");
                //tv.setText("Listening...");
            }

            @Override
            public void onRmsChanged(float v) {

            }

            @Override
            public void onBufferReceived(byte[] bytes) {

            }

            @Override
            public void onEndOfSpeech() {

            }

            @Override
            public void onError(int i) {

            }

            @Override
            public void onResults(Bundle bundle) {
                //micButton.setImageResource(R.drawable.ic_mic_black_off);
                ArrayList<String> data = bundle.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                //tv.setText(data.get(0));
                if(data.get(0).equalsIgnoreCase("Bidone") || data.get(0).equalsIgnoreCase("contenedor"))
                {
                    capturePhotoBin();
                }
                else if (data.get(0).equalsIgnoreCase("Rifiuto") || data.get(0).equalsIgnoreCase("basura"))
                {
                    capturePhotoTrash();
                }
                else
                {
                    tv.setText("Non ho capito");
                }


            }

            @Override
            public void onPartialResults(Bundle bundle) {

            }

            @Override
            public void onEvent(int i, Bundle bundle) {

            }
        });

        mic_button.setOnTouchListener((view, motionEvent) -> {
            if (motionEvent.getAction() == MotionEvent.ACTION_UP){
                //speechRecognizer.stopListening();
            }
            if (motionEvent.getAction() == MotionEvent.ACTION_DOWN){
                //mic_button.setImageResource(R.drawable.ic_mic_black_24dp);
                speechRecognizer.startListening(speechRecognizerIntent);
            }
            return false;
        });

    }


    private boolean checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            // Permission is not granted
            return false;
        }

        return true;
    }

    private boolean checkAudioPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            // Permission is not granted
            return false;
        }

        return true;
    }

    private void requestCameraPermission() {

        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.CAMERA},
                CAMERA_PERMISSION_REQUEST_CODE);
    }

    private void requestAudioPermission() {
        //if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO},
                    AUDIO_PERMISSION_REQUEST_CODE);
        //}
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case CAMERA_PERMISSION_REQUEST_CODE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(getApplicationContext(), "Permission Granted", Toast.LENGTH_SHORT).show();

                    // main logic
                } else {
                    Toast.makeText(getApplicationContext(), "Permission Denied", Toast.LENGTH_SHORT).show();
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                                != PackageManager.PERMISSION_GRANTED) {
                            showMessageOKCancel("You need to allow access permissions",
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                                requestCameraPermission();
                                            }
                                        }
                                    });
                        }
                    }
                }
                break;

            case AUDIO_PERMISSION_REQUEST_CODE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(getApplicationContext(), "Permission Granted", Toast.LENGTH_SHORT).show();

                    // main logic
                } else {
                    Toast.makeText(getApplicationContext(), "Permission Denied", Toast.LENGTH_SHORT).show();
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                                != PackageManager.PERMISSION_GRANTED) {
                            showMessageOKCancel("You need to allow access permissions",
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                                requestAudioPermission();
                                            }
                                        }
                                    });
                        }
                    }
                }
                break;


        }
    }

    private void showMessageOKCancel(String message, DialogInterface.OnClickListener okListener) {
        new AlertDialog.Builder(MainActivity.this)
                .setMessage(message)
                .setPositiveButton("OK", okListener)
                .setNegativeButton("Cancel", null)
                .create()
                .show();
    }

    @Override
    public void onClick(View view) {

        switch (view.getId())
        {
            case R.id.bin_bt:
                capturePhotoBin();
                //tv.setText("Bidone della carta");
                break;

            case R.id.trash_bt:
                //tv.setText("Bottone premuto");
                capturePhotoTrash();
                break;
        }
    }

    private void capturePhotoBin() {
        tv.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_hourglass, 0,0, 0);
        tv.setText("Attendere...");
        imageCapt.takePicture(
                getExecutor(),
                new ImageCapture.OnImageCapturedCallback() {
                    @Override
                    public void onCaptureSuccess(ImageProxy image) {
                        //Create the picture's metadata

                        Bitmap bitmap = image.toBitmap();
                        tv.setText(bitmap.getHeight()+"x"+bitmap.getWidth());

                        int centreX = bitmap.getWidth()/2;
                        int centreY = bitmap.getHeight()/2;

                        int sumRed = 0, sumGreen = 0, sumBlue = 0;

                        for (int y = centreY-(DIM_WIN/2); y < centreY+(DIM_WIN/2); y++) {
                            for (int x = centreX-(DIM_WIN/2); x < centreX+(DIM_WIN/2); x++) {
                                int px = bitmap.getPixel(y, x);

                                // Get channel values from the pixel value.
                                int r = Color.red(px);
                                int g = Color.green(px);
                                int b = Color.blue(px);

                                sumRed = sumRed + r;
                                sumGreen = sumGreen + g;
                                sumBlue = sumBlue + b;
                            }
                        }

                        int averageRed = sumRed/(DIM_WIN*DIM_WIN);
                        int averageGreen = sumGreen/(DIM_WIN*DIM_WIN);
                        int averageBlue = sumBlue/(DIM_WIN*DIM_WIN);

                        //tv.setText("blue " + averageBlue + " green " + averageGreen + " red "+ averageRed);

                        image.close();

                        String closestColour = colorUtils.getColorNameFromRgb(averageRed,averageGreen,averageBlue);
                        tv.setCompoundDrawablesWithIntrinsicBounds(0, 0,0, 0);
                        tv.setText(closestColour);
                    }
                }
        );

    }

    private Executor getExecutor() {
        return ContextCompat.getMainExecutor(this);
    }

    private void startCamera(ProcessCameraProvider cameraProvider) {
        cameraProvider.unbindAll();
        CameraSelector camSelector = new CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_BACK).build();

        Preview preview = new Preview.Builder().build();
        preview.setSurfaceProvider(pview.getSurfaceProvider());

        imageCapt = new ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY).build();
        imageAn = new ImageAnalysis.Builder().setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build();
        imageAn.setAnalyzer(getExecutor(), this);

        cameraProvider.bindToLifecycle((LifecycleOwner)this, camSelector, preview, imageCapt, imageAn);
    }

    public void capturePhotoTrash() {
        tv.setText("Attendere...");
        tv.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_hourglass, 0,0, 0);

        imageCapt.takePicture(
                getExecutor(),
                new ImageCapture.OnImageCapturedCallback() {
                    @Override
                    public void onCaptureSuccess(ImageProxy image) {
                        //Create the picture's metadata
                        //tv.setText("in onCaptureSuccess");
                        //Timestamp per durata
                        //long startTime = System.currentTimeMillis();

                        try {

                            Bitmap bitmap = Bitmap.createScaledBitmap(image.toBitmap(), 512, 384, true);
                            tv.setText(bitmap.getHeight()+"x"+bitmap.getWidth());
                            ByteBuffer input = ByteBuffer.allocateDirect(512 * 384 * 3 * 4).order(ByteOrder.nativeOrder());
                            for (int y = 0; y < 512; y++) {
                                for (int x = 0; x < 384; x++) {
                                    int px = bitmap.getPixel(y, x);

                                    // Get channel values from the pixel value.
                                    int r = Color.red(px);
                                    int g = Color.green(px);
                                    int b = Color.blue(px);

                                    input.putFloat(r);
                                    input.putFloat(g);
                                    input.putFloat(b);
                                }
                            }

                            // Creates inputs for reference.
                            TensorBuffer inputFeature0 = TensorBuffer.createFixedSize(new int[]{1, 512, 384, 3}, DataType.FLOAT32);
                            inputFeature0.loadBuffer(input);

                            // Runs model inference and gets result.
                            //ModelliteMobilenet1207.Outputs outputs = modelliteMobilenet1207.process(inputFeature0);
                            // ModelliteMobilenetNonq1212.Outputs outputs = modelliteMobilenetNonq1212.process(inputFeature0);
                            Model1116.Outputs outputs = modelResNet1116.process(inputFeature0);


                            TensorBuffer outputFeature0 = outputs.getOutputFeature0AsTensorBuffer();

                            float max=-1;
                            int indexMax=-1;
                            float [] probabilities = outputFeature0.getFloatArray();
                            for( int i=0; i < probabilities.length; i++)
                            {
                                if(probabilities[i]>max)
                                {
                                    max=probabilities[i];
                                    indexMax=i;
                                }
                            }
                            soundPool.play(sound[indexMax], 1, 1,0,0,1);
                            tv.setCompoundDrawablesWithIntrinsicBounds(0, 0,0, 0);
                            tv.setText(labels[indexMax]);

                            //Long estimatedTime = System.currentTimeMillis() - startTime;
                           // tv.setText(estimatedTime.toString());
                        } catch (Exception e) {
                            tv.setText("E: "+e.getMessage().toString());
                        }

                        finally{
                            // Releases model resources if no longer used.
                            //modelliteMobilenet1207.close();
                            image.close();
                        }
                    }
                }
        );
    }

    @Override
    public void analyze(@NonNull ImageProxy image) {

    }
}