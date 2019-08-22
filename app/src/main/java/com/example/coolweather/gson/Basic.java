package com.example.coolweather.gson;

import com.google.gson.annotations.SerializedName;

public class Basic {

    @SerializedName("cid")
    public String weatherId;

    @SerializedName("location")
    public String cityName;

    @SerializedName("parent_city")
    public String parentCity;


}
