package rpg

/**
 * The canonical campaign structure: Prologue + 5 Chapters + Finale
 * (docs/CAMPAIGN.md). The vertical slice (docs/VERTICAL_SLICE.md) is the
 * buildable proof of [PROLOGUE] + [CH1_SEWERS]; the remaining chapters are
 * built out on top of the same proven pipeline.
 *
 * Each entry carries the metadata needed to drive map loading, the chapter
 * select / progression UI, and the page-collection payoff. Pressure, cooldowns
 * and markers always reset on the map transition between chapters
 * (docs/GAME_CONCEPT_LOCK.md).
 */
enum class Chapter(
    /** Stable order in the campaign (0 = Prologue ... 6 = Finale). */
    val order: Int,
    /** Player-facing chapter title. */
    val title: String,
    /** The map this chapter primarily takes place on. */
    val mapId: String,
    /** The chapter boss, or `null` for chapters without a combat boss. */
    val boss: CampaignBoss?,
    /** The Questbook page recovered at the end of the chapter, or `null`. */
    val pageReward: QuestbookPage?
) {
    PROLOGUE(
        order = 0,
        title = "The Four-Armed Bartender",
        mapId = "tavern",
        boss = null,
        pageReward = null
    ),
    CH1_SEWERS(
        order = 1,
        title = "The Sewers of Bad Decisions",
        mapId = "sewer",
        boss = CampaignBoss.RAT_ACCOUNTANT,
        pageReward = QuestbookPage.PAGE_OF_BEGINNINGS
    ),
    CH2_TOWN_GUARD(
        order = 2,
        title = "The Town Guard That Arrested Itself",
        mapId = "town_centre",
        boss = null,
        pageReward = QuestbookPage.PAGE_OF_TERMS_AND_CONDITIONS
    ),
    CH3_WOODS(
        order = 3,
        title = "The Woods That Had Opinions",
        mapId = "woods",
        boss = CampaignBoss.HELPFUL_TREE,
        pageReward = QuestbookPage.PAGE_OF_DIRECTIONS
    ),
    CH4_SHIP(
        order = 4,
        title = "The Ship That Was Technically Seaworthy",
        mapId = "harbour",
        boss = CampaignBoss.CAPTAIN_FORMBEARD,
        pageReward = QuestbookPage.PAGE_OF_CLAIMS_AND_REWARDS
    ),
    CH5_DRAGON(
        order = 5,
        title = "The Dragon That Was Accidentally Summoned",
        mapId = "island_cave",
        boss = CampaignBoss.ADMINISTRAGON,
        pageReward = null
    ),
    FINALE(
        order = 6,
        title = "Done Enough",
        mapId = "questbook",
        boss = null,
        pageReward = null
    );

    /** The next chapter in campaign order, or `null` after the finale. */
    fun next(): Chapter? = entries.firstOrNull { it.order == order + 1 }

    companion object {
        /** Chapters that gate progression behind a boss defeat. */
        fun withBoss(): List<Chapter> = entries.filter { it.boss != null }

        /** Lookup by stable campaign order. */
        fun ofOrder(order: Int): Chapter? = entries.firstOrNull { it.order == order }
    }
}

/** The campaign bosses (docs/CAMPAIGN.md). */
enum class CampaignBoss(val displayName: String, val chapter: Int) {
    RAT_ACCOUNTANT("The Rat Accountant", chapter = 1),
    HELPFUL_TREE("The Helpful Tree", chapter = 3),
    CAPTAIN_FORMBEARD("Captain Formbeard", chapter = 4),
    ADMINISTRAGON("The Administragon", chapter = 5)
}

/** The recoverable Questbook pages, one per page-bearing chapter. */
enum class QuestbookPage(val displayTitle: String) {
    PAGE_OF_BEGINNINGS("The Page of Beginnings"),
    PAGE_OF_TERMS_AND_CONDITIONS("The Page of Terms and Conditions"),
    PAGE_OF_DIRECTIONS("The Page of Directions"),
    PAGE_OF_CLAIMS_AND_REWARDS("The Page of Claims and Rewards")
}
