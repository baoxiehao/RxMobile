package com.yekong.rxmobile.retrofit;

import com.yekong.rxmobile.model.DoubanUserSearch;

import retrofit.http.GET;
import retrofit.http.Query;
import rx.Observable;

/**
 * Created by baoxiehao on 16/1/15.
 */
public interface DoubanService {
    String BASE_URL = "http://api.douban.com/v2/";

    @GET("user")
    Observable<DoubanUserSearch> searchUsers(@Query("q") String query,
                                             @Query("count") int count,
                                             @Query("start") int start);
}
