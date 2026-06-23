package engine

import androidx.compose.ui.graphics.drawscope.DrawScope

interface Scene {
    val name: String
    fun update(deltaTime: Float)
    fun draw(drawScope: DrawScope)
    fun onPointerMove(x: Float, y: Float)
    fun onPointerExit()
}
