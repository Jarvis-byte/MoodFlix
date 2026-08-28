package com.arka.moodflix.domain.repository

import android.content.Context
import com.arka.moodflix.domain.model.User
import kotlinx.coroutines.flow.StateFlow

interface AuthRepository {
    val authState: StateFlow<User?>
    val currentUser: User?

    /** Launches the Credential Manager Google Sign-In sheet and signs in with Firebase. */
    suspend fun signInWithGoogle(context: Context): Result<User>

    suspend fun signOut(context: Context)
}
