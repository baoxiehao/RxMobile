# RxMobile
Demonstrate the power of RxJava brings to Android platform, using douban API.

# Components
- RxJava-1.1.0
- RxAndroid-1.1.0
- RxBinding-0.3.0
- RxLifecycle-0.4.0
- EasyAdapter-1.5.0
- Retrofit-2.0.0-beta2
- Glide-3.6.1

# Features
Use [douban API](http://developers.douban.com/wiki/?title=user_v2#get_user) to query douban users, when the query is done, three suggested douban users will be shown.
The suggested users will be refreshed individually or totally, when the former results are fetched in cached response data and the latter results are initiated through a new API request.

There is progress view to indicate searching or requesting, which will be gone when response data is available. When user item is clicked, the detail user page will be shown.

# RxJava power shown
- Different ways to create observables, including the use of startWith
- How to manipulate observable items, like map, filter, debounce and throttleFirst
- How to manipulate meta observables, like flatMap
- How to combine observables, like combineLatest and amb
- How to switch threads, avoid to touch UI in non main thread and do asynchronous work in main thread
- Difference of subscribeOn and observeOn is shown clearly

# Show the code

Create observable
```java
mSearchTextStream = RxSearchView.queryTextChanges(mSearchView);
mRefreshClickStream = RxView.clicks(mRefreshAllButton);
RxBus.singleton().<Integer>toActionObservable(RxEvent.USER_ITEM_REFRESH);
```

Manipulate observable
```java
// Subscribe the clicks on the main thread just to make sure,
// or when later interacted with other observables subscribing on non main thread,
// exception will be thrown
mRefreshClickStream = RxView.clicks(mRefreshAllButton)
        .startWith((Void) null)
        .throttleFirst(500, TimeUnit.MILLISECONDS)
        .subscribeOn(AndroidSchedulers.mainThread())
        .share();
```

```java                
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
```
  
  ```java
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
 ```
