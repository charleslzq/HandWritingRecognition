@file:JvmName("RxScheduleUtils")

package com.github.charleslzq.hwr.view.support

import io.reactivex.Observable
import io.reactivex.Scheduler
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import java.util.concurrent.TimeUnit

/**
 * Created by charleslzq on 18-3-5.
 */
fun <T> callOnIo(callable: () -> T) = callOn(Schedulers.io(), callable)

fun <T : Any> callNullableOnIo(callable: () -> T?) = callNullableOn(Schedulers.io(), callable)

fun <T> callOnIoWithInterval(period: Long, callable: () -> T) =
        callWithInterval(period, Schedulers.io(), callable)

fun runOnIo(runnable: () -> Unit) = runOn(Schedulers.io(), runnable)

fun runOnIoWithInterval(period: Long, runnable: () -> Unit) =
        runWithInterval(period, Schedulers.io(), runnable)

fun <T> callOnCompute(callable: () -> T) = callOn(Schedulers.computation(), callable)

fun <T : Any> callNullableOnCompute(callable: () -> T?) =
        callNullableOn(Schedulers.computation(), callable)

fun runOnCompute(runnable: () -> Unit) = runOn(Schedulers.computation(), runnable)

fun runOnComputeWithInterval(period: Long, runnable: () -> Unit) =
        runWithInterval(period, Schedulers.computation(), runnable)

fun runOnUI(runnable: () -> Unit) = runOn(AndroidSchedulers.mainThread(), runnable)

fun runOnUIWithInterval(period: Long, runnable: () -> Unit) =
        runWithInterval(period, AndroidSchedulers.mainThread(), runnable)

fun <T> callOn(scheduler: Scheduler = Schedulers.trampoline(), callable: () -> T): T {
    return Observable.just(1).observeOn(scheduler).map { callable() }.blockingSingle()
}

fun <T : Any> callNullableOn(
        scheduler: Scheduler = Schedulers.trampoline(),
        callable: () -> T?
): T? {
    return Observable.just(1).observeOn(scheduler).map { listOfNotNull(callable()) }
            .blockingSingle().firstOrNull()
}

fun <T> callWithInterval(
        period: Long,
        scheduler: Scheduler = Schedulers.trampoline(),
        callable: () -> T
): Observable<T> {
    return Observable.interval(period, TimeUnit.MILLISECONDS, scheduler).map { callable() }
}

fun runOn(scheduler: Scheduler = Schedulers.trampoline(), runnable: () -> Unit): Disposable {
    return Observable.just(1).observeOn(scheduler).subscribe { runnable() }
}

fun runWithInterval(
        period: Long,
        scheduler: Scheduler = Schedulers.trampoline(),
        runnable: () -> Unit
): Disposable {
    return Observable.interval(period, TimeUnit.MILLISECONDS).observeOn(scheduler)
            .subscribe { runnable() }
}