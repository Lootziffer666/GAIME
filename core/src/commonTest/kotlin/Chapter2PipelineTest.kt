import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import rpg.BarkOutcome
import rpg.SliceDirector
import rpg.bark.BarkEvent
import rpg.combat.BossPhase
import rpg.combat.CombatAction
import rpg.combat.CombatEngine
import rpg.combat.CombatEvent
import rpg.combat.CombatResult
import rpg.combat.Combatant
import rpg.combat.EnemyArchetype
import rpg.combat.Side
import rpg.combat.TaxCollectorController
import rpg.questbook.QuestbookEffect
import rpg.questbook.RoomContext

/**
 * Integration tests for the Chapter 2 pipeline flow through the SliceDirector.
 * Verifies the traceable Bark -> Questbook -> Effect path for Chapter 2 content.
 */
class Chapter2PipelineTest {

    private var now = 0L

    private fun market() =
        RoomContext("stokeport_market", RoomContext.ROOM_MARKET, hasInteractableTarget = true)

    private fun forest() =
        RoomContext("forest_trail", RoomContext.ROOM_FOREST, hasEnemies = true, hasPuzzleElement = true)

    private fun shrine() =
        RoomContext("forest_trail", RoomContext.ROOM_FOREST_SHRINE, hasPuzzleElement = true)

    private fun forestBoss() =
        RoomContext("forest_trail", RoomContext.ROOM_FOREST_BOSS)

    @Test
    fun marketContextNibSmellGoldProducesQuestMarker() {
        now = 0L
        val director = SliceDirector { now }
        director.enterRoom(market())

        val result = director.fireBark(BarkEvent.NIB_SMELL_GOLD)
        assertTrue(result is BarkOutcome.Fired)
        val reaction = (result as BarkOutcome.Fired).reaction

        assertEquals(BarkEvent.NIB_SMELL_GOLD, reaction.bark)
        assertTrue(reaction.questbookText.contains("Commercial Survey Initiated"))
        assertTrue(reaction.effect is QuestbookEffect.SpawnQuestMarker)
        assertTrue(director.questMarkers.isNotEmpty())
    }

    @Test
    fun forestContextVellumCreaturesInWoodsProducesQuestMarker() {
        now = 0L
        val director = SliceDirector { now }
        director.enterRoom(forest())

        val result = director.fireBark(BarkEvent.VELLUM_CREATURES_IN_WOODS)
        assertTrue(result is BarkOutcome.Fired)
        val reaction = (result as BarkOutcome.Fired).reaction

        assertEquals(BarkEvent.VELLUM_CREATURES_IN_WOODS, reaction.bark)
        assertTrue(reaction.questbookText.contains("Wildlife Census Ordered"))
        assertTrue(reaction.effect is QuestbookEffect.SpawnQuestMarker)
        assertTrue(director.questMarkers.isNotEmpty())
    }

    @Test
    fun shrineContextVellumCallsForLightningRevealsHidden() {
        now = 0L
        val director = SliceDirector { now }
        director.enterRoom(shrine())

        val result = director.fireBark(BarkEvent.VELLUM_CALLS_FOR_LIGHTNING)
        assertTrue(result is BarkOutcome.Fired)
        val reaction = (result as BarkOutcome.Fired).reaction

        assertEquals(BarkEvent.VELLUM_CALLS_FOR_LIGHTNING, reaction.bark)
        assertTrue(reaction.questbookText.contains("Electrical Works Permit"))
        assertTrue(reaction.effect is QuestbookEffect.RevealHidden)
    }

    @Test
    fun bossCombatEventsRouteBarksThoughQuestbook() {
        now = 0L
        val director = SliceDirector { now }
        director.enterRoom(forestBoss())

        // Use low attack power (total 12) so the boss goes through all phases:
        // Boss 80 HP: 80->68->56->44->32->20->8->dead
        // At 32 HP (0.4): PHASE_2. At 20 HP (0.25): PHASE_3.
        val party = listOf(
            Combatant("nib", "Nib", maxHp = 200, side = Side.PLAYER, attackPower = 4),
            Combatant("brugg", "Brugg", maxHp = 200, side = Side.PLAYER, attackPower = 4),
            Combatant("vellum", "Vellum", maxHp = 200, side = Side.PLAYER, attackPower = 4)
        )
        val boss = EnemyArchetype.TAX_COLLECTOR_BADGER.spawn("tax_badger")
        val controller = TaxCollectorController()
        val engine = CombatEngine(party, emptyList(), boss, controller)
        director.startCombat(engine)

        // Kill wolf adds first (18 HP each, party deals 12/tick -> 2 ticks each)
        val allEvents = mutableListOf<CombatEvent>()
        val wolves = engine.livingEnemies().filter { it.id.startsWith("add_wolf") }
        for (wolf in wolves) {
            while (wolf.isAlive && engine.result == CombatResult.ONGOING) {
                now += 5000
                val turn = director.combatAction(CombatAction.Attack(wolf.id))
                allEvents += turn.events
            }
        }

        // Now fight the boss through all phases
        while (engine.result == CombatResult.ONGOING) {
            now += 5000
            val turn = director.combatAction(CombatAction.Attack("tax_badger"))
            allEvents += turn.events
        }

        assertEquals(CombatResult.VICTORY, engine.result)
        // Boss phase changes should have been emitted
        assertTrue(allEvents.any { it is CombatEvent.BossPhaseChanged && it.phase == BossPhase.PHASE_2 })
        assertTrue(allEvents.any { it is CombatEvent.BossPhaseChanged && it.phase == BossPhase.PHASE_3 })
        // VELLUM_BALANCE_LIFE_DEATH bark should have been triggered and routed through questbook
        assertTrue(allEvents.any { it is CombatEvent.BarkTriggered && it.bark == BarkEvent.VELLUM_BALANCE_LIFE_DEATH })
        // Verify the questbook log has the VELLUM_BALANCE_LIFE_DEATH entry
        assertNotNull(
            director.questbook.log.firstOrNull { it.bark == BarkEvent.VELLUM_BALANCE_LIFE_DEATH },
            "VELLUM_BALANCE_LIFE_DEATH should be in the questbook log"
        )
    }

    @Test
    fun mapTransitionResetsMarkersWhenEnteringForestFromMarket() {
        now = 0L
        val director = SliceDirector { now }
        director.enterRoom(market())

        // Fire a bark that creates a quest marker
        director.fireBark(BarkEvent.NIB_SMELL_GOLD)
        assertTrue(director.questMarkers.isNotEmpty())

        // Transition to forest (different mapId) resets markers
        director.enterRoom(forest())
        assertTrue(director.questMarkers.isEmpty(), "Quest markers should reset on map transition")
    }

    @Test
    fun shrineToForestDoesNotResetMarkersSameMap() {
        now = 0L
        val director = SliceDirector { now }
        director.enterRoom(forest())

        director.fireBark(BarkEvent.VELLUM_CREATURES_IN_WOODS)
        assertTrue(director.questMarkers.isNotEmpty())

        // Moving within same map (forest_trail) should not reset markers
        director.enterRoom(shrine())
        assertTrue(director.questMarkers.isNotEmpty(), "Same-map room change should preserve markers")
    }
}
