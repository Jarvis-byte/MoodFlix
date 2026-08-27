package com.arka.moodflix.core

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

/**
 * Single-shot read of a Flow's first emission. Extension functions on a
 * generic external interface like Flow don't bridge as Swift instance
 * methods - they surface as static calls on this file's Kt-class instead:
 *
 *   try await FlowBridgeKt.firstForSwift(someFlow)
 */
suspend fun <T> Flow<T>.firstForSwift(): T = first()

/**
 * Kotlin suspend functions surface to Swift as `async` functions automatically,
 * but a cold Flow has no Swift-side equivalent without extra tooling (e.g. the
 * KMP-NativeCoroutines library, which is worth adopting once the iOS app is
 * real). Until then, this turns a Flow into a plain suspend call that delivers
 * each emission through a callback. Same static-call caveat as firstForSwift:
 *
 *   try await FlowBridgeKt.collectForSwift(someFlow) { value in ... }
 */
suspend fun <T> Flow<T>.collectForSwift(onEach: (T) -> Unit) {
    collect { onEach(it) }
}
