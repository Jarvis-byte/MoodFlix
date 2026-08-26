# MoodFlix

A mood-first film recommender. You tell it how you feel; it tells you what to watch, where to watch it, and shows you the trailer.

Runs entirely on the user's own AI keys, so the app costs nothing to operate.

---

## How to run it

1. Open the project in Android Studio (Koala or newer).
2. Get a free TMDB API key: https://www.themoviedb.org/settings/api
3. Copy `local.properties.example` to `local.properties` and fill in:
   ```
   sdk.dir=/your/android/sdk
   TMDB_API_KEY=your_key
   ```
4. Run. On first launch, tap the settings icon and connect at least one AI provider (Gemini's free tier is the easiest starting point).

---

## Architecture

Single-module Clean Architecture with MVVM in the presentation layer. Dependencies point inward — `ui` knows about `domain`, `data` knows about `domain`, and `domain` knows about nothing.

```
ui/          Compose screens + ViewModels (StateFlow, unidirectional data flow)
domain/      Models, repository interfaces, use cases. Pure Kotlin, no Android imports.
data/        Retrofit + OkHttp clients, encrypted key storage, repository implementations.
di/          Hilt modules.
```

### The AI layer

`AiProviderClient` is one interface with three implementations (Gemini, OpenAI, Anthropic). They're bound into a `Set` via Hilt multibinding, so adding a fourth provider means writing one class and one `@Binds @IntoSet` line — nothing else changes.

`AiRouter` walks the user's providers in their configured order:

- **429 / quota exhausted** → skip to the next provider
- **401 / 403 invalid key** → skip to the next provider
- **Network failure** → abort the whole chain immediately, because trying three more hosts with no connectivity just makes the user wait three timeouts for the same error
- **All providers exhausted** → surface the last real error, with a "connect a backup" action

### Why the AI doesn't supply the facts

The model is asked for *titles and one-line reasons only*. Every factual field — rating, runtime, poster, streaming availability, trailer — is resolved from TMDB. LLMs hallucinate ratings and confidently claim films are on Netflix when they aren't.

The repository resolves all AI titles against TMDB in parallel (`async`/`awaitAll`) and silently drops any title that doesn't resolve. That's the safety net for the occasional invented film.

### Key storage

`EncryptedSharedPreferences` backed by the Android Keystore. Keys never touch a server (there isn't one), never appear in release logs (`HttpLoggingInterceptor` is `NONE` in release), and are excluded from cloud backup and device transfer in `data_extraction_rules.xml`.

### Flow usage

`MovieRepository.recommend()` returns a `Flow<RecommendationState>` that emits progressively:

```
AskingAi → AiResponded(count, provider) → Enriched(movies, provider)
```

This is why the loading label changes from "Reading your mood" to "Found 8 picks, looking them up" instead of showing one undifferentiated spinner for eight seconds.

---

## Stack

Jetpack Compose · Material 3 · Kotlin Coroutines + Flow · Hilt · Retrofit + OkHttp · kotlinx.serialization · Coil · Navigation Compose · DataStore · Security Crypto · Custom Tabs

---

## Worth building next

- **Room cache** — the dependency is already wired in `build.gradle.kts`. Cache `MoodQuery → results` so re-running the same mood doesn't burn a quota call.
- **Inline trailer playback** — Media3/ExoPlayer is already a dependency. Currently trailers open in a Custom Tab; an in-app player behind a `HorizontalPager` would be nicer.
- **Watchlist** — a Room table plus a bottom nav destination.
- **Streaming-service filter** — TMDB's `watch_providers` supports filtering `discover` by provider, so "only show me things on what I already pay for" is a small change.
- **Tests** — `AiRouter` is the highest-value target: fake three `AiProviderClient`s, assert the chain order and the network-abort behaviour. `PromptBuilder.parseSuggestions` deserves table-driven tests with real malformed model output.
