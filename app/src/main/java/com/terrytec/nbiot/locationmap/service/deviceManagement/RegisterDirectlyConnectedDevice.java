package com.terrytec.nbiot.locationmap.service.deviceManagement;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.terrytec.nbiot.locationmap.Constant;
import com.terrytec.nbiot.locationmap.GlobalVariable;
import com.terrytec.nbiot.locationmap.MainActivity;
import com.terrytec.nbiot.locationmap.classes.EnumJSONType;
import com.terrytec.nbiot.locationmap.classes.JSONFactory;
import com.terrytec.nbiot.locationmap.classes.OkHttpManger;
import com.terrytec.nbiot.locationmap.classes.ThreadToast;

import org.json.JSONException;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import static com.terrytec.nbiot.locationmap.classes.OkHttpManger.JSON_TYPE;

/**
 * Register Directly Connected Device :
 * This interface is used by NAs to register devices on the IoT platform.
 * After the registration is successful,
 * the IoT platform allocates a device ID for the device,which is used as the unique identifier of the device.
 * Unregistered devices are not allowed to access the IoT platform.
 * In NB-IoT scenarios, the Set device info interface needs to be invoked to set device information after the registration is successful.
 */
public class RegisterDirectlyConnectedDevice implements Runnable {
    private OkHttpManger mOkHttpManager = null;

    public void setmOkHttpManager(OkHttpManger manager) {
        this.mOkHttpManager = manager;
    }

    @Override
    public void run() {

        //Please make sure that the following parameter values have been modified in the Constant file.
        String appId = Constant.APPID;
        String urlReg = Constant.REGISTER_DEVICE;

        //please replace the verifyCode and nodeId and timeout, when you use the demo.
        String verifyCode = "868744030976090";
        String nodeId = verifyCode;
        Integer timeout = 0;

        Map<String, String> header = new HashMap<>();
        header.put(Constant.HEADER_APP_KEY, appId);
        header.put(Constant.HEADER_APP_AUTH, "Bearer" + " " + GlobalVariable.AccessToken);

        try {
            Response responseReg = doPostJsonGetStatusLine(urlReg, header, JSONFactory.CreatJSON(EnumJSONType.REGISTER_DIRECTLY_CONNECTED_DEVICE));
            Log.i("MainActivity", "Register:" + responseReg);

            ThreadToast.Toast(MainActivity.getMainActivity(), "注册设备", Toast.LENGTH_SHORT);
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Response doPostJsonGetStatusLine(
            String url, Map<String, String> headerMap, String content) throws IOException {
        final RequestBody requestBody = RequestBody.create(JSON_TYPE, content);
        final Request request = new Request.Builder()
                .url(url)
                .addHeader(Constant.HEADER_APP_KEY, headerMap.get(Constant.HEADER_APP_KEY))
                .addHeader(Constant.HEADER_APP_AUTH, headerMap.get(Constant.HEADER_APP_AUTH))
                .post(requestBody)
                .build();
        if (mOkHttpManager != null) {
            OkHttpClient okHttpClient = mOkHttpManager.getOkHttpClient();
            if (okHttpClient != null) {
                return okHttpClient.newCall(request).execute();
            }
        }
        return null;
    }
}
