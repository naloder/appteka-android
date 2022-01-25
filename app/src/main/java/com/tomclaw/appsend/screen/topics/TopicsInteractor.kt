package com.tomclaw.appsend.screen.topics

import com.tomclaw.appsend.core.StoreApi
import com.tomclaw.appsend.dto.TopicEntity
import com.tomclaw.appsend.user.UserDataInteractor
import com.tomclaw.appsend.util.SchedulersFactory
import io.reactivex.rxjava3.core.Observable

interface TopicsInteractor {

    fun listTopics(offset: Int = 0): Observable<List<TopicEntity>>

}

class TopicsInteractorImpl(
    private val userDataInteractor: UserDataInteractor,
    private val api: StoreApi,
    private val schedulers: SchedulersFactory
) : TopicsInteractor {

    override fun listTopics(offset: Int): Observable<List<TopicEntity>> {
        return userDataInteractor
            .getUserData()
            .flatMap {
                api.getTopicsList(
                    guid = it.guid,
                    offset = offset
                )
            }
            .map { it.result.topics }
            .toObservable()
            .subscribeOn(schedulers.io())
    }

}