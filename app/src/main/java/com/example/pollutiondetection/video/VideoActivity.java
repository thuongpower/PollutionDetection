package com.example.pollutiondetection.video;

import android.content.Intent;
import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.view.PreviewView;

import com.example.pollutiondetection.R;
import com.example.pollutiondetection.TensorFlowActivity;

import java.io.IOException;

public class VideoActivity extends AppCompatActivity {
    private Button btnDetect;
    private PreviewView previewView;

    private Uri videoUri;
    private boolean isProcessing = false;
    private final int FPS = 5;
    private final Handler handler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);
        previewView = findViewById(R.id.previewResult);
        btnDetect = findViewById(R.id.btnDetect);
        videoUri = Uri.parse(getIntent().getStringExtra("videoUri"));
        Log.e("VideoActivity", "Video URI: " + videoUri);

        processVideo();
    }

    private void processVideo() {
        isProcessing = true;

        new Thread(() -> {
            MediaMetadataRetriever retriever = new MediaMetadataRetriever();
            retriever.setDataSource(this, videoUri);

            String durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
            long videoDurationMs = Long.parseLong(durationStr);
            long frameIntervalMs = 1000 / FPS;

            for (long timeMs = 0; timeMs < videoDurationMs && isProcessing; timeMs += frameIntervalMs) {
                Bitmap frame = retriever.getFrameAtTime(timeMs * 1000);
                if (frame == null) continue;

                Bitmap scaledFrame = Bitmap.createScaledBitmap(frame, 640, 640, false);

                runOnUiThread(() -> {
                    TensorFlowActivity.sharedBitmap = scaledFrame;
                    Intent intent = new Intent(VideoActivity.this, TensorFlowActivity.class);
                    startActivity(intent);
                });

                try {
                    Thread.sleep(frameIntervalMs);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            try {
                retriever.release();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }).start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        isProcessing = false;
    }
}
