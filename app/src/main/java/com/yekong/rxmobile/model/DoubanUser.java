package com.yekong.rxmobile.model;

/**
 * Created by baoxiehao on 16/1/15.
 */
/*
    "loc_id": "108296",
    "name": "爱潜水的耗子",
    "created": "2008-07-05 15:09:30",
    "is_banned": false,
    "is_suicide": false,
    "loc_name": "上海",
    "avatar": "http://img3.doubanio.com/icon/u2669345-6.jpg",
    "signature": "我最近老脏话。不好。妈的",
    "uid": "2669345",
    "alt": "http://www.douban.com/people/2669345/",
    "desc": "blog地址：t.sina.com.cn/hzorg\n    慢慢的有了男人味，偶尔还是会有孩子气。\r\n享受当前生活，拼命工作。\r\n上海不会是终点。。。。。。\r\n    ",
    "type": "user",
    "id": "2669345",
    "large_avatar": "http://img3.doubanio.com/icon/up2669345-6.jpg"
 */
public class DoubanUser extends BaseModel {
    public static final DoubanUser EMPTY = new DoubanUser();

    public String uid;
    public String name;
    public String loc_name;
    public String signature;
    public String desc;
    public String avatar;
    public String large_avatar;

    public DoubanUser() {
        name = "姓名";
        desc = "描述";
    }
}
