import core.GameEvent
import core.GameSessionState
import core.GameStateMachine
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GameStateMachineTest {

    @Test
    fun initialStateIsIdle() {
        val sm = GameStateMachine()
        assertEquals(GameSessionState.Idle, sm.currentState)
        assertFalse(sm.kiIsReady)
    }

    @Test
    fun idleTransitionsToThinkingOnAiStartedThinking() {
        val sm = GameStateMachine()
        sm.transition(GameEvent.AiStartedThinking)
        assertEquals(GameSessionState.Thinking, sm.currentState)
    }

    @Test
    fun thinkingTransitionsToReadyButPlayingOnAiFinishedThinking() {
        val sm = GameStateMachine()
        sm.transition(GameEvent.AiStartedThinking)
        sm.transition(GameEvent.AiFinishedThinking)
        assertEquals(GameSessionState.ReadyButPlaying, sm.currentState)
        assertTrue(sm.kiIsReady)
    }

    @Test
    fun readyButPlayingTransitionsToRevealReadyOnGameOver() {
        val sm = GameStateMachine()
        sm.transition(GameEvent.AiStartedThinking)
        sm.transition(GameEvent.AiFinishedThinking)
        sm.transition(GameEvent.GameOver)
        assertEquals(GameSessionState.RevealReady, sm.currentState)
    }

    @Test
    fun gameOverDuringThinkingDoesNotReveal() {
        val sm = GameStateMachine()
        sm.transition(GameEvent.AiStartedThinking)
        assertEquals(GameSessionState.Thinking, sm.currentState)
        sm.transition(GameEvent.GameOver)
        assertEquals(GameSessionState.Thinking, sm.currentState)
        assertFalse(sm.kiIsReady)
    }

    @Test
    fun playerClosedGameTransitionsToClosed() {
        val sm = GameStateMachine()
        sm.transition(GameEvent.AiStartedThinking)
        sm.transition(GameEvent.AiFinishedThinking)
        sm.transition(GameEvent.GameOver)
        assertEquals(GameSessionState.RevealReady, sm.currentState)
        sm.transition(GameEvent.PlayerClosedGame)
        assertEquals(GameSessionState.Closed, sm.currentState)
    }

    @Test
    fun resetFromAnyStateReturnsToIdle() {
        val sm = GameStateMachine()
        sm.transition(GameEvent.AiStartedThinking)
        sm.transition(GameEvent.AiFinishedThinking)
        assertTrue(sm.kiIsReady)
        assertEquals(GameSessionState.ReadyButPlaying, sm.currentState)

        sm.transition(GameEvent.Reset)
        assertEquals(GameSessionState.Idle, sm.currentState)
        assertFalse(sm.kiIsReady)
    }

    @Test
    fun fullLifecycleTransition() {
        val sm = GameStateMachine()
        assertEquals(GameSessionState.Idle, sm.currentState)

        sm.transition(GameEvent.AiStartedThinking)
        assertEquals(GameSessionState.Thinking, sm.currentState)

        sm.transition(GameEvent.AiFinishedThinking)
        assertEquals(GameSessionState.ReadyButPlaying, sm.currentState)
        assertTrue(sm.kiIsReady)

        sm.transition(GameEvent.GameOver)
        assertEquals(GameSessionState.RevealReady, sm.currentState)

        sm.transition(GameEvent.PlayerClosedGame)
        assertEquals(GameSessionState.Closed, sm.currentState)

        sm.transition(GameEvent.Reset)
        assertEquals(GameSessionState.Idle, sm.currentState)
        assertFalse(sm.kiIsReady)
    }
}
