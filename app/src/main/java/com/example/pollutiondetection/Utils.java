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
}

