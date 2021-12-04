package com.tomclaw.appsend.screen.chat.adapter.msg

import com.avito.konveyor.blueprint.Item
import com.avito.konveyor.blueprint.ItemBlueprint
import com.avito.konveyor.blueprint.ItemPresenter
import com.avito.konveyor.blueprint.ViewHolderBuilder
import com.tomclaw.appsend.R

class MsgItemBlueprint(override val presenter: ItemPresenter<MsgItemView, MsgItem>) :
    ItemBlueprint<MsgItemView, MsgItem> {

    override val viewHolderProvider = ViewHolderBuilder.ViewHolderProvider(
        layoutId = R.layout.store_item,
        creator = { _, view -> MsgItemViewHolder(view) }
    )

    override fun isRelevantItem(item: Item) = item is MsgItem

}
