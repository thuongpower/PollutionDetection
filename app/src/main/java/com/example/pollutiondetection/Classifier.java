package com.example.pollutiondetection;

import android.graphics.Bitmap;
import android.graphics.RectF;

import java.util.List;

public interface Classifier {

    // Lớp chứa kết quả Detection hoàn chỉnh
    class DetectionResult {
        private final RectF boundingBox;
        private final String label;
        private final float confidence;
        private final int classIndex;

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

        public void scaleBoundingBox(float scaleX, float scaleY) {
            boundingBox.set(
                    boundingBox.left * scaleX,
                    boundingBox.top * scaleY,
                    boundingBox.right * scaleX,
                    boundingBox.bottom * scaleY
            );
        }

        @Override
        public String toString() {
            return String.format("%s (%.2f%%)", label, confidence * 100);
        }
    }

    List<DetectionResult> recognizeImage(Bitmap bitmap);

    void close();
}
