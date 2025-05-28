package com.example.pollutiondetection;

import android.graphics.RectF;

// Lưu thông tin kết quả phát hiện
public class DetectionResult {
    private RectF boundingBox;
    private String label;
    private float confidence;
    private int classIndex;

    public DetectionResult(RectF boundingBox, String label, float confidence, int classIndex) {
        this.boundingBox = boundingBox;
        this.label = label;
        this.confidence = confidence;
        this.classIndex = classIndex;
    }

    public RectF getBoundingBox() {
        return boundingBox;
    }

    public String getLabel() {
        return label;
    }

    public float getConfidence() {
        return confidence;
    }

    public int getClassIndex() {
        return classIndex;
    }
}



