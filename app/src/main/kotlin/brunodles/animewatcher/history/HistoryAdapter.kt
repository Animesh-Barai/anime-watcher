package brunodles.animewatcher.history

import android.annotation.SuppressLint
import android.databinding.DataBindingUtil
import android.databinding.ViewDataBinding
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import brunodles.animewatcher.ImageLoader
import brunodles.animewatcher.R
import brunodles.animewatcher.databinding.ItemEmptyBinding
import brunodles.animewatcher.databinding.ItemEpisodeBinding
import brunodles.animewatcher.databinding.ItemUnknownBinding
import brunodles.animewatcher.explorer.Episode
import brunodles.animewatcher.persistence.Firebase
import brunodles.collection.ArrayWithKeys
import brunodles.rxfirebase.EventType
import brunodles.rxfirebase.TypedEvent
import brunodles.rxfirebase.singleObservable
import brunodles.rxfirebase.typedChildObserver
import com.google.firebase.auth.FirebaseUser
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers

typealias OnItemClick<ITEM_TYPE> = (ITEM_TYPE) -> Unit

class HistoryAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        const val TYPE_UNKNOWN = 0
        const val TYPE_EMPTY = 1
        const val TYPE_EPISODE = 2
        const val TYPE_LINK = 3

        const val TAG = "HomeAdapter"
    }

    private var updatesDisposable: Disposable? = null
    private val list = ArrayWithKeys<String, Episode>()
    private var layoutInflater: LayoutInflater? = null
    private var onEpisodeClickListener: OnItemClick<Episode>? = null
    private var internalEpisodeClickListener: OnItemClick<Episode> = {
        onEpisodeClickListener?.invoke(it)
    }

    fun setEpisodeClickListener(listener: OnItemClick<Episode>) {
        onEpisodeClickListener = listener
    }

    override fun getItemCount(): Int = if (list.isEmpty()) 1 else list.size

    override fun getItemViewType(position: Int): Int {
        if (list.isEmpty()) return TYPE_EMPTY
        return TYPE_EPISODE
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (getItemViewType(position)) {
            TYPE_EPISODE -> (holder as EpisodeHolder).let {
                it.onBind(list[position])
                it.clickListener = internalEpisodeClickListener
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val context = parent.context
        if (layoutInflater == null)
            layoutInflater = LayoutInflater.from(context)

        return when (viewType) {
            TYPE_EMPTY -> EmptyHolder(
                DataBindingUtil.inflate(
                    layoutInflater!!,
                    R.layout.item_empty,
                    parent,
                    false
                )
            )
            TYPE_EPISODE -> EpisodeHolder(
                DataBindingUtil.inflate(
                    layoutInflater!!,
                    R.layout.item_episode,
                    parent,
                    false
                )
            )
            else -> UnknownHolder(
                DataBindingUtil.inflate(
                    layoutInflater!!,
                    R.layout.item_unknown,
                    parent,
                    false
                )
            )
        }
    }

    open class ViewHolder<out BINDER : ViewDataBinding, ITEM : Any>(protected val binder: BINDER) :
        RecyclerView.ViewHolder(binder.root) {

        var clickListener: OnItemClick<ITEM>? = null

        open fun onBind(item: ITEM) {
            binder.root.setOnClickListener { clickListener?.invoke(item) }
        }
    }

    class EmptyHolder(binder: ItemEmptyBinding) : ViewHolder<ItemEmptyBinding, Any>(binder)
    class UnknownHolder(binder: ItemUnknownBinding) : ViewHolder<ItemUnknownBinding, Any>(binder) {
        override fun onBind(item: Any) {
            super.onBind(item)
            binder.text.text = item.toString()
        }
    }

    class EpisodeHolder(binder: ItemEpisodeBinding) :
        ViewHolder<ItemEpisodeBinding, Episode>(binder) {
        @SuppressLint("SetTextI18n")
        override fun onBind(item: Episode) {
            super.onBind(item)
            if (item.number > 0)
                binder.description.text = "${item.number} - ${item.description}"
            else
                binder.description.text = item.description
            binder.title.text = item.animeName
            binder.image.setImageResource(R.drawable.img_loading)
            ImageLoader.loadImageInto(item.image, binder.image)
        }
    }

    fun setUser(user: FirebaseUser) {
        updatesDisposable?.dispose()
        updatesDisposable = Firebase.history(user)
            .limitToLast(100)
            .orderByKey()
            .typedChildObserver(String::class.java)
            .subscribeOn(Schedulers.io())
            .flatMapSingle { link ->
                Firebase.videoRef(link.element)
                    .singleObservable(Episode::class.java)
                    .subscribeOn(Schedulers.io())
                    .map { TypedEvent(link.event, it, link.key) }
            }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(
                onNext = {
                    when (it.event) {
                        EventType.CHANGED -> {
                            val index = list.replace(it.element, it.key)
                            if (index >= 0) notifyItemChanged(index)
                        }
                        EventType.MOVED -> TODO()
                        EventType.ADDED -> {
                            val previousSize = list.size
                            val index = list.add(it.element, it.key)
                            if (index >= 0 && previousSize == 0)
                                notifyItemChanged(index)
                            else
                                notifyItemInserted(index)
                        }
                        EventType.REMOVED -> {
                            val index = list.removeByKey(it.key)
                            if (list.isEmpty())
                                notifyItemChanged(0)
                            else
                                notifyItemRemoved(index)
                        }
                    }
                },
                onError = {
                    Log.e(TAG, "setUser: ", it)
                }
            )
    }

    fun disconnect() {
        updatesDisposable?.dispose()
        list.clear()
        notifyDataSetChanged()
    }
}
