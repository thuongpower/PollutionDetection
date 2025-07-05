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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TensorFlowActivity extends AppCompatActivity {

    private ImageView resultImage; // ImageView hiển thị kết quả
    private TextView resultLabel;  // TextView hiển thị nhãn phát hiện
    private Interpreter interpreter; // Đối tượng TFLite Interpreter
    private List<String> labels; // Danh sách nhãn
    public static Bitmap sharedBitmap = null; // Bitmap được truyền trực tiếp từ nơi khác (nếu có)

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);

        resultImage = findViewById(R.id.resultImage);
        resultLabel = findViewById(R.id.resultLabel);

        // Load danh sách nhãn từ tệp labels.txt trong assets
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

        // Load mô hình TFLite từ assets
        try {
            MappedByteBuffer tfliteModel = loadModelFile("pollution_detection.tflite");
            interpreter = new Interpreter(tfliteModel);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        // Ưu tiên xử lý Bitmap được truyền trực tiếp (nếu có)
        if (sharedBitmap != null) {
            processBitmap(sharedBitmap);
            sharedBitmap = null;
            return;
        }

        // Nhận dữ liệu từ Intent nếu có
        String imageUriString = getIntent().getStringExtra("imageUri");
        String videoUriString = getIntent().getStringExtra("videoUri");

        // Nếu truyền ảnh
        if (imageUriString != null) {
            Uri imageUri = Uri.parse(imageUriString);
            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageUri);
                processBitmap(bitmap);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        // Nếu truyền video, lấy 1 khung hình đầu để phát hiện
        else if (videoUriString != null) {
            Uri videoUri = Uri.parse(videoUriString);
            MediaMetadataRetriever retriever = new MediaMetadataRetriever();
            try {
                retriever.setDataSource(this, videoUri);
                Bitmap frame = retriever.getFrameAtTime(1000000); // Lấy tại 1s
                if (frame == null) frame = retriever.getFrameAtTime(0); // fallback nếu lỗi
                if (frame != null) processBitmap(frame);
            } catch (RuntimeException e) {
                e.printStackTrace();
                Toast.makeText(this, "Lỗi khi xử lý video", Toast.LENGTH_SHORT).show();
            } finally {
                try {
                    retriever.release();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    // Xử lý nhận diện trên 1 Bitmap bất kỳ
    private void processBitmap(Bitmap bitmap) {
        int inputSize = 640;
        Bitmap resizedBitmap = Bitmap.createScaledBitmap(bitmap, inputSize, inputSize, false);

        // Chuẩn bị dữ liệu đầu vào dạng ByteBuffer
        ByteBuffer inputBuffer = ByteBuffer.allocateDirect(1 * inputSize * inputSize * 3 * 4);
        inputBuffer.order(ByteOrder.nativeOrder());
        inputBuffer.rewind();

        for (int y = 0; y < inputSize; y++) {
            for (int x = 0; x < inputSize; x++) {
                int pixel = resizedBitmap.getPixel(x, y);
                inputBuffer.putFloat(((pixel >> 16) & 0xFF) / 255.0f);
                inputBuffer.putFloat(((pixel >> 8) & 0xFF) / 255.0f);
                inputBuffer.putFloat((pixel & 0xFF) / 255.0f);
            }
        }

        // Chuẩn bị bộ đệm cho đầu ra của mô hình
        int[] outputShape = interpreter.getOutputTensor(0).shape();
        int outputBox = outputShape[1];
        int outputDim = outputShape[2];

        ByteBuffer outputBuffer = ByteBuffer.allocateDirect(outputDim * outputBox * 4);
        outputBuffer.order(ByteOrder.nativeOrder());
        outputBuffer.rewind();

        Object[] inputArray = {inputBuffer};
        Map<Integer, Object> outputMap = new HashMap<>();
        outputMap.put(0, outputBuffer);

        // Chạy suy luận mô hình
        interpreter.runForMultipleInputsOutputs(inputArray, outputMap);
        outputBuffer.rewind();

        // Đọc kết quả đầu ra
        float[][][] outputs = new float[1][outputBox][outputDim];
        for (int i = 0; i < outputBox; i++) {
            for (int j = 0; j < outputDim; j++) {
                outputs[0][i][j] = outputBuffer.getFloat();
            }
        }

        List<DetectionResult> detections = new ArrayList<>();
        float confThreshold = 0.3f;

        // Lọc các box có độ tin cậy cao
        for (int i = 0; i < outputBox; i++) {
            float confidence = outputs[0][i][4];
            if (confidence < confThreshold) continue;

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

        // Áp dụng NMS để loại trùng box
        List<DetectionResult> finalDetections = Utils.nonMaxSuppress(detections, 0.5f);

        Bitmap outputBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);
        Canvas canvas = new Canvas(outputBitmap);

        Paint paint = new Paint();
        paint.setColor(Color.RED);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(2.0f);

        Paint textPaint = new Paint();
        textPaint.setColor(Color.RED);
        textPaint.setTextSize(30);

        // Tính tỷ lệ scale từ hệ quy chiếu mô hình về ảnh gốc
        float scaleX = (float) bitmap.getWidth() / inputSize;
        float scaleY = (float) bitmap.getHeight() / inputSize;

        StringBuilder sb = new StringBuilder();

        for (DetectionResult det : finalDetections) {
            det.scaleBoundingBox(scaleX, scaleY);
            canvas.drawRect(det.getBoundingBox(), paint);

            float x = det.getBoundingBox().left;
            float y = det.getBoundingBox().top - 10;
            if (y < 0) y = det.getBoundingBox().top + 30;

            canvas.drawText(det.getLabel() + String.format(" (%.2f)", det.getConfidence()), x, y, textPaint);
            sb.append(det.getLabel()).append(String.format(" (%.2f) ", det.getConfidence()));
        }

        // Hiển thị kết quả lên giao diện
        String resultText = sb.length() > 0 ? sb.toString() : "Không phát hiện đối tượng";
        resultLabel.setText(resultText);
        resultImage.setImageBitmap(outputBitmap);
    }

    // Hàm load file mô hình từ assets
    private MappedByteBuffer loadModelFile(String modelFileName) throws IOException {
        AssetFileDescriptor fileDescriptor = getAssets().openFd(modelFileName);
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }
}
