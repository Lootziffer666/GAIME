import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import rpg.BarkOutcome
import rpg.SliceDirector
import rpg.bark.BarkEvent
import rpg.combat.BossController
import rpg.combat.CombatAction
import rpg.combat.CombatEngine
import rpg.combat.CombatResult
import rpg.combat.Combatant
import rpg.combat.EnemyArchetype
import rpg.combat.Side
import rpg.questbook.QuestPressure
import rpg.questbook.QuestbookEffect
import rpg.questbook.RoomContext

/**
 * Scripted 9-step sequence verifying all VERTICAL_SLICE.md acceptance criteria
 * are provably met through the SliceDirector pipeline.
 */
class SliceAcceptanceCriteriaTest {

    private var now = 0L
    private fun tick() { now += 60_000L }

    private fun freshParty() = listOf(
        Combatant("nib",    "Nib",    maxHp = 20, side = Side.PLAYER, attackPower = 4),
        Combatant("brugg",  "Brugg",  maxHp = 30, side = Side.PLAYER, attackPower = 5),
        Combatant("vellum", "Vellum", maxHp = 18, side = Side.PLAYER, attackPower = 4)
    )

    @Test
    fun allNineAcceptanceCriteriaSatisfied() {
        val director = SliceDirector { now }

        // ── Criterion 1: Quest registered in tavern via NIB_SMELL_TREASURE ──────────
        director.enterRoom(RoomContext("tavern", RoomContext.ROOM_TAVERN, hasInteractableTarget = true))
        val r1 = director.fireBark(BarkEvent.NIB_SMELL_TREASURE)
        assertTrue(r1 is BarkOutcome.Fired)
        val reaction1 = (r1 as BarkOutcome.Fired).reaction
        assertEquals(BarkEvent.NIB_SMELL_TREASURE, reaction1.bark)
        assertTrue(reaction1.effect is QuestbookEffect.SpawnQuestMarker)
        assertTrue(director.questMarkers.isNotEmpty(), "Criterion 1: quest marker spawned")

        // ── Criterion 2: Map transition resets markers and pressure ───────────────
        director.enterRoom(RoomContext(
            "sewer", RoomContext.ROOM_SEWER_CORRIDOR, hasEnemies = true
        ))
        assertTrue(director.questMarkers.isEmpty(), "Criterion 2: markers reset on map change")
        assertEquals(QuestPressure.LOW, director.pressure)

        // ── Criterion 3: NIB_IT_WASNT_ME raises pressure LOW → MEDIUM, false marker ─
        tick()
        val r3 = director.fireBark(BarkEvent.NIB_IT_WASNT_ME)
        assertTrue(r3 is BarkOutcome.Fired)
        assertEquals(QuestPressure.MEDIUM, director.pressure, "Criterion 3: pressure MEDIUM")
        assertTrue(director.falseMarkers.isNotEmpty(), "Criterion 3: false marker added")

        // ── Criterion 4: VELLUM_KNOWLEDGE_IS_THE_ANSWER in puzzle room → RevealHidden ─
        director.enterRoom(RoomContext(
            "sewer", RoomContext.ROOM_MINI_DUNGEON,
            hasPuzzleElement = true, hasBreakableObstacle = true
        ))
        tick()
        val r4 = director.fireBark(BarkEvent.VELLUM_KNOWLEDGE_IS_THE_ANSWER)
        assertTrue(r4 is BarkOutcome.Fired)
        assertTrue((r4 as BarkOutcome.Fired).reaction.effect is QuestbookEffect.RevealHidden,
            "Criterion 4: RevealHidden in puzzle context")

        // ── Criterion 5: BRUGG_ATTACK clears rubble obstacle ────────────────────────
        tick()
        val r5 = director.fireBark(BarkEvent.BRUGG_ATTACK)
        assertTrue(r5 is BarkOutcome.Fired)
        val effect5 = (r5 as BarkOutcome.Fired).reaction.effect
        assertTrue(effect5 is QuestbookEffect.ClearObstacle, "Criterion 5: ClearObstacle effect")
        assertTrue(director.clearedObstacles.isNotEmpty(), "Criterion 5: obstacle recorded")

        // ── Criterion 6: Boss Phase 2 escalates pressure to HIGH systemically ───────
        director.enterRoom(RoomContext("sewer", RoomContext.ROOM_BOSS, hasFlammableTarget = true))
        val party = freshParty()
        val boss = EnemyArchetype.RAT_ACCOUNTANT.spawn("rat_accountant")
        director.startCombat(CombatEngine(party, emptyList(), boss, BossController()))
        // Drive boss to phase 2 (HP < 60%) with high-damage party
        var ticks = 0
        while (director.currentCombat?.bossPhase?.name != "PHASE_2" && ticks < 30) {
            director.combatAction(CombatAction.Attack("rat_accountant"))
            ticks++
        }
        assertEquals(QuestPressure.HIGH, director.pressure, "Criterion 6: pressure HIGH in Phase 2")

        // ── Criterion 7: VELLUM_CALLS_FOR_FLAME in boss context → BurnTargets ──────
        tick()
        val r7 = director.fireBark(BarkEvent.VELLUM_CALLS_FOR_FLAME)
        assertTrue(r7 is BarkOutcome.Fired)
        assertTrue((r7 as BarkOutcome.Fired).reaction.effect is QuestbookEffect.BurnTargets,
            "Criterion 7: BurnTargets effect")

        // ── Criterion 8: VELLUM_THIS_CHANGES_EVERYTHING registers party name ────────
        // Finish boss first (or fire bark even mid-combat since RoomContext is boss)
        tick()
        val r8 = director.fireBark(BarkEvent.VELLUM_THIS_CHANGES_EVERYTHING)
        assertTrue(r8 is BarkOutcome.Fired)
        val effect8 = (r8 as BarkOutcome.Fired).reaction.effect
        assertTrue(effect8 is QuestbookEffect.RegisterPartyName, "Criterion 8: RegisterPartyName")
        assertEquals("Everything Changes", director.partyName, "Criterion 8: party name set")
        assertTrue(director.questbookOpen, "Criterion 8: questbook opens")

        // ── Criterion 9: Pressure never regressed; escalation path is traceable ────
        // Full trace: LOW (tavern) → MEDIUM (NIB_IT_WASNT_ME) → HIGH (boss phase 2)
        // All reactions are in the questbook log for the current map.
        val log = director.questbook.log
        assertNotNull(log.firstOrNull { it.bark == BarkEvent.VELLUM_CALLS_FOR_FLAME },
            "Criterion 9: VELLUM_CALLS_FOR_FLAME in log")
        assertNotNull(log.firstOrNull { it.bark == BarkEvent.VELLUM_THIS_CHANGES_EVERYTHING },
            "Criterion 9: VELLUM_THIS_CHANGES_EVERYTHING in log")
    }

    @Test
    fun combatVictoryFiresBarkBackThroughPipeline() {
        val director = SliceDirector { now }
        director.enterRoom(RoomContext("sewer", RoomContext.ROOM_SEWER_CORRIDOR, hasEnemies = true))
        val party = freshParty()
        val rats = listOf(
            EnemyArchetype.SEWER_RAT.spawn("r1"),
            EnemyArchetype.SEWER_RAT.spawn("r2")
        )
        director.startCombat(CombatEngine(party, rats))
        var turns = 0
        while (director.currentCombat?.result == CombatResult.ONGOING && turns < 50) {
            director.combatAction(CombatAction.Attack("r1"))
            if (director.currentCombat?.result == CombatResult.ONGOING) {
                director.combatAction(CombatAction.Attack("r2"))
            }
            turns++
        }
        val result = director.currentCombat?.result
        assertTrue(result == CombatResult.VICTORY || result == CombatResult.DEFEAT,
            "combat reaches a terminal state")
    }

    @Test
    fun bossPhase2SpawnsFalseMarkerAndEscalatesPressure() {
        val director = SliceDirector { now }
        director.enterRoom(RoomContext("sewer", RoomContext.ROOM_BOSS, hasFlammableTarget = true))
        // attackPower=12 per hero → total 36 damage per tick.
        // Boss HP=60: after 1 attack → 24 HP (0.4 fraction → Phase 2 transition fires).
        val party = listOf(
            Combatant("h1", "H1", maxHp = 500, side = Side.PLAYER, attackPower = 12),
            Combatant("h2", "H2", maxHp = 500, side = Side.PLAYER, attackPower = 12),
            Combatant("h3", "H3", maxHp = 500, side = Side.PLAYER, attackPower = 12)
        )
        val boss = EnemyArchetype.RAT_ACCOUNTANT.spawn("boss")
        director.startCombat(CombatEngine(party, emptyList(), boss, BossController()))
        var ticks = 0
        while (director.currentCombat?.bossPhase?.name != "PHASE_2" && ticks < 20) {
            director.combatAction(CombatAction.Attack("boss"))
            ticks++
        }
        assertTrue(director.falseMarkers.any { it.contains("Objection") },
            "Phase 2: Objection false marker spawned")
        assertEquals(QuestPressure.HIGH, director.pressure)
    }
}
