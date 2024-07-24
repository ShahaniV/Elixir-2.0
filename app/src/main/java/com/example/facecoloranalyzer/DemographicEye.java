package com.example.facecoloranalyzer;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class DemographicEye extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.demographic_eye);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        Button blue_btn = findViewById(R.id.blue);
        Button green_btn = findViewById(R.id.green);
        Button hazel_btn = findViewById(R.id.hazel);
        Button brown_btn = findViewById(R.id.brown);
        Button grey_btn = findViewById(R.id.grey);
        Button black_btn = findViewById(R.id.black);

        View.OnClickListener buttonClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(DemographicEye.this, MainActivity.class));
                finish(); // Optional: Close the current activity if needed
            }
        };

        blue_btn.setOnClickListener(buttonClickListener);
        green_btn.setOnClickListener(buttonClickListener);
        hazel_btn.setOnClickListener(buttonClickListener);
        brown_btn.setOnClickListener(buttonClickListener);
        grey_btn.setOnClickListener(buttonClickListener);
        black_btn.setOnClickListener(buttonClickListener);

    }
}