package com.example.pollutiondetection;

import android.content.Context;
import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.net.Uri;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class VideoProcessingHelper {

    public static void processVideo(Context context, Uri videoUri, Consumer<Bitmap> frameConsumer) {
        Executors.newSingleThreadExecutor().execute(() -> {
            MediaMetadataRetriever retriever = new MediaMetadataRetriever();
            retriever.setDataSource(context, videoUri);
            String durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);

            if (durationStr == null) return;

            long durationMs = Long.parseLong(durationStr);

            for (long t = 0; t < durationMs * 1000; t += 500_000) { // Mỗi 0.5s lấy 1 frame
                Bitmap frame = retriever.getFrameAtTime(t, MediaMetadataRetriever.OPTION_CLOSEST);
                if (frame != null) frameConsumer.accept(frame);
            }
            try {
                retriever.release();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }
}
