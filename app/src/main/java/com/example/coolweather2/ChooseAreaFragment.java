package com.example.coolweather2;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SimpleExpandableListAdapter;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import org.litepal.LitePal;

import java.io.IOException;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.List;

import com.example.coolweather2.db.City;
import com.example.coolweather2.db.County;
import com.example.coolweather2.db.Province;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

import com.example.coolweather2.gson.Weather;
import com.example.coolweather2.util.HttpUtil;
import com.example.coolweather2.util.LogUtil;
import com.example.coolweather2.util.Utility;

public class ChooseAreaFragment extends Fragment {

    private static final String TAG = "ChooseAreaFragment";

    public static final int LEVEL_PROVINCE = 0;
    public static final int LEVEL_CITY = 1;
    public static final int LEVEL_COUNTY = 2;

    private int currentLevel;                // 当前选中的级别

    private Province selectedProvince;       // 选中的省份
    private City selectedCity;               // 选中的市
    private County selectedCounty;           // 选中的县

//  -------------控件-----------
    private TextView titleText;
    private Button backButton;
    private ListView listView;
    private ArrayAdapter<String> adapter;
    private ProgressDialog progressDialog;


//  -------------列表-----------
    private List<String> dataList = new ArrayList<>();
    private List<Province> provinceList;     // 省列表
    private List<City> cityList;             // 市列表
    private List<County> countyList;         // 县列表

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // 加载碎片的布局时调用
        View view = inflater.inflate(R.layout.choose_area, container, false);
        titleText = (TextView) view.findViewById(R.id.title_text);
        backButton = (Button) view.findViewById(R.id.back_button);
        listView = (ListView) view.findViewById(R.id.list_view);
        adapter = new ArrayAdapter<>(MyApplication.getContext(), android.R.layout.simple_list_item_1, dataList);
        listView.setAdapter(adapter);
        return view;

    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        // 在碎片相关联的活动已经创建完时调用
        super.onActivityCreated(savedInstanceState);
        LogUtil.d(TAG,"onActivityCreated");
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if(currentLevel == LEVEL_PROVINCE) {
                    selectedProvince = provinceList.get(position);
                    queryCities();                              // 加载市级数据
                } else if (currentLevel == LEVEL_CITY){
                    selectedCity = cityList.get(position);
                    queryCounties();                            // 加载县级数据
                } else if (currentLevel == LEVEL_COUNTY) {
                    selectedCounty = countyList.get(position);
                    String weatherId = selectedCounty.getWeatherId();
                    if (getActivity() instanceof MainActivity){
                        Intent intent = new Intent(getActivity(), WeatherActivity.class);
                        intent.putExtra("weather_id", weatherId); // 把当前选中县的weatherId传递过去
                        startActivity(intent);  // 启动WeatherActivity
                        //getActivity().finish();
                    } else if ( getActivity() instanceof WeatherActivity) {
                        WeatherActivity activity = (WeatherActivity) getActivity();
                        activity.drawerLayout.closeDrawers();
                        activity.swipeRefresh.setRefreshing(true);
                        activity.requestWeather(weatherId);
                    }
                }

            }
        });

        backButton.setOnClickListener(new View.OnClickListener() {  // 返回上一级列表
            @Override
            public void onClick(View v) {
                if(currentLevel == LEVEL_COUNTY) {
                    queryCities();
                } else if(currentLevel == LEVEL_CITY){
                    queryProvinces();
                }
            }
        });
        queryProvinces();                                       // 加载省级数据
    }


    private void queryProvinces(){ //  查询全国所有的省，优先从数据库查询，如果没有查询到再去服务器上查询
        LogUtil.d(TAG, "queryProvinces()");
        titleText.setText("中国");
        backButton.setVisibility(View.GONE);    // 省级列表自动隐藏“返回上级”按钮
        provinceList = LitePal.findAll(Province.class);
        if(provinceList.size() > 0){
            dataList.clear();
            for ( Province province : provinceList){
                dataList.add(province.getProvinceName());
            }
            adapter.notifyDataSetChanged();
            listView.setSelection(0);
            currentLevel = LEVEL_PROVINCE;
        } else {
            String address = "http://guolin.tech/api/china";
            queryFromServer(address, "province");
        }
    }

    private void queryCities(){     // 查询省内所有的市，优先从数据库查询，如果没有查询到再去服务器上查询
        LogUtil.d(TAG,"queryCities()");
        titleText.setText(selectedProvince.getProvinceName());
        backButton.setVisibility(View.VISIBLE);
        cityList = LitePal.where("provinceId = ?", String.valueOf(selectedProvince.getId())).find(City.class);
        if(cityList.size() > 0){
            dataList.clear();
            for( City city : cityList ) {
                dataList.add(city.getCityName());
            }
            adapter.notifyDataSetChanged();
            listView.setSelection(0);
            currentLevel = LEVEL_CITY;
        } else {
            int provinceCode = selectedProvince.getProvinceCode();
            String address = "http://guolin.tech/api/china/" + provinceCode;
            queryFromServer(address, "city");
        }
    }

    private void queryCounties(){   // 查询市内所有的县，优先从数据库查询，如果没有查询到再去服务器上查询
        LogUtil.d(TAG,"queryCounties()");
        titleText.setText(selectedCity.getCityName());
        backButton.setVisibility(View.VISIBLE);
        countyList = LitePal.where("cityId = ?" , String.valueOf(selectedCity.getId())).find(County.class);
        if(countyList.size() > 0) {
            dataList.clear();
            for( County county : countyList) {
                dataList.add(county.getCountyName());
            }
            adapter.notifyDataSetChanged();
            listView.setSelection(0);
            currentLevel = LEVEL_COUNTY;
        } else {
            int provinceCode = selectedProvince.getProvinceCode();
            int cityCode = selectedCity.getCityCode();
            String address = "http://guolin.tech/api/china/" + provinceCode + "/" +cityCode;
            queryFromServer(address, "county");
        }
    }

    private void queryFromServer(String address, final String type){  // 根据传入的地址和类型从服务器上查询省市县数据
        LogUtil.d(TAG,"queryFromServer()");
        showProgressDialog();   // 弹出进度对话框：正在下载
        HttpUtil.sendOkHttpRequest(address, new Callback() {

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                Log.d(TAG, "onResponse()");
                String responseText = response.body().string();
                boolean result = false;
                if ("province".equals(type)) {
                    result = Utility.handleProvinceResponse(responseText);
                } else if ("city".equals(type)){
                    result = Utility.handleCityResponse(responseText, selectedProvince.getId());
                } else if ("county".equals(type)) {
                    result = Utility.handleCountyResponse(responseText, selectedCity.getId());
                }

                if (result) {
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {  // 切换到主线程，以便进行UI操作
                            closeProgressDialog();
                            if ("province".equals(type)){
                                queryProvinces();        // 有UI操作
                            } else if ("city".equals(type)) {
                                queryCities();           // 有UI操作
                            } else if ("county".equals(type)) {
                                queryCounties();         // 有UI操作

                            }
                        }
                    });
                }
            }


            @Override
            public void onFailure(Call call, IOException e) {
                // 通过runOnUiThread回到主线程处理逻辑
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        closeProgressDialog();
                        Toast.makeText(getContext(), "加载失败", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    private void showProgressDialog(){  // 显示进度对话框
        if (progressDialog == null) {
            progressDialog = new ProgressDialog(getActivity());  // 《第一行代码》P151
            progressDialog.setMessage("正在加载");
            progressDialog.setCanceledOnTouchOutside(false);
        }
        progressDialog.show();
    }

    private void closeProgressDialog(){ // 关闭进度对话框
        if (progressDialog != null) {
            progressDialog.dismiss();
        }

    }

}
