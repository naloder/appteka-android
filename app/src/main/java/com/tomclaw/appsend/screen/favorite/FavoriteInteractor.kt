package com.tomclaw.appsend.screen.favorite

import com.tomclaw.appsend.core.StoreApi
import com.tomclaw.appsend.dto.AppEntity
import com.tomclaw.appsend.user.UserDataInteractor
import com.tomclaw.appsend.util.SchedulersFactory
import io.reactivex.rxjava3.core.Observable
import java.util.Locale

interface FavoriteInteractor {

    fun listApps(userId: Int, offsetAppId: String? = null): Observable<List<AppEntity>>

}

class FavoriteInteractorImpl(
    private val api: StoreApi,
    private val locale: Locale,
    private val userDataInteractor: UserDataInteractor,
    private val schedulers: SchedulersFactory
) : FavoriteInteractor {

    override fun listApps(userId: Int, offsetAppId: String?): Observable<List<AppEntity>> {
        return userDataInteractor
            .getUserData()
            .flatMap {
                api.getFavoriteList(
                    guid = it.guid,
                    userId = userId,
                    appId = offsetAppId,
                    locale = locale.language
                )
            }
            .map { list ->
                list.result.files
            }
            .toObservable()
            .subscribeOn(schedulers.io())
    }

}