package core

class GameStateMachine {
    var currentState: GameSessionState = GameSessionState.Idle
        private set

    var kiIsReady: Boolean = false
        private set

    fun transition(event: GameEvent) {
        when (event) {
            is GameEvent.AiStartedThinking -> {
                if (currentState == GameSessionState.Idle) {
                    currentState = GameSessionState.Thinking
                }
            }

            is GameEvent.AiFinishedThinking -> {
                if (currentState == GameSessionState.Thinking) {
                    kiIsReady = true
                    currentState = GameSessionState.ReadyButPlaying
                }
            }

            is GameEvent.GameStarted -> {
                // No state change needed - game is already in progress
            }

            is GameEvent.GameOver -> {
                if (currentState == GameSessionState.ReadyButPlaying) {
                    currentState = GameSessionState.RevealReady
                }
                // GameOver in Thinking does NOT reveal - stays in Thinking
            }

            is GameEvent.PlayerClosedGame -> {
                currentState = GameSessionState.Closed
            }

            is GameEvent.Reset -> {
                currentState = GameSessionState.Idle
                kiIsReady = false
            }
        }
    }
}
