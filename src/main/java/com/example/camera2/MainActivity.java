package com.example.camera2;

import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Base64;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.common.util.concurrent.ListenableFuture;

import android.Manifest;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.ExecutionException;

public class MainActivity extends AppCompatActivity {
    private final int CAMERA_REQUEST_CODE = 100;

    SharedPreferences savedData;
    SharedPreferences.Editor editor;
    PreviewView cameraView;
    ImageView savedImageView;
    FloatingActionButton captureButton;
    ImageCapture image;

    private ListenableFuture<ProcessCameraProvider> cameraFutureProvider;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        captureButton = findViewById(R.id.captureButton);

        savedData = getSharedPreferences("data", MODE_PRIVATE);
        editor = savedData.edit();

        if (checkGrantedPermissions()) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_REQUEST_CODE);
        } else {
            startCamera();
        }

        captureButton.setOnClickListener(v -> {
            takePhoto();
        });

    } // onCreate ends here =======================

    private boolean checkGrantedPermissions(){
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED){
            return false;
        }else{
            return true;
        }
    }
    private void takePhoto(){
        File temp = new File(getCacheDir(), "temp.jpg");
        ImageCapture.OutputFileOptions outputFileOptions = new ImageCapture.OutputFileOptions.Builder(temp).build();
        image.takePicture(outputFileOptions, ContextCompat.getMainExecutor(this), new ImageCapture.OnImageSavedCallback() {
            @Override
            public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                Bitmap bitmap = BitmapFactory.decodeFile(temp.getAbsolutePath());
                saveImage(bitmap);
            }

            @Override
            public void onError(@NonNull ImageCaptureException exception) {
                exception.printStackTrace();
            }
        });

    }

    private void saveImage(Bitmap bitmap){
        if(image == null) return;

        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.DISPLAY_NAME, "IMG_" + System.currentTimeMillis() + ".jpg");
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
        values.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/Camera2");

        Uri imageUri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

        try{
            OutputStream outputStream = getContentResolver().openOutputStream(imageUri);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
            outputStream.close();
            Toast.makeText(this, "Saved to Camera2", Toast.LENGTH_SHORT).show();
        }catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Saving failed", Toast.LENGTH_SHORT).show();
        }
    }
    private File getOutputDirectory(){
        File mediaDir = new File(getExternalFilesDir(null), "Camera2Photos");
        if(!mediaDir.exists()){
            mediaDir.mkdirs();
        }
        return mediaDir;
    }

    private void startCamera(){
        cameraFutureProvider = ProcessCameraProvider.getInstance(this);
        cameraFutureProvider.addListener(() -> {
            try{
                ProcessCameraProvider cameraProvider = cameraFutureProvider.get();
                cameraView = findViewById(R.id.cameraImageView);
                Preview preview = new Preview.Builder().build();
                CameraSelector selector = CameraSelector.DEFAULT_BACK_CAMERA;
                ImageCapture imageCapture = new ImageCapture.Builder().build();
                preview.setSurfaceProvider(cameraView.getSurfaceProvider());
                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(this, selector, preview, imageCapture);

                this.image = imageCapture;

            }catch (ExecutionException | InterruptedException e){
                e.printStackTrace();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode == RESULT_OK && requestCode == CAMERA_REQUEST_CODE){
            Bitmap bitmap = (Bitmap) data.getExtras().get("data");
            savedImageView.setImageBitmap(bitmap);
        }

    } // onActivityResult end here ================

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {

        if (requestCode == CAMERA_REQUEST_CODE) {
            if (checkGrantedPermissions()) {
                startCamera();
            } else {
                Toast.makeText(this, "Permissions not granted", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

} // class MainActivity ends here =================