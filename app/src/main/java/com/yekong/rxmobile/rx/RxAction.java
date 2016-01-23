package com.yekong.rxmobile.rx;

import com.google.gson.Gson;

/**
 * Created by baoxiehao on 16/1/18.
 */
public class RxAction {
    private static final Gson sGson = new Gson();

    public String type;
    public Object data;

    private RxAction(String type, Object data) {
        this.type = type;
        this.data = data;
    }

    @Override
    public String toString() {
        return sGson.toJson(this);
    }

    public static RxAction create(String type, Object data) {
        return new RxAction(type, data);
    }

    public static boolean equals(RxAction action1, RxAction action2) {
        if (action1 == null && action2 == null) {
            return true;
        } else if (action1 != null && action2 != null) {
            return action1.type.equals(action2.type) && action1.data.equals(action2.data);
        } else {
            return false;
        }
    }
}
