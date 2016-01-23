package com.yekong.rxmobile.model;

import com.google.gson.Gson;

/**
 * Created by baoxiehao on 16/1/21.
 */
public class BaseModel {
    private static Gson sGson = new Gson();

    @Override
    public String toString() {
        return sGson.toJson(this);
    }
}
