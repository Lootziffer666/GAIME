package engine

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class SceneEngine {
    var currentScene: Scene? by mutableStateOf(null)
        private set

    private val scenes = mutableMapOf<String, Scene>()

    fun register(scene: Scene) {
        scenes[scene.name] = scene
    }

    fun switchTo(sceneName: String) {
        currentScene = scenes[sceneName]
    }

    fun update(deltaTime: Float) {
        currentScene?.update(deltaTime)
    }

    fun registeredSceneNames(): List<String> = scenes.keys.toList()
}
