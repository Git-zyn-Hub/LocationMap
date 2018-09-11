package com.terrytec.nbiot.locationmap;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.provider.Settings;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.amap.api.fence.GeoFence;
import com.amap.api.fence.GeoFenceClient;
import com.amap.api.fence.GeoFenceListener;
import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.amap.api.location.AMapLocationListener;
import com.amap.api.location.DPoint;
import com.amap.api.maps.AMap;
import com.amap.api.maps.CameraUpdateFactory;
import com.amap.api.maps.LocationSource;
import com.amap.api.maps.MapView;
import com.amap.api.maps.model.BitmapDescriptor;
import com.amap.api.maps.model.BitmapDescriptorFactory;
import com.amap.api.maps.model.CircleOptions;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.LatLngBounds;
import com.amap.api.maps.model.Marker;
import com.amap.api.maps.model.MarkerOptions;
import com.amap.api.maps.model.MyLocationStyle;
import com.amap.api.maps.model.PolygonOptions;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
        implements
        View.OnClickListener,
        AMap.OnMapClickListener,
        CompoundButton.OnCheckedChangeListener,
        android.widget.RadioGroup.OnCheckedChangeListener,
        NavigationView.OnNavigationItemSelectedListener,
        LocationSource,
        AMapLocationListener,
        GeoFenceListener {

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

    //电子围栏所需字段。
    private LinearLayout fence_option;
    private View lyOption;

    private TextView tvGuide;
    private TextView tvResult;

    private RadioGroup rgFenceType;

    private EditText etCustomId;
    private EditText etRadius;
    private EditText etPoiType;
    private EditText etKeyword;
    private EditText etCity;
    private EditText etFenceSize;

    private CheckBox cbAlertIn;
    private CheckBox cbAlertOut;
    private CheckBox cbAldertStated;

    private Button btAddFence;
    private Button btOption;

    // 中心点坐标
    private LatLng centerLatLng = null;

    // 多边形围栏的边界点
    private List<LatLng> polygonPoints = new ArrayList<LatLng>();

    private List<Marker> markerList = new ArrayList<Marker>();

    // 当前的坐标点集合，主要用于进行地图的可视区域的缩放
    private LatLngBounds.Builder boundsBuilder = new LatLngBounds.Builder();

    // 中心点marker
    private Marker centerMarker;
    private BitmapDescriptor ICON_YELLOW = BitmapDescriptorFactory
            .defaultMarker(BitmapDescriptorFactory.HUE_YELLOW);
    private BitmapDescriptor ICON_RED = BitmapDescriptorFactory
            .defaultMarker(BitmapDescriptorFactory.HUE_RED);
    private MarkerOptions markerOption = null;
    // 记录已经添加成功的围栏
    private HashMap<String, GeoFence> fenceMap = new HashMap<String, GeoFence>();

    // 地理围栏客户端
    private GeoFenceClient fenceClient = null;
    // 要创建的围栏半径
    private float fenceRadius = 0.0F;
    // 触发地理围栏的行为，默认为进入提醒
    private int activatesAction = GeoFenceClient.GEOFENCE_IN;
    // 地理围栏的广播action
    private static final String GEOFENCE_BROADCAST_ACTION = "com.terrytec.nbiot.locationmap";


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
        creatFenceVar();
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
            aMap.getUiSettings().setRotateGesturesEnabled(false);
            setUpMap();
        }

        mLocationErrText = findViewById(R.id.location_errInfo_text);
        mLocationErrText.setVisibility(View.GONE);
    }

    /**
     * 设置一些amap的属性
     */
    private void setUpMap() {
        aMap.setOnMapClickListener(this);
        aMap.setLocationSource(this);// 设置定位监听
        aMap.getUiSettings().setMyLocationButtonEnabled(true);// 设置默认定位按钮是否显示
        aMap.setMyLocationEnabled(true);// 设置为true表示显示定位层并可触发定位，false表示隐藏定位层并不可触发定位，默认是false
        // 设置定位的类型为定位模式 ，可以由定位、跟随或地图根据面向方向旋转几种
        //aMap.setMyLocationType(1);
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


    private void creatFenceVar() {
        fenceClient = new GeoFenceClient(getApplicationContext());
        fence_option = findViewById(R.id.fence_option);
        rgFenceType = (RadioGroup) findViewById(R.id.rg_fenceType);
        lyOption = findViewById(R.id.ly_option);
        btAddFence = (Button) findViewById(R.id.bt_addFence);
        btOption = (Button) findViewById(R.id.bt_option);
        tvGuide = (TextView) findViewById(R.id.tv_guide);
        tvResult = (TextView) findViewById(R.id.tv_result);
        tvResult.setVisibility(View.GONE);
        etCustomId = (EditText) findViewById(R.id.et_customId);
        etCity = (EditText) findViewById(R.id.et_city);
        etRadius = (EditText) findViewById(R.id.et_radius);
        etPoiType = (EditText) findViewById(R.id.et_poitype);
        etKeyword = (EditText) findViewById(R.id.et_keyword);
        etFenceSize = (EditText) findViewById(R.id.et_fenceSize);

        cbAlertIn = (CheckBox) findViewById(R.id.cb_alertIn);
        cbAlertOut = (CheckBox) findViewById(R.id.cb_alertOut);
        cbAldertStated = (CheckBox) findViewById(R.id.cb_alertStated);

        markerOption = new MarkerOptions().draggable(true);
        initFence();
    }


    void initFence() {

        rgFenceType.setVisibility(View.VISIBLE);
        btOption.setVisibility(View.VISIBLE);
        btOption.setText(getString(R.string.hideOption));
        resetView();
        resetView_round();

        rgFenceType.setOnCheckedChangeListener(this);
        btAddFence.setOnClickListener(this);
        btOption.setOnClickListener(this);
        cbAlertIn.setOnCheckedChangeListener(this);
        cbAlertOut.setOnCheckedChangeListener(this);
        cbAldertStated.setOnCheckedChangeListener(this);

        IntentFilter filter = new IntentFilter();
        filter.addAction(GEOFENCE_BROADCAST_ACTION);
        registerReceiver(mGeoFenceReceiver, filter);
        /**
         * 创建pendingIntent
         */
        fenceClient.createPendingIntent(GEOFENCE_BROADCAST_ACTION);
        fenceClient.setGeoFenceListener(this);
        /**
         * 设置地理围栏的触发行为,默认为进入
         */
        fenceClient.setActivateAction(GeoFenceClient.GEOFENCE_IN);
    }

    /**
     * 接收触发围栏后的广播,当添加围栏成功之后，会立即对所有围栏状态进行一次侦测，如果当前状态与用户设置的触发行为相符将会立即触发一次围栏广播；
     * 只有当触发围栏之后才会收到广播,对于同一触发行为只会发送一次广播不会重复发送，除非位置和围栏的关系再次发生了改变。
     */
    private BroadcastReceiver mGeoFenceReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // 接收广播
            if (intent.getAction().equals(GEOFENCE_BROADCAST_ACTION)) {
                Bundle bundle = intent.getExtras();
                String customId = bundle
                        .getString(GeoFence.BUNDLE_KEY_CUSTOMID);
                String fenceId = bundle.getString(GeoFence.BUNDLE_KEY_FENCEID);
                //status标识的是当前的围栏状态，不是围栏行为
                int status = bundle.getInt(GeoFence.BUNDLE_KEY_FENCESTATUS);
                StringBuffer sb = new StringBuffer();
                switch (status) {
                    case GeoFence.STATUS_LOCFAIL:
                        sb.append("定位失败");
                        break;
                    case GeoFence.STATUS_IN:
                        sb.append("进入围栏 ");
                        break;
                    case GeoFence.STATUS_OUT:
                        sb.append("离开围栏 ");
                        break;
                    case GeoFence.STATUS_STAYED:
                        sb.append("停留在围栏内 ");
                        break;
                    default:
                        break;
                }
                if (status != GeoFence.STATUS_LOCFAIL) {
                    if (!TextUtils.isEmpty(customId)) {
                        sb.append(" customId: " + customId);
                    }
                    sb.append(" fenceId: " + fenceId);
                }
                String str = sb.toString();
                Message msg = Message.obtain();
                msg.obj = str;
                msg.what = 2;
                handler.sendMessage(msg);
            }
        }
    };


    Handler handler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 0:
                    StringBuffer sb = new StringBuffer();
                    sb.append("添加围栏成功");
                    String customId = (String) msg.obj;
                    if (!TextUtils.isEmpty(customId)) {
                        sb.append("customId: ").append(customId);
                    }
                    Toast.makeText(getApplicationContext(), sb.toString(),
                            Toast.LENGTH_SHORT).show();
                    drawFence2Map();
                    break;
                case 1:
                    int errorCode = msg.arg1;
                    Toast.makeText(getApplicationContext(),
                            "添加围栏失败 " + errorCode, Toast.LENGTH_SHORT).show();
                    break;
                case 2:
                    String statusStr = (String) msg.obj;
                    tvResult.setVisibility(View.VISIBLE);
                    tvResult.append(statusStr + "\n");
                    break;
                default:
                    break;
            }
            setRadioGroupAble(true);
        }
    };


    private void setRadioGroupAble(boolean isEnable) {
        for (int i = 0; i < rgFenceType.getChildCount(); i++) {
            rgFenceType.getChildAt(i).setEnabled(isEnable);
        }
    }

    Object lock = new Object();

    void drawFence2Map() {
        new Thread() {
            @Override
            public void run() {
                try {
                    synchronized (lock) {
                        if (null == fenceList || fenceList.isEmpty()) {
                            return;
                        }
                        for (GeoFence fence : fenceList) {
                            if (fenceMap.containsKey(fence.getFenceId())) {
                                continue;
                            }
                            drawFence(fence);
                            fenceMap.put(fence.getFenceId(), fence);
                        }
                    }
                } catch (Throwable e) {

                }
            }
        }.start();
    }

    private void drawFence(GeoFence fence) {
        switch (fence.getType()) {
            case GeoFence.TYPE_ROUND:
            case GeoFence.TYPE_AMAPPOI:
                drawCircle(fence);
                break;
            case GeoFence.TYPE_POLYGON:
            case GeoFence.TYPE_DISTRICT:
                drawPolygon(fence);
                break;
            default:
                break;
        }

        // 设置所有maker显示在当前可视区域地图中
        LatLngBounds bounds = boundsBuilder.build();
        aMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, 150));
        polygonPoints.clear();
        removeMarkers();
    }


    private void drawCircle(GeoFence fence) {
        LatLng center = new LatLng(fence.getCenter().getLatitude(),
                fence.getCenter().getLongitude());
        // 绘制一个圆形
        aMap.addCircle(new CircleOptions().center(center)
                .radius(fence.getRadius()).strokeColor(Const.STROKE_COLOR)
                .fillColor(Const.FILL_COLOR).strokeWidth(Const.STROKE_WIDTH));
        boundsBuilder.include(center);
    }

    private void drawPolygon(GeoFence fence) {
        final List<List<DPoint>> pointList = fence.getPointList();
        if (null == pointList || pointList.isEmpty()) {
            return;
        }
        for (List<DPoint> subList : pointList) {
            List<LatLng> lst = new ArrayList<LatLng>();

            PolygonOptions polygonOption = new PolygonOptions();
            for (DPoint point : subList) {
                lst.add(new LatLng(point.getLatitude(), point.getLongitude()));
                boundsBuilder.include(
                        new LatLng(point.getLatitude(), point.getLongitude()));
            }
            polygonOption.addAll(lst);

            polygonOption.strokeColor(Const.STROKE_COLOR).strokeWidth(Const.STROKE_WIDTH)
                    .fillColor(Const.FILL_COLOR);
            aMap.addPolygon(polygonOption);
        }
    }


    List<GeoFence> fenceList = new ArrayList<GeoFence>();

    @Override
    public void onGeoFenceCreateFinished(final List<GeoFence> geoFenceList,
                                         int errorCode, String customId) {
        Message msg = Message.obtain();
        if (errorCode == GeoFence.ADDGEOFENCE_SUCCESS) {
            fenceList.addAll(geoFenceList);
            msg.obj = customId;
            msg.what = 0;
        } else {
            msg.arg1 = errorCode;
            msg.what = 1;
        }
        handler.sendMessage(msg);
    }


    @Override
    public void onMapClick(LatLng latLng) {
        markerOption.icon(ICON_YELLOW);
        switch (rgFenceType.getCheckedRadioButtonId()) {
            case R.id.rb_roundFence:
            case R.id.rb_nearbyFence:
                centerLatLng = latLng;
                addCenterMarker(centerLatLng);
                tvGuide.setBackgroundColor(
                        getResources().getColor(R.color.gary));
                tvGuide.setText("选中的坐标：" + centerLatLng.longitude + ","
                        + centerLatLng.latitude);
                break;
            case R.id.rb_polygonFence:
                if (null == polygonPoints) {
                    polygonPoints = new ArrayList<LatLng>();
                }
                polygonPoints.add(latLng);
                addPolygonMarker(latLng);
                tvGuide.setBackgroundColor(
                        getResources().getColor(R.color.gary));
                tvGuide.setText("已选择" + polygonPoints.size() + "个点");
                if (polygonPoints.size() >= 3) {
                    btAddFence.setEnabled(true);
                }
                break;

        }
    }


    private void addCenterMarker(LatLng latlng) {
        if (null == centerMarker) {
            centerMarker = aMap.addMarker(markerOption);
        }
        centerMarker.setPosition(latlng);
        centerMarker.setVisible(true);
        markerList.add(centerMarker);
    }

    // 添加多边形的边界点marker
    private void addPolygonMarker(LatLng latlng) {
        markerOption.position(latlng);
        Marker marker = aMap.addMarker(markerOption);
        markerList.add(marker);
    }

    private void removeMarkers() {
        if (null != centerMarker) {
            centerMarker.remove();
            centerMarker = null;
        }
        if (null != markerList && markerList.size() > 0) {
            for (Marker marker : markerList) {
                marker.remove();
            }
            markerList.clear();
        }
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        switch (buttonView.getId()) {
            case R.id.cb_alertIn:
                if (isChecked) {
                    activatesAction |= GeoFenceClient.GEOFENCE_IN;
                } else {
                    activatesAction = activatesAction
                            & (GeoFenceClient.GEOFENCE_OUT
                            | GeoFenceClient.GEOFENCE_STAYED);
                }
                break;
            case R.id.cb_alertOut:
                if (isChecked) {
                    activatesAction |= GeoFenceClient.GEOFENCE_OUT;
                } else {
                    activatesAction = activatesAction
                            & (GeoFenceClient.GEOFENCE_IN
                            | GeoFenceClient.GEOFENCE_STAYED);
                }
                break;
            case R.id.cb_alertStated:
                if (isChecked) {
                    activatesAction |= GeoFenceClient.GEOFENCE_STAYED;
                } else {
                    activatesAction = activatesAction
                            & (GeoFenceClient.GEOFENCE_IN
                            | GeoFenceClient.GEOFENCE_OUT);
                }
                break;
            default:
                break;
        }
        if (null != fenceClient) {
            fenceClient.setActivateAction(activatesAction);
        }
    }

    @Override
    public void onCheckedChanged(RadioGroup group, int checkedId) {
        removeMarkers();
        resetView();
        centerLatLng = null;
        btAddFence.setEnabled(true);
        switch (checkedId) {
            case R.id.rb_roundFence:
                resetView_round();
                break;
            case R.id.rb_polygonFence:
                resetView_polygon();
                break;
            case R.id.rb_keywordFence:
                resetView_keyword();
                break;
            case R.id.rb_nearbyFence:
                resetView_nearby();
                break;
            case R.id.rb_districeFence:
                resetView_district();
                break;

            default:
                break;
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.bt_addFence:
                setRadioGroupAble(false);
                addFence();
                break;
            case R.id.bt_option:
                fence_option.setVisibility(View.GONE);
                break;
            default:
                break;
        }
    }


    /**
     * 添加围栏
     *
     * @author hongming.wang
     * @since 3.2.0
     */
    private void addFence() {
        switch (rgFenceType.getCheckedRadioButtonId()) {
            case R.id.rb_roundFence:
                addRoundFence();
                break;
            case R.id.rb_polygonFence:
                addPolygonFence();
                break;
            case R.id.rb_keywordFence:
                addKeywordFence();
                break;
            case R.id.rb_nearbyFence:
                addNearbyFence();
                break;
            case R.id.rb_districeFence:
                addDistrictFence();
                break;
            default:
                break;
        }
    }


    /**
     * 添加圆形围栏
     *
     * @author hongming.wang
     * @since 3.2.0
     */
    private void addRoundFence() {
        String customId = etCustomId.getText().toString();
        String radiusStr = etRadius.getText().toString();
        if (null == centerLatLng || TextUtils.isEmpty(radiusStr)) {
            Toast.makeText(getApplicationContext(), "参数不全", Toast.LENGTH_SHORT)
                    .show();
            setRadioGroupAble(true);
            return;
        }
        DPoint centerPoint = new DPoint(centerLatLng.latitude,
                centerLatLng.longitude);
        fenceRadius = Float.parseFloat(radiusStr);
        fenceClient.addGeoFence(centerPoint, fenceRadius, customId);
    }

    /**
     * 添加多边形围栏
     *
     * @author hongming.wang
     * @since 3.2.0
     */
    private void addPolygonFence() {
        String customId = etCustomId.getText().toString();
        if (null == polygonPoints || polygonPoints.size() < 3) {
            Toast.makeText(getApplicationContext(), "参数不全", Toast.LENGTH_SHORT)
                    .show();
            setRadioGroupAble(true);
            btAddFence.setEnabled(true);
            return;
        }
        List<DPoint> pointList = new ArrayList<DPoint>();
        for (LatLng latLng : polygonPoints) {
            pointList.add(new DPoint(latLng.latitude, latLng.longitude));
        }
        fenceClient.addGeoFence(pointList, customId);
    }

    /**
     * 添加关键字围栏
     *
     * @author hongming.wang
     * @since 3.2.0
     */
    private void addKeywordFence() {
        String customId = etCustomId.getText().toString();
        String keyword = etKeyword.getText().toString();
        String city = etCity.getText().toString();
        String poiType = etPoiType.getText().toString();
        String sizeStr = etFenceSize.getText().toString();
        int size = 10;
        if (!TextUtils.isEmpty(sizeStr)) {
            try {
                size = Integer.parseInt(sizeStr);
            } catch (Throwable e) {
            }
        }

        if (TextUtils.isEmpty(keyword) && TextUtils.isEmpty(poiType)) {
            Toast.makeText(getApplicationContext(), "参数不全", Toast.LENGTH_SHORT)
                    .show();
            return;
        }
        fenceClient.addGeoFence(keyword, poiType, city, size, customId);
    }

    /**
     * 添加周边围栏
     *
     * @author hongming.wang
     * @since 3.2.0
     */
    private void addNearbyFence() {
        String customId = etCustomId.getText().toString();
        String searchRadiusStr = etRadius.getText().toString();
        String keyword = etKeyword.getText().toString();
        String poiType = etPoiType.getText().toString();
        String sizeStr = etFenceSize.getText().toString();
        int size = 10;
        if (!TextUtils.isEmpty(sizeStr)) {
            try {
                size = Integer.parseInt(sizeStr);
            } catch (Throwable e) {
            }
        }

        if (null == centerLatLng) {
            Toast.makeText(getApplicationContext(), "参数不全", Toast.LENGTH_SHORT)
                    .show();
            return;
        }
        DPoint centerPoint = new DPoint(centerLatLng.latitude,
                centerLatLng.longitude);
        float aroundRadius = Float.parseFloat(searchRadiusStr);
        fenceClient.addGeoFence(keyword, poiType, centerPoint, aroundRadius,
                size, customId);
    }

    /**
     * 添加行政区划围栏
     *
     * @author hongming.wang
     * @since 3.2.0
     */
    private void addDistrictFence() {
        String keyword = etKeyword.getText().toString();
        String customId = etCustomId.getText().toString();
        if (TextUtils.isEmpty(keyword)) {
            Toast.makeText(getApplicationContext(), "参数不全", Toast.LENGTH_SHORT)
                    .show();
            return;
        }
        fenceClient.addGeoFence(keyword, customId);
    }

    private void resetView() {
        etCustomId.setVisibility(View.VISIBLE);
        etCity.setVisibility(View.GONE);
        etFenceSize.setVisibility(View.GONE);
        etKeyword.setVisibility(View.GONE);
        etPoiType.setVisibility(View.GONE);
        etRadius.setVisibility(View.GONE);
        tvGuide.setVisibility(View.GONE);
    }

    private void resetView_round() {
        etRadius.setVisibility(View.VISIBLE);
        etRadius.setHint("围栏半径");
        tvGuide.setBackgroundColor(getResources().getColor(R.color.red));
        tvGuide.setText("请点击地图选择围栏的中心点");
        tvGuide.setVisibility(View.VISIBLE);
    }

    private void resetView_polygon() {
        tvGuide.setBackgroundColor(getResources().getColor(R.color.red));
        tvGuide.setText("请顺时针或逆时针点击地图选择围栏的边界点,至少3个点");
        tvGuide.setVisibility(View.VISIBLE);
        tvGuide.setVisibility(View.VISIBLE);
        polygonPoints = new ArrayList<LatLng>();
        btAddFence.setEnabled(false);
    }

    private void resetView_keyword() {
        etKeyword.setVisibility(View.VISIBLE);
        etPoiType.setVisibility(View.VISIBLE);
        etCity.setVisibility(View.VISIBLE);
        etFenceSize.setVisibility(View.VISIBLE);
    }

    private void resetView_nearby() {
        tvGuide.setText("请点击地图选择中心点");
        etRadius.setHint("周边半径");
        tvGuide.setVisibility(View.VISIBLE);
        etKeyword.setVisibility(View.VISIBLE);
        etRadius.setVisibility(View.VISIBLE);
        etPoiType.setVisibility(View.VISIBLE);
        etFenceSize.setVisibility(View.VISIBLE);
    }

    private void resetView_district() {
        etKeyword.setVisibility(View.VISIBLE);
    }


    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.set_fence) {
            fence_option.setVisibility(View.VISIBLE);
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
        //定位一次，且将视角移动到地图中心点。
        myLocationStyle.myLocationType(MyLocationStyle.LOCATION_TYPE_LOCATE);
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

        try {
            unregisterReceiver(mGeoFenceReceiver);
        } catch (Throwable e) {
        }

        if (null != fenceClient) {
            fenceClient.removeGeoFence();
        }
        if (null != mlocationClient) {
            mlocationClient.onDestroy();
        }
    }
}
