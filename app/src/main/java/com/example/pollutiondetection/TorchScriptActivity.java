package com.example.pollutiondetection;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import org.pytorch.IValue;
import org.pytorch.Module;
import org.pytorch.Tensor;
import org.pytorch.torchvision.TensorImageUtils;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TorchScriptActivity extends AppCompatActivity {
    private ImageView resultImage;
    private TextView resultLabel;
    private Module module;
    private List<String> labels;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);

        resultImage = findViewById(R.id.resultImage);
        resultLabel = findViewById(R.id.resultLabel);

        // Load labels từ labels.txt trong assets
        labels = new ArrayList<>();
        try {
            InputStream is = getAssets().open("labels.txt");
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            String line;
            while ((line = reader.readLine()) != null) {
                labels.add(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Load model .pt từ assets
        try {
            module = Module.load(assetFilePath("oil_model.torchscript.pt"));
        } catch (IOException e) {
            Log.e("TorchScript", "Lỗi khi load mô hình!", e);
        }

        // Nhận ảnh hoặc video từ Intent
        String imageUriString = getIntent().getStringExtra("imageUri");
        String videoUriString = getIntent().getStringExtra("videoUri");

        if (imageUriString != null) {
            try {
                Uri imageUri = Uri.parse(imageUriString);
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageUri);
                runModel(bitmap);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else if (videoUriString != null) {
            try {
                Uri videoUri = Uri.parse(videoUriString);
                MediaMetadataRetriever retriever = new MediaMetadataRetriever();
                retriever.setDataSource(this, videoUri);
                Bitmap frame = retriever.getFrameAtTime(1000000); // lấy frame tại 1 giây
                if (frame == null) frame = retriever.getFrameAtTime(0);
                if (frame != null) runModel(frame);
                retriever.release();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void runModel(Bitmap originalBitmap) {
        int inputSize = 640;

        // Resize ảnh về đúng kích thước đầu vào của model
        Bitmap resized = Bitmap.createScaledBitmap(originalBitmap, inputSize, inputSize, false);

        // Tính tỉ lệ scale để khôi phục kích thước về ảnh gốc
        float scaleX = (float) originalBitmap.getWidth() / inputSize;
        float scaleY = (float) originalBitmap.getHeight() / inputSize;

        // Tạo Tensor đầu vào từ ảnh
        Tensor inputTensor = TensorImageUtils.bitmapToFloat32Tensor(
                resized,
                TensorImageUtils.TORCHVISION_NORM_MEAN_RGB,
                TensorImageUtils.TORCHVISION_NORM_STD_RGB
        );

        // Thực hiện suy luận
        IValue output = module.forward(IValue.from(inputTensor));
        Tensor outputTensor = output.toTensor();
        float[] outputs = outputTensor.getDataAsFloatArray();

        // Phân tích output thành danh sách các đối tượng
        List<DetectionResult> results = Utils.parseYOLOOutput(outputs, labels, inputSize);

        // Scale bounding box về ảnh gốc
        for (DetectionResult result : results) {
            RectF box = result.getBoundingBox();
            box.left *= scaleX;
            box.right *= scaleX;
            box.top *= scaleY;
            box.bottom *= scaleY;
        }

        // Tạo bản sao để vẽ
        Bitmap outputBitmap = originalBitmap.copy(Bitmap.Config.ARGB_8888, true);
        Canvas canvas = new Canvas(outputBitmap);
        Paint boxPaint = new Paint();
        boxPaint.setColor(Color.RED);
        boxPaint.setStyle(Paint.Style.STROKE);
        boxPaint.setStrokeWidth(2);

        Paint textPaint = new Paint();
        textPaint.setColor(Color.RED);
        textPaint.setTextSize(30);

        // Đếm số lượng theo nhãn
        Map<String, Integer> labelCounts = new HashMap<>();
        for (DetectionResult result : results) {
            RectF rect = result.getBoundingBox();
            canvas.drawRect(rect, boxPaint);
            canvas.drawText(result.getLabel() + String.format(" (%.2f)", result.getConfidence()), rect.left, rect.top - 10, textPaint);

            String label = result.getLabel();
            labelCounts.put(label, labelCounts.getOrDefault(label, 0) + 1);
        }

        // Hiển thị kết quả
        if (labelCounts.isEmpty()) {
            resultLabel.setText("Không phát hiện đối tượng");
        } else {
            StringBuilder sb = new StringBuilder("Kết quả: ");
            for (Map.Entry<String, Integer> entry : labelCounts.entrySet()) {
                sb.append(entry.getKey()).append(": ").append(entry.getValue()).append("  ");
            }
            resultLabel.setText(sb.toString().trim());
        }

        resultImage.setImageBitmap(outputBitmap);
    }

    // Copy model từ assets vào file hệ thống để load được
    private String assetFilePath(String assetName) throws IOException {
        java.io.File file = new java.io.File(getFilesDir(), assetName);
        if (file.exists() && file.length() > 0) return file.getAbsolutePath();

        try (InputStream is = getAssets().open(assetName)) {
            FileOutputStream os = new FileOutputStream(file);
            byte[] buffer = new byte[4 * 1024];
            int read;
            while ((read = is.read(buffer)) != -1) os.write(buffer, 0, read);
            os.flush();
        }
        return file.getAbsolutePath();
    }
}
