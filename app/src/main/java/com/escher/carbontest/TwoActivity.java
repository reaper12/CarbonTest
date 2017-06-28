package com.escher.carbontest;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

/**
 * Created by HeTao on 17-6-28 下午8:01
 */

/**
 * Created by HeTao on 17-6-28 下午8:00
 */

public class TwoActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_two);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d("TwoActivity","onDestroy");
    }
}
