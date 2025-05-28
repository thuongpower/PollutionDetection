package com.example.pollutiondetection;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.pollutiondetection.image.ImageActivity;
import com.example.pollutiondetection.video.VideoActivity;
import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;
import java.util.concurrent.ExecutionException;

public class MainActivity extends AppCompatActivity {
    private PreviewView previewView;
    private Button btnCapture, btnSelect;
    private ImageView imageResult;
    private TextView textLabel;
    private ImageCapture imageCapture;
    private ProcessCameraProvider cameraProvider;

    private static final int REQUEST_CAMERA_PERMISSION = 100;
    private static final int PICK_IMAGE = 101;
    private static final int PICK_VIDEO = 102;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        previewView = findViewById(R.id.previewView);
        btnCapture = findViewById(R.id.btnCapture);
        btnSelect = findViewById(R.id.btnSelect);
        imageResult = findViewById(R.id.imageResult);
        textLabel = findViewById(R.id.textLabel);

        // Kiểm tra quyền máy ảnh
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
        } else {
            startCamera();
        }

        btnCapture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                takePhoto();
            }
        });

        btnSelect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Hiển thị lựa chọn: Ảnh hoặc Video
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setTitle("Chọn")
                        .setItems(new String[]{"Ảnh", "Video"}, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int which) {
                                if (which == 0) {
                                    pickImage();
                                } else {
                                    pickVideo();
                                }
                            }
                        });
                builder.show();
            }
        });
    }

    // Khởi động camera với CameraX
    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                cameraProvider = cameraProviderFuture.get();
                bindPreviewAndImageCapture();
            } catch (ExecutionException | InterruptedException e) {
                // Lỗi
            }
        }, ContextCompat.getMainExecutor(this));
    }

    // Thiết lập Preview và ImageCapture
    private void bindPreviewAndImageCapture() {
        Preview preview = new Preview.Builder().build();
        preview.setSurfaceProvider(previewView.getSurfaceProvider());

        imageCapture = new ImageCapture.Builder().build();

        CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;
        cameraProvider.unbindAll();
        cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture);
    }

    // Chụp ảnh và lưu vào file, sau đó chuyển sang ImageActivity
    private void takePhoto() {
        if (imageCapture == null) return;
        File photoFile = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES),
                "temp.jpg");
        ImageCapture.OutputFileOptions outputOptions =
                new ImageCapture.OutputFileOptions.Builder(photoFile).build();

        imageCapture.takePicture(outputOptions,
                ContextCompat.getMainExecutor(this),
                new ImageCapture.OnImageSavedCallback() {
                    @Override
                    public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                        Uri savedUri = Uri.fromFile(photoFile);
                        Intent intent = new Intent(MainActivity.this, ImageActivity.class);
                        intent.putExtra("imageUri", savedUri.toString());
                        startActivity(intent);
                    }

                    @Override
                    public void onError(@NonNull ImageCaptureException exception) {
                        Log.e("CameraX", "Lỗi chụp ảnh: " + exception.getMessage());
                    }
                });
    }

    // Mở thư viện ảnh
    private void pickImage() {
        Intent intent = new Intent(Intent.ACTION_PICK,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, PICK_IMAGE);
    }

    // Mở thư viện video
    private void pickVideo() {
        Intent intent = new Intent(Intent.ACTION_PICK,
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, PICK_VIDEO);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK && data != null) {
            Uri selectedUri = data.getData();
            if (selectedUri == null) return;
            if (requestCode == PICK_IMAGE) {
                Intent intent = new Intent(MainActivity.this, ImageActivity.class);
                intent.putExtra("imageUri", selectedUri.toString());
                startActivity(intent);
            } else if (requestCode == PICK_VIDEO) {
                Intent intent = new Intent(MainActivity.this, VideoActivity.class);
                intent.putExtra("videoUri", selectedUri.toString());
                startActivity(intent);
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    // Kết quả yêu cầu quyền
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCamera();
            } else {
                finish(); // Không cấp quyền thì thoát
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
}

