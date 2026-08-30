package com.arka.moodflix.core

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.arka.moodflix.R

/**
 * [AppError] carries no message of its own (see its kdoc) - this is the one
 * place that maps every case to a user-facing, localized string. Provider
 * names (Gemini/ChatGPT/Claude) are brand names and stay untranslated.
 */
@Composable
fun AppError.localizedMessage(): String = when (this) {
    AppError.Network -> stringResource(R.string.error_network)
    is AppError.QuotaExceeded -> stringResource(R.string.error_quota_exceeded, provider)
    is AppError.InvalidKey -> stringResource(R.string.error_invalid_key, provider)
    AppError.NoKeysConfigured -> stringResource(R.string.error_no_keys_configured)
    AppError.ParseFailed -> stringResource(R.string.error_parse_failed)
    is AppError.ProviderError -> stringResource(R.string.error_provider_http, provider, statusCode)
    AppError.NoMatches -> stringResource(R.string.error_no_matches)
    AppError.TitleLoadFailed -> stringResource(R.string.error_title_load_failed)
    AppError.ProvidersLoadFailed -> stringResource(R.string.error_providers_load_failed)
    AppError.MonthlyTitlesLoadFailed -> stringResource(R.string.error_monthly_titles_load_failed)
    AppError.SearchFailed -> stringResource(R.string.error_search_failed)
    AppError.SimilarLoadFailed -> stringResource(R.string.error_similar_load_failed)
    AppError.Unknown -> stringResource(R.string.error_unknown)
}
