package com.simplecityapps.mediaprovider.repository

import com.simplecityapps.mediaprovider.model.Song
import io.reactivex.Completable
import io.reactivex.Observable

interface SongRepository {

    fun populate(): Completable {
        return Completable.complete()
    }

    fun getSongs(): Observable<List<Song>>

    fun getSongs(query: SongQuery): Observable<List<Song>>
}


sealed class SongQuery {
    class AlbumArtistId(val albumArtistId: Long) : SongQuery()
    class AlbumId(val albumId: Long) : SongQuery()
    class SongId(val songId: Long) : SongQuery()
}

fun SongQuery.predicate(): (Song) -> Boolean {
    return when (this) {
        is SongQuery.AlbumArtistId -> { song -> song.albumArtistId == albumArtistId }
        is SongQuery.AlbumId -> { song -> song.albumId == albumId }
        is SongQuery.SongId -> { song -> song.id == songId }
    }
}