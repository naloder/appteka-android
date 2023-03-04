package com.tomclaw.appsend.screen.upload

import android.os.Build
import android.os.Bundle
import com.avito.konveyor.adapter.AdapterPresenter
import com.avito.konveyor.blueprint.Item
import com.avito.konveyor.data_source.ListDataSource
import com.tomclaw.appsend.main.item.CommonItem
import com.tomclaw.appsend.screen.upload.adapter.ItemListener
import com.tomclaw.appsend.screen.upload.adapter.category.SelectCategoryItem
import com.tomclaw.appsend.screen.upload.adapter.description.DescriptionItem
import com.tomclaw.appsend.screen.upload.adapter.notice.NoticeItem
import com.tomclaw.appsend.screen.upload.adapter.notice.NoticeType
import com.tomclaw.appsend.screen.upload.adapter.select_app.SelectAppItem
import com.tomclaw.appsend.screen.upload.adapter.selected_app.SelectedAppItem
import com.tomclaw.appsend.screen.upload.adapter.whats_new.WhatsNewItem
import com.tomclaw.appsend.screen.upload.api.CheckExistResponse
import com.tomclaw.appsend.util.SchedulersFactory
import com.tomclaw.appsend.util.getParcelableCompat
import dagger.Lazy
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.plusAssign
import retrofit2.HttpException
import java.util.concurrent.TimeUnit

interface UploadPresenter : ItemListener {

    fun attachView(view: UploadView)

    fun detachView()

    fun attachRouter(router: UploadRouter)

    fun detachRouter()

    fun saveState(): Bundle

    fun onAppSelected(info: CommonItem)

    fun onBackPressed()

    interface UploadRouter {

        fun openSelectAppScreen()

        fun openDetailsScreen(appId: String, label: String?)

        fun leaveScreen()

    }

}

class UploadPresenterImpl(
    private val startInfo: CommonItem?,
    private val interactor: UploadInteractor,
    private val adapterPresenter: Lazy<AdapterPresenter>,
    private val schedulers: SchedulersFactory,
    state: Bundle?
) : UploadPresenter {

    private var view: UploadView? = null
    private var router: UploadPresenter.UploadRouter? = null

    private var packageInfo: CommonItem? = state?.getParcelableCompat(KEY_PACKAGE_INFO, CommonItem::class.java) ?: startInfo
    private var checkExist: CheckExistResponse? = state?.getParcelableCompat(KEY_CHECK_EXIST, CheckExistResponse::class.java)
    private var whatsNew: String = state?.getString(KEY_WHATS_NEW).orEmpty()
    private var description: String = state?.getString(KEY_DESCRIPTION).orEmpty()

    private val items = ArrayList<Item>()

    private val subscriptions = CompositeDisposable()

    override fun attachView(view: UploadView) {
        this.view = view

        subscriptions += view.navigationClicks().subscribe { onBackPressed() }
        subscriptions += view.retryClicks().subscribe { invalidate() }

        invalidate()
    }

    override fun detachView() {
        subscriptions.clear()
        this.view = null
    }

    override fun attachRouter(router: UploadPresenter.UploadRouter) {
        this.router = router
    }

    override fun detachRouter() {
        this.router = null
    }

    override fun saveState() = Bundle().apply {
        putParcelable(KEY_PACKAGE_INFO, packageInfo)
        putParcelable(KEY_CHECK_EXIST, checkExist)
        putString(KEY_WHATS_NEW, whatsNew)
        putString(KEY_DESCRIPTION, description)
    }

    override fun onAppSelected(info: CommonItem) {
        this.packageInfo = info
        checkAppUploaded()
    }

    override fun onBackPressed() {
        router?.leaveScreen()
    }

    private fun checkAppUploaded() {
        val packageInfo = packageInfo ?: return
        subscriptions += interactor
            .calculateSha1(packageInfo.path)
            .flatMap { interactor.checkExist(it) }
            .observeOn(schedulers.mainThread())
            .retryWhen { errors ->
                errors.flatMap {
                    if (it is HttpException) {
                        throw it
                    }
                    println("[upload] Retry after exception: " + it.message)
                    Observable.timer(3, TimeUnit.SECONDS)
                }
            }
            .doOnSubscribe {
                view?.hideError()
                view?.showProgress()
            }
            .subscribe(
                { onCheckExistLoaded(it) },
                { onCheckExistError() }
            )
    }

    private fun onCheckExistLoaded(response: CheckExistResponse) {
        this.checkExist = response
        view?.showContent()
        bindForm()
    }

    private fun onCheckExistError() {
        view?.showContent()
        view?.showError()
    }

    private fun bindForm() {
        var id: Long = 1

        items.clear()
        val packageInfo = this.packageInfo
        if (packageInfo != null) {
            items += SelectedAppItem(id++, packageInfo)
        } else {
            items += SelectAppItem(id++)
        }

        checkExist?.let { checkExist ->
            val clickable = checkExist.file != null
            if (checkExist.info?.isEmpty() == false) {
                items += NoticeItem(id++, NoticeType.INFO, checkExist.info, clickable)
            }
            if (checkExist.warning?.isEmpty() == false) {
                items += NoticeItem(id++, NoticeType.WARNING, checkExist.warning, clickable)
            }
            if (checkExist.error?.isEmpty() == false) {
                items += NoticeItem(id++, NoticeType.ERROR, checkExist.error, clickable)
            }
        }

        items += SelectCategoryItem(id++, category = null)

        items += WhatsNewItem(id++, text = whatsNew)

        items += DescriptionItem(id++, text = description)

        bindItems()

        view?.contentUpdated()
    }

    private fun bindItems() {
        val dataSource = ListDataSource(items)
        adapterPresenter.get().onDataSourceChanged(dataSource)
    }

    private fun invalidate() {
        if (checkExist == null && packageInfo != null) {
            checkAppUploaded()
        } else {
            bindForm()
        }
    }

    override fun onSelectAppClick() {
        router?.openSelectAppScreen()
    }

    override fun onDiscardClick() {
        this.packageInfo = null
        this.checkExist = null
        invalidate()
    }

    override fun onNoticeClick() {
        val file = checkExist?.file ?: return
        router?.openDetailsScreen(appId = file.appId, label = file.title)
    }

    override fun onCategoryClick() {

    }

    override fun onWhatsNewChanged(text: String) {
        whatsNew = text
    }

    override fun onDescriptionChanged(text: String) {
        description = text
    }

}

private const val KEY_PACKAGE_INFO = "package_info"
private const val KEY_CHECK_EXIST = "check_exist"
private const val KEY_WHATS_NEW = "whats_new"
private const val KEY_DESCRIPTION = "description"
