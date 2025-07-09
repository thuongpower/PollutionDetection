package com.example.pollutiondetection;

import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.tensorflow.lite.Interpreter;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

public class TensorFlowActivity extends AppCompatActivity {
    private ImageView resultImage;
    private TextView resultText, textAlgae, textTrash, textOil;
    private Interpreter interpreter;
    private List<String> labels;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);
        resultImage = findViewById(R.id.resultImage);
        resultText = findViewById(R.id.resultText);
        textAlgae = findViewById(R.id.textAlgae);
        textTrash = findViewById(R.id.textTrash);
        textOil = findViewById(R.id.textOil);

        // Đọc nhãn từ assets (labels.txt)
        labels = new ArrayList<>();
        try {
            InputStream is = getAssets().open("labels.txt");
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            String line;
            while ((line = reader.readLine()) != null) {
                labels.add(line);
            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        DetectionResult.setLabelList(labels);
        // Tải mô hình TFLite từ assets
        try {
            MappedByteBuffer tfliteModel = loadModelFile("detection.tflite");
            interpreter = new Interpreter(tfliteModel);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        // Nhận dữ liệu từ ImageActivity hoặc VideoActivity
        byte[] byteArray = getIntent().getByteArrayExtra("frameData");
        if (byteArray != null) {
            Bitmap bitmap = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.length);
            processBitmap(bitmap);
        }else {
            Toast.makeText(this, "Không có dữ liệu ảnh", Toast.LENGTH_SHORT).show();
            finish();
        }

    }

    // Chuyển bitmap đầu vào qua mô hình và hiển thị kết quả
    private void processBitmap(Bitmap bitmap) {
        int inputSize = 640;
        // Resize ảnh về kích thước 640x640 (theo chuẩn đầu vào YOLOv8)
        Bitmap resizedBitmap = Bitmap.createScaledBitmap(bitmap, inputSize, inputSize, false);

        // Tạo ByteBuffer cho mô hình (batch=1, 3 kênh RGB, float32)
        ByteBuffer inputBuffer = ByteBuffer.allocateDirect(1 * inputSize * inputSize * 3 * 4);
        inputBuffer.order(ByteOrder.nativeOrder());
        inputBuffer.rewind();
        for (int y = 0; y < inputSize; y++) {
            for (int x = 0; x < inputSize; x++) {
                int pixel = resizedBitmap.getPixel(x, y);
                float r = ((pixel >> 16) & 0xFF) / 255.0f;
                float g = ((pixel >> 8) & 0xFF) / 255.0f;
                float b = (pixel & 0xFF) / 255.0f;
                inputBuffer.putFloat(r);
                inputBuffer.putFloat(g);
                inputBuffer.putFloat(b);
            }
        }

        // Lấy kích thước đầu ra của mô hình: [1, classes+5, num_predictions]
        int[] outputShape = interpreter.getOutputTensor(0).shape();
        int outputBox = outputShape[2];
        int outputDim = outputShape[1];


        // Chạy suy luận
        ByteBuffer outputBuffer = ByteBuffer.allocateDirect(outputBox * outputDim * 4);
        outputBuffer.order(ByteOrder.nativeOrder());
        outputBuffer.rewind();

        Object[] inputArray = {inputBuffer};
        Map<Integer, Object> outputMap = new HashMap<>();
        outputMap.put(0, outputBuffer);

        interpreter.runForMultipleInputsOutputs(inputArray, outputMap);
        outputBuffer.rewind();

        // Đọc kết quả từ outputBuffer
        float[] outputArray = new float[outputBox * outputDim];
        outputBuffer.asFloatBuffer().get(outputArray);

        float[][][] outputs = new float[1][outputDim][outputBox];
        for (int i = 0; i < outputDim; i++) {
            for (int j = 0; j < outputBox; j++) {
                outputs[0][i][j] = outputArray[i * outputBox + j];
            }
        }

        // Giải mã các bounding box và nhãn
        List<DetectionResult> detections = new ArrayList<>();
        float confThreshold = 0.25f;
        for (int i = 0; i < outputBox; i++) {
            float confidence = outputs[0][4][i]; // objectness

            if (confidence < confThreshold) continue;

            // Tìm nhãn có xác suất cao nhất
            int detectedClass = -1;
            float maxClassProb = 0;
            for (int c = 0; c < labels.size(); c++) {
                float classProb = outputs[0][5 + c][i];
                if (classProb > maxClassProb) {
                    maxClassProb = classProb;
                    detectedClass = c;
                }
            }

            float finalProb = maxClassProb * confidence;
            if (finalProb < confThreshold) continue;

            float xCenter = outputs[0][0][i] * inputSize;
            float yCenter = outputs[0][1][i] * inputSize;
            float width = outputs[0][2][i] * inputSize;
            float height = outputs[0][3][i] * inputSize;
            float left = Math.max(0, xCenter - width / 2);
            float top = Math.max(0, yCenter - height / 2);
            float right = Math.min(inputSize - 1, xCenter + width / 2);
            float bottom = Math.min(inputSize - 1, yCenter + height / 2);
            RectF rect = new RectF(left, top, right, bottom);
            detections.add(new DetectionResult(rect, finalProb, detectedClass));
        }

        // Áp dụng Non-Max Suppression để loại khung thừa
        List<DetectionResult> finalDetections = Utils.nonMaxSuppress(detections, 0.7f);
        int maxDet = 300; // từ max_det
        if (finalDetections.size() > maxDet) {
            finalDetections = finalDetections.subList(0, maxDet);
        }
        // Paint bounding box and labels on the resized bitmap
        Bitmap outputBitmap = resizedBitmap.copy(Bitmap.Config.ARGB_8888, true);
        Canvas canvas = new Canvas(outputBitmap);
        Paint paint = new Paint();
        paint.setColor(Color.RED);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(2.0f);
        Paint textPaint = new Paint();
        textPaint.setColor(Color.RED);
        textPaint.setTextSize(30);

        int algaeCount = 0, trashCount = 0, oilCount = 0;
        StringBuilder sb = new StringBuilder();
        for (DetectionResult det : finalDetections) {
            canvas.drawRect(det.getBoundingBox(), paint);
            float x = det.getBoundingBox().left;
            float y = det.getBoundingBox().top - 10;
            if (y < 0) y = det.getBoundingBox().top + 30;
            canvas.drawText(det.getLabel() + String.format(" (%.2f)", det.getConfidence()), x, y, textPaint);
            sb.append(det.getLabel()).append(String.format(" (%.2f) ", det.getConfidence()));

            String label = det.getLabel().toLowerCase(); 
            if (label.equals("song")) {
                algaeCount++;
            } else if (label.equals("rac")) {
                trashCount++;
            } else if (label.equals("vang dau")) {
                oilCount++;
            }
        }

        // Hiển thị kết quả
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        outputBitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
        byte[] resultBytes = stream.toByteArray();

        resultText.setText(sb.length() > 0 ? sb.toString().trim() : "Không phát hiện đối tượng");
        resultImage.setImageBitmap(outputBitmap);
        textAlgae.setText("Sóng: " + algaeCount);
        textTrash.setText("Rác: " + trashCount);
        textOil.setText("Váng dầu: " + oilCount);

        Toast.makeText(this, "Đã hoàn thành nhận diện", Toast.LENGTH_SHORT).show();
        Toast.makeText(this, String.format("Sóng: %d, Rác: %d, Váng dầu: %d", algaeCount, trashCount, oilCount), Toast.LENGTH_SHORT).show();
        Intent resultIntent = new Intent();
        resultIntent.putExtra("resultImage", resultBytes);
        resultIntent.putExtra("countAlgae", algaeCount);
        resultIntent.putExtra("countTrash", trashCount);
        resultIntent.putExtra("countOil", oilCount);
        setResult(RESULT_OK, resultIntent);

        finish();
    }

    // Get model TFLite from assets
    private MappedByteBuffer loadModelFile(String modelFileName) throws IOException {
        AssetFileDescriptor fileDescriptor = getAssets().openFd(modelFileName);
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }
}
