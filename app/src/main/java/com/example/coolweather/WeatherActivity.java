package com.example.coolweather;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.coolweather.gson.AQI;
import com.example.coolweather.gson.Forecast;
import com.example.coolweather.gson.Lifestyle;
import com.example.coolweather.gson.Weather;
import com.example.coolweather.service.AutoUpdateService;
import com.example.coolweather.util.HttpUtil;
import com.example.coolweather.util.Utility;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class WeatherActivity extends AppCompatActivity {

    public DrawerLayout drawerLayout;

    private Button navButton;

    public SwipeRefreshLayout swipeRefreshLayout;

    private ScrollView weatherLayout;

    private TextView titleCity;

    private TextView titleUpdateTime;

    private TextView degreeText;

    private TextView weatherInfoText;

    private LinearLayout forecastLayout;

    private TextView aqiTest;

    private TextView pm25Text;

    private TextView comfortText;

    private TextView carWashText;

    private TextView sportText;

    private String mWeatherId;

    private String mParentCity;

    private ImageView bingPicImg;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //将背景图和状态栏融合在一起
        if (Build.VERSION.SDK_INT>=21){
            View decorView = getWindow().getDecorView();
            decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN|View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
            //将状态栏设为透明色
            getWindow().setStatusBarColor(Color.TRANSPARENT);
        }
        setContentView(R.layout.activity_weather);


        //初始化各个控件
        weatherLayout = findViewById(R.id.weather_layout);
        titleCity = findViewById(R.id.title_city);
        titleUpdateTime = findViewById(R.id.title_update_time);
        degreeText = findViewById(R.id.degree_text);
        weatherInfoText = findViewById(R.id.weather_info_text);
        forecastLayout = findViewById(R.id.forecast_layout);
        aqiTest = findViewById(R.id.aqi_txt);
        pm25Text = findViewById(R.id.pm25_txt);
        comfortText = findViewById(R.id.comfort_txt);
        carWashText = findViewById(R.id.car_wash_txt);
        sportText = findViewById(R.id.sport_txt);
        bingPicImg = findViewById(R.id.bing_pic_img);
        swipeRefreshLayout = findViewById(R.id.swipe_refresh);
        swipeRefreshLayout.setColorSchemeResources(R.color.colorPrimary);
        drawerLayout = findViewById(R.id.drawer_layout);
        navButton = findViewById(R.id.nav_button);

        //加载并显示天气数据
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String weatherString = prefs.getString("weather",null);
        String aqiString = prefs.getString("aqi",null);

        if (weatherString != null && aqiString != null){
            //有缓存时直接解析天气数据
            Weather weather = Utility.handleWeatherResponse(weatherString);
            AQI aqi = Utility.handleAQIResponse(aqiString);
            mWeatherId = weather.basic.weatherId;
            mParentCity = aqi.basic.parentCity;
            showWeatherInfo(weather);
            showAQIInfo(aqi);

        }else{
            //无缓存时去服务器查询天气
            mWeatherId = getIntent().getStringExtra("weather_id");
            mParentCity = getIntent().getStringExtra("parent_city");
            weatherLayout.setVisibility(View.INVISIBLE);
            requestWeather(mWeatherId,mParentCity);
        }

        //设置下拉刷新监听器
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                requestWeather(mWeatherId,mParentCity);
            }
        });

        //缓存背景图片
        String bingPic = prefs.getString("bing_pic",null);
        if (bingPic!=null){
            Glide.with(this).load(bingPic).into(bingPicImg);
        }else {
            loadBingPic();
        }

        //设置菜单按钮的监听器
        navButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                drawerLayout.openDrawer(GravityCompat.START);
            }
        });
    }

    /**
     * 根据天气id请求城市天气信息
     */

    public void requestWeather(final String weatherId,final String parentCity){
        String weatherUrl = "https://free-api.heweather.net/s6/weather?location=" +weatherId+"&key=2aa5062b550c4700aa97e45cc7714dc2";
        String aqiUrl = "https://free-api.heweather.net/s6/air/now?location=" + parentCity + "&key=2aa5062b550c4700aa97e45cc7714dc2";

        //解析aqiUrl
        HttpUtil.sendOkHttpRequest(aqiUrl, new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                e.printStackTrace();
                swipeRefreshLayout.setRefreshing(false);
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                final String responseText = response.body().string();
                final AQI aqi = Utility.handleAQIResponse(responseText);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if(aqi != null && "ok".equals(aqi.status)){
                            SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(WeatherActivity.this).edit();
                            editor.putString("aqi",responseText);
                            editor.apply();
                            mParentCity = aqi.basic.parentCity;
                            showAQIInfo(aqi);
                        }else{
                            Toast.makeText(WeatherActivity.this, "获取AQI信息失败", Toast.LENGTH_SHORT).show();
                        }
                        swipeRefreshLayout.setRefreshing(false);
                    }
                });
            }
        });

        //解析weatherUrl
        HttpUtil.sendOkHttpRequest(weatherUrl, new Callback() {
            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                final String responseText = response.body().string();
                final Weather weather = Utility.handleWeatherResponse(responseText);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (weather != null && "ok".equals(weather.status)){
                            SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(WeatherActivity.this).edit();
                            editor.putString("weather",responseText);
                            editor.apply();
                            mWeatherId = weather.basic.weatherId;
                            showWeatherInfo(weather);
                        }else{
                            Toast.makeText(WeatherActivity.this, "获取天气信息error", Toast.LENGTH_SHORT).show();
                        }
                        swipeRefreshLayout.setRefreshing(false);
                    }
                });
            }

            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                e.printStackTrace();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(WeatherActivity.this, "获取天气信息错误", Toast.LENGTH_SHORT).show();
                        swipeRefreshLayout.setRefreshing(false);
                    }
                });
            }


        });

        //加载必应每日一图
        loadBingPic();
    }

    /**
     * 处理并展示Weather实体类中的数据
     */

    public void showWeatherInfo(Weather weather){
        //NOW数据
        String cityName = weather.basic.cityName;
        String updateTime = weather.update.updateTime.split(" ")[1];
        String degree = weather.now.temperature+"℃";
        String weatherInfo = weather.now.info;

        titleCity.setText(cityName);
        titleUpdateTime.setText(updateTime);
        degreeText.setText(degree);
        weatherInfoText.setText(weatherInfo);


        //展示未来天气的预报  Forcast数据
        forecastLayout.removeAllViews();
        for(Forecast forecast:weather.forecastList){
            View view = LayoutInflater.from(this).inflate(R.layout.weather_forecast_item,forecastLayout,false);

            TextView dataText = view.findViewById(R.id.data_txt);
            TextView infoText = view.findViewById(R.id.info_txt);
            TextView maxText = view.findViewById(R.id.max_txt);
            TextView minText = view.findViewById(R.id.min_txt);
            dataText.setText(forecast.date);
            //infoText.setText(forecast.info);


            //判断当前是白天还是晚上
            if(getCurrentTime()){
                //晚上
                infoText.setText(forecast.info_n);
            }else{
                //白天
                infoText.setText(forecast.info_d);
            }


            maxText.setText(forecast.max);
            minText.setText(forecast.min);
            forecastLayout.addView(view);
        }


        //展示生活建议 lifestyle数据
        for (Lifestyle lifestyle:weather.lifestyleList) {
            if (lifestyle.type.equals("comf")) {
                String comfort = "舒适度：" + lifestyle.info;
                comfortText.setText(comfort);
            } else if (lifestyle.type.equals("cw")) {
                String carWash = "洗车指数：" + lifestyle.info;
                carWashText.setText(carWash);
            } else if (lifestyle.type.equals("sport")) {
                String sport = "运动指数：" + lifestyle.info;
                sportText.setText(sport);
            }
        }

        weatherLayout.setVisibility(View.VISIBLE);

        //后台更新服务
        Intent intent = new Intent(this, AutoUpdateService.class);
        startService(intent);
    }

    /**
     * 处理并展示AQI实体类中的数据
     */
    private void showAQIInfo(AQI aqi){
        //AQI数据
        if (aqi!=null){
            aqiTest.setText(aqi.city.aqi);
            pm25Text.setText(aqi.city.pm25);
        }
    }

    /**
     * 加载必应每日一图
     */
    private void loadBingPic(){
        String requestBingPic = "http://guolin.tech/api/bing_pic";
        HttpUtil.sendOkHttpRequest(requestBingPic, new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                final String bingPic = response.body().string();
                SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(WeatherActivity.this).edit();
                editor.putString("bing_pic",bingPic);
                editor.apply();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Glide.with(WeatherActivity.this).load(bingPic).into(bingPicImg);
                    }
                });
            }
        });
    }


    public boolean getCurrentTime(){
        SimpleDateFormat sdf = new SimpleDateFormat("HH");
        String hour= sdf.format(new Date());
        int k  = Integer.parseInt(hour)  ;
        if ((k>=0 && k<6) ||(k >=18 && k<24)){
            return true;
        } else {
            return false;
        }
    }

}
