package com.example.coolweather.gson;

import com.google.gson.annotations.SerializedName;

public class AQI {
    public String status;

    public Basic basic;

    @SerializedName("air_now_city")
    public AQICity city;

    public class AQICity{
        @SerializedName("aqi")
        public String aqi;

        @SerializedName("pm25")
        public String pm25;
    }
}
