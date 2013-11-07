package co.jp.fujixerox.FXWebRTC;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;

/**
 * Created with IntelliJ IDEA.
 * User: haiyang
 * Date: 10/22/13
 * Time: 1:46 PM
 * To change this template use File | Settings | File Templates.
 */
public class ApplicationEx extends Application {

    public static final String TAG="ApplicationEx";
    private static Context context;

    @Override
    public void onCreate() {

        ApplicationEx.context=getApplicationContext();



        super.onCreate();
    }


    public static Context getAppContext()
    {



        return ApplicationEx.context;


    }
}