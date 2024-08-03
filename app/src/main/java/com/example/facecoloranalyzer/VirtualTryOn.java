package com.example.facecoloranalyzer;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
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

    private String hairColor = "000000";       // Default BGR color 0,0,0
    private String eyeshadowColor = "000000";  // Default BGR color 0,0,0
    private String blushColor = "000000";      // Default BGR color 0,0,0
    private String lipsColor = "000000";       // Default BGR color 0,0,0

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
        Button btnVirtualMakeup = findViewById(R.id.btnVirtualMakeup);
        virtualMakeupImageView = findViewById(R.id.virtualMakeupImageView);

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Applying virtual makeup...");
        progressDialog.setCancelable(false);

        btnHair.setOnClickListener(view -> updateColor(btnHair));
        btnEyeshadow.setOnClickListener(view -> updateColor(btnEyeshadow));
        btnBlush.setOnClickListener(view -> updateColor(btnBlush));
        btnLips.setOnClickListener(view -> updateColor(btnLips));
        btnVirtualMakeup.setOnClickListener(view -> sendVirtualMakeupRequest());
    }

    private void updateColor(Button button) {
        ColorDrawable buttonBackground = (ColorDrawable) button.getBackground();
        if (buttonBackground != null) {
            String color = Integer.toHexString(buttonBackground.getColor());
            int id = button.getId();
            if (id == R.id.btnHair) {
                hairColor = color;
            } else if (id == R.id.btnEyeshadow) {
                eyeshadowColor = color;
            } else if (id == R.id.btnBlush) {
                blushColor = color;
            } else if (id == R.id.btnLips) {
                lipsColor = color;
            }
        } else {
            runOnUiThread(() -> Toast.makeText(this, "Button background is not a color", Toast.LENGTH_SHORT).show());
        }
    }

    private void sendVirtualMakeupRequest() {
        progressDialog.show();
        new Thread(() -> {
            try {
                Socket socket = SocketManager.getInstance().getSocket(); // Get the existing socket

                if (socket != null && socket.isConnected()) {
                    runOnUiThread(() -> Toast.makeText(this, "Socket is connected", Toast.LENGTH_SHORT).show());

                    // Send the integer value 283 to the server
                    DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream());
                    dataOutputStream.writeInt(283);
                    dataOutputStream.flush();
                    runOnUiThread(() -> Toast.makeText(this, "Sent VirtualMakeup request to the server", Toast.LENGTH_SHORT).show());

                    // Send the color data to the server
                    sendColorData(dataOutputStream, blushColor);
                    sendColorData(dataOutputStream, eyeshadowColor);
                    sendColorData(dataOutputStream, hairColor);
                    sendColorData(dataOutputStream, lipsColor);  // Ensure lipsColor is also sent
                    runOnUiThread(() -> Toast.makeText(this, "Sent all color data", Toast.LENGTH_SHORT).show());

                    // Receive the length of the image data
                    DataInputStream dataInputStream = new DataInputStream(socket.getInputStream());
                    int length = dataInputStream.readInt();

                    if (length > 0) {
                        // Receive the image data
                        byte[] imageData = new byte[length];
                        dataInputStream.readFully(imageData);

                        // Convert the byte data to a Bitmap
                        Bitmap bitmap = BitmapFactory.decodeByteArray(imageData, 0, length);

                        // Update the UI with the received image
                        runOnUiThread(() -> {
                            progressDialog.dismiss();
                            virtualMakeupImageView.setImageBitmap(bitmap);
                            new AlertDialog.Builder(VirtualTryOn.this)
                                    .setTitle("Success")
                                    .setMessage("Successfully received an image from the server.")
                                    .setPositiveButton("OK", null)
                                    .show();
                        });
                    } else {
                        throw new IOException("Received image length is 0 or negative.");
                    }
                } else {
                    runOnUiThread(() -> Toast.makeText(VirtualTryOn.this, "Socket is null or not connected", Toast.LENGTH_SHORT).show());
                    throw new IOException("Socket is null or not connected");
                }
            } catch (Exception e) {
                String errorMessage = e.getMessage() != null ? e.getMessage() : "Unknown error";
                runOnUiThread(() -> {
                    progressDialog.dismiss();
                    Toast.makeText(VirtualTryOn.this, "Failed to apply virtual makeup: " + errorMessage, Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    private void sendColorData(DataOutputStream dataOutputStream, String color) throws IOException {
        if (color != null && color.length() == 6) {
            // Parse the color string (assumed to be in hexadecimal format) into an integer
            int colorInt = (int) Long.parseLong(color, 16);

            // Extract the blue, green, and red components from the integer
            int blue = (colorInt) & 0xFF;
            int green = (colorInt >> 8) & 0xFF;
            int red = (colorInt >> 16) & 0xFF;

            // Format the color components as a string, separated by commas
            String colorString = blue + "," + green + "," + red;

            // Send the formatted color string to the server
            dataOutputStream.writeUTF(colorString);  // Use writeUTF to ensure proper string encoding
            dataOutputStream.flush();

            // Display a toast message to indicate the color data was sent
            runOnUiThread(() -> Toast.makeText(this, "Sent color data: " + colorString, Toast.LENGTH_SHORT).show());
        } else {
            // Display an error message if the color is null or has an invalid length
            runOnUiThread(() -> Toast.makeText(this, "Color is null or invalid", Toast.LENGTH_SHORT).show());
        }
    }
}
