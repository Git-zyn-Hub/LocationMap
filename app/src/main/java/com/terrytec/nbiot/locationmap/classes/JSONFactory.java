package com.terrytec.nbiot.locationmap.classes;

import com.terrytec.nbiot.locationmap.Constant;
import com.terrytec.nbiot.locationmap.GlobalVariable;

import org.json.JSONException;
import org.json.JSONObject;

public class JSONFactory {
    public static String CreatJSON(EnumJSONType type) throws JSONException {
        String result = null;
        switch (type) {
            case REFRESH_TOKEN: {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("appId", Constant.APPID);
                jsonObject.put("secret", Constant.SECRET);
                jsonObject.put("refreshToken", GlobalVariable.RefreshToken);
                result = jsonObject.toString();
            }
            case REGISTER_DIRECTLY_CONNECTED_DEVICE: {

                JSONObject paramReg = new JSONObject();
                paramReg.put("verifyCode", GlobalVariable.verifyCode.toUpperCase());
                paramReg.put("nodeId", GlobalVariable.verifyCode.toUpperCase());
                paramReg.put("timeout", 0);
                result = paramReg.toString();
            }
        }
        return result;
    }
}
