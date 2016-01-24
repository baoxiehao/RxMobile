package com.yekong.rxmobile.rx;


import android.text.TextUtils;

import rx.Observable;
import rx.functions.Func1;
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

    public <T> Observable<T> toActionObservable(final String rxEvent) {
        return mBus.filter(new Func1<Object, Boolean>() {
            @Override
            public Boolean call(Object o) {
                return (o instanceof RxAction) && TextUtils.equals(((RxAction) o).type, rxEvent);
            }
        }).map(new Func1<Object, T>() {
            @Override
            public T call(Object o) {
                return (T) ((RxAction) o).data;
            }
        });
    }
}
