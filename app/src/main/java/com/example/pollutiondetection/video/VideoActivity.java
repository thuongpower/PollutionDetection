package com.example.pollutiondetection.video;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.pollutiondetection.R;
import com.example.pollutiondetection.TensorFlowActivity;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class VideoActivity extends AppCompatActivity {

    private static final int REQUEST_DETECT = 201;
    private ImageView imageView;
    private Button btnDetect;
    private TextView textAlgae, textTrash, textOil;
    private Uri videoUri;
    private MediaMetadataRetriever retriever;
    private long videoDurationUs;
    private long currentTimeUs = 0;
    private final long frameIntervalUs = 2 * 1_000_000; // 2s

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);

        imageView = findViewById(R.id.resultImage);
        btnDetect = findViewById(R.id.btnDetect);
        textAlgae = findViewById(R.id.textAlgae);
        textTrash = findViewById(R.id.textTrash);
        textOil = findViewById(R.id.textOil);

        String videoUriString = getIntent().getStringExtra("videoUri");
        if (videoUriString != null) {
            videoUri = Uri.parse(videoUriString);
            prepareVideo();
        }

        btnDetect.setOnClickListener(v -> extractNextFrame());
    }

    private void prepareVideo() {
        try {
            retriever = new MediaMetadataRetriever();
            retriever.setDataSource(this, videoUri);
            String durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
            videoDurationUs = Long.parseLong(durationStr) * 1000; // ms to us
            currentTimeUs = 0;
            Toast.makeText(this, "Sẵn sàng tách frame", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "Lỗi đọc video", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void extractNextFrame() {
        if (retriever == null || currentTimeUs >= videoDurationUs) {
            Toast.makeText(this, "Đã hết video", Toast.LENGTH_SHORT).show();
            return;
        }

        Bitmap frame = retriever.getFrameAtTime(currentTimeUs, MediaMetadataRetriever.OPTION_CLOSEST);
        if (frame == null) {
            Toast.makeText(this, "Không lấy được frame", Toast.LENGTH_SHORT).show();
            currentTimeUs += frameIntervalUs;
            return;
        }

        imageView.setImageBitmap(frame);

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        frame.compress(Bitmap.CompressFormat.JPEG, 85, stream);
        byte[] byteArray = stream.toByteArray();

        Intent intent = new Intent(VideoActivity.this, TensorFlowActivity.class);
        intent.putExtra("frameData", byteArray);
        startActivityForResult(intent, REQUEST_DETECT);

        currentTimeUs += frameIntervalUs;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_DETECT && resultCode == RESULT_OK && data != null) {
            byte[] resultBytes = data.getByteArrayExtra("resultImage");
            int algae = data.getIntExtra("countAlgae", 0);
            int trash = data.getIntExtra("countTrash", 0);
            int oil = data.getIntExtra("countOil", 0);

            if (resultBytes != null) {
                Bitmap resultBmp = BitmapFactory.decodeByteArray(resultBytes, 0, resultBytes.length);
                imageView.setImageBitmap(resultBmp);
            }
            textAlgae.setText("Sóng: " + algae);
            textTrash.setText("Rác: " + trash);
            textOil.setText("Váng dầu: " + oil);
            Toast.makeText(this, String.format("Sóng:%d Rác:%d Váng dầu:%d", algae, trash, oil), Toast.LENGTH_SHORT).show();
            handler.postDelayed(frameRunnable, 2000);
        }
    }
    private final android.os.Handler handler = new android.os.Handler();
    private final Runnable frameRunnable = new Runnable() {
        @Override
        public void run() {
            extractNextFrame(); // tách và xử lý frame
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (retriever != null) {
            try {
                retriever.release();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
