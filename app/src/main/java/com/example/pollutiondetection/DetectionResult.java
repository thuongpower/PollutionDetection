package com.example.pollutiondetection;

import android.graphics.RectF;

import java.util.List;

// Lưu thông tin kết quả phát hiện
public class DetectionResult {
    private RectF boundingBox;
    private static List<String> label;
    private float confidence;
    private int classIndex;

    public DetectionResult(RectF boundingBox, float confidence, int classIndex) {
        this.boundingBox = boundingBox;
        this.confidence = confidence;
        this.classIndex = classIndex;
    }
    public static void setLabelList(List<String> labelList) {
        label = labelList;
    }
    public RectF getBoundingBox() {
        return boundingBox;
    }

    public String getLabel() {
        if (label != null && classIndex >= 0 && classIndex < label.size()) {
            return label.get(classIndex);
        } else {
            return "Unknown";
        }
    }

    public float getConfidence() {
        return confidence;
    }

    public int getClassIndex() {
        return classIndex;
    }
    @Override
    public String toString() {
        return getLabel() + String.format(" (%.2f)", confidence);
    }
}



