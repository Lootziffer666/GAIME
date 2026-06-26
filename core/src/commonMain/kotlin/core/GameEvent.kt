package core

sealed class GameEvent {
    data object AiStartedThinking : GameEvent()
    data object AiFinishedThinking : GameEvent()
    data object GameStarted : GameEvent()
    data object GameOver : GameEvent()
    data object PlayerClosedGame : GameEvent()
    data object Reset : GameEvent()
}
