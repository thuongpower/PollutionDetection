package com.example.pollutiondetection;

import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.RectF;

import org.tensorflow.lite.Interpreter;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class PollutionClassifier {

    private final Interpreter interpreter;
    private final List<String> labels;
    private final int inputSize;

    public PollutionClassifier(AssetManager assetManager, String modelPath, String labelPath, int inputSize, boolean quant) throws IOException {
        this.inputSize = inputSize;
        this.interpreter = new Interpreter(loadModelFile(assetManager, modelPath));
        this.labels = LabelUtils.loadLabels(assetManager, labelPath);
    }

    private MappedByteBuffer loadModelFile(AssetManager assetManager, String path) throws IOException {
        AssetFileDescriptor fileDescriptor = assetManager.openFd(path);
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, fileDescriptor.getStartOffset(), fileDescriptor.getDeclaredLength());
    }

    public List<DetectionResult> detect(Bitmap bitmap) {
        Bitmap resized = Bitmap.createScaledBitmap(bitmap, inputSize, inputSize, false);

        int[] outputShape = interpreter.getOutputTensor(0).shape();
        int numClasses = outputShape[1] - 4;
        int numCandidates = outputShape[2];

        float[][][][] input = new float[1][inputSize][inputSize][3];
        for (int y = 0; y < inputSize; y++) {
            for (int x = 0; x < inputSize; x++) {
                int pixel = resized.getPixel(x, y);
                input[0][y][x][0] = ((pixel >> 16) & 0xFF) / 255.0f;
                input[0][y][x][1] = ((pixel >> 8) & 0xFF) / 255.0f;
                input[0][y][x][2] = (pixel & 0xFF) / 255.0f;
            }
        }

        float[][][] output = new float[1][outputShape[1]][outputShape[2]];
        interpreter.run(input, output);

        List<DetectionResult> detections = new ArrayList<>();
        float confThreshold = 0.4f;

        for (int i = 0; i < numCandidates; i++) {
            float xCenter = output[0][0][i] * bitmap.getWidth();
            float yCenter = output[0][1][i] * bitmap.getHeight();
            float width = output[0][2][i] * bitmap.getWidth();
            float height = output[0][3][i] * bitmap.getHeight();

            float left = xCenter - width / 2;
            float top = yCenter - height / 2;
            float right = xCenter + width / 2;
            float bottom = yCenter + height / 2;

            int bestClass = -1;
            float bestConf = 0f;
            for (int c = 0; c < numClasses; c++) {
                float conf = output[0][4 + c][i];
                if (conf > bestConf) {
                    bestConf = conf;
                    bestClass = c;
                }
            }

            if (bestConf >= confThreshold && bestClass >= 0) {
                String label = labels.get(bestClass);
                detections.add(new DetectionResult(new RectF(left, top, right, bottom), label, bestConf));
            }
        }

        return nonMaxSuppression(detections, 0.5f);
    }

    /**
     * Thuật toán Non-Max Suppression (NMS) để loại bỏ bounding box chồng lấn
     * @param detections Danh sách phát hiện ban đầu
     * @param iouThreshold Ngưỡng IoU để lọc (0.5 là hợp lý)
     * @return Danh sách bounding box đã loại bỏ trùng lặp
     */
    private List<DetectionResult> nonMaxSuppression(List<DetectionResult> detections, float iouThreshold) {
        List<DetectionResult> results = new ArrayList<>();
        Collections.sort(detections, (o1, o2) -> Float.compare(o2.getConfidence(), o1.getConfidence()));

        boolean[] removed = new boolean[detections.size()];

        for (int i = 0; i < detections.size(); i++) {
            if (removed[i]) continue;
            DetectionResult detA = detections.get(i);
            results.add(detA);

            for (int j = i + 1; j < detections.size(); j++) {
                if (removed[j]) continue;
                DetectionResult detB = detections.get(j);
                if (calculateIoU(detA.getBoundingBox(), detB.getBoundingBox()) > iouThreshold) {
                    removed[j] = true;
                }
            }
        }
        return results;
    }

    /**
     * Tính toán IoU (Intersection over Union) giữa 2 bounding box
     */
    private float calculateIoU(RectF a, RectF b) {
        float intersectLeft = Math.max(a.left, b.left);
        float intersectTop = Math.max(a.top, b.top);
        float intersectRight = Math.min(a.right, b.right);
        float intersectBottom = Math.min(a.bottom, b.bottom);

        float intersectArea = Math.max(0, intersectRight - intersectLeft) * Math.max(0, intersectBottom - intersectTop);
        float areaA = (a.right - a.left) * (a.bottom - a.top);
        float areaB = (b.right - b.left) * (b.bottom - b.top);

        return intersectArea / (areaA + areaB - intersectArea);
    }
}
