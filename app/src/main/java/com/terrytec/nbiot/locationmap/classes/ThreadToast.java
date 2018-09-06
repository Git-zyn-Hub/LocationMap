package com.terrytec.nbiot.locationmap.classes;

import android.content.Context;
import android.os.Looper;
import android.widget.Toast;

import com.terrytec.nbiot.locationmap.MainActivity;

public class ThreadToast {
    public static void Toast(Context context, String msg, int duration) {
        Looper.prepare();
        Toast.makeText(context, msg, duration).show();
        Looper.loop();
    }
}
