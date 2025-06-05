package com.example.lotto;

import android.app.Application;
import com.jakewharton.threetenabp.AndroidThreeTen;

public class MyApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        // ThreeTenABP 라이브러리 초기화
        AndroidThreeTen.init(this);
    }
}
