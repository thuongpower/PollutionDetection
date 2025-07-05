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
    // Hàm scale bounding box về đúng kích thước gốc
    public void scaleBoundingBox(float scaleX, float scaleY) {
        boundingBox = new RectF(
                boundingBox.left * scaleX,
                boundingBox.top * scaleY,
                boundingBox.right * scaleX,
                boundingBox.bottom * scaleY
        );
    }
}
