package com.example.helloword;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.core.resolutionselector.ResolutionSelector;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AlertDialog;
import androidx.lifecycle.LifecycleOwner;

import android.Manifest;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.media.Image;
import android.util.Size;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.helloword.ml.Model1116;
import com.google.common.util.concurrent.ListenableFuture;

import org.tensorflow.lite.DataType;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.Executor;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, ImageAnalysis.Analyzer {

    private ListenableFuture<ProcessCameraProvider> provider;
    private static final int PERMISSION_REQUEST_CODE = 200;
    private Button picture_bt, analysis_bt;
    private PreviewView pview;
    private TextView tv;
    private ImageCapture imageCapt;
    private ImageAnalysis imageAn;
    private boolean analysis_on;
    private int our_width = 512;
    private int our_height = 384;
    private String [] labels = {"Cartone", "Vetro", "Metallo", "Carta", "Plastica", "Indifferenziato"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (! checkPermission())
            requestPermission();

        picture_bt = findViewById(R.id.bin_bt);
        analysis_bt = findViewById(R.id.trash_bt);
        tv = findViewById(R.id.textView);
        pview = findViewById(R.id.previewView);

        picture_bt.setOnClickListener(this);
        analysis_bt.setOnClickListener(this);
        this.analysis_on = false;


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
    }

    private boolean checkPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            // Permission is not granted
            return false;
        }
        return true;
    }

    private void requestPermission() {

        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.CAMERA},
                PERMISSION_REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case PERMISSION_REQUEST_CODE:
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
                                                requestPermission();
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
                //capturePhoto();
                tv.setText("Bidone della carta");
                break;

            case R.id.trash_bt:
                tv.setText("Bottone premuto");
                capturePhotoTrash();
                break;
        }
    }

    @Override
    public void analyze(@NonNull ImageProxy image) {

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
        tv.setText("in capturePhotoTrash");
        imageCapt.takePicture(
                getExecutor(),
                new ImageCapture.OnImageCapturedCallback() {
                    @Override
                    public void onCaptureSuccess(ImageProxy image) {
                        //Create the picture's metadata
                        tv.setText("in onCaptureSuccess");

                        try {
                            Model1116 model = Model1116.newInstance(getApplicationContext());

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
                            Model1116.Outputs outputs = model.process(inputFeature0);
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
                            tv.setText(labels[indexMax]);



                            // Releases model resources if no longer used.
                            model.close();
                            image.close();
                        } catch (Exception e) {
                            // TODO Handle the exception
                        }
                    }
                }
        );
    }
}