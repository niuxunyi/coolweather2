package com.example.coolweather2;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.media.Image;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.ListPreloader;
import com.example.coolweather2.gson.Forecast;
import com.example.coolweather2.gson.Weather;
import com.example.coolweather2.service.AutoUpdateService;
import com.example.coolweather2.util.HttpUtil;
import com.example.coolweather2.util.LogUtil;
import com.example.coolweather2.util.Utility;

import org.w3c.dom.Text;

import java.io.IOException;
import java.util.prefs.PreferenceChangeEvent;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

import static android.view.View.INVISIBLE;

public class WeatherActivity extends AppCompatActivity {
    private static final String TAG = "WeatherActivity";
    private ScrollView weatherLayout;
    private TextView titleCity;
    private TextView titleUpdateTime;
    private TextView degreeText;
    private TextView weatherInfoText;
    private LinearLayout forecastLayout;
    private TextView aqiText;
    private TextView pm25Text;
    private TextView comfortText;
    private TextView carWashText;
    private TextView sportText;
    private String weatherId;

    private ImageView bingPicImg;

    public SwipeRefreshLayout swipeRefresh; // 下拉刷新
    //private String mWeatherId;

    public DrawerLayout drawerLayout;
    private Button navButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (Build.VERSION.SDK_INT >= 21) {
            LogUtil.d(TAG, "SDK_INT>=21");
            View decorView = getWindow().getDecorView();  // 拿到当前活动的DecorView
            decorView.setSystemUiVisibility(    // 改变系统UI的显示
                    View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            );  // 让活动的布局显示在状态栏上
            getWindow().setStatusBarColor(Color.TRANSPARENT); //  将状态栏设置为透明色
        }

        setContentView(R.layout.activity_weather);


        // 初始化各个控件
        weatherLayout = (ScrollView) findViewById(R.id.weather_layout); //  在 layout/activity_weather.xml 中定义
        titleCity = (TextView) findViewById(R.id.title_city);  //  在 layout/title.xml 中定义
        titleUpdateTime = (TextView) findViewById(R.id.title_update_time);   //  同上
        degreeText = (TextView) findViewById(R.id.degree_text); // 在 layout/now.xml 中定义
        weatherInfoText = (TextView) findViewById(R.id.weather_info_text); // 同上
        forecastLayout = (LinearLayout) findViewById(R.id.forecast_layout); // 在 layout/forecast.xml 中定义
        aqiText = (TextView) findViewById(R.id.aqi_text);   // 在 layout/aqi.xml 中定义
        pm25Text = (TextView) findViewById(R.id.pm25_text);     // 同上
        comfortText = (TextView) findViewById(R.id.comfort_text);   // 在 layout/suggestion.xml 中定义
        carWashText = (TextView) findViewById(R.id.car_wash_text);  // 同上
        sportText = (TextView) findViewById(R.id.car_wash_text);    // 同上

        bingPicImg = (ImageView) findViewById(R.id.bing_pic_img); //  在 layout/activity_weather.xml 中定义

        swipeRefresh = (SwipeRefreshLayout) findViewById(R.id.swipe_refresh);
        swipeRefresh.setColorSchemeColors(R.color.colorPrimary);

        drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        navButton = (Button) findViewById(R.id.nav_button);
        navButton = (Button) findViewById(R.id.nav_button);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String weatherString = prefs.getString("weather", null);
        weatherId = getIntent().getStringExtra("weather_id");

        String bingPic = prefs.getString("bing_pic", null);
        if(bingPic!= null){
            Glide.with(this).load(bingPic).into(bingPicImg);
        } else {
            loadBingPic();
        }

        navButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                drawerLayout.openDrawer(GravityCompat.START);
            }
        });
        if(weatherString != null && weatherId.equals(weatherString.substring(31, 42))){
            // 有缓存时直接解析天气数据
            Weather weather = Utility.handleWeatherResponse(weatherString);
            showWeatherInfo(weather);
        } else {
            // 无缓存时去服务器查询天气
            weatherLayout.setVisibility(INVISIBLE);
            requestWeather(weatherId);
        }



        swipeRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                requestWeather(weatherId);
            }
        });

    }


    /*
     * 根据天气id请求城市天气信息
     */
    public void requestWeather(String weatherId) {
        this.weatherId = weatherId;
        LogUtil.d(TAG, "requestWeather()");
        String weatherUrl = "http://guolin.tech/api/weather?cityid=" + weatherId + "&key=4b7ad80fa15241acb7d8daee3228df7a";
        HttpUtil.sendOkHttpRequest(weatherUrl, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(WeatherActivity.this, "获取天气信息失败", Toast.LENGTH_SHORT).show();
                        swipeRefresh.setRefreshing(false);
                    }
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final String responseText = response.body().string();
                final Weather weather = Utility.handleWeatherResponse(responseText);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (weather != null && "ok".equals(weather.status)) {
                            SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(WeatherActivity.this).edit();
                            editor.putString("weather",responseText);
                            editor.apply();
                            showWeatherInfo(weather);

                        } else {
                            Toast.makeText(WeatherActivity.this, "获取天气信息失败",Toast.LENGTH_SHORT).show();
                        }
                        swipeRefresh.setRefreshing(false);
                    }
                });
            }
        });
        loadBingPic();
    }


    /**
     * 处理并展示Weather实体类中的数据
     */
    private  void showWeatherInfo(Weather weather) {
        LogUtil.d(TAG,"showWeatherInfo()");
        String cityName = weather.basic.cityName;
        String updateTime = weather.basic.update.updateTime.split(" ")[1];
        String degree = weather.now.temperature + "℃";
        String weatherInfo = weather.now.more.info;
        titleCity.setText(cityName);
        titleUpdateTime.setText(updateTime);
        degreeText.setText(degree);
        weatherInfoText.setText(weatherInfo);
        forecastLayout.removeAllViews();
        LogUtil.d(TAG, weather.toString());
        for(Forecast forecast : weather.forecastList) {
            View view = LayoutInflater.from(this).inflate(R.layout.forecast_item, forecastLayout, false);
            TextView dateText = (TextView) view.findViewById(R.id.date_text);
            TextView infoText = (TextView) view.findViewById(R.id.info_text);
            TextView maxText = (TextView) view.findViewById(R.id.max_text);
            TextView minText= (TextView) view.findViewById(R.id.min_text);
            dateText.setText(forecast.date);
            infoText.setText(forecast.more.info);
            maxText.setText(forecast.temperature.max);
            minText.setText(forecast.temperature.min);
            forecastLayout.addView(view);
        }
        if (weather.aqi != null) {
            aqiText.setText(weather.aqi.city.aqi);
            pm25Text.setText(weather.aqi.city.pm25);
        }

        String comfort = "舒适度" + weather.suggestion.comfort.info;
        String carWash = "洗车指数" + weather.suggestion.carWash.info;
        String sport = "运动建议" +  weather.suggestion.sport.info;
        comfortText.setText(comfort);
        carWashText.setText(carWash);
        sportText.setText(sport);
        weatherLayout.setVisibility(View.VISIBLE);
        if(weather != null && "ok".equals(weather.status)){
            Intent intent = new Intent(this, AutoUpdateService.class);
            startService(intent);
        } else {
            Toast.makeText(WeatherActivity.this, "获取天气信息失败啦", Toast.LENGTH_SHORT).show();
        }
    }


    /**
     * 加载必应每日一图
     */
    private void loadBingPic()  {
        LogUtil.d(TAG, "loadBingPic");
        String requestBingPic = "http://guolin.tech/api/bing_pic";
        HttpUtil.sendOkHttpRequest(requestBingPic, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final String bingPic = response.body().string();
                LogUtil.d(TAG, "bingPic"+bingPic);
                SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(WeatherActivity.this).edit();
                editor.putString("bing_pic", bingPic);
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
}
