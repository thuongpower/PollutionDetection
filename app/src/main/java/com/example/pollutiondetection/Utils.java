package com.example.pollutiondetection;

import android.graphics.RectF;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

// Hàm hỗ trợ xử lý kết quả phát hiện
public class Utils {
    // Non-Maximum Suppression: giữ khung có độ tin cậy cao và loại các khung chồng lấp (IOU > threshold)
    public static List<DetectionResult> nonMaxSuppress(List<DetectionResult> detections, float iouThreshold) {
        List<DetectionResult> outputList = new ArrayList<>();
        // Sắp xếp giảm dần theo confidence
        Collections.sort(detections, new Comparator<DetectionResult>() {
            @Override
            public int compare(DetectionResult o1, DetectionResult o2) {
                return Float.compare(o2.getConfidence(), o1.getConfidence());
            }
        });
        // Duyệt và loại bỏ các khung trùng lặp
        for (DetectionResult det : detections) {
            boolean keep = true;
            for (DetectionResult sel : outputList) {
                if (iou(det.getBoundingBox(), sel.getBoundingBox()) > iouThreshold) {
                    keep = false;
                    break;
                }
            }
            if (keep) {
                outputList.add(det);
            }
        }
        return outputList;
    }

    // Tính IOU của hai RectF (hình chữ nhật)
    public static float iou(RectF a, RectF b) {
        float interLeft = Math.max(a.left, b.left);
        float interTop = Math.max(a.top, b.top);
        float interRight = Math.min(a.right, b.right);
        float interBottom = Math.min(a.bottom, b.bottom);
        float interArea = 0;
        if (interRight > interLeft && interBottom > interTop) {
            interArea = (interRight - interLeft) * (interBottom - interTop);
        }
        float areaA = (a.right - a.left) * (a.bottom - a.top);
        float areaB = (b.right - b.left) * (b.bottom - b.top);
        float unionArea = areaA + areaB - interArea;
        if (unionArea <= 0) return 0;
        return interArea / unionArea;
    }

    // Phân tích output từ YOLO và lọc box bằng confidence + NMS
    public static List<DetectionResult> parseYOLOOutput(float[] outputs, List<String> labels, int inputSize) {
        List<DetectionResult> results = new ArrayList<>();
        float confThreshold = 0.5f; // Tăng độ lọc box kém tin cậy
        float iouThreshold = 0.4f;  // Dùng cho NMS

        int numElements = outputs.length;
        int numDetections = numElements / (labels.size() + 5);

        for (int i = 0; i < numDetections; i++) {
            int offset = i * (labels.size() + 5);
            float x = outputs[offset];
            float y = outputs[offset + 1];
            float w = outputs[offset + 2];
            float h = outputs[offset + 3];
            float objectness = outputs[offset + 4];

            // Bỏ qua box nếu không đủ độ tin cậy
            if (objectness < confThreshold) continue;

            // Tìm nhãn có xác suất cao nhất
            int bestClass = -1;
            float bestScore = 0;
            for (int j = 0; j < labels.size(); j++) {
                float score = outputs[offset + 5 + j];
                if (score > bestScore) {
                    bestScore = score;
                    bestClass = j;
                }
            }

            float confidence = bestScore * objectness;
            if (confidence < confThreshold) continue;

            // Scale bounding box về tọa độ
            float left = x - w / 2;
            float top = y - h / 2;
            float right = x + w / 2;
            float bottom = y + h / 2;
            RectF rect = new RectF(left, top, right, bottom);

            results.add(new DetectionResult(rect, labels.get(bestClass), confidence, bestClass));
        }

        // Áp dụng Non-Max Suppression để giảm box trùng lặp
        return nonMaxSuppress(results, iouThreshold);
    }
}
