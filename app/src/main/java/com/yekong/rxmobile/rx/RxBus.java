package com.yekong.rxmobile.rx;


import rx.Observable;
import rx.subjects.PublishSubject;
import rx.subjects.SerializedSubject;
import rx.subjects.Subject;

/**
 * Created by baoxiehao on 16/1/16.
 */
public class RxBus {
    private static RxBus sRxBus = new RxBus();

    private final Subject<Object, Object> mBus = new SerializedSubject<>(PublishSubject.create());

    private RxBus() {
    }

    public static RxBus singleton() {
        return sRxBus;
    }

    public void send(Object o) {
        mBus.onNext(o);
    }

    public Observable<Object> toObservable() {
        return mBus;
    }
}
