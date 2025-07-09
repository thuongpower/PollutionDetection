package com.example.pollutiondetection.image;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.pollutiondetection.R;
import com.example.pollutiondetection.TensorFlowActivity;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class ImageActivity extends AppCompatActivity {

    private static final int REQUEST_DETECT = 200;
    private ImageView resultImage;
    private TextView textAlgae, textTrash, textOil;
    private Button btnDetect;
    private Bitmap imageBitmap;
    private String imageUriString;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);

        resultImage = findViewById(R.id.resultImage);
        textAlgae = findViewById(R.id.textAlgae);
        textTrash = findViewById(R.id.textTrash);
        textOil = findViewById(R.id.textOil);
        btnDetect = findViewById(R.id.btnDetect);

        imageUriString = getIntent().getStringExtra("imageUri");
        if (imageUriString != null) {
            Uri imageUri = Uri.parse(imageUriString);
            try {
                imageBitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
                resultImage.setImageBitmap(imageBitmap);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        btnDetect.setOnClickListener(v -> {
            if (imageBitmap != null) {
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                imageBitmap.compress(Bitmap.CompressFormat.JPEG, 85, stream);
                byte[] byteArray = stream.toByteArray();

                Intent intent = new Intent(ImageActivity.this, TensorFlowActivity.class);
                intent.putExtra("frameData", byteArray);
                // start for result
                startActivityForResult(intent, REQUEST_DETECT);
            } else {
                Toast.makeText(this, "Không có ảnh để xử lý", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_DETECT && resultCode == RESULT_OK && data != null) {
            // Nhận về kết quả từ TensorFlowActivity
            byte[] resultBytes = data.getByteArrayExtra("resultImage");
            int algae = data.getIntExtra("countAlgae", 0);
            int trash = data.getIntExtra("countTrash", 0);
            int oil = data.getIntExtra("countOil", 0);

            if (resultBytes != null) {
                Bitmap resultBmp = BitmapFactory.decodeByteArray(resultBytes, 0, resultBytes.length);
                resultImage.setImageBitmap(resultBmp);
            }
            textAlgae.setText("Sóng: " + algae);
            textTrash.setText("Rác: " + trash);
            textOil.setText("Váng dầu: " + oil);
            //Toast.makeText(this, "Hoàn thành nhận diện", Toast.LENGTH_SHORT).show();

        }
    }
}