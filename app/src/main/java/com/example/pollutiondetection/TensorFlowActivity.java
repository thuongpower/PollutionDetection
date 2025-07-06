package com.example.pollutiondetection;

import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.RectF;

import org.tensorflow.lite.Interpreter;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TensorFlowActivity implements Classifier {

    private Interpreter interpreter;
    private List<String> labelList;
    private int inputSize;

    private static final int PIXEL_SIZE = 3;
    private static final float IMAGE_STD = 255f;
    private static final float CONF_THRESHOLD = 0.2f;
    private static final float NMS_THRESHOLD = 0.5f;

    public void initialize(AssetManager assetManager, String modelPath, String labelPath, int inputSize) throws IOException {
        this.interpreter = new Interpreter(loadModelFile(assetManager, modelPath));
        this.labelList = loadLabelList(assetManager, labelPath);
        this.inputSize = inputSize;
    }

    private MappedByteBuffer loadModelFile(AssetManager assetManager, String modelPath) throws IOException {
        AssetFileDescriptor fileDescriptor = assetManager.openFd(modelPath);
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, fileDescriptor.getStartOffset(), fileDescriptor.getDeclaredLength());
    }

    private List<String> loadLabelList(AssetManager assetManager, String labelPath) throws IOException {
        List<String> labels = new ArrayList<>();
        BufferedReader reader = new BufferedReader(new InputStreamReader(assetManager.open(labelPath)));
        String line;
        while ((line = reader.readLine()) != null) {
            labels.add(line);
        }
        reader.close();
        return labels;
    }

    private ByteBuffer convertBitmapToByteBuffer(Bitmap bitmap) {
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(4 * inputSize * inputSize * PIXEL_SIZE);
        byteBuffer.order(ByteOrder.nativeOrder());

        Bitmap resized = Bitmap.createScaledBitmap(bitmap, inputSize, inputSize, false);
        int[] pixels = new int[inputSize * inputSize];
        resized.getPixels(pixels, 0, inputSize, 0, 0, inputSize, inputSize);

        for (int val : pixels) {
            byteBuffer.putFloat(((val >> 16) & 0xFF) / IMAGE_STD);
            byteBuffer.putFloat(((val >> 8) & 0xFF) / IMAGE_STD);
            byteBuffer.putFloat((val & 0xFF) / IMAGE_STD);
        }

        return byteBuffer;
    }

    @Override
    public List<DetectionResult> recognizeImage(Bitmap bitmap) {
        ByteBuffer input = convertBitmapToByteBuffer(bitmap);

        float[][][] output = new float[1][7][8400];  // [1, 7, 8400] chuẩn YOLOv8 float
        Object[] inputs = new Object[]{input};
        interpreter.runForMultipleInputsOutputs(inputs, Collections.singletonMap(0, output));

        List<DetectionResult> detections = new ArrayList<>();

        for (int i = 0; i < 8400; i++) {
            float x = output[0][0][i];
            float y = output[0][1][i];
            float w = output[0][2][i];
            float h = output[0][3][i];
            float objectness = output[0][4][i];

            if (objectness < CONF_THRESHOLD) continue;

            int bestClass = -1;
            float bestScore = -1f;

            for (int c = 0; c < labelList.size(); c++) {
                float classProb = output[0][5 + c][i];
                if (classProb > bestScore) {
                    bestScore = classProb;
                    bestClass = c;
                }
            }

            float confidence = objectness * bestScore;
            if (confidence < CONF_THRESHOLD) continue;

            float left = (x - w / 2) * bitmap.getWidth();
            float top = (y - h / 2) * bitmap.getHeight();
            float right = (x + w / 2) * bitmap.getWidth();
            float bottom = (y + h / 2) * bitmap.getHeight();

            RectF rect = new RectF(left, top, right, bottom);
            detections.add(new DetectionResult(rect, labelList.get(bestClass), confidence, bestClass));
        }

        // Áp dụng NMS loại trùng
        return Utils.nonMaxSuppress(detections, NMS_THRESHOLD);
    }

    @Override
    public void close() {
        if (interpreter != null) {
            interpreter.close();
            interpreter = null;
        }
    }
}
