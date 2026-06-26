package signals

import core.GameEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class ManualSignalSource : AiSignalSource {
    private val _events = MutableSharedFlow<GameEvent>(extraBufferCapacity = 10)
    override val events: Flow<GameEvent> = _events.asSharedFlow()

    fun startThinking() {
        _events.tryEmit(GameEvent.AiStartedThinking)
    }

    fun finishThinking() {
        _events.tryEmit(GameEvent.AiFinishedThinking)
    }

    fun gameStarted() {
        _events.tryEmit(GameEvent.GameStarted)
    }

    fun gameOver() {
        _events.tryEmit(GameEvent.GameOver)
    }

    fun playerClosedGame() {
        _events.tryEmit(GameEvent.PlayerClosedGame)
    }

    fun reset() {
        _events.tryEmit(GameEvent.Reset)
    }
}
