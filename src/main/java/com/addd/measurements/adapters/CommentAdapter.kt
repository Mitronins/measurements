package com.addd.measurements.adapters

import android.annotation.SuppressLint
import android.os.Build
import android.support.constraint.ConstraintLayout
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import com.addd.measurements.MyApp
import com.addd.measurements.R
import com.addd.measurements.formatDateTime
import com.addd.measurements.modelAPI.Comment
import com.addd.measurements.modelAPI.User


/**
 * Created by addd on 13.12.2017.
 */
class CommentAdapter(notesList: List<Comment>) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private var mCommentsList: List<Comment> = notesList
    private val ITEM = 0
    private val LOADING = 1
    private var isLoadingAdded = false

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder?, position: Int) {
        when (getItemViewType(position)) {
            ITEM -> {
                val viewHolder = holder as ViewHolder
                viewHolder.name.text = mCommentsList[position].user.firstName + " " + mCommentsList[position].user.lastName
                if (mCommentsList[position]?.user?.type ?: 1 >= 3) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        viewHolder.constraintLayout.setBackgroundColor(MyApp.instance.resources.getColor(R.color.backgroundAdmin, MyApp.instance.theme))
                    } else {
                        viewHolder.constraintLayout.setBackgroundColor(MyApp.instance.resources.getColor(R.color.backgroundAdmin))
                    }
                }
                viewHolder.date.text = formatDateTime(mCommentsList[position].date)
                viewHolder.text.text = mCommentsList[position].text
            }
            LOADING -> {
            }
        }

    }

    fun isEmpty() = mCommentsList.isEmpty()

    override fun getItemCount(): Int {
        return mCommentsList.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val v: View
        return if (viewType == ITEM) {
            v = LayoutInflater.from(parent?.context).inflate(R.layout.list_item_comment_problem, parent, false)
            return CommentAdapter.ViewHolder(v)
        } else {
            v = LayoutInflater.from(parent.context).inflate(R.layout.progressbar_item, parent, false)
            LoadingVH(v)
        }
    }

    fun add(mc: Comment) {
        (mCommentsList as ArrayList<Comment>).add(mc)
        notifyItemInserted(mCommentsList.size - 1)
    }

    fun addLoadingFooter() {
        isLoadingAdded = true
        add(Comment(0, User(), "", "", 0))
    }

    override fun getItemViewType(position: Int): Int {
        return if (position == mCommentsList.size - 1 && isLoadingAdded) LOADING else ITEM
    }

    fun removeLoadingFooter() {
        isLoadingAdded = false

        val position = mCommentsList.size - 1
        val item = getItem(position)

        if (item != null) {
            (mCommentsList as ArrayList<Comment>).removeAt(position)
            notifyItemRemoved(position)
        }
    }

    protected inner class LoadingVH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val proressBar: ProgressBar

        init {
            proressBar = itemView.findViewById(R.id.progressBar1)
        }
    }

    fun getItem(position: Int): Comment? {
        return mCommentsList[position]
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        var name: TextView
        var date: TextView
        var text: TextView
        var constraintLayout: ConstraintLayout

        init {
            date = itemView.findViewById(R.id.dateComment)
            name = itemView.findViewById(R.id.nameComment)
            text = itemView.findViewById(R.id.textComment)
            constraintLayout = itemView.findViewById(R.id.constraintLayout)
        }


    }
}