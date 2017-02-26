package com.example.bkozik.butterfliesandhurricanes;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.TextView;

public class colorActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_color);

        Intent intent = getIntent();
        double[] eegArray = intent.getDoubleArrayExtra("EEG Array");

        TextView eeg1 = (TextView)findViewById(R.id.eeg1);
        TextView eeg2 = (TextView)findViewById(R.id.eeg2);
        TextView eeg3 = (TextView)findViewById(R.id.eeg3);
        TextView eeg4 = (TextView)findViewById(R.id.eeg4);

        eeg1.setText("1: " + String.format("%6.2f", eegArray[0]));
        eeg2.setText("2: " + String.format("%6.2f", eegArray[1]));
        eeg3.setText("3: " + String.format("%6.2f", eegArray[2]));
        eeg4.setText("4: " + String.format("%6.2f", eegArray[3]));

        ViewGroup layout = (ViewGroup) findViewById(R.id.activity_color);
        layout.addView();

    }
}
