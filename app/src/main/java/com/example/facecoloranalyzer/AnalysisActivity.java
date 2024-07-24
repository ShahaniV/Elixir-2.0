package com.example.facecoloranalyzer;

// ResultActivity.java

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class AnalysisActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_analysis);

        TextView resultTextView = findViewById(R.id.resultTextView);
        Button paletteBtn = findViewById(R.id.paletteButton);

        // Get the result from the intent
        String result = getIntent().getStringExtra("SEASONAL");
        resultTextView.setText(result);

        Bitmap receivedBitmap = getIntent().getParcelableExtra("imageBitmap");


        paletteBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Start the SecondActivity
                Intent intent = new Intent(AnalysisActivity.this, Output.class);
                intent.putExtra("SEASONAL", result);
                intent.putExtra("imageBitmap", receivedBitmap);
                startActivity(intent);
            }
        });

    }
}
