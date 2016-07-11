package com.lyz.communication.demo;

import android.app.Application;

import com.easemob.chat.EMChat;

/**
 * Created by liyanzhen on 16/6/25.
 */
public class TestApplication extends Application{
    @Override
    public void onCreate() {
        super.onCreate();
        EMChat.getInstance().init(this);
    }
}
