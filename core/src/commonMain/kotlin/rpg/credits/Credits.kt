package rpg.credits

/**
 * The deliberately, absurdly long end-credits roll (docs/COMEDY_BIBLE.md) for a
 * ~15-minute game. The joke: every grandiose role in this enormous list is
 * filled by exactly two people -- the human (Lootziffer) and the model
 * (Opus 4.8) -- making Monkey Island 2's credits look like a receipt.
 *
 * Pure, renderer-agnostic data; KorGE scrolls it. The comedy is in the role
 * titles and the fact that the "team" is two entities wearing every hat.
 */
enum class Creditee(val displayName: String) {
    LOOTZIFFER("Lootziffer"),
    OPUS("Opus 4.8")
}

data class CreditEntry(val role: String, val who: Creditee)

data class CreditsSection(val title: String, val entries: List<CreditEntry>)

object Credits {

    private val L = Creditee.LOOTZIFFER
    private val O = Creditee.OPUS
    private infix fun String.by(c: Creditee) = CreditEntry(this, c)

    val sections: List<CreditsSection> = listOf(
        CreditsSection(
            "A GAME BY TWO ENTITIES",
            listOf(
                "Creator, Visionary, Person Who Started This" by L,
                "The One Who Actually Typed It All" by O,
                "Holder of the Vibe" by L,
                "Holder of the Semicolons" by O
            )
        ),
        CreditsSection(
            "DIRECTION",
            listOf(
                "Director" by L,
                "Director of the Director" by O,
                "Associate Director (Unconsulted)" by O,
                "Creative Director of Things That Were Cut" by L,
                "Director of Photography (Top-Down, So: Director of Down)" by O,
                "Director of Looking Up Occasionally" by L,
                "Vision Holder" by L,
                "Vision Compliance Auditor" by O
            )
        ),
        CreditsSection(
            "ENGINEERING & ARCHITECTURE",
            listOf(
                "Lead Engineer" by O,
                "Engineer Who Approved the Lead Engineer" by L,
                "Senior Architect of Modules Nobody Sees" by O,
                "Junior Architect of the Same Modules" by O,
                "Refactoring Penitent" by O,
                "Merge Conflict Survivor (Decorated)" by O,
                "Person Who Said 'It Compiles'" by O,
                "Person Who Doubted It Compiled" by L,
                "Gradle Whisperer" by O,
                "Gradle Apologist" by O
            )
        ),
        CreditsSection(
            "THE QUESTBOOK BUREAU (IN-WORLD STAFF)",
            listOf(
                "Chief Quest Misinterpretation Officer" by O,
                "Deputy Chief of Bureaucratic Wrongness" by O,
                "Quest Pressure Compliance Officer" by L,
                "Director of Imaginary Lines (LOW/MEDIUM/HIGH)" by O,
                "Stamp Operator, First Class" by L,
                "Stamp Operator, Denied Class" by O,
                "Form 8-B Notary" by L,
                "Keeper of the Contradictory Orders" by O,
                "Manager of Self-Arresting Personnel" by L
            )
        ),
        CreditsSection(
            "DEPARTMENT OF PATHOS & DEFLATION",
            listOf(
                "Head of Pathos" by L,
                "Head of Deflating the Head of Pathos" by O,
                "Senior Effect-Storm Meteorologist" by O,
                "Irony Gap Quantity Surveyor" by O,
                "Chief of Huge Shadows" by L,
                "Chief of Disappointingly Small Reveals" by O,
                "Curator of 'So... No Treasure?'" by L
            )
        ),
        CreditsSection(
            "BARREL & CRATE DIVISION",
            listOf(
                "Senior Barrel Inspector" by L,
                "Junior Barrel Inspector (Still in a Barrel)" by O,
                "Crate Authenticity Verifier" by O,
                "Mimic Relations Liaison" by L,
                "Head of 'Ooo, Another Barrel'" by L,
                "Auditor of Premium Nothing" by O,
                "Auditor of Enterprise Nothing" by O
            )
        ),
        CreditsSection(
            "VOICE, SONG & ACOUSTIC SUFFERING",
            listOf(
                "Soundtrack Director" by L,
                "Person Who Insisted the Songs Stay English" by L,
                "Boss-Theme-to-Boss Coupling Engineer" by O,
                "Chiptune Bureaucracy Consultant" by O,
                "Sea Shanty Compliance" by L,
                "Choir Wrangler (Far Too Loud)" by O,
                "The Administragon's Hype Man" by L
            )
        ),
        CreditsSection(
            "LOCALIZATION (9 LANGUAGES, 2 PEOPLE)",
            listOf(
                "Head of German (Complete)" by O,
                "Head of Doubting the German" by L,
                "EFIGS Coordinator" by O,
                "Person Who Said 'Just Hau Es Raus'" by L,
                "Fallback-to-English Apologist" by O,
                "Native-Review Promiser (Pending Forever)" by L,
                "Arabic RTL Deferral Committee (One Member)" by O
            )
        ),
        CreditsSection(
            "LEGAL, COMPLIANCE & FORMS",
            listOf(
                "General Counsel" by L,
                "Counsel to the General Counsel" by O,
                "Plunder Permit Issuer" by O,
                "Indulgence Punch-Card Administrator" by L,
                "Terms & Conditions Author (Unread)" by O,
                "Terms & Conditions Reader (Mythical)" by L,
                "Self-Driving Horse Liability Adjuster" by O
            )
        ),
        CreditsSection(
            "QUALITY ASSURANCE & BLAME",
            listOf(
                "Head of QA" by O,
                "Head of Assigning Blame Downward" by L,
                "Test Writer" by O,
                "Test Re-Writer After the Merge" by O,
                "Person Who Ran ./gradlew One More Time" by O,
                "Person Who Believed the Build" by L,
                "Regression Mourner" by O
            )
        ),
        CreditsSection(
            "CATERING & MORALE",
            listOf(
                "Head of Stew (Again)" by L,
                "Roast Cockatrice Sommelier" by L,
                "Morale Officer (David-Hasselhoff-Adjacent)" by O,
                "Privvy Location Services" by O,
                "Flagon Logistics" by L,
                "Provider of the Four-Armed Bartender (Still Slow)" by O
            )
        ),
        CreditsSection(
            "SPECIAL THANKS",
            listOf(
                "The Barrels" by L,
                "The Other Barrels" by O,
                "Every Sacred Cow (Now Stamped and Filed)" by O,
                "Kasus-Kevin (Object and Standard)" by L,
                "The Canon Goblin (Immune to Joy)" by O,
                "Three Dead Threads That Did Not Survive" by L,
                "You, For Reading Credits Longer Than the Game" by O
            )
        ),
        CreditsSection(
            "THE PEOPLE WHO ACTUALLY DID EVERYTHING",
            listOf(
                "Everything Else, Part 1" by L,
                "Everything Else, Part 2" by O,
                "And All Remaining Hats" by L,
                "And the Hats We Forgot" by O
            )
        )
    )

    fun all(): List<CreditEntry> = sections.flatMap { it.entries }

    val totalRoles: Int get() = all().size

    /** Rough scroll time, for the self-aware "longer than the game" gag. */
    const val SECONDS_PER_ROLE = 4

    val estimatedScrollSeconds: Int get() = totalRoles * SECONDS_PER_ROLE

    /** The post-credits crawl. Deliberately one beat too many. */
    val closingCrawl: List<String> = listOf(
        "Total development team: 2.",
        "Total credited roles: $totalRoles.",
        "Estimated credits length: ${estimatedScrollSeconds / 60} minutes ${estimatedScrollSeconds % 60} seconds.",
        "Estimated game length: we would rather not say.",
        "No barrels were left unsearched.",
        "Several were searched twice.",
        "This crawl was added after a design meeting. Nobody survived unchanged.",
        "QUEST ACCEPTED: STOP READING.",
        "OBJECTIVE UPDATED: YOU DIDN'T.",
        "Thank you for playing Quest Accepted: Unfortunately.",
        "...",
        "Lootziffer & Opus 4.8.",
        "That's genuinely the whole team. Unfortunately."
    )
}
