package rpg.bark

/** Mechanical classification of a bark (docs/BARK_TRIGGER_TABLE.md). */
enum class BarkType {
    /** Atmosphere only, no mechanical effect. */
    SAFE_BARK,

    /** Triggers a clear, deterministic effect. */
    TRIGGER_BARK,

    /** Increases Quest Pressure. */
    PRESSURE_BARK,

    /** Consciously used by the player for puzzle/progress. */
    UTILITY_BARK,

    /** Helps but creates risk (pressure increase + effect). */
    DANGER_BARK
}
