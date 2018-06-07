package jp.naist.ubi_lab.kotsu;

import android.app.Application;
import android.content.Context;


public class Kotsu extends Application {

    private static Context context;

    @Override
    public void onCreate() {
        super.onCreate();
        Kotsu.context = getApplicationContext();
    }

    public static Context getAppContext() {
        return Kotsu.context;
    }

}
