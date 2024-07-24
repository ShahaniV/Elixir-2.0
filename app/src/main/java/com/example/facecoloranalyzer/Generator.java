package com.example.facecoloranalyzer;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Random;

import yuku.ambilwarna.AmbilWarnaDialog;

public class Generator extends AppCompatActivity {

    private EditText hexInput;
    private LinearLayout analogousPalette;
    private LinearLayout relatedPalette;
    private Button generateButton;
    private Button randomButton;
    private Button pickColorButton;
    private int defaultColor;
    private ImageView avoidedColorsButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_generator);

        hexInput = findViewById(R.id.hexInput);
        analogousPalette = findViewById(R.id.analogousPalette);
        relatedPalette = findViewById(R.id.relatedPalette);
        generateButton = findViewById(R.id.generateButton);
        randomButton = findViewById(R.id.randomButton);
        pickColorButton = findViewById(R.id.pickColorButton);
        avoidedColorsButton = findViewById(R.id.avoidColors);

        defaultColor = Color.WHITE;

        Button backbtn = findViewById(R.id.backButton);
        backbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish(); // This will close the current activity and return to the previous one
            }
        });

        generateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String hex = hexInput.getText().toString();
                if (!hex.isEmpty()) {
                    generatePalettes(Color.parseColor(hex));
                }
            }
        });

        randomButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Random rnd = new Random();
                int color = Color.argb(255, rnd.nextInt(256), rnd.nextInt(256), rnd.nextInt(256));
                generatePalettes(color);
            }
        });

        pickColorButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openColorPickerDialog();
            }
        });

        // Receive array from intent
        String[] bestColorsArray = getIntent().getStringArrayExtra("BEST_COLORS");
        String result = getIntent().getStringExtra("SEASON");

        if (result != null) {
            if (result.contains("Autumn")) {
                avoidedColorsButton.setImageResource(R.drawable.autumn_avoid);
            } else if (result.contains("Summer")) {
                avoidedColorsButton.setImageResource(R.drawable.summer_avoid);
            } else if (result.contains("Spring")) {
                avoidedColorsButton.setImageResource(R.drawable.spring_avoid);
            } else if (result.contains("Winter")) {
                avoidedColorsButton.setImageResource(R.drawable.winter_avoid);
            } else {
                // If none of the specified seasons are found, assign a default image
                avoidedColorsButton.setImageResource(R.drawable.none);
            }
        } else {
            // Handle the case when 'result' is null
            avoidedColorsButton.setImageResource(R.drawable.none);
        }

        for (int i = 0; i < 6; i++) {
            int buttonId = getResources().getIdentifier("button" + (i + 1), "id", getPackageName());
            Button button = findViewById(buttonId);

            // Set background color from bestColorsArray
            String hexColor = bestColorsArray[i];
            int color = android.graphics.Color.parseColor(hexColor);
            button.setBackgroundColor(color);

            // Set click listener to copy hex value to clipboard
            final String hexValue = hexColor; // Final for use in onClick
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                    ClipData clip = ClipData.newPlainText("Hex Color", hexValue);
                    clipboard.setPrimaryClip(clip);
                    // Optionally provide feedback or toast message
                    // Toast.makeText(YourActivity.this, "Hex color copied: " + hexValue, Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void openColorPickerDialog() {
        AmbilWarnaDialog colorPickerDialog = new AmbilWarnaDialog(this, defaultColor, new AmbilWarnaDialog.OnAmbilWarnaListener() {
            @Override
            public void onCancel(AmbilWarnaDialog dialog) {
                // Do nothing
            }

            @Override
            public void onOk(AmbilWarnaDialog dialog, int color) {
                defaultColor = color;
                String hexColor = String.format("#%06X", (0xFFFFFF & color));
                hexInput.setText(hexColor);
                generatePalettes(color);
            }
        });
        colorPickerDialog.show();
    }

    private void generatePalettes(int color) {
        generateAnalogousPalette(color);
        generateRelatedPalette(color);
    }

    private void generateAnalogousPalette(int baseColor) {
        analogousPalette.removeAllViews();
        for (int i = -2; i <= 2; i++) {
            int analogousColor = shiftHue(baseColor, i * 30);
            addColorView(analogousPalette, analogousColor);
        }
    }

    private void generateRelatedPalette(int baseColor) {
        relatedPalette.removeAllViews();
        for (int i = 1; i <= 3; i++) {
            int tint = applyTint(baseColor, i * 30);
            int shade = applyShade(baseColor, i * 30);
            addColorView(relatedPalette, tint);
            addColorView(relatedPalette, shade);
        }
    }

    private void addColorView(LinearLayout layout, int color) {
        View view = new View(this);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, 200, 1);
        params.setMargins(4, 0, 4, 0);
        view.setLayoutParams(params);
        view.setBackgroundColor(color);
        layout.addView(view);
    }

    private int shiftHue(int color, float degrees) {
        float[] hsv = new float[3];
        Color.colorToHSV(color, hsv);
        hsv[0] = (hsv[0] + degrees) % 360;
        if (hsv[0] < 0) {
            hsv[0] += 360;
        }
        return Color.HSVToColor(hsv);
    }

    private int applyTint(int color, int amount) {
        int red = Color.red(color) + amount;
        int green = Color.green(color) + amount;
        int blue = Color.blue(color) + amount;
        return Color.rgb(Math.min(red, 255), Math.min(green, 255), Math.min(blue, 255));
    }

    private int applyShade(int color, int amount) {
        int red = Color.red(color) - amount;
        int green = Color.green(color) - amount;
        int blue = Color.blue(color) - amount;
        return Color.rgb(Math.max(red, 0), Math.max(green, 0), Math.max(blue, 0));
    }
}
