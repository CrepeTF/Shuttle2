package com.simplecityapps.adapter

import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListUpdateCallback
import androidx.recyclerview.widget.RecyclerView
import com.simplecityapps.diff.DiffCallbacks
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import timber.log.Timber

class RecyclerAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    var items = mutableListOf<ViewBinder>()
        private set

    private var disposable: Disposable? = null

    fun setData(newItems: List<ViewBinder>, animateChanges: Boolean = true, completion: (() -> Unit)? = null) {
        disposable?.dispose()

        if (animateChanges) {
            disposable = Single.fromCallable { DiffUtil.calculateDiff(DiffCallbacks(items, newItems)) }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { diffResult ->
                    items = newItems.toMutableList()
                    diffResult.dispatchUpdatesTo(LoggingListUpdateCallback())
                    diffResult.dispatchUpdatesTo(this)
                    completion?.invoke()
                }
        } else {
            items = newItems.toMutableList()
            notifyDataSetChanged()
        }
    }

    fun addItem(index: Int, newItem: ViewBinder) {
        items.add(index, newItem)
        notifyItemInserted(index)
    }

    fun addItem(newItem: ViewBinder) {
        addItem(items.size, newItem)
    }

    fun removeItem(index: Int) {
        items.removeAt(index)
        notifyItemRemoved(index)
    }

    fun removeItem(item: ViewBinder) {
        removeItem(items.indexOf(item))
    }

    fun clear(animateChanges: Boolean = true) {
        val count = items.size
        items.clear()

        if (animateChanges) {
            notifyItemRangeRemoved(0, count)
        } else {
            notifyDataSetChanged()
        }
    }

    override fun getItemViewType(position: Int): Int {
        return items[position].viewType().ordinal
    }

    override fun getItemCount(): Int {
        return items.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return items.firstOrNull { adapterViewModel -> adapterViewModel.viewType().ordinal == viewType }?.createViewHolder(
            parent
        ) ?: throw IllegalStateException("Cannot create ViewHolder for view viewType: $viewType")
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        @Suppress("UNCHECKED_CAST")
        items[position].bindViewHolder(holder as ViewBinder.ViewHolder<ViewBinder>)
    }

    private class LoggingListUpdateCallback : ListUpdateCallback {
        override fun onChanged(position: Int, count: Int, payload: Any?) {
            Timber.d("onChanged() $count")
        }

        override fun onMoved(fromPosition: Int, toPosition: Int) {
            Timber.d("onMoved() from: $fromPosition to: $toPosition")
        }

        override fun onInserted(position: Int, count: Int) {
            Timber.d("onInserted() $count")
        }

        override fun onRemoved(position: Int, count: Int) {
            Timber.d("onRemoved() $count")
        }
    }
}