package com.example.pollutiondetection;

import android.graphics.RectF;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class Utils {

    // NMS chuẩn sử dụng DetectionResult lồng trong Classifier
    public static List<Classifier.DetectionResult> nonMaxSuppress(List<Classifier.DetectionResult> detections, float iouThreshold) {
        List<Classifier.DetectionResult> outputList = new ArrayList<>();
        Collections.sort(detections, Comparator.comparing(Classifier.DetectionResult::getConfidence).reversed());

        for (Classifier.DetectionResult det : detections) {
            boolean keep = true;
            for (Classifier.DetectionResult sel : outputList) {
                if (IoU(det.getBoundingBox(), sel.getBoundingBox()) > iouThreshold) {
                    keep = false;
                    break;
                }
            }
            if (keep) outputList.add(det);
        }
        return outputList;
    }

    public static float IoU(RectF a, RectF b) {
        float interLeft = Math.max(a.left, b.left);
        float interTop = Math.max(a.top, b.top);
        float interRight = Math.min(a.right, b.right);
        float interBottom = Math.min(a.bottom, b.bottom);

        float interArea = Math.max(0, interRight - interLeft) * Math.max(0, interBottom - interTop);
        float areaA = (a.right - a.left) * (a.bottom - a.top);
        float areaB = (b.right - b.left) * (b.bottom - b.top);

        float unionArea = areaA + areaB - interArea;
        if (unionArea <= 0) return 0f;
        return interArea / unionArea;
    }
}
