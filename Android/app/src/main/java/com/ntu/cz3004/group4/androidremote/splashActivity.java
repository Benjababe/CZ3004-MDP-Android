package com.ntu.cz3004.group4.androidremote;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

public class splashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        Handler handler;
        setContentView(R.layout.activity_splash);

            handler=new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    Intent intent=new Intent(splashActivity.this,MainActivity.class);
                    startActivity(intent);
                    finish();
                }
            },1500);

        }
    }