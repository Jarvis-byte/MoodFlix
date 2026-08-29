package com.arka.moodflix.domain.repository

import android.content.Context
import com.arka.moodflix.domain.model.User
import kotlinx.coroutines.flow.StateFlow

interface AuthRepository {
    val authState: StateFlow<User?>
    val currentUser: User?

    /** Launches the Credential Manager Google Sign-In sheet and signs in with Firebase. */
    suspend fun signInWithGoogle(context: Context): Result<User>

    /** Signs in with a Firebase email/password account. */
    suspend fun signInWithEmail(email: String, password: String): Result<User>

    /** Creates a new Firebase email/password account and signs into it. */
    suspend fun signUpWithEmail(email: String, password: String): Result<User>

    suspend fun signOut(context: Context)
}
