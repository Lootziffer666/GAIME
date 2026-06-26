package rpg.bark

/**
 * Static metadata for a single bark key, mirroring one row of the Bark Table
 * in docs/BARK_TRIGGER_TABLE.md.
 */
data class BarkDefinition(
    val key: BarkEvent,
    val character: PartyCharacter,
    /** The line the character speaks (atmosphere; not parsed by the Questbook). */
    val audioText: String,
    val type: BarkType,
    val scope: BarkScope,
    val cooldownSeconds: Int,
    /** Whether the vertical slice exercises this bark. */
    val usedInSlice: Boolean
)
