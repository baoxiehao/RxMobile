package com.yekong.rxmobile.ui;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.SearchView;

import com.jakewharton.rxbinding.view.RxView;
import com.jakewharton.rxbinding.widget.RxSearchView;
import com.trello.rxlifecycle.components.RxFragment;
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

public class DoubanUserSearchFragment extends RxFragment {
    private static final String TAG = "DoubanUserSearchFragment";

    private static final String ARG_QUERY = "ARG_QUERY";

    static final int NUM_ITEMS = 3;

    SearchView mSearchView;
    ProgressBar mProgressBar;
    Button mRefreshAllButton;
    ListView mListView;
    EasyAdapter<DoubanUser> mEasyAdapter;

    Observable<Void> mRefreshClickStream;
    Observable<String> mSearchTextStream;
    Observable<List<DoubanUser>> mResponseStream;
    List<Observable<Void>> mCloseClickStreams = new ArrayList<>(NUM_ITEMS);
    List<Observable<DoubanUser>> mSuggestionStreams = new ArrayList<>(NUM_ITEMS);

    List<DoubanUser> mDoubanUsers = new ArrayList<>(NUM_ITEMS);

    private String mQuery;

    public DoubanUserSearchFragment() {
        // Required empty public constructor
    }

    public static DoubanUserSearchFragment newInstance(String query) {
        DoubanUserSearchFragment fragment = new DoubanUserSearchFragment();
        Bundle args = new Bundle();
        args.putString(ARG_QUERY, query);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mQuery = getArguments().getString(ARG_QUERY);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_douban_user_search, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initView(view);
        initData();
        initRx();
        Logger.d("baoyibao", "onViewCreated");
    }

    private void initView(View view) {
        mSearchView = (SearchView) view.findViewById(R.id.search_user);
        mProgressBar = (ProgressBar) view.findViewById(R.id.progress);
        mRefreshAllButton = (Button) view.findViewById(R.id.refreshAll);
        mListView = (ListView) view.findViewById(R.id.listView);

        mSearchView.requestFocus();
        mProgressBar.setIndeterminate(true);
        mProgressBar.setVisibility(View.GONE);
    }

    private void initData() {
        mDoubanUsers.clear();
        for (int i = 0; i < NUM_ITEMS; i++) {
            mDoubanUsers.add(DoubanUser.EMPTY);
        }
        mQuery = "夏";
        mEasyAdapter = new EasyAdapter<>(getActivity(), DoubanUserViewHolder.class, mDoubanUsers);
        mListView.setAdapter(mEasyAdapter);
        mSearchView.setQuery(mQuery, true);
    }

    private void initRx() {
        initSearchTextStream();
        initRefreshClickStream();
        initCloseClickStream();
        initResponseStream();
        initSuggestionStream();
        initProgressStream();
        subscribeSuggestionStreams();
    }

    private void initSearchTextStream() {
        // Subscribe the query text changes on the main thread just to make sure,
        // or when later interacted with other observables subscribing on non main thread,
        // exception will be thrown
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
        // Observe on the main thread when touching the UI
        mSearchTextStream
                .compose(this.<String>bindToLifecycle())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<String>() {
                    @Override
                    public void call(String s) {
                        mProgressBar.setVisibility(View.VISIBLE);
                        RxBus.singleton().send(RxAction.create(RxEvent.USER_ITEM_SEARCH, s));
                    }
                });
    }

    private void initRefreshClickStream() {
        // Subscribe the clicks on the main thread just to make sure,
        // or when later interacted with other observables subscribing on non main thread,
        // exception will be thrown
        mRefreshClickStream = RxView.clicks(mRefreshAllButton)
                .startWith((Void) null)
                .throttleFirst(500, TimeUnit.MILLISECONDS)
                .subscribeOn(AndroidSchedulers.mainThread())
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
                // Show the progress view either refresh or search
                Observable.combineLatest(
                        mRefreshClickStream,
                        mSearchTextStream,
                        new Func2<Void, String, Boolean>() {
                            @Override
                            public Boolean call(Void aVoid, String s) {
                                return true;
                            }
                        }),
                // Hide the progress view when response is available
                mResponseStream.map(new Func1<List<DoubanUser>, Boolean>() {
                    @Override
                    public Boolean call(List<DoubanUser> doubanUsers) {
                        return false;
                    }
                }))
                // Observe on the main thread when touching UI
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
                // Observe the refresh or search on the non main thread,
                // or NetworkOnMainThreadException will be thrown
                mRefreshClickStream.observeOn(Schedulers.io()),
                mSearchTextStream.observeOn(Schedulers.io()),
                new Func2<Void, String, Observable<DoubanUserSearch>>() {
                    @Override
                    public Observable<DoubanUserSearch> call(Void aVoid, String query) {
                        int start = (int) Math.floor(Math.random() * 1000);
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
                // Observe on main thread when touching UI
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<List<DoubanUser>>() {
                    @Override
                    public void call(List<DoubanUser> doubanUsers) {
                        mEasyAdapter.setItems(doubanUsers);
                        mProgressBar.setVisibility(View.GONE);
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        Logger.d(TAG, "Suggest users error", throwable);
                    }
                });
    }

    private Observable<Void> createCloseClickStream(final int position) {
        return RxBus.singleton().<Integer>toActionObservable(RxEvent.USER_ITEM_REFRESH)
                .filter(new Func1<Integer, Boolean>() {
                    @Override
                    public Boolean call(Integer integer) {
                        return integer == position;
                    }
                })
                .throttleFirst(500, TimeUnit.MILLISECONDS)
                .map(new Func1<Object, Void>() {
                    @Override
                    public Void call(Object o) {
                        return null;
                    }
                })
                .startWith((Void) null);
    }

    private Observable<DoubanUser> createSuggestionStream(final int position) {
        return Observable.combineLatest(
                mCloseClickStreams.get(position),
                mResponseStream,
                new Func2<Void, List<DoubanUser>, DoubanUser>() {
                    @Override
                    public DoubanUser call(Void aVoid, List<DoubanUser> users) {
                        if (users.isEmpty()) {
                            return DoubanUser.EMPTY;
                        }
                        int randomIndex = (int) Math.floor(Math.random() * users.size());
                        DoubanUser user = users.get(randomIndex);
                        return user;
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
