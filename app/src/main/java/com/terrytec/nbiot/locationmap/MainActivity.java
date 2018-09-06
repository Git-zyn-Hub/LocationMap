package com.terrytec.nbiot.locationmap;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.amap.api.maps2d.MapView;
import com.terrytec.nbiot.locationmap.classes.EnumJSONType;
import com.terrytec.nbiot.locationmap.classes.JSONFactory;
import com.terrytec.nbiot.locationmap.classes.OkHttpManger;
import com.terrytec.nbiot.locationmap.classes.ThreadToast;
import com.terrytec.nbiot.locationmap.classes.TrustAllCerts;
import com.terrytec.nbiot.locationmap.service.deviceManagement.RegisterDirectlyConnectedDevice;

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

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;

import okhttp3.Call;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private OkHttpClient okHttpClient;
    private static MainActivity mainActivity;
    private MapView mapView;

    public MainActivity() {
        mainActivity = this;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        mapView = (MapView) findViewById(R.id.map);
        mapView.onCreate(savedInstanceState);

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {

                    OkHttpManger okHttpManger = OkHttpManger.getInstance(getApplicationContext());
                    okHttpManger.cert();

                    AuthThread authThread = new AuthThread();
                    Thread auth_Thread = new Thread(authThread);
                    auth_Thread.start();

//                    Thread.sleep(5000);
////                    new Thread(new RefreshTokenThread()).start();
//                    RegisterDirectlyConnectedDevice register = new RegisterDirectlyConnectedDevice();
//                    register.setmOkHttpManager(okHttpManger);
//                    new Thread(register).start();
                } catch (Exception e) {
                    Log.d("MainActivity", "++++++++++++++++");
                }
            }
        }).start();
    }

    public static MainActivity getMainActivity() {
        return mainActivity;
    }

    public class AuthThread implements Runnable {
        private String responseStr;
        private Handler mHandler;
        String appId = Constant.APPID;
        String secret = Constant.SECRET;
        String urlLogin = Constant.APP_AUTH;

        @Override
        public void run() {
            try {
                final HashMap<String, String> maps = new HashMap<>();
                maps.put("appId", appId);
                maps.put("secret", secret);

                OkHttpManger okHttpManger = OkHttpManger.getInstance(getApplicationContext());
                Response response = okHttpManger.postSync(urlLogin, maps);
                mHandler = okHttpManger.getOkHttpHandler();
                if (response.isSuccessful()) {
                    responseStr = response.body().string();
                    mHandler.sendEmptyMessage(1);
                    Log.i("MainActivity", "Auth:" + responseStr);
                    JSONObject accessToken = new JSONObject(responseStr);
                    GlobalVariable.AccessToken = accessToken.getString("accessToken");
                    GlobalVariable.RefreshToken = accessToken.getString("refreshToken");
                    GlobalVariable.ExpiresIn = accessToken.getInt("expiresIn");
                    ThreadToast.Toast(MainActivity.this, "成功鉴权", Toast.LENGTH_SHORT);
                }
            } catch (IOException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    public class RefreshTokenThread implements Runnable {
        private String responseStr;
        private Handler mHandler;
        String urlRefreshToken = Constant.REFRESH_TOKEN;

        @Override
        public void run() {
            try {
                OkHttpManger okHttpManger = OkHttpManger.getInstance(getApplicationContext());
                Response response = okHttpManger.postSyncJson(urlRefreshToken, JSONFactory.CreatJSON(EnumJSONType.REFRESH_TOKEN));
                mHandler = okHttpManger.getOkHttpHandler();
                if (response.isSuccessful()) {
                    responseStr = response.body().string();
                    mHandler.sendEmptyMessage(1);
                    Log.i("MainActivity", "RefreshToken:" + responseStr);
                    JSONObject accessToken = new JSONObject(responseStr);
                    GlobalVariable.AccessToken = accessToken.getString("accessToken");
                    GlobalVariable.RefreshToken = accessToken.getString("refreshToken");
                    GlobalVariable.ExpiresIn = accessToken.getInt("expiresIn");
                }
            } catch (IOException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_camera) {
            // Handle the camera action
        } else if (id == R.id.nav_gallery) {

        } else if (id == R.id.nav_slideshow) {

        } else if (id == R.id.nav_manage) {

        } else if (id == R.id.nav_share) {

        } else if (id == R.id.nav_send) {

        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }
}
