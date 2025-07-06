package com.example.pollutiondetection.video;

import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.pollutiondetection.Classifier;
import com.example.pollutiondetection.R;
import com.example.pollutiondetection.TensorFlowActivity;

import java.util.List;

public class VideoActivity extends AppCompatActivity {

    private ImageView resultImage;
    private TextView resultLabel, textAlgae, textTrash, textOil;
    private Button btnDetect;
    private Classifier classifier;
    private Uri videoUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);

        resultImage = findViewById(R.id.resultImage);
        resultLabel = findViewById(R.id.resultLabel);
        textAlgae = findViewById(R.id.textAlgae);
        textTrash = findViewById(R.id.textTrash);
        textOil = findViewById(R.id.textOil);
        btnDetect = findViewById(R.id.btnDetect);

        loadModel();

        videoUri = Uri.parse(getIntent().getStringExtra("videoUri"));

        btnDetect.setOnClickListener(view -> processVideoFrame());
    }

    private void processVideoFrame() {
        if (classifier == null) {
            Toast.makeText(this, "Mô hình chưa sẵn sàng", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            MediaMetadataRetriever retriever = new MediaMetadataRetriever();
            retriever.setDataSource(this, videoUri);
            Bitmap frame = retriever.getFrameAtTime(1000000);
            retriever.release();

            if (frame == null) {
                Toast.makeText(this, "Không thể lấy khung hình", Toast.LENGTH_SHORT).show();
                return;
            }

            resultImage.setImageBitmap(frame);

            List<Classifier.DetectionResult> results = classifier.recognizeImage(frame);

            if (results == null || results.isEmpty()) {
                Toast.makeText(this, "Không phát hiện đối tượng nào", Toast.LENGTH_SHORT).show();
                return;
            }

            int algaeCount = 0, trashCount = 0, oilCount = 0;
            StringBuilder sb = new StringBuilder();

            for (Classifier.DetectionResult r : results) {
                sb.append(r.toString()).append("\n");
                int classIndex = r.getClassIndex();
                if (classIndex == 0) algaeCount++;
                else if (classIndex == 1) trashCount++;
                else if (classIndex == 2) oilCount++;
            }

            resultLabel.setText(sb.toString().trim());
            textAlgae.setText("Sóng: " + algaeCount);
            textTrash.setText("Rác: " + trashCount);
            textOil.setText("Váng dầu: " + oilCount);

            Toast.makeText(this, "Đã hoàn thành nhận diện", Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            Toast.makeText(this, "Lỗi xử lý video", Toast.LENGTH_SHORT).show();
            Log.e("VideoActivity", "Lỗi video: " + e.getMessage());
        }
    }

    private void loadModel() {
        try {
            classifier = new TensorFlowActivity();
            ((TensorFlowActivity) classifier).initialize(getAssets(), "pollution_detection.tflite", "labels.txt", 640);
            Log.d("VideoActivity", "Tải mô hình thành công");
        } catch (Exception e) {
            Log.e("VideoActivity", "Lỗi tải mô hình: " + e.getMessage());
            Toast.makeText(this, "Lỗi tải mô hình", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (classifier != null) classifier.close();
    }
}
