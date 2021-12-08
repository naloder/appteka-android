package com.tomclaw.appsend.screen.chat.di

import android.content.Context
import android.os.Bundle
import com.avito.konveyor.ItemBinder
import com.avito.konveyor.adapter.AdapterPresenter
import com.avito.konveyor.adapter.SimpleAdapterPresenter
import com.avito.konveyor.blueprint.ItemBlueprint
import com.tomclaw.appsend.screen.chat.ChatInteractor
import com.tomclaw.appsend.screen.chat.ChatInteractorImpl
import com.tomclaw.appsend.screen.chat.ChatPresenter
import com.tomclaw.appsend.screen.chat.ChatPresenterImpl
import com.tomclaw.appsend.screen.chat.adapter.msg.IncomingMsgItemBlueprint
import com.tomclaw.appsend.screen.chat.adapter.msg.IncomingMsgItemPresenter
import com.tomclaw.appsend.screen.chat.adapter.outgoing.OutgoingMsgItemBlueprint
import com.tomclaw.appsend.screen.chat.adapter.outgoing.OutgoingMsgItemPresenter
import com.tomclaw.appsend.util.PerActivity
import com.tomclaw.appsend.util.SchedulersFactory
import dagger.Lazy
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoSet

@Module
class ChatModule(
    private val context: Context,
    private val topicId: Int,
    private val state: Bundle?
) {

    @Provides
    @PerActivity
    internal fun providePresenter(
        interactor: ChatInteractor,
        adapterPresenter: Lazy<AdapterPresenter>,
        schedulers: SchedulersFactory
    ): ChatPresenter = ChatPresenterImpl(
        topicId,
        interactor,
        adapterPresenter,
        schedulers,
        state
    )

    @Provides
    @PerActivity
    internal fun provideInteractor(
        schedulers: SchedulersFactory
    ): ChatInteractor = ChatInteractorImpl(schedulers)

    @Provides
    @PerActivity
    internal fun provideAdapterPresenter(binder: ItemBinder): AdapterPresenter {
        return SimpleAdapterPresenter(binder, binder)
    }

    @Provides
    @PerActivity
    internal fun provideItemBinder(
        blueprintSet: Set<@JvmSuppressWildcards ItemBlueprint<*, *>>
    ): ItemBinder {
        return ItemBinder.Builder().apply {
            blueprintSet.forEach { registerItem(it) }
        }.build()
    }

    @Provides
    @IntoSet
    @PerActivity
    internal fun provideIncomingMsgItemBlueprint(
        presenter: IncomingMsgItemPresenter
    ): ItemBlueprint<*, *> = IncomingMsgItemBlueprint(presenter)

    @Provides
    @IntoSet
    @PerActivity
    internal fun provideOutgoingMsgItemBlueprint(
        presenter: OutgoingMsgItemPresenter
    ): ItemBlueprint<*, *> = OutgoingMsgItemBlueprint(presenter)

    @Provides
    @PerActivity
    internal fun provideIncomingMsgItemPresenter(presenter: ChatPresenter) =
        IncomingMsgItemPresenter(presenter)

    @Provides
    @PerActivity
    internal fun provideOutgoingMsgItemPresenter(presenter: ChatPresenter) =
        OutgoingMsgItemPresenter(presenter)

}
