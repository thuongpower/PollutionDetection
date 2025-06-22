package com.example.pollutiondetection.image;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.pollutiondetection.R;
import com.example.pollutiondetection.TorchScriptActivity;

import java.io.IOException;

public class ImageActivity extends AppCompatActivity {
    private ImageView imageView;
    private Button btnDetect;
    private String imageUriString;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image);
        imageView = findViewById(R.id.imageView);
        btnDetect = findViewById(R.id.btnDetect);

        // Lấy URI ảnh từ Intent
        imageUriString = getIntent().getStringExtra("imageUri");
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

        // Khi nhấn nút "Nhận diện", mở TorchScriptActivity
        btnDetect.setOnClickListener(v -> {
            Intent intent = new Intent(ImageActivity.this, TorchScriptActivity.class);
            intent.putExtra("imageUri", imageUriString);
            startActivity(intent);
        });
    }
}


