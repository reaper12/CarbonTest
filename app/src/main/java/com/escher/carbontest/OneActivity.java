package com.escher.carbontest;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

/**
 * Created by HeTao on 17-6-28 下午8:00
 */

public class OneActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_one);
    }

    public void onJump(View view){
        Intent intent=new Intent(this,TwoActivity.class);
        startActivity(intent);
    }


}
