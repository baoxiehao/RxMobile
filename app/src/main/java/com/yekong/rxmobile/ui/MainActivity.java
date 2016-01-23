package com.yekong.rxmobile.ui;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.SearchView;

import com.jakewharton.rxbinding.view.RxView;
import com.jakewharton.rxbinding.widget.RxSearchView;
import com.trello.rxlifecycle.components.support.RxAppCompatActivity;
import com.yekong.rxmobile.R;
import com.yekong.rxmobile.model.DoubanUser;
import com.yekong.rxmobile.model.DoubanUserSearch;
import com.yekong.rxmobile.retrofit.DoubanService;
import com.yekong.rxmobile.rx.RxAction;
import com.yekong.rxmobile.rx.RxBus;
import com.yekong.rxmobile.rx.RxEvent;
import com.yekong.rxmobile.util.Logger;
import com.yekong.rxmobile.view.DoubanUserViewHolder;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import retrofit.GsonConverterFactory;
import retrofit.Retrofit;
import retrofit.RxJavaCallAdapterFactory;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.functions.Func2;
import rx.functions.FuncN;
import rx.schedulers.Schedulers;
import uk.co.ribot.easyadapter.EasyAdapter;

public class MainActivity extends RxAppCompatActivity {

    static final String TAG = "MainActivity";

    static final int NUM_ITEMS = 3;

    SearchView mSearchView;
    ProgressBar mProgressBar;
    Button mRefreshAllButton;
    ListView mListView;
    EasyAdapter<DoubanUser> mEasyAdapter;

    Observable<Object> mBusStream;
    Observable<Void> mRefreshClickStream;
    Observable<String> mSearchTextStream;
    Observable<List<DoubanUser>> mResponseStream;
    Observable<Boolean> mProgressStream;
    List<Observable<Void>> mCloseClickStreams = new ArrayList<>(NUM_ITEMS);
    List<Observable<DoubanUser>> mSuggestionStreams = new ArrayList<>(NUM_ITEMS);

    List<DoubanUser> mDoubanUsers = new ArrayList<>(NUM_ITEMS);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initUsers();
        initView();
        initRx();
    }

    private void initView() {
        mSearchView = (SearchView) findViewById(R.id.search_user);
        mProgressBar = (ProgressBar) findViewById(R.id.progress);
        mRefreshAllButton = (Button) findViewById(R.id.refreshAll);
        mListView = (ListView) findViewById(R.id.listView);
        mEasyAdapter = new EasyAdapter<>(this, DoubanUserViewHolder.class, mDoubanUsers);
        mListView.setAdapter(mEasyAdapter);
        mSearchView.requestFocus();
        mSearchView.setQuery("夏", true);
        mProgressBar.setIndeterminate(true);
        mProgressBar.setVisibility(View.GONE);
    }

    private void initUsers() {
        mDoubanUsers.clear();
        for (int i = 0; i < NUM_ITEMS; i++) {
            mDoubanUsers.add(DoubanUser.EMPTY);
        }
    }

    private void initRx() {
        mBusStream = RxBus.singleton().toObservable();
        initSearchTextStream();
        initRefreshClickStream();
        initCloseClickStream();
        initResponseStream();
        initSuggestionStream();
        initProgressStream();
        subscribeSuggestionStreams();
    }

    private void initSearchTextStream() {
        mSearchTextStream = RxSearchView.queryTextChanges(mSearchView)
                .debounce(500, TimeUnit.MILLISECONDS)
                .map(new Func1<CharSequence, String>() {
                    @Override
                    public String call(CharSequence charSequence) {
                        return charSequence.toString().trim();
                    }
                })
                .filter(new Func1<String, Boolean>() {
                    @Override
                    public Boolean call(String s) {
                        return !TextUtils.isEmpty(s);
                    }
                })
                .distinctUntilChanged()
                .subscribeOn(AndroidSchedulers.mainThread())
                .share();
        mSearchTextStream
                .compose(this.<String>bindToLifecycle())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnUnsubscribe(new Action0() {
                    @Override
                    public void call() {
                        Logger.d(TAG, "search stream unsub progress");
                    }
                })
                .subscribe(new Action1<String>() {
                    @Override
                    public void call(String s) {
                        mProgressBar.setVisibility(View.VISIBLE);
                    }
                });
    }


    private void initRefreshClickStream() {
        mRefreshClickStream = RxView.clicks(mRefreshAllButton)
                .startWith((Void) null)
                .throttleFirst(500, TimeUnit.MILLISECONDS)
                .doOnNext(new Action1<Void>() {
                    @Override
                    public void call(Void aVoid) {
                        Logger.d(TAG, "Refresh click stream next: " + Thread.currentThread());
                    }
                })
                .doOnError(new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        Logger.d(TAG, "Refresh click stream error: " + Thread.currentThread());
                    }
                })
                .subscribeOn(AndroidSchedulers.mainThread()) // IMPORTANT !?
                .share();
    }

    private void initCloseClickStream() {
        mCloseClickStreams.clear();
        for (int i = 0; i < NUM_ITEMS; i++) {
            mCloseClickStreams.add(createCloseClickStream(i));
        }
    }

    private void initSuggestionStream() {
        mSuggestionStreams.clear();
        for (int i = 0; i < NUM_ITEMS; i++) {
            mSuggestionStreams.add(createSuggestionStream(i));
        }
    }

    private void initProgressStream() {
        Observable.amb(
                Observable.combineLatest(
                        mRefreshClickStream,
                        mSearchTextStream,
                        new Func2<Void, String, Boolean>() {
                            @Override
                            public Boolean call(Void aVoid, String s) {
                                return true;
                            }
                        }),
                mResponseStream.map(new Func1<List<DoubanUser>, Boolean>() {
                    @Override
                    public Boolean call(List<DoubanUser> doubanUsers) {
                        return false;
                    }
                }))
                .observeOn(AndroidSchedulers.mainThread())
                .compose(this.<Boolean>bindToLifecycle())
                .subscribe(new Action1<Boolean>() {
                    @Override
                    public void call(Boolean flag) {
                        mProgressBar.setVisibility(flag ? View.VISIBLE : View.GONE);
                    }
                });
    }

    private void initResponseStream() {
        mResponseStream = Observable.combineLatest(
                mRefreshClickStream.observeOn(Schedulers.io()),
                mSearchTextStream.observeOn(Schedulers.io()),
                new Func2<Void, String, Observable<DoubanUserSearch>>() {
                    @Override
                    public Observable<DoubanUserSearch> call(Void aVoid, String query) {
                        int start = (int) Math.floor(Math.random() * 500);
                        Logger.d(TAG, "Request douban users: " + query);
                        Logger.d(TAG, "Thread: " + Thread.currentThread());
                        return new Retrofit.Builder()
                                .baseUrl(DoubanService.BASE_URL)
                                .addConverterFactory(GsonConverterFactory.create())
                                .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
                                .build()
                                .create(DoubanService.class)
                                .searchUsers(query, 100, start);
                    }
                })
                .flatMap(new Func1<Observable<DoubanUserSearch>, Observable<DoubanUserSearch>>() {
                    @Override
                    public Observable<DoubanUserSearch> call(Observable<DoubanUserSearch> doubanUserSearchObservable) {
                        return doubanUserSearchObservable;
                    }
                })
                .map(new Func1<DoubanUserSearch, List<DoubanUser>>() {
                    @Override
                    public List<DoubanUser> call(DoubanUserSearch doubanUserSearch) {
                        return doubanUserSearch.users;
                    }
                })
                .doOnNext(new Action1<List<DoubanUser>>() {
                    @Override
                    public void call(List<DoubanUser> doubanUsers) {
                        Logger.d(TAG, "Response douban users with size: " + doubanUsers.size());
                        Logger.d(TAG, "Thread: " + Thread.currentThread());
                    }
                })
                .doOnError(new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        Logger.d(TAG, "Request douban users error", throwable);
                    }
                })
                .share();
    }

    private void subscribeSuggestionStreams() {
        Observable.combineLatest(mSuggestionStreams,
                new FuncN<List<DoubanUser>>() {
                    @Override
                    public List<DoubanUser> call(Object... users) {
                        List<DoubanUser> doubanUsers = new ArrayList<>(users.length);
                        for (Object user : users) {
                            doubanUsers.add((DoubanUser) user);
                        }
                        Logger.d(TAG, "combine users " + Thread.currentThread());
                        return doubanUsers;
                    }
                })
                .compose(this.<List<DoubanUser>>bindToLifecycle())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<List<DoubanUser>>() {
                    @Override
                    public void call(List<DoubanUser> doubanUsers) {
                        mEasyAdapter.setItems(doubanUsers);
                        mProgressBar.setVisibility(View.GONE);
                        Logger.d(TAG, "Suggest users: " + doubanUsers);
                        Logger.d(TAG, "Suggest users thread: " + Thread.currentThread());
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        Logger.d(TAG, "Suggest users error", throwable);
                        Logger.d(TAG, "Suggest users thread: " + Thread.currentThread());
                    }
                });
    }

    private Observable<Void> createCloseClickStream(final int position) {
        return mBusStream
                .filter(new Func1<Object, Boolean>() {
                    @Override
                    public Boolean call(Object o) {
                        if (o instanceof RxAction) {
                            return RxAction.equals((RxAction) o,
                                    RxAction.create(RxEvent.USER_ITEM_CLOSE, position));
                        }
                        return false;
                    }
                })
                .throttleFirst(500, TimeUnit.MILLISECONDS)
                .map(new Func1<Object, Void>() {
                    @Override
                    public Void call(Object o) {
                        return null;
                    }
                })
                .doOnNext(new Action1<Void>() {
                    @Override
                    public void call(Void aVoid) {
                        Logger.d(TAG, "Close click stream next: " + position);
                        Logger.d(TAG, "Close click stream thread: " + Thread.currentThread());
                    }
                })
                .startWith((Void) null);
    }

    private Observable<DoubanUser> createSuggestionStream(final int position) {
        return Observable.combineLatest(mCloseClickStreams.get(position),
                mResponseStream,
                new Func2<Void, List<DoubanUser>, DoubanUser>() {
                    @Override
                    public DoubanUser call(Void aVoid, List<DoubanUser> users) {
                        if (users.isEmpty()) {
                            return DoubanUser.EMPTY;
                        }
                        int randomIndex = (int) Math.floor(Math.random() * users.size());
                        DoubanUser user = users.get(randomIndex);
                        Logger.d(TAG, "Suggestion stream next: " + user);
                        Logger.d(TAG, "Thread: " + Thread.currentThread());
                        return user;
                    }
                })
                .doOnNext(new Action1<DoubanUser>() {
                    @Override
                    public void call(DoubanUser user) {
                        Logger.d(TAG, "Suggestion stream next: " + position);
                        Logger.d(TAG, "Thread: " + Thread.currentThread());
                    }
                })
                .doOnSubscribe(new Action0() {
                    @Override
                    public void call() {
                        Logger.d(TAG, "Suggestion stream subscribe: " + position);
                    }
                })
                .doOnUnsubscribe(new Action0() {
                    @Override
                    public void call() {
                        Logger.d(TAG, "Suggestion stream unsubscribe: " + position);
                    }
                });
    }
}
