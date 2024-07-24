package com.example.facecoloranalyzer;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.exifinterface.media.ExifInterface;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Output extends AppCompatActivity {

    private ImageView capturedImage;
    private Intent intent;
    private Button retryBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_output);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        capturedImage = findViewById(R.id.capturedImage);
        Uri imageUri = Uri.parse(getIntent().getStringExtra("imageUri"));
        if (imageUri != null) {
            try {
                InputStream inputStream = getContentResolver().openInputStream(imageUri);
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inJustDecodeBounds = false;
                options.inSampleSize = calculateInSampleSize(options, 1024, 1024);
                Bitmap bitmap = BitmapFactory.decodeStream(inputStream, null, options);
                bitmap = rotateImageIfRequired(bitmap, imageUri);
                inputStream.close();

                // Save bitmap to a file and remove background
                saveImageToFile(bitmap);

            } catch (IOException e) {
                e.printStackTrace();
            }
        }

// Calculate sample size based on required dimensions



        // Set an OnClickListener to the button
        retryBtn = findViewById(R.id.retryButton);
        retryBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Create an Intent to navigate to AnotherActivity
                Intent retryintent = new Intent(Output.this, MainActivity.class);
                // Start the new activity
                startActivity(retryintent);
            }
        });

        // Get the result from the intent
        String result = getIntent().getStringExtra("SEASONAL");
        ImageView palette = findViewById(R.id.palette);
        Button seePalette = findViewById(R.id.seePaletteButton);

        // Regular expression to match hex color codes (e.g., #FFFFFF)
        String regex = "#([A-Fa-f0-9]{6})";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(result);

        List<String> bestColors = new ArrayList<>();

        // Extract hex color codes from the paragraph
        while (matcher.find()) {
            String hexColor = matcher.group(0); // Get the matched string
            bestColors.add(hexColor);
        }

        // Convert ArrayList to array if needed
        String[] bestColorsArray = bestColors.toArray(new String[0]);

        if (result != null) {
            if (result.contains("Autumn")) {
                palette.setImageResource(R.drawable.autumn_preview);
                intent = new Intent(Output.this, DetailedPalette.class);
                intent.putExtra("image_resource_id", R.drawable.autumn);
                intent.putExtra("BEST_COLORS", bestColorsArray);
                intent.putExtra("SEASON", result);
                capturedImage.setBackgroundResource(R.drawable.autumn_wheel);

            } else if (result.contains("Summer")) {
                palette.setImageResource(R.drawable.summer_preview);
                intent = new Intent(Output.this, DetailedPalette.class);
                intent.putExtra("image_resource_id", R.drawable.summer);
                intent.putExtra("BEST_COLORS", bestColorsArray);
                intent.putExtra("SEASON", result);
                capturedImage.setBackgroundResource(R.drawable.summer_wheel);
            } else if (result.contains("Spring")) {
                palette.setImageResource(R.drawable.spring_preview);
                intent = new Intent(Output.this, DetailedPalette.class);
                intent.putExtra("image_resource_id", R.drawable.spring);
                intent.putExtra("BEST_COLORS", bestColorsArray);
                intent.putExtra("SEASON", result);
                capturedImage.setBackgroundResource(R.drawable.spring_wheel);
            } else if (result.contains("Winter")) {
                palette.setImageResource(R.drawable.winter_preview);
                intent = new Intent(Output.this, DetailedPalette.class);
                intent.putExtra("image_resource_id", R.drawable.winter);
                intent.putExtra("BEST_COLORS", bestColorsArray);
                intent.putExtra("SEASON", result);
                capturedImage.setBackgroundResource(R.drawable.winter_wheel);

            } else {
                // If none of the specified seasons are found, assign a default image
                palette.setImageResource(R.drawable.no_face_detected);
                seePalette.setVisibility(View.INVISIBLE);
            }
        } else {
            // Handle the case when 'result' is null
            palette.setImageResource(R.drawable.no_face_detected);
            seePalette.setVisibility(View.INVISIBLE);
        }



        seePalette.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Start the DetailedPalette activity
                //startActivity(intent);
                Intent guideIntent = new Intent(Output.this, ClickGuide.class);
                guideIntent.putExtra("paletteIntent", intent); // Pass the original intent
                startActivity(guideIntent);
            }
        });
    }

    private void saveImageToFile(Bitmap bitmap) {
        File imageFile = new File(getCacheDir(), "selected_image.png");
        try (OutputStream os = new FileOutputStream(imageFile)) {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, os);
            removeBackground(imageFile);
        } catch (IOException e) {
            Log.e("ImageSaveError", "Failed to save image", e);
            Toast.makeText(this, "Failed to save image", Toast.LENGTH_SHORT).show();
        }
    }

    private void removeBackground(File imageFile) {
        new Thread(() -> {
            try {
                URL url = new URL("https://api.remove.bg/v1.0/removebg");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.setRequestProperty("X-Api-Key", "yzXPMJpFppZbCGwC2f1zwm84");

                String boundary = "---------------------------" + System.currentTimeMillis();
                conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
                try (OutputStream os = conn.getOutputStream();
                     PrintWriter writer = new PrintWriter(new OutputStreamWriter(os, StandardCharsets.UTF_8), true)) {

                    writer.append("--").append(boundary).append("\r\n");
                    writer.append("Content-Disposition: form-data; name=\"image_file\"; filename=\"selected_image.png\"").append("\r\n");
                    writer.append("Content-Type: application/octet-stream").append("\r\n").append("\r\n");
                    writer.flush();
                    try (InputStream is = new FileInputStream(imageFile)) {
                        byte[] buffer = new byte[4096];
                        int bytesRead;
                        while ((bytesRead = is.read(buffer)) != -1) {
                            os.write(buffer, 0, bytesRead);
                        }
                    }
                    os.flush();
                    writer.append("\r\n");
                    writer.append("--").append(boundary).append("--").append("\r\n");
                }

                int statusCode = conn.getResponseCode();
                if (statusCode == HttpURLConnection.HTTP_OK) {
                    try (InputStream is = conn.getInputStream()) {
                        Bitmap bitmap = BitmapFactory.decodeStream(is);
                        runOnUiThread(() -> capturedImage.setImageBitmap(bitmap));
                    }
                } else {
                    runOnUiThread(() -> Toast.makeText(Output.this, "Failed to remove background: " + statusCode, Toast.LENGTH_SHORT).show());
                }
            } catch (IOException e) {
                Log.e("RemoveBgError", "Failed to remove background", e);
                runOnUiThread(() -> Toast.makeText(Output.this, "Failed to remove background", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }

    private Bitmap rotateImageIfRequired(Bitmap img, Uri selectedImage) throws IOException {
        InputStream input = getContentResolver().openInputStream(selectedImage);
        ExifInterface ei = new ExifInterface(input);
        int orientation = ei.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED);
        input.close();

        switch (orientation) {
            case ExifInterface.ORIENTATION_ROTATE_90:
                return rotateImage(img, 90);
            case ExifInterface.ORIENTATION_ROTATE_180:
                return rotateImage(img, 180);
            case ExifInterface.ORIENTATION_ROTATE_270:
                return rotateImage(img, 270);
            default:
                return img;
        }
    }
    private Bitmap rotateImage(Bitmap img, int degree) {
        Matrix matrix = new Matrix();
        matrix.postRotate(degree);
        Bitmap rotatedImg = Bitmap.createBitmap(img, 0, 0, img.getWidth(), img.getHeight(), matrix, true);
        img.recycle();
        return rotatedImg;
    }
}
