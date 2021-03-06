package brunodles.animewatcher.nextepisodes

import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import brunodles.animewatcher.R
import brunodles.animewatcher.explorer.Episode
import brunodles.animewatcher.home.HomeActivity
import brunodles.animewatcher.persistence.Firebase
import brunodles.animewatcher.player.PlayerActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import java.util.concurrent.TimeUnit

class NextEpisodeFragment : Fragment() {
    private lateinit var adapter: EpisodeAdapter
    private lateinit var recyclerView: RecyclerView
    private val disposable = CompositeDisposable()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        recyclerView = inflater.inflate(R.layout.fragment_episode, container, false) as RecyclerView
        adapter = EpisodeAdapter()
        recyclerView.adapter = adapter
        recyclerView.layoutManager = GridLayoutManager(
            this.context,
            resources.getInteger(R.integer.home_columns),
            GridLayoutManager.VERTICAL,
            true
        )
        return recyclerView
    }

    override fun onStart() {
        super.onStart()
        adapter.setEpisodeClickListener(::onItemClick)
        FirebaseAuth.getInstance().currentUser?.let { suggestNextEpisode(it) }
    }

    private fun onItemClick(episode: Episode) {
        context?.let {
            startActivity(PlayerActivity.newIntent(it, episode))
        }
    }

    override fun onStop() {
        super.onStop()
        adapter.setEpisodeClickListener(null)
    }

    private fun suggestNextEpisode(user: FirebaseUser) {
        adapter.clear()
        disposable.add(Firebase.nextEpisodes(user)
            .subscribeOn(Schedulers.io())
            .take(5, TimeUnit.SECONDS)
            .doOnNext {
                Log.d(
                    HomeActivity.TAG,
                    "suggestNextEpisode: ${it.animeName} - ${it.number} ${it.description}"
                )
            }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(
                onNext = { adapter.add(it) },
                onError = { Log.e(HomeActivity.TAG, "suggestNextEpisode: ", it) },
                onComplete = {
                    Log.d(
                        HomeActivity.TAG,
                        "suggestNextEpisode: nextAdapter.count = ${adapter.itemCount}"
                    )
                }
            ))
    }
}
