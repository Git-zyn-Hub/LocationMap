package com.terrytec.nbiot.locationmap;

import android.graphics.Color;
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
import android.widget.TextView;
import android.widget.Toast;

import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.amap.api.location.AMapLocationListener;
import com.amap.api.maps2d.AMap;
import com.amap.api.maps2d.CameraUpdateFactory;
import com.amap.api.maps2d.LocationSource;
import com.amap.api.maps2d.MapView;
import com.amap.api.maps2d.model.BitmapDescriptorFactory;
import com.amap.api.maps2d.model.MyLocationStyle;
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
        implements NavigationView.OnNavigationItemSelectedListener, LocationSource,
        AMapLocationListener {

    private OkHttpClient okHttpClient;
    private static MainActivity mainActivity;
    private MapView mapView;
    private AMap aMap;
    private boolean mZoomOnce = false;
    private TextView mLocationErrText;
    private OnLocationChangedListener mListener;
    private AMapLocationClient mlocationClient;
    private AMapLocationClientOption mLocationOption;
    private static final int STROKE_COLOR = Color.argb(180, 3, 145, 255);
    private static final int FILL_COLOR = Color.argb(10, 0, 0, 180);


    public MainActivity() {
        mainActivity = this;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "没事笑一下", Snackbar.LENGTH_LONG)
                        .setAction("撤销", new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                Toast.makeText(MainActivity.this, "单击了撤销", Toast.LENGTH_SHORT).show();
                            }
                        }).show();
            }
        });

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close) {

            @Override
            public void onDrawerClosed(View drawerView) {
                super.onDrawerClosed(drawerView);
                NavigationView navigationView = drawerView.findViewById(R.id.nav_view);
                Menu menu = navigationView.getMenu();
                int size = menu.size();
                for (int i = 0; i < size; i++) {
                    menu.getItem(i).setChecked(false);
                }
//                Log.i("MainActivity", "menuSize:" + size);
            }
        };
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        mapView = findViewById(R.id.map);
        mapView.onCreate(savedInstanceState);
        initMap();
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

    /**
     * 初始化
     */
    private void initMap() {
        if (aMap == null) {
            aMap = mapView.getMap();
            setUpMap();
        }

        mLocationErrText = findViewById(R.id.location_errInfo_text);
        mLocationErrText.setVisibility(View.GONE);
    }

    /**
     * 设置一些amap的属性
     */
    private void setUpMap() {
        aMap.setLocationSource(this);// 设置定位监听
        aMap.getUiSettings().setMyLocationButtonEnabled(true);// 设置默认定位按钮是否显示
        aMap.setMyLocationEnabled(true);// 设置为true表示显示定位层并可触发定位，false表示隐藏定位层并不可触发定位，默认是false
        setupLocationStyle();
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
            } catch (JSONException ee) {
                ee.printStackTrace();
            } catch (IOException e) {
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
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
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

        if (id == R.id.set_fence) {

        } else if (id == R.id.nav_gallery) {

        } else if (id == R.id.nav_slideshow) {

        } else if (id == R.id.nav_manage) {

        } else if (id == R.id.nav_share) {

        } else if (id == R.id.nav_send) {

        }

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    private void setupLocationStyle() {
        // 自定义系统定位蓝点
        MyLocationStyle myLocationStyle = new MyLocationStyle();
        // 自定义定位蓝点图标
        myLocationStyle.myLocationIcon(BitmapDescriptorFactory.
                fromResource(R.drawable.gps_point));
        // 自定义精度范围的圆形边框颜色
        myLocationStyle.strokeColor(STROKE_COLOR);
        //自定义精度范围的圆形边框宽度
        myLocationStyle.strokeWidth(2);
        // 设置圆形的填充颜色
        myLocationStyle.radiusFillColor(FILL_COLOR);
        // 将自定义的 myLocationStyle 对象添加到地图上
        aMap.setMyLocationStyle(myLocationStyle);
    }

    /**
     * 定位成功后回调函数
     */
    @Override
    public void onLocationChanged(AMapLocation amapLocation) {
        if (mListener != null && amapLocation != null) {
            if (amapLocation.getErrorCode() == 0) {
                mLocationErrText.setVisibility(View.GONE);
                mListener.onLocationChanged(amapLocation);// 显示系统小蓝点
                if (!mZoomOnce) {
                    aMap.moveCamera(CameraUpdateFactory.zoomTo(18));
                    mZoomOnce = true;
                }
            } else {
                try {
                    String errText = "定位失败," + amapLocation.getErrorCode() + ": " + amapLocation.getErrorInfo();
                    Log.e("AmapErr", errText);
                    mLocationErrText.setVisibility(View.VISIBLE);
                    mLocationErrText.setText(errText);
                } catch (NullPointerException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 激活定位
     */
    @Override
    public void activate(OnLocationChangedListener listener) {
        mListener = listener;
        if (mlocationClient == null) {
            mlocationClient = new AMapLocationClient(this);
            mLocationOption = new AMapLocationClientOption();
            //设置定位监听
            mlocationClient.setLocationListener(this);
            //设置为高精度定位模式
            mLocationOption.setLocationMode(AMapLocationClientOption.AMapLocationMode.Hight_Accuracy);
            //设置定位参数
            mlocationClient.setLocationOption(mLocationOption);
            // 此方法为每隔固定时间会发起一次定位请求，为了减少电量消耗或网络流量消耗，
            // 注意设置合适的定位时间的间隔（最小间隔支持为2000ms），并且在合适时间调用stopLocation()方法来取消定位请求
            // 在定位结束后，在合适的生命周期调用onDestroy()方法
            // 在单次定位情况下，定位无论成功与否，都无需调用stopLocation()方法移除请求，定位sdk内部会移除
            mlocationClient.startLocation();
        }
    }

    /**
     * 停止定位
     */
    @Override
    public void deactivate() {
        mListener = null;
        if (mlocationClient != null) {
            mlocationClient.stopLocation();
            mlocationClient.onDestroy();
        }
        mlocationClient = null;
    }

    /**
     * 方法必须重写
     */
    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
    }

    /**
     * 方法必须重写
     */
    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
        deactivate();
    }

    /**
     * 方法必须重写
     */
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }

    /**
     * 方法必须重写
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
        if (null != mlocationClient) {
            mlocationClient.onDestroy();
        }
    }
}
