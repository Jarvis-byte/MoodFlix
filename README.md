# 🎬 MoodFlix

> Tell it how you feel. It tells you what to watch, where to watch it, and shows you the trailer.

**MoodFlix is a mood-first film & web-series recommender powered by your own AI keys — Gemini,
ChatGPT, or Claude. The app costs ₹0 to run. No backend. No ads. No subscription.**

---

## 📸 Screenshots

<p align="center">
   <img src="https://raw.githubusercontent.com/Jarvis-byte/MoodFlix/main/Screenshot_20260827_141117.png" alt="Discover" width="220"/>
   <img src="https://raw.githubusercontent.com/Jarvis-byte/MoodFlix/main/Screenshot_20260827_141150.png" alt="Settings" width="220"/>
   <img src="https://raw.githubusercontent.com/Jarvis-byte/MoodFlix/main/Screenshot_20260827_141129.png" alt="Loading" width="220"/>
   <img src="https://raw.githubusercontent.com/Jarvis-byte/MoodFlix/main/Screenshot_20260827_141139.png" alt="Results" width="220"/>
   <img src="https://github.com/Jarvis-byte/MoodFlix/blob/main/Screenshot_20260827_142157.png" alt="Detail screen" width="220"/>
</p>

---

## ✨ Features

### 🎭 Mood-first discovery

Pick from 8 moods — Cozy, Hyped, Melancholy, Mind-bending, Romantic, Funny, Tense, or Inspired. The
AI picks titles that genuinely fit the emotional state, not just the genre tag.

### 🎬 Movies + Web Series

Toggle between **Movies**, **Series**, or **Both** — results mix films and shows in the same list.
Series cards show season count and per-episode runtime instead of a total runtime, because 45 min/ep
and 5 seasons are different information from a 2-hour film.

### 📺 OTT platform filter

Choose one or more platforms (Netflix, JioHotstar, Amazon Prime, SonyLIV, Zee5, Apple TV+…). The
list is fetched live from TMDB per region — so it always reflects current branding without an app
update. MoodFlix uses TMDB's own `display_priorities` field to rank platforms by local popularity.

### 🤖 BYOK — Bring Your Own Key

Your AI cost is ₹0 because MoodFlix uses your own free API keys:

- **Google Gemini** — [Get a free key](https://aistudio.google.com/app/apikey) (easiest starting
  point)
- **OpenAI** — [Get a key](https://platform.openai.com/api-keys)
- **Anthropic Claude** — [Get a key](https://console.anthropic.com/settings/keys)

Connect as many as you like. If one quota runs out, MoodFlix automatically falls back to the next.

### 🔄 Automatic AI fallback chain

If Gemini's free quota is exhausted, MoodFlix silently tries OpenAI, then Claude — in your
configured order. You never see a dead screen; results just keep coming. A network failure aborts
the chain immediately (no point waiting through three timeouts with no connectivity).

### 🗄️ TMDB fallback when all AI fails

Even with zero AI keys connected, the app works — it falls back to TMDB's own
popularity/rating-sorted discover endpoint, filtered by your genre, rating, and OTT selection. The
UI honestly labels these as "popular picks" rather than AI-curated ones.

### 🎞️ Animated loading state

A spinning film-reel icon with rotating playful status lines ("Consulting the film critics", "
Untangling the film reels"…) keeps users on screen during the AI call — which can take 4-8 seconds
on a free Gemini key.

### 🔒 Keys stay on your device

API keys are encrypted with `EncryptedSharedPreferences` backed by the Android Keystore. They never
leave the phone — there is no MoodFlix backend, no analytics, no server of any kind. Keys are
explicitly excluded from cloud backup and device transfer.

### 📄 Progressive loading

Results appear as they resolve — the AI answers first, then each title is enriched with TMDB data (
poster, rating, trailer, watch providers) in parallel. The loading label updates from *"Reading your
mood"* → *"Found 8 picks, looking them up"* so users always know what's happening.

### ▶️ Trailers + where to watch

Every card shows streaming availability (with platform logos) for your country, and a YouTube
trailer thumbnail with a play button. Tapping opens the trailer in a Custom Tab — the user stays in
their existing Google session.

### 💬 First-run coach mark

On first launch, a tooltip automatically pops up pointing at the Settings icon, prompting the user
to connect their AI provider. It shows once and never again (persisted via DataStore).

---

## 🚀 How to run

### Prerequisites

- Android Studio Koala (2024.1.1) or newer
- JDK 17
- Android SDK 26+

### Setup

1. **Clone the repo**
   ```bash
   git clone https://github.com/YOUR_USERNAME/MoodFlix.git
   cd MoodFlix
   ```

2. **Get a free TMDB API key**
   Sign up at [themoviedb.org](https://www.themoviedb.org/settings/api) — it's instant and free.

3. **Create `local.properties`**
   ```properties
   sdk.dir=/Users/your-name/Library/Android/sdk
   TMDB_API_KEY=your_tmdb_v3_key_here
   ```
   > ⚠️ Never commit `local.properties` — it's already in `.gitignore`.

4. **Build and run**
   Open in Android Studio → Run on emulator or device.

5. **Connect an AI provider**
   On first launch, tap the ⚙️ icon → paste your Gemini key → tap **Save**. Done.

---

## 🏗️ Architecture

Single-module **Clean Architecture** with **MVVM** in the presentation layer. Dependencies point
strictly inward.

```
ui/
  discover/      Mood + genre + OTT + media type picker
  results/       Progressive results list with load-more
  detail/        Full film/series detail with trailer and watch providers
  settings/      AI key management and fallback order
  components/    MovieCard, MoodChip, ProviderChip, RatingSlider, ReelLoadingAnimation
  navigation/    Type-safe nav graph (mood + OTT filter encoded in route)
  theme/         Material 3 dark/light colour schemes, typography

domain/
  model/         Movie, Mood, Genre, MoodQuery, OttProvider, MediaType(Filter), AiSuggestion
  repository/    MovieRepository, AiKeyRepository interfaces
  usecase/       GetRecommendationsUseCase, GetOttProvidersUseCase, ObserveConnectedProvidersUseCase

data/
  remote/tmdb/   TmdbApi (Retrofit), DTOs, Mapper
  remote/ai/     AiProviderClient interface, GeminiClient, OpenAiClient, AnthropicClient, AiRouter, PromptBuilder
  repository/    MovieRepositoryImpl, AiKeyRepositoryImpl
  local/         SecureKeyStore (EncryptedSharedPreferences), UserPreferences (DataStore)

di/
  NetworkModule  OkHttp, Retrofit, Json
  RepositoryModule  Hilt multibinding for AI providers
```

---

## 🤖 The AI layer in detail

### Why multibinding?

`AiProviderClient` is a single interface. Each provider is a `@Singleton` class injected into a
`Set<AiProviderClient>` via Hilt multibinding. Adding a fourth provider (say, Mistral) means writing
one class + one `@Binds @IntoSet` line — nothing else changes.

### The fallback chain (`AiRouter`)

```
Try provider 1 (user's configured order)
  429 / quota exhausted  → skip, try provider 2
  401 / invalid key      → skip, try provider 2
  Network failure        → abort chain, surface error immediately
  Success                → return result
Try provider 2...
Try provider 3...
All failed               → surface last real error + "add a backup key" action
```

### Why the AI only picks titles

The model returns **only titles + one-line mood reasons**. Every factual field — rating, runtime,
poster URL, streaming availability, trailer — comes from TMDB. This matters because:

- LLMs hallucinate IMDb ratings and confidently claim films are on Netflix when they aren't
- TMDB data is always current; model training data has a cutoff
- Separating opinion (AI) from fact (TMDB) makes the JSON schema predictable and testable

### Resolution flow for a single title suggestion

```
AI returns: { title: "Dune: Part Two", year: "2024", type: "movie", reason: "..." }
  → searchMovie("Dune: Part Two", year: "2024")  // TMDB search
  → getMovieDetail(id, append: "videos,watch/providers")  // one round-trip
  → toDomain(moodReason, countryCode)  // mapper produces the Movie domain object
  If search returns nothing → try searchTv() as fallback (AI classification is ~95% right)
  If still nothing → silently drop (hallucination safety net)
```

### TMDB-only fallback (`discoverFallback`)

When all AI providers fail or the user has zero keys:

- `discover/movie` and `discover/tv` run in parallel (both `async`, budget split 5+5)
- Filtered by genre, minimum rating, OTT provider IDs (pipe-separated = OR), and TMDB page
- "Load more" increments `MoodQuery.page` so successive taps page through TMDB's ranked list rather
  than re-fetching page 1

### OTT filter accuracy

`GET /watch/providers/movie?watch_region=IN` returns the live provider catalog with real IDs.
Filtering uses `providerId` (not name string) so a rename like JioCinema + Disney+ Hotstar →
JioHotstar in Feb 2025 never breaks the filter. TMDB's `display_priorities` field sorts providers by
local relevance automatically.

### Genre mapping (TV caveat)

TMDB's TV genre list is a different, smaller set. Where no honest equivalent exists (Horror,
Romance, Thriller have no TV-side genre), the genre filter is simply not applied on the TV discover
call rather than silently mis-mapping. The Discover screen tells the user when this applies.

---

## 🛡️ Key storage

| What         | How                                                                          |
|--------------|------------------------------------------------------------------------------|
| Encryption   | `EncryptedSharedPreferences` (AES-256-GCM)                                   |
| Key material | Android Keystore (never leaves secure hardware)                              |
| Logging      | `NONE` in release builds — keys never appear in Logcat                       |
| Backup       | Excluded from cloud backup and device transfer (`data_extraction_rules.xml`) |
| Backend      | None. Keys go directly from the app to the AI provider's endpoint.           |

---

## 📦 Stack

| Layer         | Library                                                  |
|---------------|----------------------------------------------------------|
| UI            | Jetpack Compose + Material 3                             |
| State         | Kotlin StateFlow + `collectAsStateWithLifecycle`         |
| DI            | Hilt (multibinding for AI providers)                     |
| Networking    | Retrofit + OkHttp + kotlinx.serialization                |
| Image loading | Coil                                                     |
| Navigation    | Navigation Compose (type-safe routes)                    |
| Persistence   | DataStore (preferences) + EncryptedSharedPreferences     |
| Browser       | Custom Tabs (trailers, JustWatch links, key-setup pages) |
| Build         | Gradle version catalog (`libs.versions.toml`)            |

---

## 🗂️ Adding screenshots to this README

1. Take screenshots on your device (Power + Volume Down)
2. Transfer to your Mac (Finder → phone → `DCIM`)
3. Create a `screenshots/` folder at the project root
4. Name them: `discover.png`, `results.png`, `detail.png`, `settings.png`, `loading.png`,
   `ott_filter.png`
5. Commit and push:
   ```bash
   git add screenshots/
   git commit -m "Add app screenshots"
   git push
   ```
6. Replace `YOUR_USERNAME` in the screenshot URLs at the top of this file with your actual GitHub
   username

---

## 🔮 What's worth building next

| Feature                                      | Effort     | Notes                                                                                                                                                                                                                                                                    |
|----------------------------------------------|------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Room cache**                               | Low        | Dependency already in `build.gradle.kts`. Cache `MoodQuery → results` to avoid re-burning quota on repeat searches.                                                                                                                                                      |
| **Inline trailer player**                    | Medium     | Media3/ExoPlayer already a dependency. `HorizontalPager` over the backdrop image with an ExoPlayer surface would replace the Custom Tab.                                                                                                                                 |
| **Watchlist**                                | Medium     | Room table + `SavedStateHandle` for screen result + a bottom nav icon.                                                                                                                                                                                                   |
| **Streaming-service-only filter on AI path** | Low        | AI already returns titles; TMDB `enrich()` already filters by `providerId`. Just need to surface "nothing matched your platforms" more gracefully.                                                                                                                       |
| **Theme picker in Settings**                 | Low        | `Color.kt` already has Amber and Blue palettes. DataStore + a `when` in `MoodFlixTheme` is enough.                                                                                                                                                                       |
| **Tests**                                    | High value | `AiRouter` is the top priority — fake three `AiProviderClient`s, assert fallback order, assert network-abort behaviour. `PromptBuilder.parseSuggestions` needs table-driven tests with real malformed model output (markdown fences, nested objects, wrong field names). |

---

## 👤 Author

Built by **Arka Mazumder** — Android SWE, Google alumni.

Follow the build journey on Instagram: [@push__to__prod](https://www.instagram.com/push__to__prod)

---

## 📄 License

```
MIT License — do whatever you want, just don't hold me liable.
```