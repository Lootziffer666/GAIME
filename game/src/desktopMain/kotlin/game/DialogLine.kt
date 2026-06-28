package game

/**
 * One line of in-world dialogue.
 *
 * [speaker] — displayed bold above the text; empty string = narrator line.
 * [text]    — the dialogue text (word-wrapped manually with \n, max ~60 chars/line).
 */
data class DialogLine(
    val speaker: String,
    val text: String,
)
