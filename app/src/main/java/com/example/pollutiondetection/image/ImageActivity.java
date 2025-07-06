package com.example.pollutiondetection.image;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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

import java.io.InputStream;
import java.util.List;

public class ImageActivity extends AppCompatActivity {

    private ImageView resultImage;
    private TextView resultLabel, textAlgae, textTrash, textOil;
    private Classifier classifier;
    private Button btnDetect;
    private Bitmap imageBitmap;

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

        Uri imageUri = Uri.parse(getIntent().getStringExtra("imageUri"));
        if (imageUri != null) loadImage(imageUri);

        btnDetect.setOnClickListener(view -> processImage());
    }

    private void loadImage(Uri uri) {
        try (InputStream inputStream = getContentResolver().openInputStream(uri)) {
            imageBitmap = BitmapFactory.decodeStream(inputStream);
            if (imageBitmap != null) {
                resultImage.setImageBitmap(imageBitmap);
                Toast.makeText(this, "Đã tải ảnh thành công", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Không thể đọc ảnh", Toast.LENGTH_SHORT).show();
            Log.e("ImageActivity", "Lỗi đọc ảnh: " + e.getMessage());
        }
    }

    private void processImage() {
        if (imageBitmap == null) {
            Toast.makeText(this, "Ảnh không hợp lệ", Toast.LENGTH_SHORT).show();
            return;
        }

        if (classifier == null) {
            Toast.makeText(this, "Mô hình chưa sẵn sàng", Toast.LENGTH_SHORT).show();
            return;
        }

        List<Classifier.DetectionResult> results = classifier.recognizeImage(imageBitmap);

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

    }

    private void loadModel() {
        try {
            classifier = new TensorFlowActivity();
            ((TensorFlowActivity) classifier).initialize(getAssets(), "pollution_detection.tflite", "labels.txt", 640);
            Log.d("ImageActivity", "Tải mô hình thành công");
        } catch (Exception e) {
            Log.e("ImageActivity", "Lỗi tải mô hình: " + e.getMessage());
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
