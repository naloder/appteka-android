package com.tomclaw.appsend.screen.subscribers

import android.os.Bundle
import com.avito.konveyor.adapter.AdapterPresenter
import com.avito.konveyor.blueprint.Item
import com.avito.konveyor.data_source.ListDataSource
import com.tomclaw.appsend.screen.subscribers.adapter.ItemListener
import com.tomclaw.appsend.screen.subscribers.adapter.subscriber.SubscriberItem
import com.tomclaw.appsend.screen.subscribers.api.SubscriberEntity
import com.tomclaw.appsend.util.SchedulersFactory
import com.tomclaw.appsend.util.getParcelableArrayListCompat
import com.tomclaw.appsend.util.retryWhenNonAuthErrors
import dagger.Lazy
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.plusAssign

interface SubscribersPresenter : ItemListener {

    fun attachView(view: SubscribersView)

    fun detachView()

    fun attachRouter(router: SubscribersRouter)

    fun detachRouter()

    fun saveState(): Bundle

    fun onUpdate()

    fun invalidateApps()

    interface SubscribersRouter {

        fun openProfileScreen(userId: Int)

        fun leaveScreen()

    }

}

class SubscribersPresenterImpl(
    private val userId: Int,
    private val interactor: SubscribersInteractor,
    private val adapterPresenter: Lazy<AdapterPresenter>,
    private val converter: UserConverter,
    private val schedulers: SchedulersFactory,
    state: Bundle?
) : SubscribersPresenter {

    private var view: SubscribersView? = null
    private var router: SubscribersPresenter.SubscribersRouter? = null

    private val subscriptions = CompositeDisposable()

    private var items: List<SubscriberItem>? =
        state?.getParcelableArrayListCompat(KEY_APPS, SubscriberItem::class.java)
    private var isError: Boolean = state?.getBoolean(KEY_ERROR) ?: false

    override fun attachView(view: SubscribersView) {
        this.view = view

        subscriptions += view.retryClicks().subscribe {
            loadApps()
        }
        subscriptions += view.refreshClicks().subscribe {
            invalidateApps()
        }

        if (isError) {
            onError()
            onReady()
        } else {
            items?.let { onReady() } ?: loadApps()
        }
    }

    override fun detachView() {
        subscriptions.clear()
        this.view = null
    }

    override fun attachRouter(router: SubscribersPresenter.SubscribersRouter) {
        this.router = router
    }

    override fun detachRouter() {
        this.router = null
    }

    override fun saveState() = Bundle().apply {
        putParcelableArrayList(KEY_APPS, items?.let { ArrayList(items.orEmpty()) })
        putBoolean(KEY_ERROR, isError)
    }

    override fun invalidateApps() {
        items = null
        loadApps()
    }

    private fun loadApps() {
        subscriptions += interactor.listSubscribers(userId)
            .observeOn(schedulers.mainThread())
            .doOnSubscribe { if (view?.isPullRefreshing() == false) view?.showProgress() }
            .doAfterTerminate { onReady() }
            .subscribe(
                { onLoaded(it) },
                {
                    it.printStackTrace()
                    onError()
                }
            )
    }

    private fun loadApps(offsetId: Int) {
        subscriptions += interactor.listSubscribers(userId, offsetId)
            .observeOn(schedulers.mainThread())
            .retryWhenNonAuthErrors()
            .doAfterTerminate { onReady() }
            .subscribe(
                { onLoaded(it) },
                { onLoadMoreError() }
            )
    }

    private fun onLoaded(entities: List<SubscriberEntity>) {
        isError = false
        val newItems = entities
            .map { converter.convert(it) }
            .toList()
            .apply { if (isNotEmpty()) last().hasMore = true }
        this.items = this.items
            ?.apply { if (isNotEmpty()) last().hasProgress = false }
            ?.plus(newItems) ?: newItems
    }

    private fun onReady() {
        val items = this.items
        when {
            isError -> {
                view?.showError()
            }

            items.isNullOrEmpty() -> {
                view?.showPlaceholder()
            }

            else -> {
                val dataSource = ListDataSource(items)
                adapterPresenter.get().onDataSourceChanged(dataSource)
                view?.let {
                    it.contentUpdated()
                    if (it.isPullRefreshing()) {
                        it.stopPullRefreshing()
                    } else {
                        it.showContent()
                    }
                }
            }
        }
    }

    private fun onError() {
        this.isError = true
    }

    private fun onLoadMoreError() {
        items?.last()
            ?.apply {
                hasProgress = false
                hasMore = false
                hasError = true
            }
    }

    override fun onUpdate() {
        view?.contentUpdated()
    }

    override fun onItemClick(item: Item) {
        val sub = items?.find { it.id == item.id } ?: return
        router?.openProfileScreen(sub.user.userId)
    }

    override fun onRetryClick(item: Item) {
        val sub = items?.find { it.id == item.id } ?: return
        if (items?.isNotEmpty() == true) {
            items?.last()?.let {
                it.hasProgress = true
                it.hasError = false
            }
            items?.indexOf(sub)?.let {
                view?.contentUpdated(it)
            }
        }
        loadApps(sub.id.toInt())
    }

    override fun onLoadMore(item: Item) {
        val sub = items?.find { it.id == item.id } ?: return
        loadApps(sub.id.toInt())
    }

}

private const val KEY_APPS = "apps"
private const val KEY_ERROR = "error"
