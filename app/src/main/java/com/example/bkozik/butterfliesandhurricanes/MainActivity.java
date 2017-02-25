package com.example.bkozik.butterfliesandhurricanes;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import com.choosemuse.libmuse.MuseManagerAndroid;

public class MainActivity extends AppCompatActivity {

    private MuseManagerAndroid manager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }
}
