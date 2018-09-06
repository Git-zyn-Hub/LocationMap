package com.terrytec.nbiot.locationmap.classes;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.util.Log;


import com.terrytec.nbiot.locationmap.Constant;
import com.terrytec.nbiot.locationmap.MainActivity;

import org.json.JSONException;
import org.json.JSONObject;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.CertificateFactory;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

import okhttp3.Call;
import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class OkHttpManger {

    private Handler okHttpHandler;
    private OkHttpClient mOkHttpClient;
    private static OkHttpManger mManager;
    private Context mContext;
    public static final MediaType JSON_TYPE = MediaType.parse("application/json; charset=utf-8");

    private OkHttpManger(Context context) {
//        this.mOkHttpClient = client;
        this.mContext = context;
        this.okHttpHandler = new Handler(Looper.getMainLooper());
    }

    public static final OkHttpManger getInstance(Context context) {
        if (mManager == null) {
            mManager = new OkHttpManger(context);
        }
        return mManager;
    }


    public Response postSync(String url, Map<String, String> params) throws IOException {
        final Request request = buildPostRequst(url, params);
        final Call call = mOkHttpClient.newCall(request);
        return call.execute();
    }

    public Response postSyncJson(String url, String json) throws IOException {
        final RequestBody requestBody = RequestBody.create(JSON_TYPE,json);
        final Request request = new Request.Builder().url(url).post(requestBody).build();
        return mOkHttpClient.newCall(request).execute();
    }
    private Request buildPostRequst(String url, Map<String, String> params) {
        Request request = null;
        if (params == null) {
            params = new HashMap<>();
        }
        if (params != null) {
            Set<Map.Entry<String, String>> entries = params.entrySet();
            FormBody.Builder builder = new FormBody.Builder();
            for (Map.Entry<String, String> entry : entries) {
                builder.add(entry.getKey(), entry.getValue());
            }
            request = new Request.Builder().url(url).post(builder.build()).build();
        }
        return request;
    }

    public Handler getOkHttpHandler() {
        return okHttpHandler;
    }

    public OkHttpClient getOkHttpClient(){
        return mOkHttpClient;
    }

    public void cert() throws Exception {

        // 服务器端需要验证的客户端证书
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        // 客户端信任的服务器端证书
        KeyStore trustStore = KeyStore.getInstance("BKS");

        InputStream ksIn = mContext.getAssets().open(Constant.SELFCERTPATH);
        InputStream tsIn = mContext.getAssets().open(Constant.TRUSTCAPATH);
        try {
            keyStore.load(ksIn, Constant.SELFCERTPWD.toCharArray());
            trustStore.load(tsIn, Constant.TRUSTCAPWD.toCharArray());

            setCertificates(trustStore, keyStore, tsIn);

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                ksIn.close();
            } catch (Exception ignore) {
            }
            try {
                tsIn.close();
            } catch (Exception ignore) {
            }
        }
//        MainActivity.AuthThread authThread = new MainActivity.AuthThread();
//        authThread.setClient(this.mOkHttpClient);
//        Thread auth_Thread = new Thread(authThread);
//        auth_Thread.start();
    }

    public void setCertificates(KeyStore trustStore, KeyStore keyStore, InputStream... certificates) {
        try {
            CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509", "BC");
            int index = 0;
            for (InputStream certificate : certificates) {
                String certificateAlias = Integer.toString(index++);
                trustStore.setCertificateEntry(certificateAlias, certificateFactory.
                        generateCertificate(certificate));
                try {
                    if (certificate != null)
                        certificate.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            SSLContext sslContext = SSLContext.getInstance("TLS");
            TrustManagerFactory trustManagerFactory =
                    TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init(trustStore);

            KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory
                    .getDefaultAlgorithm());
            keyManagerFactory.init(keyStore, Constant.SELFCERTPWD.toCharArray());
            sslContext.init(keyManagerFactory.getKeyManagers(), trustManagerFactory.getTrustManagers(), new SecureRandom());
//            okHttpClient.setSslSocketFactory(sslContext.getSocketFactory());
//            this.okHttpClient = new OkHttpClient.Builder()
//                    .sslSocketFactory(sslContext.getSocketFactory())
//                    .connectTimeout(10, TimeUnit.SECONDS)
//                    .readTimeout(10, TimeUnit.SECONDS)
//                    .build();
            this.mOkHttpClient = new OkHttpClient.Builder()
                    .connectTimeout(10, TimeUnit.SECONDS)
                    .sslSocketFactory(sslContext.getSocketFactory(), new TrustAllCerts())
                    .hostnameVerifier(new TrustAllCerts.TrustAllHostnameVerifier())
                    .readTimeout(10, TimeUnit.SECONDS)
                    .build();
            Log.i("lhh", "setCertificates: ");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
