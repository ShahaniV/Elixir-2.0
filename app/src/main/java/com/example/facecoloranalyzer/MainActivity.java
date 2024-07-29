package com.example.facecoloranalyzer;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Socket;

public class MainActivity extends AppCompatActivity {

    private static final int CAMERA_PERMISSION_CODE = 102;
    private static final int STORAGE_PERMISSION_CODE = 103;
    private static final String SERVER_IP = "192.168.100.72"; // Replace with your server IP
    private static final int SERVER_PORT = 5000;

    private ImageView imageView;
    private ImageView uploadButton;
    private ImageView uploadImage;
    private ProgressDialog progressDialog;
    private ImageView progressImage;
    private Handler handler = new Handler();
    private boolean isLogo1 = true;
    private Runnable imageSwitcher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        imageView = findViewById(R.id.imageView);
        uploadButton = findViewById(R.id.uploadButton);
        uploadImage = findViewById(R.id.uploadImage);

        // Initialize the progress dialog and its custom view
        LayoutInflater inflater = LayoutInflater.from(this);
        View customView = inflater.inflate(R.layout.custom_progress_dialog, null);

        progressDialog = new ProgressDialog(this);

        // Set the custom view to the dialog only after show() is called
        progressDialog.setOnShowListener(dialog -> {
            progressDialog.setContentView(customView);
            progressImage = customView.findViewById(R.id.progress_image);

            // Start the image switching task
            handler.post(imageSwitcher);
        });

        // Create the image switching task
        imageSwitcher = new Runnable() {
            @Override
            public void run() {
                // Switch the image every 2 seconds
                if (isLogo1) {
                    progressImage.setImageResource(R.drawable.mixing_process_logo);
                } else {
                    progressImage.setImageResource(R.drawable.mixing_process2_logo);
                }
                isLogo1 = !isLogo1;
                handler.postDelayed(this, 2000); // Repeat every 2 seconds
            }
        };

        progressDialog.setOnDismissListener(dialog -> handler.removeCallbacks(imageSwitcher));


        uploadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (checkPermission(Manifest.permission.CAMERA, CAMERA_PERMISSION_CODE)) {
                    dispatchTakePictureIntent();
                }
            }
        });

        uploadImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // Android 13 and above
                    if (checkPermission(Manifest.permission.READ_MEDIA_IMAGES, STORAGE_PERMISSION_CODE)) {
                        uploadImageFromGallery();
                    }
                } else {
                    if (checkPermission(Manifest.permission.READ_EXTERNAL_STORAGE, STORAGE_PERMISSION_CODE)) {
                        uploadImageFromGallery();
                    }
                }
            }
        });
    }

    private void uploadImageFromGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        galleryResultLauncher.launch(intent);
    }

    private boolean checkPermission(String permission, int requestCode) {
        if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(this, new String[]{permission}, requestCode);
            return false;
        } else {
            return true;
        }
    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            cameraResultLauncher.launch(takePictureIntent);
        } else {
            Toast.makeText(this, "Camera app not available", Toast.LENGTH_SHORT).show();
        }
    }

    private final ActivityResultLauncher<Intent> cameraResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Bitmap bitmap = (Bitmap) result.getData().getExtras().get("data");
                    Uri imageUri = getImageUri(bitmap);
                    processImage(bitmap, imageUri);
                }
            }
    );

    private final ActivityResultLauncher<Intent> galleryResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri selectedImageUri = result.getData().getData();
                    if (selectedImageUri != null) {
                        try {
                            Bitmap bitmap = loadAndResizeImage(selectedImageUri);
                            bitmap = rotateImageIfRequired(bitmap, selectedImageUri);
                            processImage(bitmap, selectedImageUri);
                        } catch (IOException e) {
                            Log.e("MainActivity", "Failed to load and resize image", e);
                            Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show();
                        }
                    }
                } else {
                    Toast.makeText(this, "Image selection canceled", Toast.LENGTH_SHORT).show();
                }
            }
    );

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                dispatchTakePictureIntent();
            } else {
                Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == STORAGE_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                uploadImageFromGallery();
            } else {
                Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private Bitmap loadAndResizeImage(Uri uri) throws IOException {
        final int maxSize = 1024; // Maximum image size in pixels

        InputStream inputStream = getContentResolver().openInputStream(uri);
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeStream(inputStream, null, options);
        inputStream.close();

        int width = options.outWidth;
        int height = options.outHeight;
        int scale = 1;

        while (width / scale >= maxSize || height / scale >= maxSize) {
            scale *= 2;
        }

        options.inSampleSize = scale;
        options.inJustDecodeBounds = false;

        inputStream = getContentResolver().openInputStream(uri);
        Bitmap resizedBitmap = BitmapFactory.decodeStream(inputStream, null, options);
        inputStream.close();

        return resizedBitmap;
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

    private void processImage(Bitmap bitmap, Uri imageUri) {
        imageView.setImageBitmap(null);
        imageView.setImageBitmap(bitmap);
        sendImageToServer(bitmap, imageUri);
    }

    private void sendImageToServer(Bitmap bitmap, Uri imageUri) {
        progressDialog.show();
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Socket socket = new Socket(SERVER_IP, SERVER_PORT);
                    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream);
                    byte[] imageData = byteArrayOutputStream.toByteArray();
                    int imageLength = imageData.length;
                    DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream());
                    dataOutputStream.writeInt(imageLength);
                    dataOutputStream.write(imageData, 0, imageLength);
                    dataOutputStream.flush();

                    // Receive the result from the server
                    InputStream inputStream = socket.getInputStream();
                    InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                    char[] buffer = new char[1024];
                    int bytesRead = inputStreamReader.read(buffer);
                    final String seasonal = new String(buffer, 0, bytesRead);


                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            progressDialog.dismiss();
                            Intent resultIntent = new Intent(MainActivity.this, Output.class);
                            resultIntent.putExtra("imageUri", imageUri.toString());
                            //resultIntent.putExtra("RESULT", result);
                            resultIntent.putExtra("SEASONAL", seasonal);
                            startActivity(resultIntent);
                        }
                    });

                    socket.close();
                } catch (IOException e) {
                    Log.e("MainActivity", "Error sending image to server", e);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            progressDialog.dismiss();
                            Toast.makeText(MainActivity.this, "Failed to send image to server", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        }).start();
    }

    private Uri getImageUri(Bitmap bitmap) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
        String path = MediaStore.Images.Media.insertImage(getContentResolver(), bitmap, "Title", null);
        return Uri.parse(path);
    }
}