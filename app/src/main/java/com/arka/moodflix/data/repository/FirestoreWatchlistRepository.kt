package com.arka.moodflix.data.repository

import com.arka.moodflix.core.Logger
import com.arka.moodflix.domain.model.MediaType
import com.arka.moodflix.domain.model.Movie
import com.arka.moodflix.domain.model.watchlistId
import com.arka.moodflix.domain.repository.AuthRepository
import com.arka.moodflix.domain.repository.WatchlistRepository
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

/**
 * Per-user watchlist stored at users/{uid}/watchlist/{tmdbId:mediaType} in
 * Firestore, so it follows the signed-in account across devices instead of
 * living only on this phone (the previous Room-backed implementation).
 * Firestore's own offline cache (on by default) keeps it usable without
 * connectivity, syncing once back online.
 *
 * Both observe functions key off [AuthRepository.authState] rather than
 * reading the uid once, so a sign-out/sign-in mid-session (or switching
 * accounts) swaps to the new account's collection automatically instead of
 * continuing to stream the previous user's data or erroring out.
 */
class FirestoreWatchlistRepository(
    private val firestore: FirebaseFirestore,
    private val authRepository: AuthRepository
) : WatchlistRepository {

    private fun collection(uid: String): CollectionReference =
        firestore.collection(USERS_COLLECTION).document(uid).collection(WATCHLIST_COLLECTION)

    override fun observeWatchlist(): Flow<List<Movie>> =
        authRepository.authState
            .map { it?.uid }
            .distinctUntilChanged()
            .flatMapLatest { uid ->
                if (uid == null) flowOf(emptyList()) else observeCollection(uid)
            }

    private fun observeCollection(uid: String): Flow<List<Movie>> = callbackFlow {
        val registration = collection(uid)
            .orderBy(ADDED_AT_FIELD, Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    // A dropped listener (permissions hiccup, momentarily offline
                    // with no cache yet) shouldn't crash the collecting StateFlow -
                    // an empty list is the same "nothing saved yet" state the UI
                    // already renders correctly.
                    Logger.w(TAG, "observeWatchlist failed: $error")
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                trySend(snapshot?.documents?.mapNotNull { it.toMovie() }.orEmpty())
            }
        awaitClose { registration.remove() }
    }

    override fun observeIsSaved(tmdbId: Int, mediaType: MediaType): Flow<Boolean> {
        val id = "$tmdbId:${mediaType.name}"
        return authRepository.authState
            .map { it?.uid }
            .distinctUntilChanged()
            .flatMapLatest { uid ->
                if (uid == null) flowOf(false) else observeIsSavedDoc(uid, id)
            }
    }

    private fun observeIsSavedDoc(uid: String, id: String): Flow<Boolean> = callbackFlow {
        val registration = collection(uid).document(id)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Logger.w(TAG, "observeIsSaved failed: $error")
                    trySend(false)
                    return@addSnapshotListener
                }
                trySend(snapshot?.exists() == true)
            }
        awaitClose { registration.remove() }
    }

    override suspend fun toggle(movie: Movie): Boolean {
        val uid = authRepository.currentUser?.uid ?: return false
        val docRef = collection(uid).document(movie.watchlistId)

        return if (docRef.get().await().exists()) {
            docRef.delete().await()
            false
        } else {
            docRef.set(movie.toWatchlistFields()).await()
            true
        }
    }

    private fun Movie.toWatchlistFields(): Map<String, Any?> = mapOf(
        "tmdbId" to tmdbId,
        "mediaType" to mediaType.name,
        "title" to title,
        "year" to year,
        "posterUrl" to posterUrl,
        "rating" to rating,
        ADDED_AT_FIELD to System.currentTimeMillis()
    )

    private fun DocumentSnapshot.toMovie(): Movie? {
        val tmdbId = getLong("tmdbId")?.toInt() ?: return null
        val mediaType = getString("mediaType")?.let {
            runCatching { MediaType.valueOf(it) }.getOrNull()
        } ?: return null

        return Movie(
            tmdbId = tmdbId,
            mediaType = mediaType,
            title = getString("title").orEmpty(),
            year = getString("year").orEmpty(),
            overview = "",
            posterUrl = getString("posterUrl"),
            backdropUrl = null,
            rating = getDouble("rating")?.toFloat() ?: 0f,
            voteCount = 0,
            runtimeMinutes = null,
            seasonCount = null,
            episodeCount = null,
            genres = emptyList(),
            moodReason = "",
            trailer = null,
            watchProviders = emptyList(),
            justWatchLink = null
        )
    }

    private companion object {
        const val TAG = "FirestoreWatchlist"
        const val USERS_COLLECTION = "users"
        const val WATCHLIST_COLLECTION = "watchlist"
        const val ADDED_AT_FIELD = "addedAtEpochMillis"
    }
}
