package com.example.facecoloranalyzer;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class VirtualTryOn extends AppCompatActivity {

    private String hairColor;
    private String eyeshadowColor;
    private String blushColor;
    private String lipsColor;
    private String clothesColor;

    private ImageView virtualMakeupImageView;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_virtual_try_on);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        Button btnHair = findViewById(R.id.btnHair);
        Button btnEyeshadow = findViewById(R.id.btnEyeshadow);
        Button btnBlush = findViewById(R.id.btnBlush);
        Button btnLips = findViewById(R.id.btnLips);
        Button btnClothes = findViewById(R.id.btnClothes);
        Button btnVirtualMakeup = findViewById(R.id.btnVirtualMakeup);
        virtualMakeupImageView = findViewById(R.id.virtualMakeupImageView);

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Applying virtual makeup...");
        progressDialog.setCancelable(false);

        btnHair.setOnClickListener(view -> updateColor(btnHair));
        btnEyeshadow.setOnClickListener(view -> updateColor(btnEyeshadow));
        btnBlush.setOnClickListener(view -> updateColor(btnBlush));
        btnLips.setOnClickListener(view -> updateColor(btnLips));
        btnClothes.setOnClickListener(view -> updateColor(btnClothes));
        btnVirtualMakeup.setOnClickListener(view -> sendVirtualMakeupRequest());
    }

    private void updateColor(Button button) {
        String color = Integer.toHexString(((ColorDrawable) button.getBackground()).getColor());
        int id = button.getId();
        if (id == R.id.btnHair) {
            hairColor = color;
        } else if (id == R.id.btnEyeshadow) {
            eyeshadowColor = color;
        } else if (id == R.id.btnBlush) {
            blushColor = color;
        } else if (id == R.id.btnLips) {
            lipsColor = color;
        } else if (id == R.id.btnClothes) {
            clothesColor = color;
        }
    }

    private void sendVirtualMakeupRequest() {
        progressDialog.show();
        new Thread(() -> {
            try {
                Socket socket = SocketManager.getInstance().getSocket(); // Get the existing socket

                if (socket != null && socket.isConnected()) {
                    Log.d("VirtualTryOn", "Socket is connected");

                    // Send the "VirtualMakeup" string to the server
                    DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream());
                    dataOutputStream.writeUTF("VirtualMakeup");
                    dataOutputStream.flush();
                    Log.d("VirtualTryOn", "Sent VirtualMakeup request to the server");

                    // Send the color data to the server
                    sendColorData(dataOutputStream, blushColor);
                    sendColorData(dataOutputStream, eyeshadowColor);
                    sendColorData(dataOutputStream, hairColor);
                    sendColorData(dataOutputStream, lipsColor);

                    // Receive the length of the image data
                    DataInputStream dataInputStream = new DataInputStream(socket.getInputStream());
                    int length = dataInputStream.readInt();
                    Log.d("VirtualTryOn", "Received image length: " + length);

                    if (length > 0) {
                        // Receive the image data
                        byte[] imageData = new byte[length];
                        dataInputStream.readFully(imageData);
                        Log.d("VirtualTryOn", "Received image data of length: " + imageData.length);

                        // Convert the byte data to a Bitmap
                        Bitmap bitmap = BitmapFactory.decodeByteArray(imageData, 0, length);
                        Log.d("VirtualTryOn", "Converted image data to Bitmap");

                        // Update the UI with the received image
                        runOnUiThread(() -> {
                            progressDialog.dismiss();
                            virtualMakeupImageView.setImageBitmap(bitmap);
                            Log.d("VirtualTryOn", "Displayed virtual makeup image");
                            new AlertDialog.Builder(VirtualTryOn.this)
                                    .setTitle("Success")
                                    .setMessage("Successfully received an image from the server.")
                                    .setPositiveButton("OK", null)
                                    .show();
                        });
                    } else {
                        Log.e("VirtualTryOn", "Received image length is 0 or negative");
                        throw new IOException("Received image length is 0 or negative.");
                    }
                } else {
                    Log.e("VirtualTryOn", "Socket is null or not connected");
                    throw new IOException("Socket is null or not connected");
                }
            } catch (Exception e) {
                Log.e("VirtualTryOn", "Error in virtual makeup request", e);
                String errorMessage = e.getMessage() != null ? e.getMessage() : "Unknown error";
                runOnUiThread(() -> {
                    progressDialog.dismiss();
                    Toast.makeText(VirtualTryOn.this, "Failed to apply virtual makeup: " + errorMessage, Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    private void sendColorData(DataOutputStream dataOutputStream, String color) throws IOException {
        int colorInt = (int) Long.parseLong(color, 16);
        int blue = (colorInt) & 0xFF;
        int green = (colorInt >> 8) & 0xFF;
        int red = (colorInt >> 16) & 0xFF;
        dataOutputStream.writeUTF(blue + "," + green + "," + red);
        dataOutputStream.flush();
    }
}
