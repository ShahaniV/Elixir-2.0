package com.example.facecoloranalyzer;

import android.Manifest;
import android.app.Dialog;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.service.controls.actions.FloatAction;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class DetailedPalette extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 100;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.detailed_palette);
        // Receive array from intent
        String[] bestColorsArray = getIntent().getStringArrayExtra("BEST_COLORS");
        String result = getIntent().getStringExtra("SEASON");


        ImageView palette = findViewById(R.id.palette);
        int imageResourceId = getIntent().getIntExtra("image_resource_id", -1);
        if (imageResourceId != -1) {
            palette.setImageResource(imageResourceId);
        }

        Button palette1 = findViewById(R.id.palette1);
        Button palette2 = findViewById(R.id.palette2);
        FloatingActionButton fab = findViewById(R.id.fab);

        String resourceName = getResources().getResourceEntryName(imageResourceId);

        palette1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String newResourceName = resourceName + "_palette1";
                int drawableId = getResources().getIdentifier(newResourceName, "drawable", getPackageName());
                if (drawableId != 0) {
                    palette.setImageResource(drawableId);
                }
            }
        });

        palette2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String newResourceName = resourceName + "_palette2";
                int drawableId = getResources().getIdentifier(newResourceName, "drawable", getPackageName());
                if (drawableId != 0) {
                    palette.setImageResource(drawableId);
                }
            }
        });

        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showBottomDialog();
            }
        });
    }

    private boolean checkPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED;
        } else {
            return ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        }
    }

    private void requestPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_MEDIA_IMAGES}, PERMISSION_REQUEST_CODE);
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSION_REQUEST_CODE);
        }
    }

    private void captureAndSaveScreenshot() {
        ScrollView scrollView = findViewById(R.id.scrollView);
        LinearLayout downloadButton = findViewById(R.id.download);

        downloadButton.setVisibility(View.GONE);

        Bitmap bitmap = Bitmap.createBitmap(scrollView.getChildAt(0).getWidth(), scrollView.getChildAt(0).getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        scrollView.getChildAt(0).draw(canvas);

        downloadButton.setVisibility(View.VISIBLE);

        saveBitmap(bitmap);
    }

    private void saveBitmap(Bitmap bitmap) {
        OutputStream fos;
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContentValues values = new ContentValues();
                values.put(MediaStore.Images.Media.DISPLAY_NAME, "screenshot_" + System.currentTimeMillis() + ".png");
                values.put(MediaStore.Images.Media.MIME_TYPE, "image/png");
                values.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/FaceColorAnalyzer");

                Uri uri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
                if (uri == null) {
                    throw new IOException("Failed to create new MediaStore record.");
                }
                fos = getContentResolver().openOutputStream(uri);
            } else {
                File directory = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "FaceColorAnalyzer");
                if (!directory.exists()) {
                    directory.mkdirs();
                }
                String filePath = directory.getAbsolutePath() + "/screenshot_" + System.currentTimeMillis() + ".png";
                File file = new File(filePath);
                fos = new FileOutputStream(file);

                Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                Uri contentUri = Uri.fromFile(file);
                mediaScanIntent.setData(contentUri);
                sendBroadcast(mediaScanIntent);
            }

            if (fos != null) {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
                fos.close();
                Toast.makeText(this, "Result saved to gallery", Toast.LENGTH_SHORT).show();
            }
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to save the result", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                captureAndSaveScreenshot();
            } else {
                Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void showBottomDialog() {

        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.bottomsheetlayout);

        LinearLayout download = dialog.findViewById(R.id.download);
        LinearLayout guidesAndTips = dialog.findViewById(R.id.guidesAndTips);
        LinearLayout pGenerator = dialog.findViewById(R.id.paletteGenerator);
        LinearLayout TryonButton = dialog.findViewById(R.id.virtualTryOn);
        ImageView cancelButton = dialog.findViewById(R.id.cancelButton);

        // Receive array from intent
        String[] bestColorsArray = getIntent().getStringArrayExtra("BEST_COLORS");
        String result = getIntent().getStringExtra("SEASON");



        download.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (checkPermission()) {
                    dialog.dismiss();
                    captureAndSaveScreenshot();
                } else {
                    dialog.dismiss();
                    requestPermission();
                }
            }
        });

        guidesAndTips.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                Intent intent = new Intent(DetailedPalette.this, tips.class);
                startActivity(intent);
            }
        });

        pGenerator.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                Intent intent = new Intent(DetailedPalette.this, Generator.class);
                intent.putExtra("BEST_COLORS", bestColorsArray);
                intent.putExtra("SEASON", result);
                startActivity(intent);
            }
        });

        TryonButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                Intent intent = new Intent(DetailedPalette.this, VirtualTryOn.class);
                startActivity(intent);
            }
        });

        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
            }
        });

        dialog.show();
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.getWindow().getAttributes().windowAnimations = R.style.DialogAnimation;
        dialog.getWindow().setGravity(Gravity.BOTTOM);

    }
}
