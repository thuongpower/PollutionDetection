package com.example.pollutiondetection.video;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.VideoView;

import androidx.appcompat.app.AppCompatActivity;
import com.example.pollutiondetection.R;
import com.example.pollutiondetection.TensorFlowActivity;

public class VideoActivity extends AppCompatActivity {
    private VideoView videoView;
    private Button btnDetect;
    private String videoUriString;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video);
        videoView = findViewById(R.id.videoView);
        btnDetect = findViewById(R.id.btnDetect);

        // Lấy URI video từ Intent
        videoUriString = getIntent().getStringExtra("videoUri");
        if (videoUriString != null) {
            Uri videoUri = Uri.parse(videoUriString);
            videoView.setVideoURI(videoUri);
            videoView.start();
        }

        btnDetect.setOnClickListener(v -> {
            Intent intent = new Intent(VideoActivity.this, TensorFlowActivity.class);
            intent.putExtra("videoUri", videoUriString);
            startActivity(intent);
        });
    }
}

