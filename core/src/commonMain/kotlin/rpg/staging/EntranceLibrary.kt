package rpg.staging

import rpg.CampaignBoss
import rpg.staging.EntranceBeatType.CLOSE_UP
import rpg.staging.EntranceBeatType.DEFLATE
import rpg.staging.EntranceBeatType.EFFECT_STORM
import rpg.staging.EntranceBeatType.PROCLAMATION
import rpg.staging.EntranceBeatType.REVEAL
import rpg.staging.EntranceBeatType.SHADOW_LOOM
import rpg.staging.EntranceBeatType.TITLE_CARD
import rpg.staging.StageEffect.BLOOM
import rpg.staging.StageEffect.CHOIR
import rpg.staging.StageEffect.CONFETTI
import rpg.staging.StageEffect.EMBERS
import rpg.staging.StageEffect.LIGHTNING
import rpg.staging.StageEffect.MARTIAL_DRUMS
import rpg.staging.StageEffect.QUAKE
import rpg.staging.StageEffect.SLOW_MOTION
import rpg.staging.StageEffect.SPARKLES
import rpg.staging.StageEffect.SPOTLIGHT
import rpg.staging.StageEffect.THUNDER
import rpg.staging.StageEffect.WIND

/**
 * Concrete dramatic entrances (docs/COMEDY_BIBLE.md). Sprite keys name the extra
 * art the renderer (KorGE) must supply (close-ups, reveals).
 */
object EntranceLibrary {

    /**
     * The signature missed-expectation gag: a single Sewer Rat staged as the
     * apocalypse. Maximum buildup, minimum threat.
     */
    fun theDreadShadow(): DramaticEntrance = DramaticEntrance(
        id = "dread_shadow",
        subjectName = "The Dread Shadow of the Deep",
        buildupIntensity = 10,
        actualThreat = 1,
        beats = listOf(
            EntranceBeat(SHADOW_LOOM, effects = listOf(StageEffect.FOG), intensity = 10, durationMs = 2500),
            EntranceBeat(EFFECT_STORM, effects = listOf(LIGHTNING, THUNDER, QUAKE, CHOIR, WIND), intensity = 10),
            EntranceBeat(TITLE_CARD, text = "THE DREAD SHADOW OF THE DEEP"),
            EntranceBeat(PROCLAMATION, text = "TREMBLE. I AM THE END OF ALL THINGS, GREAT AND SMALL."),
            EntranceBeat(REVEAL, spriteKey = "sewer_rat", durationMs = 1000),
            EntranceBeat(DEFLATE, text = "It's a rat.", speaker = "Nib")
        )
    )

    /** Earned, maximal pathos — then punctured. The final-boss myth machine. */
    fun administragon(): DramaticEntrance = DramaticEntrance(
        id = "administragon",
        subjectName = "The Administragon",
        buildupIntensity = 10,
        actualThreat = 9,
        beats = listOf(
            EntranceBeat(EFFECT_STORM, effects = listOf(BLOOM, EMBERS, CHOIR, SLOW_MOTION), intensity = 10, durationMs = 3000),
            EntranceBeat(CLOSE_UP, spriteKey = "administragon_eye", durationMs = 2000),
            EntranceBeat(
                PROCLAMATION,
                text = "I AM THE FLAG WHEN YOU ARE AFRAID. I AM THE MARKET WHEN YOU ARE HUNGRY. " +
                    "I AM THE PROPHET WHEN YOU ARE LOST. I AM THE QUEST WHEN YOU CANNOT CHOOSE.",
                durationMs = 4000
            ),
            EntranceBeat(REVEAL, spriteKey = "administragon"),
            EntranceBeat(DEFLATE, text = "So... no treasure?", speaker = "Nib")
        )
    )

    /** Bureaucratic menace: stamps and spectacles. */
    fun ratAccountant(): DramaticEntrance = DramaticEntrance(
        id = "rat_accountant",
        subjectName = "The Rat Accountant",
        buildupIntensity = 6,
        actualThreat = 5,
        beats = listOf(
            EntranceBeat(EFFECT_STORM, effects = listOf(SPOTLIGHT), intensity = 3, durationMs = 1200),
            EntranceBeat(CLOSE_UP, spriteKey = "rat_accountant_spectacles", durationMs = 1500),
            EntranceBeat(PROCLAMATION, text = "Hee hee hee. Let us review your forms. Your heroism is... DENIED."),
            EntranceBeat(REVEAL, spriteKey = "rat_accountant"),
            EntranceBeat(DEFLATE, text = "He's a rat behind a desk made of garbage.", speaker = "Nib")
        )
    )

    /** A captain whose beard is folded plunder applications. */
    fun captainFormbeard(): DramaticEntrance = DramaticEntrance(
        id = "captain_formbeard",
        subjectName = "Captain Formbeard",
        buildupIntensity = 7,
        actualThreat = 6,
        beats = listOf(
            EntranceBeat(EFFECT_STORM, effects = listOf(WIND, THUNDER, SPOTLIGHT), intensity = 6),
            EntranceBeat(CLOSE_UP, spriteKey = "formbeard_beard", durationMs = 1800),
            EntranceBeat(PROCLAMATION, text = "PRESENT YOUR MANIFEST. THIS PLUNDER IS ILLEGAL, IMMORAL, AND FULLY DOCUMENTED."),
            EntranceBeat(REVEAL, spriteKey = "captain_formbeard"),
            EntranceBeat(DEFLATE, text = "Avenge me. (He means his form.)", speaker = "Nib")
        )
    )

    /** Far too celebratory for a tree that just won't stop helping. */
    fun helpfulTree(): DramaticEntrance = DramaticEntrance(
        id = "helpful_tree",
        subjectName = "The Helpful Tree",
        buildupIntensity = 9,
        actualThreat = 4,
        beats = listOf(
            EntranceBeat(EFFECT_STORM, effects = listOf(CONFETTI, SPARKLES, CHOIR, BLOOM), intensity = 9, durationMs = 2500),
            EntranceBeat(TITLE_CARD, text = "FANFARE FOR THE SEEKER"),
            EntranceBeat(PROCLAMATION, text = "WELCOME, SEEKER! YOUR PATH TO OPTIMAL EFFICIENCY BEGINS NOW!"),
            EntranceBeat(REVEAL, spriteKey = "helpful_tree"),
            EntranceBeat(DEFLATE, text = "It's a tree. It has opinions.", speaker = "Vellum")
        )
    )

    /** Maximum martial buildup for a captain who then cannot legally move. */
    fun guardCaptain(): DramaticEntrance = DramaticEntrance(
        id = "guard_captain",
        subjectName = "The Guard Captain Who Cannot Legally Move",
        buildupIntensity = 8,
        actualThreat = 3,
        beats = listOf(
            EntranceBeat(SHADOW_LOOM, intensity = 8, durationMs = 2000),
            EntranceBeat(EFFECT_STORM, effects = listOf(MARTIAL_DRUMS, SPOTLIGHT, QUAKE), intensity = 8),
            EntranceBeat(TITLE_CARD, text = "THE WARDEN'S MANDATE"),
            EntranceBeat(PROCLAMATION, text = "BY PROTOCOL NINE THIS PRECINCT IS MINE -- AND I HAVE PLACED MYSELF UNDER ARREST."),
            EntranceBeat(REVEAL, spriteKey = "guard_captain"),
            EntranceBeat(DEFLATE, text = "He cannot move.", speaker = "Brugg")
        )
    )

    /** The entrance to play before each boss fight. */
    fun forBoss(boss: CampaignBoss): DramaticEntrance = when (boss) {
        CampaignBoss.RAT_ACCOUNTANT -> ratAccountant()
        CampaignBoss.GUARD_CAPTAIN_CANNOT_MOVE -> guardCaptain()
        CampaignBoss.HELPFUL_TREE -> helpfulTree()
        CampaignBoss.CAPTAIN_FORMBEARD -> captainFormbeard()
        CampaignBoss.ADMINISTRAGON -> administragon()
    }

    /** Every authored entrance (bosses + the stand-alone gag). */
    fun all(): List<DramaticEntrance> =
        CampaignBoss.entries.map { forBoss(it) } + theDreadShadow()
}
