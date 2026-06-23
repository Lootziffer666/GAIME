package core

enum class GameSessionState {
    Idle,
    Thinking,
    ReadyButPlaying,
    RevealReady,
    Closed,
    Failed
}
