package com.yekong.rxmobile.ui;

import android.os.Bundle;

import com.trello.rxlifecycle.components.support.RxAppCompatActivity;
import com.yekong.rxmobile.R;
import com.yekong.rxmobile.model.DoubanUser;
import com.yekong.rxmobile.rx.RxBus;
import com.yekong.rxmobile.rx.RxEvent;

import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;

public class MainActivity extends RxAppCompatActivity {

    static final String TAG = "MainActivity";
    static final String ARG_QUERY = "ARG_QUERY";

    private String mQuery;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mQuery = "夏";
        getFragmentManager().beginTransaction().add(R.id.container,
                DoubanUserSearchFragment.newInstance(mQuery), "UserSearch").commit();
        initRx();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(ARG_QUERY, mQuery);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        if (savedInstanceState != null && savedInstanceState.containsKey(ARG_QUERY)) {
            mQuery = savedInstanceState.getString(ARG_QUERY);
        }
    }

    private void showUserFragment(DoubanUser user) {
        getFragmentManager().beginTransaction().replace(R.id.container,
                DoubanUserFragment.newInstance(user.toString())).addToBackStack(null).commit();
    }

    @Override
    public void onBackPressed() {
        if (!getFragmentManager().popBackStackImmediate()) {
            super.onBackPressed();
        }
    }

    private void initRx() {
        initQueryStream();
        initOpenClickStream();
    }

    private void initQueryStream() {
        RxBus.singleton().<String>toActionObservable(RxEvent.USER_ITEM_SEARCH)
                .compose(this.<String>bindToLifecycle())
                .subscribe(new Action1<String>() {
                    @Override
                    public void call(String s) {
                        mQuery = s;
                    }
                });
    }

    private void initOpenClickStream() {
        RxBus.singleton().<DoubanUser>toActionObservable(RxEvent.USER_ITEM_OPEN)
                .compose(this.<DoubanUser>bindToLifecycle())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<DoubanUser>() {
                    @Override
                    public void call(DoubanUser doubanUser) {
                        showUserFragment(doubanUser);
                    }
                });
    }
}
