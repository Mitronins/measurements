package ru.nextf.measurements.modelAPI

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName


/**
 * Created by addd on 28.12.2017.
 */
class Mount {
    @SerializedName("id")
    @Expose
    val id: Int? = null
    @SerializedName("actions")
    @Expose
    val actions: List<Action>? = null
    @SerializedName("comments")
    @Expose
    val comments: List<Comment>? = null
    @SerializedName("installers")
    @Expose
    val installers: List<Installers>? = null
    @SerializedName("company")
    @Expose
    val company: Company? = null
    @SerializedName("date_mount")
    @Expose
    val dateMount: String? = null
    @SerializedName("date")
    @Expose
    val date: String? = null
    @SerializedName("status")
    @Expose
    val status: Int? = null
    @SerializedName("description")
    @Expose
    val description: String? = null
    @SerializedName("deal")
    @Expose
    val deal: Int? = null
    @SerializedName("user")
    @Expose
    val user: User? = null
}