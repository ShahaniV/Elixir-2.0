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

public class DemographicHair extends AppCompatActivity{



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.demographic_hair);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        Button light_brown_btn = findViewById(R.id.light_brown);
        Button medium_brown_btn = findViewById(R.id.medium_brown);
        Button dark_brown_btn = findViewById(R.id.dark_brown);
        Button black_btn = findViewById(R.id.black);

        View.OnClickListener buttonClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(DemographicHair.this, DemographicEye.class));
                finish(); // Optional: Close the current activity if needed
            }
        };

        light_brown_btn.setOnClickListener(buttonClickListener);
        medium_brown_btn.setOnClickListener(buttonClickListener);
        dark_brown_btn.setOnClickListener(buttonClickListener);
        black_btn.setOnClickListener(buttonClickListener);
    }


}