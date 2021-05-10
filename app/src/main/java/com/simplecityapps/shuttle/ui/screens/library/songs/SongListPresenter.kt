package com.simplecityapps.shuttle.ui.screens.library.songs

import android.content.Context
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import com.simplecityapps.mediaprovider.MediaImporter
import com.simplecityapps.mediaprovider.MediaProvider
import com.simplecityapps.mediaprovider.model.Song
import com.simplecityapps.mediaprovider.repository.SongQuery
import com.simplecityapps.mediaprovider.repository.SongRepository
import com.simplecityapps.mediaprovider.repository.SongSortOrder
import com.simplecityapps.playback.PlaybackManager
import com.simplecityapps.playback.queue.QueueManager
import com.simplecityapps.shuttle.R
import com.simplecityapps.shuttle.ui.common.error.UserFriendlyError
import com.simplecityapps.shuttle.ui.common.mvp.BaseContract
import com.simplecityapps.shuttle.ui.common.mvp.BasePresenter
import com.simplecityapps.shuttle.ui.screens.library.SortPreferenceManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*
import javax.inject.Inject

interface SongListContract {

    sealed class LoadingState {
        object Scanning : LoadingState()
        object Loading : LoadingState()
        object Empty : LoadingState()
        object None : LoadingState()
    }

    interface View {
        fun setData(songs: List<Song>, resetPosition: Boolean)
        fun updateSortOrder(sortOrder: SongSortOrder)
        fun showLoadError(error: Error)
        fun onAddedToQueue(songs: List<Song>)
        fun setLoadingState(state: LoadingState)
        fun setLoadingProgress(progress: Float)
        fun showDeleteError(error: Error)
    }

    interface Presenter : BaseContract.Presenter<View> {
        fun loadSongs(resetPosition: Boolean)
        fun onSongClicked(song: Song)
        fun addToQueue(songs: List<Song>)
        fun playNext(song: Song)
        fun exclude(song: Song)
        fun delete(song: Song)
        fun setSortOrder(songSortOrder: SongSortOrder)
        fun updateSortOrder()
        fun getFastscrollPrefix(song: Song): String?
    }
}

class SongListPresenter @Inject constructor(
    private val context: Context,
    private val playbackManager: PlaybackManager,
    private val songRepository: SongRepository,
    private val mediaImporter: MediaImporter,
    private val sortPreferenceManager: SortPreferenceManager,
    private val queueManager: QueueManager
) : BasePresenter<SongListContract.View>(),
    SongListContract.Presenter {

    var songs: List<Song> = emptyList()

    private val mediaImporterListener = object : MediaImporter.Listener {
        override fun onProgress(providerType: MediaProvider.Type, progress: Int, total: Int, song: Song) {
            view?.setLoadingProgress(progress / total.toFloat())
        }
    }

    override fun bindView(view: SongListContract.View) {
        super.bindView(view)

        view.updateSortOrder(sortPreferenceManager.sortOrderSongList)
    }

    override fun unbindView() {
        super.unbindView()

        mediaImporter.listeners.remove(mediaImporterListener)
    }

    override fun loadSongs(resetPosition: Boolean) {
        if (songs.isEmpty()) {
            if (mediaImporter.isImporting) {
                view?.setLoadingState(SongListContract.LoadingState.Scanning)
            } else {
                view?.setLoadingState(SongListContract.LoadingState.Loading)
            }
        }
        launch {
            songRepository
                .getSongs(SongQuery.All(sortOrder = sortPreferenceManager.sortOrderSongList))
                .filterNotNull()
                .distinctUntilChanged()
                .flowOn(Dispatchers.IO)
                .collect { songs ->
                    this@SongListPresenter.songs = songs
                    if (songs.isEmpty()) {
                        if (mediaImporter.isImporting) {
                            mediaImporter.listeners.add(mediaImporterListener)
                            view?.setLoadingState(SongListContract.LoadingState.Scanning)
                        } else {
                            mediaImporter.listeners.remove(mediaImporterListener)
                            view?.setLoadingState(SongListContract.LoadingState.Empty)
                        }
                    } else {
                        mediaImporter.listeners.remove(mediaImporterListener)
                        view?.setLoadingState(SongListContract.LoadingState.None)
                    }
                    view?.setData(songs, resetPosition)
                }
        }
    }

    override fun onSongClicked(song: Song) {
        launch {
            if (queueManager.setQueue(songs = songs, position = songs.indexOf(song))) {
                playbackManager.load { result ->
                    result.onSuccess {
                        playbackManager.play()
                    }
                    result.onFailure { error ->
                        view?.showLoadError(error as Error)
                    }
                }
            }
        }
    }

    override fun addToQueue(songs: List<Song>) {
        launch {
            playbackManager.addToQueue(songs)
            view?.onAddedToQueue(songs)
        }
    }

    override fun playNext(song: Song) {
        launch {
            playbackManager.playNext(listOf(song))
            view?.onAddedToQueue(listOf(song))
        }
    }

    override fun exclude(song: Song) {
        launch {
            songRepository.setExcluded(listOf(song), true)
            queueManager.remove(queueManager.getQueue().filter { queueItem -> songs.contains(queueItem.song) })
        }
    }

    override fun delete(song: Song) {
        val uri = song.path.toUri()
        val documentFile = DocumentFile.fromSingleUri(context, uri)
        if (documentFile?.delete() == true) {
            launch {
                songRepository.remove(song)
                queueManager.remove(queueManager.getQueue().filter { it.song == song })
            }
        } else {
            view?.showDeleteError(UserFriendlyError(context.getString(R.string.delete_song_failed)))
        }
    }

    override fun setSortOrder(songSortOrder: SongSortOrder) {
        if (sortPreferenceManager.sortOrderSongList != songSortOrder) {
            launch {
                withContext(Dispatchers.IO) {
                    sortPreferenceManager.sortOrderSongList = songSortOrder
                    this@SongListPresenter.songs = songs.sortedWith(songSortOrder.comparator)
                }
                view?.setData(songs, true)
                view?.updateSortOrder(songSortOrder)
            }
        }
    }

    override fun updateSortOrder() {
        view?.updateSortOrder(sortPreferenceManager.sortOrderSongList)
    }

    override fun getFastscrollPrefix(song: Song): String? {
        return when (sortPreferenceManager.sortOrderSongList) {
            SongSortOrder.SongName -> song.name?.firstOrNull()?.toString()
            SongSortOrder.ArtistGroupKeyComparator -> song.artistGroupKey.key?.firstOrNull()?.toString()?.toUpperCase(Locale.getDefault())
            SongSortOrder.AlbumName -> song.albumGroupKey.album?.firstOrNull()?.toString()?.toUpperCase(Locale.getDefault())
            SongSortOrder.Year -> song.year.toString()
            else -> null
        }
    }
}