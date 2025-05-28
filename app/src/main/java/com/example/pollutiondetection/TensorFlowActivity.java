package com.example.pollutiondetection;

import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
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
    private TextView resultLabel;
    private Interpreter interpreter;
    private List<String> labels;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);
        resultImage = findViewById(R.id.resultImage);
        resultLabel = findViewById(R.id.resultLabel);

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

        // Tải mô hình TFLite từ assets
        try {
            MappedByteBuffer tfliteModel = loadModelFile("oil_model.tflite");
            MappedByteBuffer tfliteModel1 = loadModelFile("");
            interpreter = new Interpreter(tfliteModel);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        // Kiểm tra xem có ảnh hoặc video được truyền vào không
        String imageUriString = getIntent().getStringExtra("imageUri");
        String videoUriString = getIntent().getStringExtra("videoUri");

        if (imageUriString != null) {
            // Xử lý ảnh đầu vào
            Uri imageUri = Uri.parse(imageUriString);
            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageUri);
                processBitmap(bitmap);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else if (videoUriString != null) {
            // Xử lý video: lấy khung hình đầu tiên để phát hiện
            Uri videoUri = Uri.parse(videoUriString);
            MediaMetadataRetriever retriever = new MediaMetadataRetriever();
            boolean isProcessed = false;
            try {
                retriever.setDataSource(this, videoUri);
                Bitmap frame = retriever.getFrameAtTime(1000000); // lấy tại 1 giây
                if (frame == null) {
                    frame = retriever.getFrameAtTime(0); // fallback nếu không lấy được ở 1s
                }
                if (frame != null) {
                    processBitmap(frame); // hàm xử lý bitmap với model TFLite
                    isProcessed = true;
                }
            } catch (RuntimeException e) {
                e.printStackTrace();
                Toast.makeText(this, "Lỗi khi xử lý video", Toast.LENGTH_SHORT).show();
            } finally {
                try {
                    retriever.release();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (isProcessed) {
                Toast.makeText(this, "Video đã được xử lý", Toast.LENGTH_SHORT).show();
            }
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
        int outputDim = outputShape[1];
        int outputBox = outputShape[2];

        // Chạy suy luận
        ByteBuffer outputBuffer = ByteBuffer.allocateDirect(outputDim * outputBox * 4);
        outputBuffer.order(ByteOrder.nativeOrder());
        outputBuffer.rewind();
        Object[] inputArray = {inputBuffer};
        Map<Integer, Object> outputMap = new HashMap<>();
        outputMap.put(0, outputBuffer);
        interpreter.runForMultipleInputsOutputs(inputArray, outputMap);
        outputBuffer.rewind();

        // Đọc kết quả
        float[][][] outputs = new float[1][outputBox][outputDim];
        for (int i = 0; i < outputBox; i++) {
            for (int j = 0; j < outputDim; j++) {
                outputs[0][i][j] = outputBuffer.getFloat();
            }
        }

        // Giải mã các bounding box và nhãn
        List<DetectionResult> detections = new ArrayList<>();
        float confThreshold = 0.3f;
        for (int i = 0; i < outputBox; i++) {
            float confidence = outputs[0][i][4];
            if (confidence < confThreshold) continue;
            // Tìm nhãn có xác suất cao nhất
            int detectedClass = -1;
            float maxClassProb = 0;
            for (int c = 0; c < labels.size(); c++) {
                float classProb = outputs[0][i][5 + c];
                if (classProb > maxClassProb) {
                    maxClassProb = classProb;
                    detectedClass = c;
                }
            }
            float finalProb = maxClassProb * confidence;
            if (finalProb < confThreshold) continue;

            // Tọa độ đã được scale theo inputSize
            float xCenter = outputs[0][i][0] * inputSize;
            float yCenter = outputs[0][i][1] * inputSize;
            float width = outputs[0][i][2] * inputSize;
            float height = outputs[0][i][3] * inputSize;
            float left = Math.max(0, xCenter - width / 2);
            float top = Math.max(0, yCenter - height / 2);
            float right = Math.min(inputSize - 1, xCenter + width / 2);
            float bottom = Math.min(inputSize - 1, yCenter + height / 2);
            RectF rect = new RectF(left, top, right, bottom);
            detections.add(new DetectionResult(rect, labels.get(detectedClass), finalProb, detectedClass));
        }

        // Áp dụng Non-Max Suppression để loại khung thừa
        List<DetectionResult> finalDetections = Utils.nonMaxSuppress(detections, 0.5f);

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

        StringBuilder sb = new StringBuilder();
        for (DetectionResult det : finalDetections) {
            canvas.drawRect(det.getBoundingBox(), paint);
            float x = det.getBoundingBox().left;
            float y = det.getBoundingBox().top - 10;
            if (y < 0) y = det.getBoundingBox().top + 30;
            canvas.drawText(det.getLabel() + String.format(" (%.2f)", det.getConfidence()), x, y, textPaint);
            sb.append(det.getLabel()).append(String.format(" (%.2f) ", det.getConfidence()));
        }
        String resultText = sb.length() > 0 ? sb.toString() : "Không phát hiện đối tượng";
        resultLabel.setText(resultText);
        resultImage.setImageBitmap(outputBitmap);
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
