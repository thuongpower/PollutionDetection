package com.example.pollutiondetection;

import android.content.res.AssetManager;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class LabelUtils {

    /**
     * Đọc danh sách nhãn từ tệp labels.txt trong thư mục assets
     * @param assetManager AssetManager của ứng dụng
     * @param fileName Tên tệp labels.txt
     * @return Danh sách nhãn dưới dạng List<String>
     * @throws IOException
     */
    public static List<String> loadLabels(AssetManager assetManager, String fileName) throws IOException {
        List<String> labels = new ArrayList<>();
        InputStream is = assetManager.open(fileName);
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        String line;
        while ((line = reader.readLine()) != null) {
            labels.add(line);
        }
        reader.close();
        return labels;
    }
}
