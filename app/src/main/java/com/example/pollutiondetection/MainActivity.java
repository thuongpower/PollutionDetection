// MainActivity.java
package com.example.pollutiondetection;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.common.util.concurrent.ListenableFuture;

import org.tensorflow.lite.Interpreter;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.channels.FileChannel;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class MainActivity extends AppCompatActivity {
    private final int PICK_IMAGE = 1000;
    private final int REQUEST_CAMERA_PERMISSION = 101;
    private PreviewView previewView;
    private Interpreter tflite;
    private List<String> labels;
    private TextView resultText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        previewView = findViewById(R.id.previewView);
        Button btnSelect = findViewById(R.id.buttonSelectImage);
        resultText = findViewById(R.id.textViewResult);

        // Yêu cầu quyền CAMERA nếu chưa cấp
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
        } else {
            startCamera(); // Bắt đầu camera nếu đã có quyền
        }

        // Nút chọn ảnh từ thư viện
        btnSelect.setOnClickListener(v -> {
            Intent intent = new Intent();
            intent.setType("image/*");
            intent.setAction(Intent.ACTION_GET_CONTENT);
            startActivityForResult(Intent.createChooser(intent, "Chọn ảnh"), PICK_IMAGE);
        });

        // Tải model TFLite và nhãn từ assets
        try {
            tflite = new Interpreter(loadModelFile());
            labels = loadLabelList();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Hàm khởi chạy Preview với CameraX:contentReference[oaicite:9]{index=9}
    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                bindPreview(cameraProvider);
            } catch (ExecutionException | InterruptedException e) {
                // Xử lý lỗi nếu cần
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void bindPreview(@NonNull ProcessCameraProvider cameraProvider) {
        Preview preview = new Preview.Builder().build();
        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK).build();

        preview.setSurfaceProvider(previewView.getSurfaceProvider());
        Camera camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview);
    }

    // Kết quả chọn ảnh từ thư viện
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == PICK_IMAGE && resultCode == RESULT_OK && data != null) {
            try {
                // Đọc ảnh từ URI
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(
                        this.getContentResolver(), data.getData());
                // Tiền xử lý: resize về kích thước model (ví dụ 224x224) và normalize
                int imageSize = 224;
                bitmap = Bitmap.createScaledBitmap(bitmap, imageSize, imageSize, false);
                ByteBuffer inputBuffer = ByteBuffer.allocateDirect(4 * imageSize * imageSize * 3);
                inputBuffer.order(ByteOrder.nativeOrder());
                // Chuyển pixel vào ByteBuffer (giả sử model yêu cầu float RGB chuẩn hóa)
                for (int y = 0; y < imageSize; y++) {
                    for (int x = 0; x < imageSize; x++) {
                        int px = bitmap.getPixel(x, y);
                        // Lấy kênh màu
                        float r = (Color.red(px)   & 0xFF) / 255.0f;
                        float g = (Color.green(px) & 0xFF) / 255.0f;
                        float b = (Color.blue(px)  & 0xFF) / 255.0f;
                        inputBuffer.putFloat(r);
                        inputBuffer.putFloat(g);
                        inputBuffer.putFloat(b);
                    }
                }
                // Chạy inference với TFLite:contentReference[oaicite:10]{index=10}
                float[][] output = new float[1][labels.size()];
                tflite.run(inputBuffer, output);
                // Tìm nhãn có xác suất cao nhất
                int maxIdx = 0;
                float maxProb = output[0][0];
                for (int i = 1; i < labels.size(); i++) {
                    if (output[0][i] > maxProb) {
                        maxProb = output[0][i];
                        maxIdx = i;
                    }
                }
                String label = labels.get(maxIdx);
                resultText.setText("Phát hiện: " + label);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    // Yêu cầu quyền cho camera
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCamera(); // nếu được phép, khởi động camera
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    /** Đọc file model TFLite từ assets (Memory-map). Ví dụ tham khảo:contentReference[oaicite:11]{index=11} */
    private MappedByteBuffer loadModelFile() throws IOException {
        AssetFileDescriptor fileDescriptor = getAssets().openFd("oil_model.tflite");
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

    /** Đọc file labels.txt từ assets vào List<String> */
    private List<String> loadLabelList() throws IOException {
        List<String> labelList = new ArrayList<>();
        BufferedReader reader = new BufferedReader(
                new InputStreamReader(getAssets().open("labels.txt")));
        String line;
        while ((line = reader.readLine()) != null) {
            labelList.add(line);
        }
        reader.close();
        return labelList;
    }
}
