package com.arka.moodflix.domain.model

/**
 * The mood options shown as chips on the discover screen.
 * [promptDescriptor] is what actually gets sent to the model - it is more
 * descriptive than the label so the LLM has something to reason about.
 */
enum class Mood(val label: String, val emojiFreeIcon: String, val promptDescriptor: String) {
    COZY("Cozy", "home", "warm, comforting, low-stakes; something to watch under a blanket"),
    HYPED("Hyped", "bolt", "high energy, adrenaline, big set pieces, gets the blood pumping"),
    MELANCHOLY("Melancholy", "cloud", "quiet, sad, reflective; sits with grief rather than fixing it"),
    MIND_BENDING("Mind-bending", "help", "complex, twisty, ambiguous; rewards paying close attention"),
    ROMANTIC("Romantic", "heart", "tender, longing, chemistry-driven"),
    FUNNY("Need a laugh", "mood-happy", "genuinely funny, light, rewatchable comedy"),
    TENSE("Tense", "alert", "suspenseful, dread-building, edge of the seat"),
    INSPIRED("Want to feel inspired", "flame", "uplifting, ambitious, leaves you wanting to do something");

    companion object {
        fun fromLabelOrNull(label: String) = entries.firstOrNull { it.label.equals(label, true) }
    }
}
