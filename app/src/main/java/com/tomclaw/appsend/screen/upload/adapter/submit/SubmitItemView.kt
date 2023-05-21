package com.tomclaw.appsend.screen.upload.adapter.submit

import android.view.View
import android.widget.Button
import com.avito.konveyor.adapter.BaseViewHolder
import com.avito.konveyor.blueprint.ItemView
import com.tomclaw.appsend.R

interface SubmitItemView : ItemView {

    fun setEditMode()

    fun setUploadMode()

    fun setOnClickListener(listener: (() -> Unit)?)

}

class SubmitItemViewHolder(view: View) : BaseViewHolder(view), SubmitItemView {

    private val context = view.context
    private val uploadButton: Button = view.findViewById(R.id.upload_button)

    private var clickListener: (() -> Unit)? = null

    init {
        uploadButton.setOnClickListener { clickListener?.invoke() }
    }

    override fun setEditMode() {
        uploadButton.text = context.getText(R.string.edit_meta)
    }

    override fun setUploadMode() {
        uploadButton.text = context.getText(R.string.upload_app)
    }

    override fun setOnClickListener(listener: (() -> Unit)?) {
        this.clickListener = listener
    }

    override fun onUnbind() {
        this.clickListener = null
    }

}
