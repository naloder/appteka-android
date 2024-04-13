package com.tomclaw.appsend.screen.home.api

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
data class StartupResponse(
    @SerializedName("files_count")
    val filesCount: Int,
    @SerializedName("msgs_count")
    val msgsCount: Int,
    @SerializedName("ratings_count")
    val ratingsCount: Int,
) : Parcelable
