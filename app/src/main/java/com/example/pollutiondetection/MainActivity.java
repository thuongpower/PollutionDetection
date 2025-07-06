package com.example.pollutiondetection;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.*;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.*;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.*;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class MainActivity extends AppCompatActivity {

    private PreviewView previewView;
    private Button btnCapture, btnSelect;
    private ImageView imageViewResult;
    private TextView textResult;
    private ImageCapture imageCapture;
    private PollutionClassifier classifier;
    private static final int REQUEST_CAMERA_PERMISSION = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        previewView = findViewById(R.id.previewView);
        btnCapture = findViewById(R.id.btnCapture);
        btnSelect = findViewById(R.id.btnSelect);
        imageViewResult = findViewById(R.id.imageViewResult);
        textResult = findViewById(R.id.textResult);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
        } else {
            startCamera();
        }

        try {
            classifier = new PollutionClassifier(getAssets(), "detection_model.tflite", "labels.txt", 640, false);
        } catch (IOException e) {
            e.printStackTrace();
        }

        btnCapture.setOnClickListener(v -> capturePhoto());

        btnSelect.setOnClickListener(v -> showMediaPicker());
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> future = ProcessCameraProvider.getInstance(this);
        future.addListener(() -> {
            try {
                ProcessCameraProvider provider = future.get();
                Preview preview = new Preview.Builder().build();
                imageCapture = new ImageCapture.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());
                provider.unbindAll();
                provider.bindToLifecycle(this, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageCapture);
            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void capturePhoto() {
        imageCapture.takePicture(ContextCompat.getMainExecutor(this), new ImageCapture.OnImageCapturedCallback() {
            @Override
            public void onCaptureSuccess(@NonNull ImageProxy image) {
                Bitmap bitmap = ImageUtils.imageProxyToBitmap(image);
                image.close();
                processImage(bitmap);
            }
        });
    }

    private void processImage(Bitmap bitmap) {
        List<DetectionResult> results = classifier.detect(bitmap);
        // Vẽ bounding box và nhãn lên ảnh
        Bitmap mutableBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);
        Canvas canvas = new Canvas(mutableBitmap);
        Paint paint = new Paint();
        paint.setColor(Color.RED);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(3);

        Paint textPaint = new Paint();
        textPaint.setColor(Color.YELLOW);
        textPaint.setTextSize(40);

        StringBuilder resultText = new StringBuilder();
        for (DetectionResult result : results) {
            canvas.drawRect(result.getBoundingBox(), paint);
            canvas.drawText(result.getLabel() + String.format(" (%.2f)", result.getConfidence()),
                    result.getBoundingBox().left,
                    result.getBoundingBox().top - 10,
                    textPaint);
            resultText.append(result.getLabel())
                    .append(String.format(" (%.2f)\n", result.getConfidence()));
        }

        runOnUiThread(() -> {
            imageViewResult.setVisibility(View.VISIBLE);
            textResult.setVisibility(View.VISIBLE);
            imageViewResult.setImageBitmap(mutableBitmap);
            textResult.setText(resultText.toString());
        });
    }

    private void showMediaPicker() {
        String[] options = {"Chọn Ảnh", "Chọn Video"};
        new AlertDialog.Builder(this)
                .setTitle("Chọn phương tiện")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) pickImageLauncher.launch("image/*");
                    else pickVideoLauncher.launch("video/*");
                }).show();
    }

    private final ActivityResultLauncher<String> pickImageLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) handleImageUri(uri);
            });

    private final ActivityResultLauncher<String> pickVideoLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) handleVideoUri(uri);
            });

    private void handleImageUri(Uri uri) {
        try {
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), uri);
            processImage(bitmap);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleVideoUri(Uri uri) {
        VideoProcessingHelper.processVideo(this, uri, frame -> runOnUiThread(() -> processImage(frame)));
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_CAMERA_PERMISSION && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else {
            finish();
        }
    }
}
