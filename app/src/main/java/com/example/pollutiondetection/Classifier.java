package com.example.pollutiondetection;

import android.graphics.Bitmap;
import java.util.List;

public interface Classifier {

    class Recognition {
        private final String id;
        private final String title;
        private final Float confidence;
        private final boolean quant;

        public Recognition(String id, String title, Float confidence, boolean quant) {
            this.id = id;
            this.title = title;
            this.confidence = confidence;
            this.quant = quant;
        }

        public String getId() { return id; }
        public String getTitle() { return title; }
        public Float getConfidence() { return confidence; }

        @Override
        public String toString() {
            String result = "";
            if (id != null) result += "[" + id + "] ";
            if (title != null) result += title + " ";
            if (confidence != null) result += String.format("(%.1f%%) ", confidence * 100);
            return result.trim();
        }
    }

    List<? extends Recognition> recognizeImage(Bitmap bitmap);
    void close();
}