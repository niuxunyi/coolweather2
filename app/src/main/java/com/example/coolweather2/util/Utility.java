package com.example.coolweather2.util;

import android.text.TextUtils;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.example.coolweather2.db.City;
import com.example.coolweather2.db.County;
import com.example.coolweather2.db.Province;
import com.example.coolweather2.gson.Weather;
import com.google.gson.Gson;

public class Utility {    // 工具类，用于解析和处理服务器返回的JSON格式的省、市、县数据
    private static final String TAG = "Utility";
    /**
     *  用于解析和处理服务器返回的JSON格式的省级数据
     */

    public static boolean handleProvinceResponse(String response){
        Log.d(TAG, "handleProvinceResponse");
        if (!TextUtils.isEmpty(response)) {
            try{
                Log.d(TAG,response);
                JSONArray allProvinces = new JSONArray(response);       // 将服务器返回的JSON数组传入JsonArray中
                for (int i = 0; i < allProvinces.length(); i++) {
                    JSONObject provinceObject = allProvinces.getJSONObject(i);      // 从JsonArray对象中取出JsonObject对象
                    Province province = new Province();
                    province.setProvinceName(provinceObject.getString("name"));         // 从JsonObject对象中取出数据，把数据组装成Province实体类对象
                    province.setProvinceCode(provinceObject.getInt("id"));
                    province.save();            // 储存到数据库中
                }
                return true;
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    /**
     * 解析和处理服务器返回的城市级数据
     */
    public static boolean handleCityResponse(String response, int provinceId){
        if (!TextUtils.isEmpty(response)) {
            try {
                JSONArray allCities = new JSONArray(response);      // 将服务器返回的JSON数据传入JsonArray中
                for (int i = 0; i < allCities.length(); i++ ){
                    JSONObject cityObject = allCities.getJSONObject(i);        // 从JSONArray中取出JSONObject对象
                    City city = new City();
                    city.setCityName(cityObject.getString("name")); // 从JSONObject对象中取出数据，组装成City实体类对象
                    city.setCityCode(cityObject.getInt("id"));
                    city.setProvinceId(provinceId);
                    city.save();        // 储存到数据库中
                }
            } catch (JSONException e){
                e.printStackTrace();
            }
        }
        return false;
    }


    /**
     * 解析和处理服务器返回的县级数据
     */
    public static boolean handleCountyResponse(String response, int cityId){
        if(!TextUtils.isEmpty(response)) {
            try {
                JSONArray counties = new JSONArray(response);   // 将服务器返回的JSON数据传入JSONarray中
                for (int i = 0; i < counties.length(); i++) {
                    JSONObject countyObject = counties.getJSONObject(i); // 从JSONArray中取出JSONObject对象
                    County county = new County();
                    county.setCityId(cityId);                                   // 从JSONObject对象中取出数据，组装成一个County实体类的对象
                    county.setCountyName(countyObject.getString("name"));
                    county.setWeatherId(countyObject.getString("weather_id"));
                    county.save(); // 储存到数据库中
                }
            } catch (JSONException e){
                e.printStackTrace();
            }
        }
        return false;
    }

    /*
     * 将返回的JSON数据解析成Weather实体类
     */
    public static Weather handleWeatherResponse(String response){
        try{
            JSONObject jsonObject = new JSONObject(response);
            JSONArray jsonArray = jsonObject.getJSONArray("HeWeather");
            String weatherContent = jsonArray.getJSONObject(0).toString();
            return new Gson().fromJson(weatherContent, Weather.class);
        } catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }
}
