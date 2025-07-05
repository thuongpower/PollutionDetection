package com.example.pollutiondetection.image;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Preview;
import androidx.camera.view.PreviewView;

import com.example.pollutiondetection.R;
import com.example.pollutiondetection.TensorFlowActivity;

import java.io.IOException;

public class ImageActivity extends AppCompatActivity {
    private ImageView imageView;
    private Button btnDetect;
    private String imageUriString;
    private PreviewView previewView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);
        previewView = findViewById(R.id.previewResult);
        btnDetect = findViewById(R.id.btnDetect);

        // Lấy URI ảnh từ Intent
        imageUriString = getIntent().getStringExtra("imageUri");
        Log.e("ImageActivity", "Image URI: " + imageUriString);
        if (imageUriString != null) {
            Uri imageUri = Uri.parse(imageUriString);
            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(
                        this.getContentResolver(), imageUri);
                imageView.setImageBitmap(bitmap);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // Khi nhấn nút "Nhận diện", mở TensorFlowActivity
        btnDetect.setOnClickListener(v -> {
            Intent intent = new Intent(ImageActivity.this, TensorFlowActivity.class);
            intent.putExtra("imageUri", imageUriString);
            startActivity(intent);
        });
    }
}


