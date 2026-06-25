package signals

import core.GameEvent
import kotlinx.coroutines.flow.Flow

interface AiSignalSource {
    val events: Flow<GameEvent>
}
