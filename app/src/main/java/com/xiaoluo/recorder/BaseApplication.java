package com.xiaoluo.recorder;

import android.app.Application;

/**
 * author: xiaoluo 
 * date: 2017/5/11 16:16
 */
public class BaseApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        CrashHandler.getInstance().init(this);
    }
}
