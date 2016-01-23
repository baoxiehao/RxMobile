package com.yekong.rxmobile.model;

import java.util.List;

/**
 * Created by baoxiehao on 16/1/21.
 */
/*
    {
        "count": 20,
        "start": 100,
        "total": 151528,
        "users": [ .... ]
    }
 */
public class DoubanUserSearch {
    public int count;
    public int start;
    public int total;
    public List<DoubanUser> users;
}
